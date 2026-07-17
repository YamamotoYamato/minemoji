package dev.tet.minemoji.slack;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.tet.minemoji.MinemojiClient;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class StandardEmojiDataset {
	private static final Gson GSON = new Gson();
	private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {
	}.getType();
	private static final String RESOURCE_PATH = "/minemoji/standard-emoji-map.json";
	private static final StandardEmojiDataset INSTANCE = new StandardEmojiDataset(load());

	private final Map<String, String> unicodeByName;

	private StandardEmojiDataset(Map<String, String> unicodeByName) {
		this.unicodeByName = unicodeByName;
	}

	public static StandardEmojiDataset getInstance() {
		return INSTANCE;
	}

	public String unicodeFor(String shortName) {
		return this.unicodeByName.getOrDefault(shortName, "");
	}

	private static Map<String, String> load() {
		InputStream stream = StandardEmojiDataset.class.getResourceAsStream(RESOURCE_PATH);
		if (stream == null) {
			MinemojiClient.LOGGER.error("Standard emoji dataset resource is missing: {}", RESOURCE_PATH);
			return Map.of();
		}

		try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			return GSON.fromJson(reader, MAP_TYPE);
		} catch (IOException | RuntimeException exception) {
			MinemojiClient.LOGGER.error("Failed to load standard emoji dataset from {}", RESOURCE_PATH, exception);
			return Map.of();
		}
	}
}
