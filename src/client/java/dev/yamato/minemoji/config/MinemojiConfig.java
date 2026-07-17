package dev.yamato.minemoji.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.yamato.minemoji.MinemojiClient;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MinemojiConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public String slackToken = "";
	public int maxSuggestions = 8;
	public int minimumQueryLength = 1;
	public int hoverEmojiSize = 24;
	public boolean refreshOnStartup = true;

	private transient Path path;

	public static MinemojiConfig load(Path path) {
		if (!Files.exists(path)) {
			MinemojiConfig config = new MinemojiConfig();
			config.path = path;
			config.save();
			return config;
		}

		try (Reader reader = Files.newBufferedReader(path)) {
			MinemojiConfig config = GSON.fromJson(reader, MinemojiConfig.class);
			if (config == null) {
				config = new MinemojiConfig();
			}
			config.path = path;
			config.normalize();
			return config;
		} catch (IOException | RuntimeException exception) {
			MinemojiClient.LOGGER.error("Failed to load config from {}", path, exception);
			MinemojiConfig config = new MinemojiConfig();
			config.path = path;
			return config;
		}
	}

	public boolean isConfigured() {
		return !this.slackToken.isBlank();
	}

	public Path path() {
		return this.path;
	}

	public void reload() {
		MinemojiConfig loaded = load(this.path);
		this.slackToken = loaded.slackToken;
		this.maxSuggestions = loaded.maxSuggestions;
		this.minimumQueryLength = loaded.minimumQueryLength;
		this.hoverEmojiSize = loaded.hoverEmojiSize;
		this.refreshOnStartup = loaded.refreshOnStartup;
		this.normalize();
	}

	public void save() {
		this.normalize();
		try {
			Files.createDirectories(this.path.getParent());
			try (Writer writer = Files.newBufferedWriter(this.path)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException exception) {
			MinemojiClient.LOGGER.error("Failed to save config to {}", this.path, exception);
		}
	}

	private void normalize() {
		this.slackToken = this.slackToken == null ? "" : this.slackToken.trim();
		this.maxSuggestions = Math.max(1, this.maxSuggestions);
		this.minimumQueryLength = Math.max(1, this.minimumQueryLength);
		this.hoverEmojiSize = Math.max(1, Math.min(128, this.hoverEmojiSize));
	}
}

