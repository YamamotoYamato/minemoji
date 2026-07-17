package dev.yamato.minemoji.slack;

public record SlackEmojiRefreshResult(boolean success, String message, int loadedCount) {
	public static SlackEmojiRefreshResult success(int loadedCount) {
		return new SlackEmojiRefreshResult(true, "Loaded " + loadedCount + " Slack emoji.", loadedCount);
	}

	public static SlackEmojiRefreshResult failure(String message, int loadedCount) {
		return new SlackEmojiRefreshResult(false, message, loadedCount);
	}
}

