package dev.tet.minemoji;

import com.mojang.brigadier.Command;
import dev.tet.minemoji.config.MinemojiConfig;
import dev.tet.minemoji.debug.PreviewDebugController;
import dev.tet.minemoji.screen.MinemojiConfigScreen;
import dev.tet.minemoji.slack.SlackEmojiRefreshResult;
import dev.tet.minemoji.slack.SlackEmojiService;
import dev.tet.minemoji.slack.SlackEmojiTextureCache;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MinemojiClient implements ClientModInitializer {
	public static final String MOD_ID = "minemoji";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static MinemojiClient instance;

	private final MinemojiConfig config;
	private final SlackEmojiService slackEmojiService;
	private final SlackEmojiTextureCache slackEmojiTextureCache;

	public MinemojiClient() {
		this.config = MinemojiConfig.load(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json"));
		this.slackEmojiService = new SlackEmojiService(this.config, this::clearTextureCache);
		this.slackEmojiTextureCache = new SlackEmojiTextureCache(this.slackEmojiService);
	}

	public static MinemojiClient getInstance() {
		return instance;
	}

	public MinemojiConfig config() {
		return this.config;
	}

	public SlackEmojiService slackEmojiService() {
		return this.slackEmojiService;
	}

	public SlackEmojiTextureCache slackEmojiTextureCache() {
		return this.slackEmojiTextureCache;
	}

	private void clearTextureCache() {
		if (this.slackEmojiTextureCache != null) {
			this.slackEmojiTextureCache.clear();
		}
	}

	@Override
	public void onInitializeClient() {
		instance = this;
		PreviewDebugController.register();
		registerCommands();
		if (this.config.refreshOnStartup && this.config.isConfigured()) {
			this.slackEmojiService.refreshAsync();
		}
	}

	private void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
			ClientCommands.literal(MOD_ID)
				.executes(context -> {
					this.config.reload();
					context.getSource().sendFeedback(Component.literal(this.slackEmojiService.describeStatus()));
					return Command.SINGLE_SUCCESS;
				})
				.then(ClientCommands.literal("config")
					.executes(context -> {
						Minecraft minecraft = Minecraft.getInstance();
						minecraft.execute(() -> minecraft.setScreen(new MinemojiConfigScreen(null)));
						return Command.SINGLE_SUCCESS;
					})
				)
				.then(ClientCommands.literal("refresh")
					.executes(context -> {
						this.config.reload();
						if (!this.config.isConfigured()) {
							this.slackEmojiService.clear();
							context.getSource().sendError(Component.literal("Set slackToken in " + this.config.path() + " first."));
							return 0;
						}

						context.getSource().sendFeedback(Component.literal("Refreshing Slack emoji..."));
						this.slackEmojiService.refreshAsync()
							.whenComplete((result, throwable) -> Minecraft.getInstance().execute(() -> reportRefresh(context.getSource()::sendFeedback, context.getSource()::sendError, result, throwable)));
						return Command.SINGLE_SUCCESS;
					})
				)
		));
	}

	private void reportRefresh(java.util.function.Consumer<Component> feedbackConsumer, java.util.function.Consumer<Component> errorConsumer, SlackEmojiRefreshResult result, Throwable throwable) {
		if (throwable != null) {
			errorConsumer.accept(Component.literal("Slack refresh failed: " + describeThrowable(throwable)));
			return;
		}

		if (result.success()) {
			feedbackConsumer.accept(Component.literal(result.message()));
			return;
		}

		errorConsumer.accept(Component.literal(result.message()));
	}

	private static String describeThrowable(Throwable throwable) {
		String message = throwable.getMessage();
		if (message != null && !message.isBlank()) {
			return message;
		}
		return throwable.getClass().getSimpleName();
	}
}
