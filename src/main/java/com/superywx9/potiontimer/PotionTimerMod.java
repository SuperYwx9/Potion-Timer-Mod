package com.superywx9.potiontimer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class PotionTimerMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        HudRenderCallback.EVENT.register((guiGraphics, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) return;

            MobEffectInstance effect = mc.player.getEffect(MobEffects.WEAKNESS);
            if (effect != null && effect.getDuration() > 0) {
                drawTimer(guiGraphics, mc, effect);
            }
        });
    }

    private void drawTimer(GuiGraphics guiGraphics, Minecraft mc, MobEffectInstance effect) {
        int seconds = effect.getDuration() / 20;
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;

        String text;
        if (minutes > 0) {
            text = String.format("%d:%02d", minutes, remainingSeconds);
        } else {
            text = seconds + " сек";
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int textWidth = mc.font.width(text);
        int x = screenWidth / 2 - textWidth / 2;
        int y = screenHeight / 2 + 50;

        int textColor = getColor(seconds);

        guiGraphics.drawString(
                mc.font,
                Component.literal(text),
                x,
                y,
                textColor,
                false
        );
    }

    private int getColor(int seconds) {
        if (seconds > 60) {
            return 0xFF00FF00;
        } else if (seconds > 30) {
            return 0xFFFFFF00;
        } else {
            return 0xFFFF0000;
        }
    }
}