package dev.yamato.minemoji.mixin.client;

import dev.yamato.minemoji.MinemojiClient;
import dev.yamato.minemoji.chat.EmojiCompletion;
import dev.yamato.minemoji.slack.SlackEmoji;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
abstract class ChatScreenMixin {
	@Unique
	private static final int MINEMOJI_POPUP_MIN_WIDTH = 160;

	@Unique
	private static final int MINEMOJI_POPUP_PADDING_X = 6;

	@Unique
	private static final int MINEMOJI_POPUP_PADDING_Y = 5;

	@Unique
	private static final int MINEMOJI_POPUP_BACKGROUND = 0xF21A1C22;

	@Unique
	private static final int MINEMOJI_POPUP_BORDER = 0xFF6D7688;

	@Unique
	private static final int MINEMOJI_POPUP_ACCENT = 0xFF9FCA72;

	@Unique
	private static final int MINEMOJI_POPUP_SELECTION = 0xFF315F8A;

	@Unique
	private static final int MINEMOJI_POPUP_TEXT = 0xFFF4F7FB;

	@Unique
	private static final int MINEMOJI_POPUP_SELECTED_TEXT = 0xFFFFFFFF;

	@Unique
	private static final int MINEMOJI_ICON_SIZE = 12;

	@Unique
	private static final int MINEMOJI_ICON_GAP = 6;

	@Shadow
	private EditBox input;

	@Unique
	private EmojiCompletion.Query minemojiActiveQuery;

	@Unique
	private List<SlackEmoji> minemojiSuggestions = List.of();

	@Unique
	private int minemojiSelectedIndex;

	@Unique
	private String minemojiLastToken = "";

	@Unique
	private String minemojiDismissedToken = "";

