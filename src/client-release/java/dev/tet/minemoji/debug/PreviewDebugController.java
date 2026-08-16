package dev.tet.minemoji.debug;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;

public final class PreviewDebugController {
	private PreviewDebugController() {
	}

	public static void register() {
	}

	public static boolean isPreviewFixEnabled() {
		return true;
	}

	public static boolean isEmojiHit(GuiGraphicsExtractor graphics, int tokenIndex, int left, int right, int top, int bottom, int mouseX, int mouseY) {
		return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
	}

	public static void logPreviewRenderContext(Screen screen, ChatComponent.DisplayMode displayMode, boolean focused) {
	}

	public static void logPreviewHit(String emojiName, int mouseX, int mouseY) {
	}
}
