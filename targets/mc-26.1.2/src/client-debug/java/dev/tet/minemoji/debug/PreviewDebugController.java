package dev.tet.minemoji.debug;

import com.mojang.blaze3d.platform.InputConstants;
import dev.tet.minemoji.MinemojiClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class PreviewDebugController {
	private static final int DEBUG_HITBOX_MARGIN_X = 128;
	private static final int DEBUG_HITBOX_MARGIN_Y = 32;
	private static final KeyMapping TOGGLE_PREVIEW_FIX_KEY = new KeyMapping(
		"key.minemoji.toggle_preview_fix",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_F8,
		KeyMapping.Category.MISC
	);
	private static final KeyMapping TOGGLE_PREVIEW_DEBUG_KEY = new KeyMapping(
		"key.minemoji.toggle_preview_debug",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_F9,
		KeyMapping.Category.MISC
	);
	private static final KeyMapping TOGGLE_PREVIEW_FORCE_KEY = new KeyMapping(
		"key.minemoji.toggle_preview_force",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_F10,
		KeyMapping.Category.MISC
	);

	private static boolean previewFixEnabled;
	private static boolean previewDebugEnabled;
	private static boolean previewForceEnabled;
	private static String lastPreviewRenderContext = "";
	private static String lastPreviewHit = "";

	private PreviewDebugController() {
	}

	public static void register() {
		KeyMappingHelper.registerKeyMapping(TOGGLE_PREVIEW_FIX_KEY);
		KeyMappingHelper.registerKeyMapping(TOGGLE_PREVIEW_DEBUG_KEY);
		KeyMappingHelper.registerKeyMapping(TOGGLE_PREVIEW_FORCE_KEY);
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			while (TOGGLE_PREVIEW_FIX_KEY.consumeClick()) {
				previewFixEnabled = !previewFixEnabled;
				showPreviewDebugState("Fix");
			}
			while (TOGGLE_PREVIEW_DEBUG_KEY.consumeClick()) {
				previewDebugEnabled = !previewDebugEnabled;
				showPreviewDebugState("Wide hitbox");
			}
			while (TOGGLE_PREVIEW_FORCE_KEY.consumeClick()) {
				previewForceEnabled = !previewForceEnabled;
				showPreviewDebugState("Force first preview");
			}
		});
	}

	public static boolean isPreviewFixEnabled() {
		return previewFixEnabled;
	}

	public static boolean isEmojiHit(GuiGraphicsExtractor graphics, int tokenIndex, int left, int right, int top, int bottom, int mouseX, int mouseY) {
		int hitLeft = previewDebugEnabled ? left - DEBUG_HITBOX_MARGIN_X : left;
		int hitRight = previewDebugEnabled ? right + DEBUG_HITBOX_MARGIN_X : right;
		int hitTop = previewDebugEnabled ? top - DEBUG_HITBOX_MARGIN_Y : top;
		int hitBottom = previewDebugEnabled ? bottom + DEBUG_HITBOX_MARGIN_Y : bottom;
		if (previewDebugEnabled) {
			graphics.fill(hitLeft, hitTop, hitRight, hitBottom, 0x302C8DFF);
			graphics.fill(left, top, right, bottom, 0x8066FF66);
		}

		boolean mouseHit = mouseX >= hitLeft && mouseX <= hitRight && mouseY >= hitTop && mouseY <= hitBottom;
		return mouseHit || previewForceEnabled && tokenIndex == 0;
	}

	public static void logPreviewRenderContext(Screen screen, ChatComponent.DisplayMode displayMode, boolean focused) {
		if (!previewDebugEnabled && !previewForceEnabled) {
			return;
		}

		String screenName = screen == null ? "none" : screen.getClass().getSimpleName();
		String context = "screen=" + screenName + ", displayMode=" + displayMode + ", focused=" + focused;
		if (!context.equals(lastPreviewRenderContext)) {
			lastPreviewRenderContext = context;
			MinemojiClient.LOGGER.info("Chat emoji preview render call: {}", context);
		}
	}

	public static void logPreviewHit(String emojiName, int mouseX, int mouseY) {
		if (!previewDebugEnabled && !previewForceEnabled) {
			return;
		}

		String hit = "emoji=" + emojiName + ", mouse=(" + mouseX + "," + mouseY + ")";
		if (!hit.equals(lastPreviewHit)) {
			lastPreviewHit = hit;
			MinemojiClient.LOGGER.info("Chat emoji preview hit: {}", hit);
		}
	}

	private static void showPreviewDebugState(String changedSetting) {
		lastPreviewRenderContext = "";
		lastPreviewHit = "";
		String message = "Minemoji " + changedSetting + ": " + (settingValue(changedSetting) ? "ON" : "OFF")
			+ " | fix=" + onOff(previewFixEnabled)
			+ " wide=" + onOff(previewDebugEnabled)
			+ " force=" + onOff(previewForceEnabled);
		Minecraft.getInstance().gui.setOverlayMessage(Component.literal(message), false);
		MinemojiClient.LOGGER.info(message);
	}

	private static boolean settingValue(String setting) {
		return switch (setting) {
			case "Fix" -> previewFixEnabled;
			case "Wide hitbox" -> previewDebugEnabled;
			case "Force first preview" -> previewForceEnabled;
			default -> false;
		};
	}

	private static String onOff(boolean enabled) {
		return enabled ? "ON" : "OFF";
	}
}
