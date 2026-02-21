package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.ClientTeamData;
import com.example.examplemod.network.PacketChooseTeam;
import com.example.examplemod.network.PacketHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class TeamSelectScreen extends Screen {

    private static final ResourceLocation TEAM1_TEX = new ResourceLocation(ExampleMod.MODID, "textures/gui/team1_card.png");
    private static final ResourceLocation TEAM2_TEX = new ResourceLocation(ExampleMod.MODID, "textures/gui/team2_card.png");

    public TeamSelectScreen() {
        super(Component.literal("Выберите группировку"));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        int cardW = 140;
        int cardH = 220;
        int gap = 60;

        int startX = this.width / 2 - cardW - (gap / 2);
        int startY = this.height / 2 - (cardH / 2);

        this.addRenderableWidget(new TeamCardButton(startX, startY, cardW, cardH, TEAM1_TEX, (btn) -> {
            PacketHandler.INSTANCE.sendToServer(new PacketChooseTeam(1));
            Minecraft.getInstance().setScreen(new KitSelectorScreen());
        }));

        this.addRenderableWidget(new TeamCardButton(startX + cardW + gap, startY, cardW, cardH, TEAM2_TEX, (btn) -> {
            PacketHandler.INSTANCE.sendToServer(new PacketChooseTeam(2));
            Minecraft.getInstance().setScreen(new KitSelectorScreen());
        }));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // === Отрисовка количества игроков ===
        int cardW = 140;
        int gap = 60;
        int startX = this.width / 2 - cardW - (gap / 2);
        int textY = this.height / 2 + (220 / 2) + 15;

        Minecraft mc = Minecraft.getInstance();

        // Считаем только тех игроков Одиночек, которые сейчас на сервере
        int t1Count = 0;
        for (java.util.UUID uuid : ClientTeamData.team1Players) {
            if (mc.getConnection() != null && mc.getConnection().getPlayerInfo(uuid) != null) {
                t1Count++;
            }
        }

        // Считаем только тех игроков Бандитов, которые сейчас на сервере
        int t2Count = 0;
        for (java.util.UUID uuid : ClientTeamData.team2Players) {
            if (mc.getConnection() != null && mc.getConnection().getPlayerInfo(uuid) != null) {
                t2Count++;
            }
        }

        guiGraphics.drawCenteredString(this.font, "Игроков: " + t1Count, startX + cardW / 2, textY, 0xAAAAAA);
        guiGraphics.drawCenteredString(this.font, "Игроков: " + t2Count, startX + cardW + gap + cardW / 2, textY, 0xAAAAAA);
    }

    // === Внутренний класс для анимированной карточки ===
    private class TeamCardButton extends Button {
        private final ResourceLocation texture;

        // Текущие значения для плавной анимации
        private float hoverOffset = 0;
        private float scale = 1.0f;
        private float colorTint = 0.8f;

        public TeamCardButton(int x, int y, int width, int height, ResourceLocation texture, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.texture = texture;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // 1. Целевые значения (к чему стремится анимация)
            float targetOffset = isHovered ? -8f : 0f;        // Всплытие на 8 пикселей
            float targetScale = isHovered ? 1.05f : 1.0f;     // Увеличение на 5%
            float targetColor = isHovered ? 1.0f : 0.8f;      // Осветление до 100%

            // 2. Плавная математика (коэффициент 0.15f делает движение мягким)
            this.hoverOffset = Mth.lerp(0.15f, this.hoverOffset, targetOffset);
            this.scale = Mth.lerp(0.15f, this.scale, targetScale);
            this.colorTint = Mth.lerp(0.15f, this.colorTint, targetColor);

            RenderSystem.enableBlend();
            // Устанавливаем плавный цвет (затемнение/осветление)
            RenderSystem.setShaderColor(this.colorTint, this.colorTint, this.colorTint, 1.0f);

            guiGraphics.pose().pushPose();

            // Находим центр карточки, чтобы масштабировать ровно от центра
            float centerX = getX() + width / 2.0f;
            float centerY = getY() + height / 2.0f;

            // Сдвигаем матрицу к центру, применяем смещение Y, масштабируем и возвращаем обратно
            guiGraphics.pose().translate(centerX, centerY + this.hoverOffset, 0);
            guiGraphics.pose().scale(this.scale, this.scale, 1.0f);
            guiGraphics.pose().translate(-centerX, -centerY, 0);

            // Рисуем текстуру (она растянется/сдвинется автоматически благодаря матрице выше)
            guiGraphics.blit(texture, getX(), getY(), 0, 0, width, height, width, height);

            guiGraphics.pose().popPose();

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // Сброс цвета
            RenderSystem.disableBlend();
        }
    }
}