package dev.tet.minemoji.chat;

import dev.tet.minemoji.MinemojiClient;
import dev.tet.minemoji.slack.SlackEmoji;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ChatEmojiOverlay {
	private static final int CHAT_EMOJI_HEIGHT = 9;

	private ChatEmojiOverlay() {
	}

	public static void renderTooltipScreen(GuiGraphicsExtractor graphics, Font font, int y, String text, int mouseX, int mouseY, float scale, int imageSize) {
		for (ChatEmojiToken token : ChatEmojiParser.find(text)) {
			int left = 4 + Math.round(scale * font.width(text.substring(0, token.start())));
			int right = left + Math.round(scale * font.width(token.text()));
			int top = Math.round(scale * (y - 1));
			int bottom = top + Math.round(scale * CHAT_EMOJI_HEIGHT);
			if (mouseX < left || mouseX > right || mouseY < top || mouseY > bottom) {
				continue;
			}

			SlackEmoji emoji = MinemojiClient.getInstance().slackEmojiService().find(token.name());
			var texture = emoji == null ? null : MinemojiClient.getInstance().slackEmojiTextureCache().getTexture(emoji);
			if (texture == null) {
				return;
			}

			int tooltipX = mouseX + 8;
			int tooltipY = Math.max(4, mouseY - imageSize - 4);
			graphics.fill(tooltipX - 3, tooltipY - 3, tooltipX + imageSize + 3, tooltipY + imageSize + 3, 0xEE101217);
			graphics.blit(texture, tooltipX, tooltipY, tooltipX + imageSize, tooltipY + imageSize, 0.0F, 1.0F, 0.0F, 1.0F);
			return;
		}
	}
}
