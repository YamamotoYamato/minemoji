package dev.yamato.minemoji.chat;

import java.util.ArrayList;
import java.util.List;

/** チャット文字列からSlack形式の絵文字トークンを抽出する */
public final class ChatEmojiParser {
	private ChatEmojiParser() {
	}

	/** チャット文字列中の有効な絵文字トークンを返す */
	public static List<ChatEmojiToken> find(String text) {
		List<ChatEmojiToken> tokens = new ArrayList<>();
		int start = 0;
		while ((start = text.indexOf(':', start)) >= 0) {
			int end = text.indexOf(':', start + 1);
			if (end < 0) break;
			String name = text.substring(start + 1, end);
			if (isName(name)) tokens.add(new ChatEmojiToken(start, end + 1, name));
			start = end + 1;
		}
		return List.copyOf(tokens);
	}

	/** Slack絵文字名として利用できる文字列か判定する */
	private static boolean isName(String name) {
		if (name.isBlank()) return false;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '+')) return false;
		}
		return true;
	}
}
