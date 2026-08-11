package dev.tet.minemoji.chat;

import java.util.ArrayList;
import java.util.List;

public final class ChatEmojiParser {
	private ChatEmojiParser() {
	}

	public static List<ChatEmojiToken> find(String text) {
		List<ChatEmojiToken> tokens = new ArrayList<>();
		int start = 0;
		while ((start = text.indexOf(':', start)) >= 0) {
			int end = text.indexOf(':', start + 1);
			if (end < 0) {
				break;
			}
			String name = text.substring(start + 1, end);
			if (isName(name)) {
				tokens.add(new ChatEmojiToken(start, end + 1, name));
			}
			start = end + 1;
		}
		return List.copyOf(tokens);
	}

	private static boolean isName(String name) {
		if (name.isBlank()) {
			return false;
		}
		for (int index = 0; index < name.length(); index++) {
			char character = name.charAt(index);
			if (!(Character.isLetterOrDigit(character) || character == '_' || character == '-' || character == '+')) {
				return false;
			}
		}
		return true;
	}
}
