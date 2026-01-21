package com.superywx9.potiontimer;

import com.superywx9.potiontimer.config.ConfigManager;
import com.superywx9.potiontimer.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.lwjgl.glfw.GLFW;

public class PotionTimerMod implements ClientModInitializer {

    private static KeyMapping configKeyMapping;
    private int lastPlayedSoundAt = -1;
    private long lastSoundTime = 0;
    private static final long SOUND_COOLDOWN_MS = 500;

    @Override
    public void onInitializeClient() {
        ConfigManager.initialize();

        configKeyMapping = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.potiontimer.config",
                GLFW.GLFW_KEY_P,
                "category.potiontimer"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configKeyMapping.consumeClick()) {
                client.setScreen(ConfigManager.createConfigScreen(client.screen));
            }
        });

        HudRenderCallback.EVENT.register((guiGraphics, delta) -> {
            renderTimer(guiGraphics);
        });
    }

    private void renderTimer(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        MobEffectInstance effect = mc.player.getEffect(MobEffects.WEAKNESS);
        if (effect == null || effect.getDuration() <= 0) {
            lastPlayedSoundAt = -1;
            return;
        }

        int totalSeconds = effect.getDuration() / 20;

        ModConfig config = ConfigManager.getConfig();

        checkAndPlaySound(config, totalSeconds);

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Рисуем основной таймер если включен
        if (config.showTimer) {
            renderMainTimer(guiGraphics, config, totalSeconds, screenWidth, screenHeight);
        }

        // Рисуем прогресс-бар в стиле полосы здоровья боссов
        if (config.showProgressBar) {
            renderBossStyleProgressBar(guiGraphics, config, totalSeconds, screenWidth, screenHeight);
        }

        // Рисуем виньетку если время меньше порога
        if (config.vignetteEffect && totalSeconds <= config.vignetteThreshold) {
            drawVignette(guiGraphics, screenWidth, screenHeight, config, totalSeconds);
        }
    }

    private void renderMainTimer(GuiGraphics guiGraphics, ModConfig config, int totalSeconds, int screenWidth, int screenHeight) {
        int minutes = totalSeconds / 60;
        int remainingSeconds = totalSeconds % 60;

        String text;
        if (minutes > 0) {
            text = String.format("%d:%02d", minutes, remainingSeconds);
        } else {
            String secondsText = I18n.get("text.potiontimer.seconds", totalSeconds);
            text = secondsText;
        }

        int textWidth = Minecraft.getInstance().font.width(text);
        int textHeight = Minecraft.getInstance().font.lineHeight;

        int x = calculateXPosition(config.posX, screenWidth, textWidth);
        int y = calculateYPosition(config.posY, screenHeight, textHeight);

        int textColor = getColor(totalSeconds);

        // Рисуем текст таймера
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.literal(text),
                x,
                y,
                textColor,
                false
        );
    }

    private void renderBossStyleProgressBar(GuiGraphics guiGraphics, ModConfig config, int currentSeconds, int screenWidth, int screenHeight) {
        int barWidth = config.progressBarWidth;
        int barHeight = config.progressBarHeight;

        // Рассчитываем позицию (центрирование по X если progressBarX = -1)
        int barX;
        if (config.progressBarX < 0) {
            barX = (screenWidth - barWidth) / 2;
        } else {
            int minX = 5;
            int maxX = Math.max(minX, screenWidth - barWidth - 5);
            barX = Math.max(minX, Math.min(config.progressBarX, maxX));
        }

        int barY;
        int minY = 5;
        int maxY = Math.max(minY, screenHeight - barHeight - 5);
        barY = Math.max(minY, Math.min(config.progressBarY, maxY));

        // Максимальное время - 8 минут = 480 секунд
        int maxSeconds = 480;
        float progress = Math.min(1.0f, currentSeconds / (float) maxSeconds);

        // Ширина заполненной части
        int filledWidth = (int)(barWidth * progress);

        // ФИКСИРОВАННЫЙ ЦВЕТ ФОНА (темно-серый, как у боссов Minecraft)
        int backgroundColor = 0xFF404040; // Темно-серый, непрозрачный

        // Рисуем фон (черная рамка как у боссов)
        // Внешняя темная рамка
        guiGraphics.fill(
                barX - 1, barY - 1,
                barX + barWidth + 1, barY + barHeight + 1,
                0xFF000000
        );

        // Фон полосы (фиксированный темно-серый цвет)
        guiGraphics.fill(
                barX, barY,
                barX + barWidth, barY + barHeight,
                backgroundColor
        );

        // Рисуем заполненную часть
        if (filledWidth > 0) {
            // Динамический цвет: зеленый → желтый → красный
            int color = getBossBarColor(progress);
            guiGraphics.fill(
                    barX, barY,
                    barX + filledWidth, barY + barHeight,
                    color
            );

            // Добавляем светлую полоску сверху для эффекта объема
            guiGraphics.fill(
                    barX, barY,
                    barX + filledWidth, barY + 1,
                    0x80FFFFFF
            );
        }
    }

    private int getBossBarColor(float progress) {
        // Цвет как у полос здоровья боссов Minecraft
        if (progress > 0.5f) {
            // Зеленый для >50%
            return 0xFF00FF00;
        } else if (progress > 0.25f) {
            // Желтый для 25-50%
            return 0xFFFFFF00;
        } else if (progress > 0.1f) {
            // Оранжевый для 10-25%
            return 0xFFFFA500;
        } else {
            // Красный для <10%
            return 0xFFFF0000;
        }
    }

    private void checkAndPlaySound(ModConfig config, int currentSeconds) {
        if (!config.soundNotification) return;

        long currentTime = System.currentTimeMillis();

        if (currentSeconds <= config.soundThreshold &&
                currentSeconds != lastPlayedSoundAt &&
                (currentTime - lastSoundTime) > SOUND_COOLDOWN_MS) {

            Minecraft mc = Minecraft.getInstance();
            SoundManager soundManager = mc.getSoundManager();

            float volume = config.soundVolume / 100.0f;
            float pitch = 1.0f;

            SimpleSoundInstance sound = SimpleSoundInstance.forUI(
                    SoundEvents.NOTE_BLOCK_PLING.value(),
                    pitch,
                    volume
            );

            soundManager.play(sound);
            lastPlayedSoundAt = currentSeconds;
            lastSoundTime = currentTime;
        }

        if (currentSeconds > config.soundThreshold) {
            lastPlayedSoundAt = -1;
        }
    }

    private void drawVignette(GuiGraphics guiGraphics, int screenWidth, int screenHeight, ModConfig config, int currentSeconds) {
        float intensity = 1.0f - (currentSeconds / (float) config.vignetteThreshold);

        int gradientWidth = 10 + (int)(30 * intensity);
        int maxAlpha = (int)((config.vignetteOpacity / 100.0f) * 255 * intensity);

        int baseColor = config.vignetteColor;
        int baseRed = (baseColor >> 16) & 0xFF;
        int baseGreen = (baseColor >> 8) & 0xFF;
        int baseBlue = baseColor & 0xFF;

        for (int i = 0; i < gradientWidth; i++) {
            float progress = i / (float) gradientWidth;
            float alphaMultiplier = 1.0f - (progress * progress);

            int alpha = (int) (maxAlpha * alphaMultiplier);
            if (alpha > 255) alpha = 255;

            int color = (alpha << 24) | (baseRed << 16) | (baseGreen << 8) | baseBlue;

            guiGraphics.fill(0, i, screenWidth, i + 1, color);
            guiGraphics.fill(0, screenHeight - i - 1, screenWidth, screenHeight - i, color);
            guiGraphics.fill(i, 0, i + 1, screenHeight, color);
            guiGraphics.fill(screenWidth - i - 1, 0, screenWidth - i, screenHeight, color);
        }

        for (int i = 0; i < gradientWidth / 2; i++) {
            for (int j = 0; j < gradientWidth / 2; j++) {
                float distance = (float) Math.sqrt(i * i + j * j);
                float maxDistance = gradientWidth / 2.0f;
                float cornerProgress = distance / maxDistance;

                if (cornerProgress <= 1.0f) {
                    float alphaMultiplier = 1.0f - cornerProgress;
                    int alpha = (int) (maxAlpha * alphaMultiplier * 1.2f);
                    if (alpha > 255) alpha = 255;

                    int color = (alpha << 24) | (baseRed << 16) | (baseGreen << 8) | baseBlue;

                    guiGraphics.fill(i, j, i + 1, j + 1, color);
                    guiGraphics.fill(screenWidth - i - 1, j, screenWidth - i, j + 1, color);
                    guiGraphics.fill(i, screenHeight - j - 1, i + 1, screenHeight - j, color);
                    guiGraphics.fill(screenWidth - i - 1, screenHeight - j - 1, screenWidth - i, screenHeight - j, color);
                }
            }
        }

        if (currentSeconds < 5) {
            float pulseTime = System.currentTimeMillis() / 1000.0f;
            float pulse = (float) (Math.sin(pulseTime * Math.PI * 2) * 0.3 + 0.7);

            int pulseAlpha = (int)(maxAlpha * pulse);
            if (pulseAlpha > 255) pulseAlpha = 255;

            for (int i = 0; i < 3; i++) {
                int alpha = (int)(pulseAlpha * (1.0f - i / 3.0f));
                int color = (alpha << 24) | (0xFF << 16) | (0x40 << 8) | 0x40;

                guiGraphics.fill(0, i, screenWidth, i + 1, color);
                guiGraphics.fill(0, screenHeight - i - 1, screenWidth, screenHeight - i, color);
                guiGraphics.fill(i, 0, i + 1, screenHeight, color);
                guiGraphics.fill(screenWidth - i - 1, 0, screenWidth - i, screenHeight, color);
            }
        }
    }

    private int calculateXPosition(int configX, int screenWidth, int textWidth) {
        if (configX < 0) {
            return Math.max(5, screenWidth / 2 - textWidth / 2);
        } else {
            int minX = 5;
            int maxX = Math.max(minX, screenWidth - textWidth - 5);
            return Math.max(minX, Math.min(configX, maxX));
        }
    }

    private int calculateYPosition(int configY, int screenHeight, int textHeight) {
        int minY = 5;
        int maxY = Math.max(minY, screenHeight - textHeight - 5);
        return Math.max(minY, Math.min(configY, maxY));
    }

    private int getColor(int seconds) {
        if (seconds > 60) return 0xFF00FF00;
        if (seconds > 30) return 0xFFFFFF00;
        return 0xFFFF0000;
    }
}