	@Inject(method = "render", at = @At("TAIL"))
	private void minemoji$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta, CallbackInfo callbackInfo) {
		this.minemoji$updateSuggestions();
		if (this.minemojiSuggestions.isEmpty()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		int rowHeight = font.lineHeight + 4;
		int contentWidth = this.minemojiSuggestions.stream()
			.map(SlackEmoji::displayText)
			.mapToInt(font::width)
			.max()
			.orElse(0);
		int popupWidth = Math.max(MINEMOJI_POPUP_MIN_WIDTH, Math.min(this.input.getWidth(), contentWidth + MINEMOJI_POPUP_PADDING_X * 2 + MINEMOJI_ICON_SIZE + MINEMOJI_ICON_GAP));
		int popupHeight = this.minemojiSuggestions.size() * rowHeight + MINEMOJI_POPUP_PADDING_Y * 2;
		int popupX = 4;
		int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
		popupX = Math.max(4, Math.min(popupX, screenWidth - popupWidth - 4));
		int popupY = this.input.getY() - popupHeight - 4;
		if (popupY < 4) {
			popupY = this.input.getBottom() + 4;
		}

		guiGraphics.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xAA000000);
		guiGraphics.fill(popupX + 1, popupY + 1, popupX + popupWidth - 1, popupY + popupHeight - 1, MINEMOJI_POPUP_BACKGROUND);
		guiGraphics.fill(popupX, popupY, popupX + popupWidth, popupY + 1, MINEMOJI_POPUP_BORDER);
		guiGraphics.fill(popupX, popupY + popupHeight - 1, popupX + popupWidth, popupY + popupHeight, MINEMOJI_POPUP_BORDER);
		guiGraphics.fill(popupX, popupY, popupX + 1, popupY + popupHeight, MINEMOJI_POPUP_BORDER);
		guiGraphics.fill(popupX + popupWidth - 1, popupY, popupX + popupWidth, popupY + popupHeight, MINEMOJI_POPUP_BORDER);
		guiGraphics.fill(popupX + 1, popupY + 1, popupX + popupWidth - 1, popupY + 3, MINEMOJI_POPUP_ACCENT);

		for (int index = 0; index < this.minemojiSuggestions.size(); index++) {
			SlackEmoji emoji = this.minemojiSuggestions.get(index);
			int rowY = popupY + MINEMOJI_POPUP_PADDING_Y + index * rowHeight;
			if (index == this.minemojiSelectedIndex) {
				guiGraphics.fill(popupX + 2, rowY - 1, popupX + popupWidth - 2, rowY + rowHeight - 1, MINEMOJI_POPUP_SELECTION);
			}

			int iconX = popupX + MINEMOJI_POPUP_PADDING_X;
			int iconY = rowY;
			guiGraphics.fill(iconX, iconY, iconX + MINEMOJI_ICON_SIZE, iconY + MINEMOJI_ICON_SIZE, 0xFF252932);
			Identifier textureId = MinemojiClient.getInstance().slackEmojiTextureCache().getTexture(emoji);
			if (textureId != null) {
				guiGraphics.blit(textureId, iconX, iconY, iconX + MINEMOJI_ICON_SIZE, iconY + MINEMOJI_ICON_SIZE, 0.0F, 1.0F, 0.0F, 1.0F);
			}

			int textColor = index == this.minemojiSelectedIndex ? MINEMOJI_POPUP_SELECTED_TEXT : MINEMOJI_POPUP_TEXT;
				guiGraphics.drawString(font, emoji.displayText(), iconX + MINEMOJI_ICON_SIZE + MINEMOJI_ICON_GAP, rowY + 2, textColor, true);
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void minemoji$keyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> callbackInfo) {
		this.minemoji$updateSuggestions();
		if (this.minemojiSuggestions.isEmpty()) {
			return;
		}

		int key = keyEvent.key();
		if (key == GLFW.GLFW_KEY_UP) {
			this.minemoji$moveSelection(-1);
			callbackInfo.setReturnValue(true);
			return;
		}
		if (key == GLFW.GLFW_KEY_DOWN) {
			this.minemoji$moveSelection(1);
			callbackInfo.setReturnValue(true);
			return;
		}
		if (key == GLFW.GLFW_KEY_TAB || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			if (this.minemoji$applySelection()) {
				callbackInfo.setReturnValue(true);
			}
			return;
		}
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			this.minemojiDismissedToken = this.minemojiActiveQuery == null ? "" : this.minemojiActiveQuery.rawToken();
			this.minemojiSuggestions = List.of();
			this.minemojiActiveQuery = null;
			callbackInfo.setReturnValue(true);
		}
	}

	@Unique
	private void minemoji$moveSelection(int delta) {
		int size = this.minemojiSuggestions.size();
		this.minemojiSelectedIndex = Math.floorMod(this.minemojiSelectedIndex + delta, size);
	}

	@Unique
	private boolean minemoji$applySelection() {
		if (this.minemojiActiveQuery == null || this.minemojiSuggestions.isEmpty()) {
			return false;
		}

		EmojiCompletion.Applied applied = EmojiCompletion.apply(this.input.getValue(), this.minemojiActiveQuery, this.minemojiSuggestions.get(this.minemojiSelectedIndex).name());
		this.input.setValue(applied.value());
		this.input.setCursorPosition(applied.cursor());
		this.minemojiSuggestions = List.of();
		this.minemojiActiveQuery = null;
		this.minemojiLastToken = "";
		this.minemojiDismissedToken = "";
		return true;
	}

	@Unique
	private void minemoji$updateSuggestions() {
		MinemojiClient client = MinemojiClient.getInstance();
		if (client == null || this.input == null) {
			return;
		}

		String currentText = this.input.getValue();
		if (currentText.startsWith("/")) {
			this.minemoji$clearSuggestions();
			return;
		}

		EmojiCompletion.Query query = EmojiCompletion.find(currentText, this.input.getCursorPosition(), client.config().minimumQueryLength).orElse(null);
		if (query == null) {
			this.minemoji$clearSuggestions();
			return;
		}

		if (!query.rawToken().equals(this.minemojiLastToken)) {
			this.minemojiSelectedIndex = 0;
			this.minemojiLastToken = query.rawToken();
		}

		if (query.rawToken().equals(this.minemojiDismissedToken)) {
			this.minemojiActiveQuery = query;
			this.minemojiSuggestions = List.of();
			return;
		}

		List<SlackEmoji> suggestions = client.slackEmojiService().suggestions(query.query());
		if (suggestions.isEmpty()) {
			this.minemoji$clearSuggestions();
			return;
		}

		this.minemojiActiveQuery = query;
		this.minemojiSuggestions = suggestions;
		this.minemojiSelectedIndex = Math.min(this.minemojiSelectedIndex, suggestions.size() - 1);
	}

	@Unique
	private void minemoji$clearSuggestions() {
		this.minemojiActiveQuery = null;
		this.minemojiSuggestions = List.of();
		this.minemojiSelectedIndex = 0;
		this.minemojiLastToken = "";
		this.minemojiDismissedToken = "";
	}
}

