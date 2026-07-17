package dev.tet.minemoji.slack;

import com.mojang.blaze3d.platform.NativeImage;
import dev.tet.minemoji.MinemojiClient;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class SlackEmojiTextureCache {
	private final SlackEmojiService slackEmojiService;
	private final HttpClient httpClient = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.build();
	private final Map<String, Identifier> textureIdsByUrl = new ConcurrentHashMap<>();
	private final Set<String> loadingUrls = ConcurrentHashMap.newKeySet();
	private final Set<String> failedUrls = ConcurrentHashMap.newKeySet();

	public SlackEmojiTextureCache(SlackEmojiService slackEmojiService) {
		this.slackEmojiService = slackEmojiService;
	}

	public Identifier getTexture(SlackEmoji emoji) {
		String imageUrl = this.slackEmojiService.imageUrlFor(emoji);
		if (imageUrl == null || imageUrl.isBlank()) {
			return null;
		}

		Identifier existing = this.textureIdsByUrl.get(imageUrl);
		if (existing != null) {
			return existing;
		}
		if (this.failedUrls.contains(imageUrl)) {
			return null;
		}
		if (this.loadingUrls.add(imageUrl)) {
			this.loadAsync(imageUrl);
		}
		return null;
	}

	public void clear() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft != null) {
			this.textureIdsByUrl.values().forEach(minecraft.getTextureManager()::release);
		}
		this.textureIdsByUrl.clear();
		this.loadingUrls.clear();
		this.failedUrls.clear();
	}

	private void loadAsync(String imageUrl) {
		CompletableFuture.supplyAsync(() -> this.downloadTexture(imageUrl))
			.whenComplete((nativeImage, throwable) -> {
				if (throwable != null || nativeImage == null) {
					this.loadingUrls.remove(imageUrl);
					this.failedUrls.add(imageUrl);
					if (throwable != null) {
						MinemojiClient.LOGGER.debug("Failed to load Slack emoji icon {}", imageUrl, throwable);
					}
					return;
				}

				Minecraft minecraft = Minecraft.getInstance();
				minecraft.execute(() -> this.registerTexture(imageUrl, nativeImage));
			});
	}

	private NativeImage downloadTexture(String imageUrl) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl))
			.timeout(Duration.ofSeconds(15))
			.GET()
			.build();

		try {
			HttpResponse<InputStream> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() != 200) {
				return null;
			}

			try (InputStream stream = response.body()) {
				return NativeImage.read(stream);
			}
		} catch (IOException | InterruptedException exception) {
			if (exception instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			return null;
		}
	}

	private void registerTexture(String imageUrl, NativeImage nativeImage) {
		try {
			Identifier textureId = Identifier.fromNamespaceAndPath(MinemojiClient.MOD_ID, "slack/" + digest(imageUrl));
			DynamicTexture texture = new DynamicTexture(() -> "Minemoji Slack emoji", nativeImage);
			Minecraft.getInstance().getTextureManager().register(textureId, texture);
			this.textureIdsByUrl.put(imageUrl, textureId);
		} finally {
			this.loadingUrls.remove(imageUrl);
		}
	}

	private static String digest(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder(hash.length * 2);
			for (byte hashByte : hash) {
				builder.append(Character.forDigit((hashByte >> 4) & 0xF, 16));
				builder.append(Character.forDigit(hashByte & 0xF, 16));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-1 is not available", exception);
		}
	}
}
