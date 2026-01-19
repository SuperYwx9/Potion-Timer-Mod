package com.superywx9.potiontimer.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "potiontimer")
public class ModConfig implements ConfigData {

    @ConfigEntry.Category("display")
    @ConfigEntry.Gui.Tooltip(count = 2)
    @ConfigEntry.BoundedDiscrete(min = -1, max = 2000)
    public int posX = -1;

    @ConfigEntry.Category("display")
    @ConfigEntry.Gui.Tooltip(count = 2)
    @ConfigEntry.BoundedDiscrete(min = -1, max = 2000)
    public int posY = 300;

    @ConfigEntry.Category("sound")
    @ConfigEntry.Gui.Tooltip
    public boolean soundNotification = true;

    @ConfigEntry.Category("sound")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 60)
    public int soundThreshold = 10;

    @ConfigEntry.Category("sound")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int soundVolume = 100;

    @ConfigEntry.Category("effects")
    @ConfigEntry.Gui.Tooltip
    public boolean vignetteEffect = true;

    @ConfigEntry.Category("effects")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 30)
    public int vignetteThreshold = 10;

    @ConfigEntry.Category("effects")
    @ConfigEntry.ColorPicker(allowAlpha = false)
    public int vignetteColor = 0xFF0000;

    @ConfigEntry.Category("effects")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int vignetteOpacity = 40;
}