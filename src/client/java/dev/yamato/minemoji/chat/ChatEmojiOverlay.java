package dev.yamato.minemoji.chat;

import dev.yamato.minemoji.MinemojiClient;
import dev.yamato.minemoji.slack.SlackEmoji;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** 通常のチャット描画後にSlack絵文字を重ねて描画する */
public final class ChatEmojiOverlay {
	private static final int CHAT_EMOJI_HEIGHT = 9;
	private ChatEmojiOverlay() {
	}

	/** 画面座標で絵文字のホバー画像を表示する */
	public static void renderTooltipScreen(GuiGraphics graphics, Font font, int y, String text, int mouseX, int mouseY, float scale, int imageSize) {
		for (ChatEmojiToken token : ChatEmojiParser.find(text)) {
			int left = 4 + Math.round(scale * font.width(text.substring(0, token.start())));
			int right = left + Math.round(scale * font.width(token.text()));
			int top = Math.round(scale * (y - 1));
			int bottom = top + Math.round(scale * CHAT_EMOJI_HEIGHT);
			if (mouseX < left || mouseX > right || mouseY < top || mouseY > bottom) continue;
			SlackEmoji emoji = MinemojiClient.getInstance().slackEmojiService().find(token.name());
			var texture = emoji == null ? null : MinemojiClient.getInstance().slackEmojiTextureCache().getTexture(emoji);
			if (texture == null) return;
			int tooltipX = mouseX + 8;
			int tooltipY = Math.max(4, mouseY - 28);
			graphics.fill(tooltipX - 3, tooltipY - 3, tooltipX + imageSize + 3, tooltipY + imageSize + 3, 0xEE101217);
			graphics.blit(texture, tooltipX, tooltipY, tooltipX + imageSize, tooltipY + imageSize, 0.0F, 1.0F, 0.0F, 1.0F);
			return;
		}
	}
}
