package com.superywx9.potiontimer.integration;

import com.superywx9.potiontimer.config.ConfigManager;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return ConfigManager::createConfigScreen;
    }
}