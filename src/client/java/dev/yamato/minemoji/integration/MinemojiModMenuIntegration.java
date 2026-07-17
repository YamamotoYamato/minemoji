package dev.yamato.minemoji.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.yamato.minemoji.screen.MinemojiConfigScreen;

public final class MinemojiModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return MinemojiConfigScreen::new;
	}
}

