package dev.tet.minemoji.slack;

import com.mojang.blaze3d.platform.NativeImage;
import dev.tet.minemoji.MinemojiClient;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class SlackEmojiTextureCache {
	private final SlackEmojiService slackEmojiService;
	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	private final Map<String, AnimatedTexture> texturesByUrl = new ConcurrentHashMap<>();
	private final Set<String> loadingUrls = ConcurrentHashMap.newKeySet();
	private final Set<String> failedUrls = ConcurrentHashMap.newKeySet();

	/** Slack絵文字の画像テクスチャを管理する */
	public SlackEmojiTextureCache(SlackEmojiService slackEmojiService) {
		this.slackEmojiService = slackEmojiService;
	}

	/** 絵文字に対応する現在のアニメーションフレームを返す */
	public Identifier getTexture(SlackEmoji emoji) {
		String imageUrl = this.slackEmojiService.imageUrlFor(emoji);
		if (imageUrl == null || imageUrl.isBlank()) return null;
		AnimatedTexture texture = this.texturesByUrl.get(imageUrl);
		if (texture != null) return texture.current(System.nanoTime() / 1_000_000L);
		if (!this.failedUrls.contains(imageUrl) && this.loadingUrls.add(imageUrl)) this.loadAsync(imageUrl);
		return null;
	}

	/** 登録済みのSlack絵文字テクスチャを解放する */
	public void clear() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft != null) this.texturesByUrl.values().forEach(texture -> texture.release(minecraft));
		this.texturesByUrl.clear();
		this.loadingUrls.clear();
		this.failedUrls.clear();
	}

	/** 画像をバックグラウンドで取得してクライアントスレッドへ渡す */
	private void loadAsync(String imageUrl) {
		CompletableFuture.supplyAsync(() -> this.downloadTexture(imageUrl)).whenComplete((decoded, throwable) -> {
			if (throwable != null || decoded == null || decoded.frames().isEmpty()) {
				this.loadingUrls.remove(imageUrl);
				this.failedUrls.add(imageUrl);
				if (throwable != null) MinemojiClient.LOGGER.debug("Failed to load Slack emoji icon {}", imageUrl, throwable);
				return;
			}
			Minecraft.getInstance().execute(() -> this.registerTexture(imageUrl, decoded));
		});
	}

	/** GIF、WebPを含む画像をフレーム単位でデコードする */
	private DecodedTexture downloadTexture(String imageUrl) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl)).timeout(Duration.ofSeconds(15)).GET().build();
		try {
			HttpResponse<byte[]> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() == 404 && imageUrl.startsWith("https://cdnjs.cloudflare.com/")) {
				String fallbackUrl = imageUrl.substring(0, imageUrl.length() - 4) + "-fe0f.png";
				request = HttpRequest.newBuilder(URI.create(fallbackUrl)).timeout(Duration.ofSeconds(15)).GET().build();
				response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			}
			if (response.statusCode() != 200) return null;
			List<NativeImage> frames = new ArrayList<>();
			try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(response.body()))) {
				if (input == null) return null;
				var readers = ImageIO.getImageReaders(input);
				if (!readers.hasNext()) return null;
				ImageReader reader = readers.next();
				try {
					reader.setInput(input, false, false);
					int count = reader.getNumImages(true);
					for (int i = 0; i < count; i++) frames.add(toNativeImage(reader.read(i)));
				} finally { reader.dispose(); }
			}
			return new DecodedTexture(frames);
		} catch (IOException | InterruptedException exception) {
			if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
			return null;
		}
	}

	/** ImageIOのフレームをMinecraftの画像形式へ変換する */
	private static NativeImage toNativeImage(BufferedImage image) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, "PNG", output);
		return NativeImage.read(new ByteArrayInputStream(output.toByteArray()));
	}

	/** デコード済みフレームをゲーム内テクスチャとして登録する */
	private void registerTexture(String imageUrl, DecodedTexture decoded) {
		List<Identifier> ids = new ArrayList<>();
		try {
			String base = "slack/" + digest(imageUrl);
			for (int i = 0; i < decoded.frames().size(); i++) {
				Identifier id = Identifier.fromNamespaceAndPath(MinemojiClient.MOD_ID, base + "/frame" + i);
				Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Minemoji Slack emoji", decoded.frames().get(i)));
				ids.add(id);
			}
			this.texturesByUrl.put(imageUrl, new AnimatedTexture(ids));
		} finally { this.loadingUrls.remove(imageUrl); }
	}

	/** 画像URLから安定したテクスチャ識別子を作る */
	private static String digest(String value) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder(hash.length * 2);
			for (byte b : hash) { builder.append(Character.forDigit((b >> 4) & 0xF, 16)); builder.append(Character.forDigit(b & 0xF, 16)); }
			return builder.toString();
		} catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-1 is not available", exception); }
	}

	private record DecodedTexture(List<NativeImage> frames) {}


	private static final class AnimatedTexture {
		private final List<Identifier> frameIds;
		private int frame;
		private long nextFrameAt;

		private AnimatedTexture(List<Identifier> frameIds) { this.frameIds = frameIds; }

		/** 経過時間に応じてアニメーションフレームを進める */
		private Identifier current(long now) {
			if (this.frameIds.size() > 1 && now >= this.nextFrameAt) {
				this.frame = (this.frame + 1) % this.frameIds.size();
				this.nextFrameAt = now + 100;
			}
			return this.frameIds.get(this.frame);
		}

		/** 登録した全フレームを解放する */
		private void release(Minecraft minecraft) { this.frameIds.forEach(minecraft.getTextureManager()::release); }
	}
}
