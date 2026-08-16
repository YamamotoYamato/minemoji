package dev.tet.minemoji.chat;

public record ChatEmojiToken(int start, int end, String name) {
	public String text() {
		return ":" + this.name + ":";
	}
}
