package com.example.examplemod.client;

import com.example.examplemod.network.PacketHandler;
import com.example.examplemod.network.PacketSaveKit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class KitEditorScreen extends Screen {

    public KitEditorScreen() {
        super(Component.literal("Редактор Наборов"));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    protected void init() {
        super.init();
        int midX = this.width / 2;
        int startY = 60;
        int btnWidth = 200;
        int btnHeight = 20;
        int gap = 5;

        // Кнопки для сохранения в фиксированные слоты
        this.addRenderableWidget(new ModernButton(midX - btnWidth / 2, startY, btnWidth, btnHeight, Component.literal("Сохранить как: ШТУРМОВИК"), (btn) -> {
            sendSavePacket(0);
        }));

        startY += btnHeight + gap;
        this.addRenderableWidget(new ModernButton(midX - btnWidth / 2, startY, btnWidth, btnHeight, Component.literal("Сохранить как: ПИСТОЛЕТ-ПУЛЕМЁТ"), (btn) -> {
            sendSavePacket(1);
        }));

        startY += btnHeight + gap;
        this.addRenderableWidget(new ModernButton(midX - btnWidth / 2, startY, btnWidth, btnHeight, Component.literal("Сохранить как: ДРОБОВИК"), (btn) -> {
            sendSavePacket(2);
        }));

        startY += btnHeight + gap;
        this.addRenderableWidget(new ModernButton(midX - btnWidth / 2, startY, btnWidth, btnHeight, Component.literal("Сохранить как: СНАЙПЕР"), (btn) -> {
            sendSavePacket(3);
        }));

        // Кнопка закрыть
        this.addRenderableWidget(new ModernButton(midX - 50, this.height - 30, 100, 20, Component.literal("Закрыть"), (btn) -> {
            this.onClose();
        }));
    }

    private void sendSavePacket(int id) {
        PacketHandler.INSTANCE.sendToServer(new PacketSaveKit(id));
        Minecraft.getInstance().player.displayClientMessage(Component.literal("Набор сохранен в слот " + (id + 1)), true);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, "Возьмите предметы и выберите слот для сохранения", this.width / 2, 30, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}