package com.example.examplemod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ModernButton extends Button {

    // Цвета (ARGB)
    private static final int COLOR_NORMAL = 0xAA000000; // Черный полупрозрачный
    private static final int COLOR_HOVER = 0xDD404040;  // Серый при наведении
    private static final int COLOR_BORDER = 0xFFFFFFFF; // Белая рамка

    public ModernButton(int x, int y, int width, int height, Component title, OnPress onPress) {
        super(x, y, width, height, title, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Определяем цвет фона: если мышка наведена, то светлее
        int bgColor = this.isHovered ? COLOR_HOVER : COLOR_NORMAL;

        // Рисуем фон
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

        // Рисуем тонкую рамку
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + 1, COLOR_BORDER); // Верх
        guiGraphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, COLOR_BORDER); // Низ
        guiGraphics.fill(getX(), getY(), getX() + 1, getY() + height, COLOR_BORDER); // Лево
        guiGraphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, COLOR_BORDER); // Право

        // Рисуем текст по центру
        int textColor = this.active ? 0xFFFFFF : 0xA0A0A0; // Белый или серый (если неактивна)
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), getX() + width / 2, getY() + (height - 8) / 2, textColor);
    }
}