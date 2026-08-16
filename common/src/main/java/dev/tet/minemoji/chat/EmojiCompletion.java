package dev.tet.minemoji.chat;

import java.util.Locale;
import java.util.Optional;

public final class EmojiCompletion {
	private EmojiCompletion() {
	}

	public static Optional<Query> find(String text, int cursor, int minimumQueryLength) {
		if (text.isEmpty() || cursor <= 0 || cursor > text.length()) {
			return Optional.empty();
		}

		int queryStart = cursor;
		while (queryStart > 0 && isEmojiNameChar(text.charAt(queryStart - 1))) {
			queryStart--;
		}

		int colonIndex = queryStart - 1;
		if (colonIndex < 0 || text.charAt(colonIndex) != ':') {
			return Optional.empty();
		}
		if (colonIndex > 0 && isEmojiNameChar(text.charAt(colonIndex - 1))) {
			return Optional.empty();
		}

		String query = text.substring(queryStart, cursor);
		if (query.length() < minimumQueryLength) {
			return Optional.empty();
		}

		int replaceTo = cursor;
		while (replaceTo < text.length() && isEmojiNameChar(text.charAt(replaceTo))) {
			replaceTo++;
		}
		if (replaceTo < text.length() && text.charAt(replaceTo) == ':') {
			replaceTo++;
		}

		return Optional.of(new Query(query.toLowerCase(Locale.ROOT), colonIndex, replaceTo, text.substring(colonIndex, replaceTo)));
	}

	public static Applied apply(String text, Query query, String emojiName) {
		String replacement = ":" + emojiName + ":";
		String updated = text.substring(0, query.replaceFrom()) + replacement + text.substring(query.replaceTo());
		return new Applied(updated, query.replaceFrom() + replacement.length());
	}

	private static boolean isEmojiNameChar(char character) {
		return Character.isLetterOrDigit(character) || character == '_' || character == '-' || character == '+';
	}

	public record Query(String query, int replaceFrom, int replaceTo, String rawToken) {
	}

	public record Applied(String value, int cursor) {
	}
}
