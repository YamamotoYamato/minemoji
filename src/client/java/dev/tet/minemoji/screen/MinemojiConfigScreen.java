package dev.tet.minemoji.screen;

import dev.tet.minemoji.MinemojiClient;
import dev.tet.minemoji.config.MinemojiConfig;
import dev.tet.minemoji.slack.SlackEmojiRefreshResult;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MinemojiConfigScreen extends Screen {
	private static final Component TITLE = Component.literal("Minemoji Config");
	private static final Component TOKEN_LABEL = Component.literal("Slack Token");
	private static final Component MAX_SUGGESTIONS_LABEL = Component.literal("Max Suggestions");
	private static final Component MIN_QUERY_LABEL = Component.literal("Minimum Query Length");
	private static final Component REFRESH_ON_STARTUP_LABEL = Component.literal("Refresh On Startup");

	private final Screen parent;
	private final MinemojiClient client;

	private EditBox tokenBox;
	private EditBox maxSuggestionsBox;
	private EditBox minimumQueryLengthBox;
	private CycleButton<Boolean> refreshOnStartupButton;

	private Component statusMessage = Component.literal("Edit values and press Save.");
	private int statusColor = 0xFFB8C0CC;
	private boolean saving;

	public MinemojiConfigScreen(Screen parent) {
		super(TITLE);
		this.parent = parent;
		this.client = MinemojiClient.getInstance();
	}

	@Override
	protected void init() {
		super.init();

		MinemojiConfig config = this.client.config();
		int left = this.width / 2 - 155;
		int contentWidth = 310;
		int fieldWidth = 310;
		int fieldHeight = 20;
		int rowGap = 26;
		int top = 52;

		this.tokenBox = new EditBox(this.font, left, top + 12, fieldWidth, fieldHeight, TOKEN_LABEL);
		this.tokenBox.setMaxLength(512);
		this.tokenBox.setValue(config.slackToken);
		this.addRenderableWidget(this.tokenBox);

		this.maxSuggestionsBox = new EditBox(this.font, left, top + 12 + rowGap * 2, 150, fieldHeight, MAX_SUGGESTIONS_LABEL);
		this.maxSuggestionsBox.setMaxLength(3);
		this.maxSuggestionsBox.setValue(Integer.toString(config.maxSuggestions));
		this.addRenderableWidget(this.maxSuggestionsBox);

		this.minimumQueryLengthBox = new EditBox(this.font, left + 160, top + 12 + rowGap * 2, 150, fieldHeight, MIN_QUERY_LABEL);
		this.minimumQueryLengthBox.setMaxLength(3);
		this.minimumQueryLengthBox.setValue(Integer.toString(config.minimumQueryLength));
		this.addRenderableWidget(this.minimumQueryLengthBox);

		this.refreshOnStartupButton = CycleButton.onOffBuilder(config.refreshOnStartup)
			.create(left, top + 12 + rowGap * 4, contentWidth, 20, REFRESH_ON_STARTUP_LABEL, (button, value) -> {
			});
		this.addRenderableWidget(this.refreshOnStartupButton);

		this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> this.save())
			.bounds(left, this.height - 34, 100, 20)
			.build());
		this.addRenderableWidget(Button.builder(Component.literal("Refresh"), button -> this.refreshFromScreen())
			.bounds(left + 105, this.height - 34, 100, 20)
			.build());
		this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose())
			.bounds(left + 210, this.height - 34, 100, 20)
			.build());

		this.setInitialFocus(this.tokenBox);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float deltaTicks) {
		this.renderBackdrop(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, deltaTicks);

		int left = this.width / 2 - 155;
		int top = 52;

		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
		guiGraphics.drawString(this.font, Component.literal(this.client.config().path().toString()), left, 34, 0xFF7F8A99, false);

		guiGraphics.drawString(this.font, TOKEN_LABEL, left, top, 0xFFE8EDF2, false);
		guiGraphics.drawString(this.font, MAX_SUGGESTIONS_LABEL, left, top + 52, 0xFFE8EDF2, false);
		guiGraphics.drawString(this.font, MIN_QUERY_LABEL, left + 160, top + 52, 0xFFE8EDF2, false);
		guiGraphics.drawString(this.font, Component.literal("Values are saved to JSON and used immediately."), left, top + 102, 0xFF9FA8B7, false);
		guiGraphics.drawString(this.font, this.statusMessage, left, this.height - 48, this.statusColor, false);
	}

	private void renderBackdrop(GuiGraphics guiGraphics) {
		int left = this.width / 2 - 167;
		int top = 16;
		int right = this.width / 2 + 167;
		int bottom = this.height - 12;

		guiGraphics.fill(0, 0, this.width, this.height, 0xB0101116);
		guiGraphics.fill(left, top, right, bottom, 0xF11A1E26);
		guiGraphics.fill(left, top, right, top + 1, 0xFF697588);
		guiGraphics.fill(left, bottom - 1, right, bottom, 0xFF697588);
		guiGraphics.fill(left, top, left + 1, bottom, 0xFF697588);
		guiGraphics.fill(right - 1, top, right, bottom, 0xFF697588);
		guiGraphics.fill(left + 1, top + 1, right - 1, top + 4, 0xFF9FCA72);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	private void save() {
		ParsedValues values = this.parseValues();
		if (values == null) {
			return;
		}

		MinemojiConfig config = this.client.config();
		config.slackToken = values.slackToken();
		config.maxSuggestions = values.maxSuggestions();
		config.minimumQueryLength = values.minimumQueryLength();
		config.refreshOnStartup = values.refreshOnStartup();
		config.save();

		this.setStatus("Saved " + config.path().getFileName() + ".", 0xFF8AD481);
	}

	private void refreshFromScreen() {
		ParsedValues values = this.parseValues();
		if (values == null) {
			return;
		}

		MinemojiConfig config = this.client.config();
		config.slackToken = values.slackToken();
		config.maxSuggestions = values.maxSuggestions();
		config.minimumQueryLength = values.minimumQueryLength();
		config.refreshOnStartup = values.refreshOnStartup();
		config.save();

		if (!config.isConfigured()) {
			this.client.slackEmojiService().clear();
			this.setStatus("Saved. Slack token is blank, so emoji cache was cleared.", 0xFFE7C46A);
			return;
		}

		if (this.saving) {
			return;
		}

		this.saving = true;
		this.setStatus("Saved. Refreshing Slack emoji...", 0xFFB8C0CC);
		CompletableFuture<SlackEmojiRefreshResult> refresh = this.client.slackEmojiService().refreshAsync();
		refresh.whenComplete((result, throwable) -> Minecraft.getInstance().execute(() -> {
			this.saving = false;
			if (throwable != null) {
				this.setStatus("Slack refresh failed: " + throwable.getMessage(), 0xFFFF7E7E);
				return;
			}

			if (result.success()) {
				this.setStatus(result.message(), 0xFF8AD481);
				return;
			}

			this.setStatus(result.message(), 0xFFFF7E7E);
		}));
	}

	private ParsedValues parseValues() {
		String slackToken = this.tokenBox.getValue().trim();
		Integer maxSuggestions = this.parsePositiveInt(this.maxSuggestionsBox.getValue(), "Max Suggestions");
		if (maxSuggestions == null) {
			return null;
		}

		Integer minimumQueryLength = this.parsePositiveInt(this.minimumQueryLengthBox.getValue(), "Minimum Query Length");
		if (minimumQueryLength == null) {
			return null;
		}

		return new ParsedValues(slackToken, maxSuggestions, minimumQueryLength, this.refreshOnStartupButton.getValue());
	}

	private Integer parsePositiveInt(String value, String label) {
		try {
			int parsed = Integer.parseInt(value.trim());
			if (parsed < 1) {
				this.setStatus(label + " must be 1 or greater.", 0xFFFF7E7E);
				return null;
			}
			return parsed;
		} catch (NumberFormatException exception) {
			this.setStatus(label + " must be a number.", 0xFFFF7E7E);
			return null;
		}
	}

	private void setStatus(String message, int color) {
		this.statusMessage = Component.literal(message);
		this.statusColor = color;
	}

	private record ParsedValues(String slackToken, int maxSuggestions, int minimumQueryLength, boolean refreshOnStartup) {
	}
}
