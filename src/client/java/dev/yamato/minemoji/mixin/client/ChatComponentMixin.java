package dev.yamato.minemoji.mixin.client;

import dev.yamato.minemoji.chat.ChatEmojiOverlay;
import dev.yamato.minemoji.MinemojiClient;
import dev.yamato.minemoji.chat.ChatEmojiParser;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 通常のチャット描画後にSlack絵文字を重ねるMixin */
@Mixin(ChatComponent.class)
abstract class ChatComponentMixin {
	@Shadow private List<GuiMessage.Line> trimmedMessages;
	@Shadow private int chatScrollbarPos;
	@Shadow protected abstract double getScale();

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void minemoji$renderOverlay(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, int currentTick, ChatComponent.DisplayMode displayMode, boolean focused, CallbackInfo callback) {
		Minecraft minecraft = Minecraft.getInstance();
		var window = minecraft.getWindow();
		int actualMouseX = (int)Math.floor(minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / (double)window.getScreenWidth());
		int actualMouseY = (int)Math.floor(minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / (double)window.getScreenHeight());
		int linesPerPage = ((ChatComponent)(Object)this).getLinesPerPage();
		double scale = this.getScale();
		int spacing = (int)(9.0 * (net.minecraft.client.Minecraft.getInstance().options.chatLineSpacing().get() + 1.0));
		int baseline = (int)Math.floor((graphics.guiHeight() - 40) / scale);
		int offset = (int)Math.round(8.0 * (net.minecraft.client.Minecraft.getInstance().options.chatLineSpacing().get() + 1.0) - 4.0 * net.minecraft.client.Minecraft.getInstance().options.chatLineSpacing().get());
		float chatOpacity = net.minecraft.client.Minecraft.getInstance().options.chatOpacity().get().floatValue() * 0.9F + 0.1F;
		graphics.pose().pushMatrix();
		graphics.pose().scale((float)scale, (float)scale);
		graphics.pose().translate(4.0F, 0.0F);
		int visibleLines = Math.min(this.trimmedMessages.size() - this.chatScrollbarPos, linesPerPage);
		for (int index = 0; index < visibleLines; index++) {
			GuiMessage.Line line = this.trimmedMessages.get(index + this.chatScrollbarPos);
			int age = currentTick - line.addedTime();
			if (!focused && age >= 200) continue;
			float opacity = (focused ? 1.0F : fadeOpacity(line.addedTime(), currentTick)) * chatOpacity;
			int lineY = baseline - index * spacing - offset;
			if (opacity > 0.08F) {
				String text = toText(line.content());
			}
		}
		graphics.pose().popMatrix();
		for (int index = 0; index < visibleLines; index++) {
			GuiMessage.Line line = this.trimmedMessages.get(index + this.chatScrollbarPos);
			int age = currentTick - line.addedTime();
			if (!focused && age >= 200) continue;
			float opacity = (focused ? 1.0F : fadeOpacity(line.addedTime(), currentTick)) * chatOpacity;
			if (opacity > 0.08F) ChatEmojiOverlay.renderTooltipScreen(graphics, font, baseline - index * spacing - offset, toText(line.content()), actualMouseX, actualMouseY, (float)scale, MinemojiClient.getInstance().config().hoverEmojiSize);
		}
	}

	/** バニラチャットと同じ経過時間による透明度を計算する */
	private static float fadeOpacity(int addedTime, int currentTick) {
		double value = Math.max(0.0, Math.min(1.0, (1.0 - (currentTick - addedTime) / 200.0) * 10.0));
		return (float)(value * value);
	}

	/** 書式付き文字列を絵文字検索用の文字列へ変換する */
	private static String toText(FormattedCharSequence sequence) {
		StringBuilder text = new StringBuilder();
		sequence.accept((index, style, codePoint) -> { text.appendCodePoint(codePoint); return true; });
		return text.toString();
	}
}
