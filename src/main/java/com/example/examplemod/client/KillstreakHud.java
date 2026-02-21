package com.example.examplemod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class KillstreakHud implements IGuiOverlay {

    public static final KillstreakHud INSTANCE = new KillstreakHud();

    // Состояние
    private static String currentTitle = "";
    private static String currentSubtitle = "";
    private static int currentColor = 0xFFFFFF;
    private static long showTime = 0;

    // Метод для запуска анимации (вызывается из сетевого пакета)
    public static void trigger(String title, String subtitle, int color) {
        currentTitle = title;
        currentSubtitle = subtitle;
        currentColor = color;
        showTime = Util.getMillis(); // Запоминаем время старта анимации
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        long timePassed = Util.getMillis() - showTime;

        // Настройка таймингов (в миллисекундах)
        int appearDuration = 250;   // Быстрое появление с отскоком
        int displayDuration = 3000; // Сколько времени висит на экране
        int fadeDuration = 500;     // Плавное исчезновение
        int totalDuration = displayDuration + fadeDuration;

        // Если время вышло или текста нет - не рисуем
        if (timePassed > totalDuration || currentTitle.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int centerX = screenWidth / 2;
        int targetY = 45; // Целевая позиция Y под основным худом

        // Начальные значения для анимации
        float alpha = 1.0f;
        float scale = 1.0f;
        float yOffset = 0f;

        // --- ФАЗА 1: АНИМАЦИЯ ПОЯВЛЕНИЯ (Слайд вниз + Увеличение с отскоком) ---
        if (timePassed < appearDuration) {
            float progress = (float) timePassed / appearDuration;

            // Математическая функция для эффекта пружинящего "отскока" (easeOutBack)
            float back = 1.7f;
            scale = (float) ((progress - 1) * progress * ((back + 1) * progress + back) + 1);

            // Слайд сверху вниз (начинаем на 20 пикселей выше)
            yOffset = -20f * (1.0f - easeOutCubic(progress));
            // Плавное появление прозрачности
            alpha = easeOutCubic(progress);

        }
        // --- ФАЗА 2: АНИМАЦИЯ ИСЧЕЗНОВЕНИЯ (Слайд вверх + Растворение) ---
        else if (timePassed > displayDuration) {
            float progress = (float) (timePassed - displayDuration) / fadeDuration;
            // Растворяем
            alpha = 1.0f - progress;
            // Слайд немного вверх при исчезновении (на 10 пикселей)
            yOffset = -10f * easeOutCubic(progress);
        }

        // Ограничиваем значения, чтобы избежать визуальных артефактов
        scale = Mth.clamp(scale, 0.0f, 1.5f);
        alpha = Mth.clamp(alpha, 0.0f, 1.0f);

        // Если почти прозрачное - перестаем рисовать
        if (alpha < 0.05f) return;

        // Применяем альфа-канал к цветам
        int alphaInt = (int) (alpha * 255);
        int textColor = (alphaInt << 24) | (currentColor & 0x00FFFFFF);
        int subTextColor = (alphaInt << 24) | 0xAAAAAA; // Светло-серый для подзаголовка
        int bgColor = ((int)(alpha * 230) << 24) | 0x1A1A1A; // Плотный темный фон
        int lineColor = (alphaInt << 24) | (currentColor & 0x00FFFFFF); // Цветная линия

        // --- НАЧАЛО ОТРИСОВКИ С ТРАНСФОРМАЦИЯМИ ---
        guiGraphics.pose().pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Вычисляем центр плашки для правильного масштабирования
        float centerY = targetY + 13;

        // Применяем цепочку трансформаций: Сдвиг -> Масштаб -> Обратный сдвиг
        guiGraphics.pose().translate(centerX, centerY + yOffset, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.pose().translate(-centerX, -centerY, 0);

        // Размеры плашки
        int titleWidth = mc.font.width(currentTitle);
        int subWidth = mc.font.width(currentSubtitle);
        int boxWidth = Math.max(titleWidth, subWidth) + 50; // Делаем пошире для солидности
        int boxHeight = 26;
        int startX = centerX - boxWidth / 2;

        // 1. Рисуем фон
        guiGraphics.fill(startX, targetY, startX + boxWidth, targetY + boxHeight, bgColor);

        // 2. Рисуем толстую акцентную линию слева
        guiGraphics.fill(startX, targetY, startX + 4, targetY + boxHeight, lineColor);

        // 3. Рисуем текст с кастомной тенью для объема
        drawShadowedString(guiGraphics, mc, currentTitle, centerX, targetY + 5, textColor);
        drawShadowedString(guiGraphics, mc, currentSubtitle, centerX, targetY + 16, subTextColor);

        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
        // --- КОНЕЦ ОТРИСОВКИ ---
    }

    // Вспомогательная функция плавности (быстрый старт, медленный конец)
    private float easeOutCubic(float x) {
        return 1.0f - (float) Math.pow(1.0f - x, 3);
    }

    // Вспомогательная функция для рисования текста с правильной прозрачной тенью
    private void drawShadowedString(GuiGraphics gui, Minecraft mc, String text, int x, int y, int color) {
        // Вычисляем цвет тени на основе основного цвета
        int shadowColor = (color & 0xFCFCFC) >> 2 | (color & 0xFF000000);
        // Сначала рисуем тень со смещением
        gui.drawCenteredString(mc.font, text, x + 1, y + 1, shadowColor);
        // Потом основной текст поверх
        gui.drawCenteredString(mc.font, text, x, y, color);
    }
}