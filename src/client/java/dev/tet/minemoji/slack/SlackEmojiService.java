package dev.tet.minemoji.slack;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.tet.minemoji.MinemojiClient;
import dev.tet.minemoji.config.MinemojiConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class SlackEmojiService {
	private static final URI EMOJI_LIST_URI = URI.create("https://slack.com/api/emoji.list");
	private static final String TWEMOJI_BASE_URL = "https://cdnjs.cloudflare.com/ajax/libs/twemoji/14.0.2/72x72/";

	private final MinemojiConfig config;
	private final StandardEmojiDataset standardEmojiDataset = StandardEmojiDataset.getInstance();
	private final HttpClient httpClient = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.build();

	private volatile List<SlackEmoji> emojis = List.of();
	private volatile Map<String, SlackEmoji> emojisByName = Map.of();
	private volatile String lastError = "";
	private volatile Instant lastRefreshAt;
	private volatile CompletableFuture<SlackEmojiRefreshResult> activeRefresh;

	public SlackEmojiService(MinemojiConfig config) {
		this.config = config;
	}

	public List<SlackEmoji> suggestions(String query) {
		String normalizedQuery = query.toLowerCase(Locale.ROOT);
		int maxSuggestions = this.config.maxSuggestions;

		return this.emojis.stream()
			.filter(emoji -> emoji.normalizedName().contains(normalizedQuery))
			.sorted(Comparator
				.comparingInt((SlackEmoji emoji) -> score(emoji, normalizedQuery))
				.thenComparing(SlackEmoji::isAlias)
				.thenComparingInt(emoji -> emoji.name().length())
				.thenComparing(SlackEmoji::name))
			.limit(maxSuggestions)
			.toList();
	}

	public SlackEmoji find(String name) {
		return this.emojisByName.get(name.toLowerCase(Locale.ROOT));
	}

	public CompletableFuture<SlackEmojiRefreshResult> refreshAsync() {
		if (!this.config.isConfigured()) {
			this.lastError = "No Slack token configured.";
			return CompletableFuture.completedFuture(SlackEmojiRefreshResult.failure("No Slack token configured.", this.emojis.size()));
		}

		CompletableFuture<SlackEmojiRefreshResult> running = this.activeRefresh;
		if (running != null && !running.isDone()) {
			return running;
		}

		CompletableFuture<SlackEmojiRefreshResult> refresh = CompletableFuture.supplyAsync(this::refreshBlocking);
		this.activeRefresh = refresh;
		return refresh;
	}

	public void clear() {
		this.emojis = List.of();
		this.emojisByName = Map.of();
		this.lastError = "";
		this.lastRefreshAt = null;
		MinemojiClient.getInstance().slackEmojiTextureCache().clear();
	}

	public String imageUrlFor(SlackEmoji emoji) {
		SlackEmoji resolved = this.resolveImageEmoji(emoji);
		if (resolved == null || resolved.isAlias()) {
			return null;
		}
		if (resolved.isUnicode()) {
			return TWEMOJI_BASE_URL + toTwemojiCodepoint(resolved.unicodeValue()) + ".png";
		}
		return resolved.value();
	}

	public String describeStatus() {
		if (!this.config.isConfigured()) {
			return "Slack token is not configured. Edit " + this.config.path() + ".";
		}
		if (!this.lastError.isBlank()) {
			return "Loaded " + this.emojis.size() + " emoji. Last error: " + this.lastError;
		}
		if (this.lastRefreshAt == null) {
			return "Slack emoji have not been refreshed yet.";
		}
		return "Loaded " + this.emojis.size() + " emoji. Last refresh: " + DateTimeFormatter.ISO_INSTANT.format(this.lastRefreshAt) + ".";
	}

	private SlackEmojiRefreshResult refreshBlocking() {
		try {
			HttpRequest request = HttpRequest.newBuilder(EMOJI_LIST_URI)
				.timeout(Duration.ofSeconds(20))
				.header("Authorization", "Bearer " + this.config.slackToken)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString("include_categories=true"))
				.build();

			HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				String message = "Slack emoji.list returned HTTP " + response.statusCode() + ".";
				this.lastError = message;
				return SlackEmojiRefreshResult.failure(message, this.emojis.size());
			}

			JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
			if (!root.get("ok").getAsBoolean()) {
				String error = root.has("error") ? root.get("error").getAsString() : "unknown_error";
				String message = this.formatSlackError(error);
				this.lastError = message;
				return SlackEmojiRefreshResult.failure(message, this.emojis.size());
			}

			JsonObject emojiObject = root.getAsJsonObject("emoji");
			List<SlackEmoji> loaded = new ArrayList<>(emojiObject.size());
			for (java.util.Map.Entry<String, JsonElement> entry : emojiObject.entrySet()) {
				String value = entry.getValue().getAsString();
				String aliasOf = value.startsWith("alias:") ? value.substring("alias:".length()) : "";
				loaded.add(new SlackEmoji(entry.getKey(), aliasOf, value));
			}
			this.addSlackStandardEmoji(root, loaded);

			loaded.sort(Comparator.comparing(SlackEmoji::name));
			this.emojis = List.copyOf(loaded);
			this.emojisByName = loaded.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(SlackEmoji::name, emoji -> emoji, (left, right) -> left));
			this.lastError = "";
			this.lastRefreshAt = Instant.now();
			MinemojiClient.getInstance().slackEmojiTextureCache().clear();
			return SlackEmojiRefreshResult.success(loaded.size());
		} catch (IOException | InterruptedException exception) {
			if (exception instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			String message = "Slack refresh failed: " + describeException(exception);
			this.lastError = message;
			MinemojiClient.LOGGER.error("Failed to refresh Slack emoji", exception);
			return SlackEmojiRefreshResult.failure(message, this.emojis.size());
		} catch (RuntimeException exception) {
			String message = "Slack refresh failed: " + describeException(exception);
			this.lastError = message;
			MinemojiClient.LOGGER.error("Failed to parse Slack emoji response", exception);
			return SlackEmojiRefreshResult.failure(message, this.emojis.size());
		}
	}

	private static int score(SlackEmoji emoji, String query) {
		String name = emoji.normalizedName();
		if (name.equals(query)) {
			return 0;
		}
		if (name.startsWith(query)) {
			return 1;
		}
		return 2;
	}

	private String formatSlackError(String error) {
		if ("missing_scope".equals(error)) {
			return "Slack emoji.list failed: missing_scope (add emoji:read and reinstall the app to the workspace).";
		}
		if ("invalid_auth".equals(error) || "token_revoked".equals(error) || "token_expired".equals(error)) {
			return "Slack emoji.list failed: " + error + " (check slackToken in " + this.config.path() + ").";
		}
		return "Slack emoji.list failed: " + error;
	}

	private void addSlackStandardEmoji(JsonObject root, List<SlackEmoji> loaded) {
		if (!root.has("categories") || !root.get("categories").isJsonArray()) {
			return;
		}

		java.util.Set<String> existingNames = loaded.stream()
			.map(SlackEmoji::name)
			.collect(java.util.stream.Collectors.toCollection(java.util.HashSet::new));

		for (JsonElement categoryElement : root.getAsJsonArray("categories")) {
			if (!categoryElement.isJsonObject()) {
				continue;
			}

			JsonArray emojiNames = categoryElement.getAsJsonObject().getAsJsonArray("emoji_names");
			if (emojiNames == null) {
				continue;
			}

			for (JsonElement emojiNameElement : emojiNames) {
				String emojiName = emojiNameElement.getAsString();
				if (!existingNames.add(emojiName)) {
					continue;
				}

				String unicode = this.standardEmojiDataset.unicodeForSlackName(emojiName);
				if (!unicode.isBlank()) {
					loaded.add(SlackEmoji.unicode(emojiName, unicode));
				}
			}
		}
	}

	private SlackEmoji resolveImageEmoji(SlackEmoji emoji) {
		SlackEmoji current = emoji;
		for (int depth = 0; depth < 8 && current != null; depth++) {
			if (!current.isAlias()) {
				return current;
			}
			current = this.emojisByName.get(current.aliasOf());
		}
		return null;
	}

	private static String toTwemojiCodepoint(String unicode) {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (int offset = 0; offset < unicode.length(); ) {
			int codepoint = unicode.codePointAt(offset);
			offset += Character.charCount(codepoint);
			if (codepoint == 0xFE0F) {
				continue;
			}
			if (!first) {
				builder.append('-');
			}
			builder.append(Integer.toHexString(codepoint));
			first = false;
		}
		return builder.toString();
	}

	private static String describeException(Exception exception) {
		String message = exception.getMessage();
		if (message != null && !message.isBlank()) {
			return message;
		}
		return exception.getClass().getSimpleName();
	}
}
