package com.superywx9.potiontimer.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;

@Environment(EnvType.CLIENT)
public class ConfigManager {

    private static ConfigHolder<ModConfig> configHolder;

    public static void initialize() {
        configHolder = AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
    }

    public static ModConfig getConfig() {
        return configHolder.getConfig();
    }

    public static Screen createConfigScreen(Screen parent) {
        return AutoConfig.getConfigScreen(ModConfig.class, parent).get();
    }

    public static void save() {
        configHolder.save();
    }
}