package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.ClientTeamData;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class ScoreHud implements IGuiOverlay {

    public static final ScoreHud INSTANCE = new ScoreHud();

    // Пути к эмблемам
    private static final ResourceLocation LOGO_T1 = new ResourceLocation(ExampleMod.MODID + ":textures/gui/logo_t1.png");
    private static final ResourceLocation LOGO_T2 = new ResourceLocation(ExampleMod.MODID + ":textures/gui/logo_t2.png");

    // Цвета
    private static final int COLOR_T1 = 0xFFd19a36; // Золотистый/Оранжевый
    private static final int COLOR_T2 = 0xFF5d79ae; // Синий
    private static final int COLOR_BG = 0xFF2a2a2a; // Темно-серый фон для текста
    private static final int COLOR_BORDER = 0xFF555555; // Цвет рамки

    // Переменные для анимации
    private int lastScore1 = 0;
    private int lastScore2 = 0;
    private long lastChangeTime1 = 0;
    private long lastChangeTime2 = 0;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (Minecraft.getInstance().player == null) return;

        int s1 = ClientTeamData.clientScore1;
        int s2 = ClientTeamData.clientScore2;
        long currentTime = Util.getMillis();

        if (s1 > lastScore1) { lastScore1 = s1; lastChangeTime1 = currentTime; }
        if (s2 > lastScore2) { lastScore2 = s2; lastChangeTime2 = currentTime; }

        int centerX = screenWidth / 2;
        int y = 10;

        int nameWidth = 75; // Немного увеличим, чтобы влезло и лого, и текст
        int scoreWidth = 30;
        int height = 20;
        int totalWidth = (nameWidth + scoreWidth) * 2;
        int startX = centerX - totalWidth / 2;

        guiGraphics.fill(startX - 1, y - 1, startX + totalWidth + 1, y + height + 1, COLOR_BORDER);
        guiGraphics.fill(startX, y, startX + nameWidth, y + height, COLOR_BG);
        guiGraphics.fill(startX + nameWidth + scoreWidth + scoreWidth, y, startX + totalWidth, y + height, COLOR_BG);

        Minecraft mc = Minecraft.getInstance();

        // --- ОДИНОЧКИ (Левая панель) ---
        // Эмблема слева
        guiGraphics.blit(LOGO_T1, startX + 2, y + 2, 0, 0, 16, 16, 16, 16);
        // Текст после эмблемы
        guiGraphics.drawString(mc.font, "ОДИНОЧКИ", startX + 20, y + 6, COLOR_T1, false);

        // Блок счета (оранжевый)
        renderScoreBlock(guiGraphics, mc, startX + nameWidth, y, scoreWidth, height, s1, COLOR_T1, currentTime - lastChangeTime1);

        // --- БАНДОСЫ (Правая панель) ---
        // Блок счета (синий)
        renderScoreBlock(guiGraphics, mc, startX + nameWidth + scoreWidth, y, scoreWidth, height, s2, COLOR_T2, currentTime - lastChangeTime2);

        // Начало правой текстовой панели
        int team2StartX = startX + nameWidth + scoreWidth + scoreWidth;
        // Эмблема слева
        guiGraphics.blit(LOGO_T2, team2StartX + 2, y + 2, 0, 0, 16, 16, 16, 16);
        // Текст после эмблемы
        guiGraphics.drawString(mc.font, "БАНДОСЫ", team2StartX + 20, y + 6, COLOR_T2, false);

        // Прогресс-бар для режима Удержания
        if (com.example.examplemod.network.ClientTeamData.currentGameMode.equals("domination")) {
            int barWidth = 180; // Сделаем чуть шире
            int barHeight = 10;
            int barStartX = centerX - barWidth / 2;
            int barStartY = 35; // Чуть ниже основного счета

            int bar_s1 = com.example.examplemod.network.ClientTeamData.clientScore1;
            int bar_s2 = com.example.examplemod.network.ClientTeamData.clientScore2;

            // Получаем цель из данных клиента
            int target = com.example.examplemod.network.ClientTeamData.clientTargetScore;
            if (target <= 0) target = 100; // Защита от деления на ноль

            // Вычисляем, какую часть полоски занимает каждая команда
            float ratio1 = Math.min(1.0f, (float) bar_s1 / target);
            float ratio2 = Math.min(1.0f, (float) bar_s2 / target);

            int fill1 = (int) (barWidth * ratio1);
            int fill2 = (int) (barWidth * ratio2);

            // 1. Фон (Темная подложка)
            guiGraphics.fill(barStartX - 2, barStartY - 2, barStartX + barWidth + 2, barStartY + barHeight + 2, 0xAA000000);

            // 2. Полоска Одиночек (Заполняется слева)
            if (fill1 > 0) {
                guiGraphics.fill(barStartX, barStartY, barStartX + fill1, barStartY + barHeight, 0xFFFFA500);
            }

            // 3. Полоска Бандосов (Заполняется справа навстречу)
            if (fill2 > 0) {
                guiGraphics.fill(barStartX + barWidth - fill2, barStartY, barStartX + barWidth, barStartY + barHeight, 0xFF0000FF);
            }

            // 4. Текстовые индикаторы очков поверх полоски
            String scoreText = s1 + " / " + target + "  |  " + s2 + " / " + target;
            guiGraphics.drawCenteredString(mc.font, scoreText, centerX, barStartY + 1, 0xFFFFFF);

            // 5. Статус владельца зоны (текст под баром)
            String status = "ЗОНА: ";
            int color = 0xAAAAAA;
            int owner = com.example.examplemod.network.ClientTeamData.clientZoneOwner;
            if (owner == 1) { status += "ПОД КОНТРОЛЕМ ОДИНОЧЕК"; color = 0xFFFFA500; }
            else if (owner == 2) { status += "ПОД КОНТРОЛЕМ БАНДОСОВ"; color = 0xFF0000FF; }
            else { status += "НЕЙТРАЛЬНА"; }

            guiGraphics.drawCenteredString(mc.font, status, centerX, barStartY + 14, color);
        }
    }

    // Метод для отрисовки цветного блока со счетом и анимацией
    private void renderScoreBlock(GuiGraphics gui, Minecraft mc, int x, int y, int w, int h, int score, int color, long timePassed) {
        // Фон цвета команды
        gui.fill(x, y, x + w, y + h, color);

        // Анимация вспышки (белая, поверх цвета)
        if (timePassed < 1000) {
            float alpha = 1.0f - (timePassed / 1000f);
            int flashColor = ((int)(alpha * 150) << 24) | 0xFFFFFF;
            gui.fill(x, y, x + w, y + h, flashColor);
        }

        // Цифра счета
        gui.drawCenteredString(mc.font, String.valueOf(score), x + w / 2, y + 6, 0xFFFFFF);
    }
}