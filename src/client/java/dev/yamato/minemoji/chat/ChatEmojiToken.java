package dev.yamato.minemoji.chat;

/** チャット文字列中の絵文字トークンを表す */
public record ChatEmojiToken(int start, int end, String name) {
	/** トークン全体をSlack形式の絵文字名として返す */
	public String text() {
		return ":" + this.name + ":";
	}
}
