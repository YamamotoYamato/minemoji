package dev.tet.minemoji.mixin.client;

import dev.tet.minemoji.MinemojiClient;
import dev.tet.minemoji.chat.ChatEmojiOverlay;
import dev.tet.minemoji.debug.PreviewDebugController;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
abstract class ChatComponentMixin {
	@Shadow
	private List<GuiMessage.Line> trimmedMessages;

	@Shadow
	private int chatScrollbarPos;

	@Shadow
	protected abstract double getScale();

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void minemoji$renderOverlay(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, int currentTick, ChatComponent.DisplayMode displayMode, boolean focused, CallbackInfo callback) {
		Minecraft minecraft = Minecraft.getInstance();
		MinemojiClient client = MinemojiClient.getInstance();
		if (client == null) {
			return;
		}
		PreviewDebugController.logPreviewRenderContext(minecraft.gui.screen(), displayMode, focused);
		if (PreviewDebugController.isPreviewFixEnabled() && !(minecraft.gui.screen() instanceof ChatScreen)) {
			return;
		}

		var window = minecraft.getWindow();
		int actualMouseX = (int)Math.floor(minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / (double)window.getScreenWidth());
		int actualMouseY = (int)Math.floor(minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / (double)window.getScreenHeight());
		int linesPerPage = ((ChatComponent)(Object)this).getLinesPerPage();
		double scale = this.getScale();
		int spacing = (int)(9.0 * (minecraft.options.chatLineSpacing().get() + 1.0));
		int baseline = (int)Math.floor((graphics.guiHeight() - 40) / scale);
		int offset = (int)Math.round(8.0 * (minecraft.options.chatLineSpacing().get() + 1.0) - 4.0 * minecraft.options.chatLineSpacing().get());
		float chatOpacity = minecraft.options.chatOpacity().get().floatValue() * 0.9F + 0.1F;
		int visibleLines = Math.min(Math.max(this.trimmedMessages.size() - this.chatScrollbarPos, 0), linesPerPage);

		for (int index = 0; index < visibleLines; index++) {
			GuiMessage.Line line = this.trimmedMessages.get(index + this.chatScrollbarPos);
			int age = currentTick - line.addedTime();
			if (!focused && age >= 200) {
				continue;
			}
			float opacity = (focused ? 1.0F : fadeOpacity(line.addedTime(), currentTick)) * chatOpacity;
			if (opacity > 0.08F) {
				int lineY = baseline - index * spacing - offset;
				ChatEmojiOverlay.renderTooltipScreen(graphics, font, lineY, toText(line.content()), actualMouseX, actualMouseY, (float)scale, client.config().hoverEmojiSize);
			}
		}
	}

	private static float fadeOpacity(int addedTime, int currentTick) {
		double value = Math.max(0.0, Math.min(1.0, (1.0 - (currentTick - addedTime) / 200.0) * 10.0));
		return (float)(value * value);
	}

	private static String toText(FormattedCharSequence sequence) {
		StringBuilder text = new StringBuilder();
		sequence.accept((index, style, codePoint) -> {
			text.appendCodePoint(codePoint);
			return true;
		});
		return text.toString();
	}
}
