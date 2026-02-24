package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.ClientTeamData; // Импорт для currentGameMode
import com.example.examplemod.network.PacketHandler;
import com.example.examplemod.network.PacketSelectKit;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class KitSelectorScreen extends Screen {

    private static final ResourceLocation BANNER_TEXTURE = new ResourceLocation(ExampleMod.MODID + ":textures/gui/kit_banner.png");

    private static final ResourceLocation[] KIT_TEXTURES = {
            new ResourceLocation(ExampleMod.MODID + ":textures/gui/kits/kit_ar.png"),
            new ResourceLocation(ExampleMod.MODID + ":textures/gui/kits/kit_smg.png"),
            new ResourceLocation(ExampleMod.MODID + ":textures/gui/kits/kit_sg.png"),
            new ResourceLocation(ExampleMod.MODID + ":textures/gui/kits/kit_sr.png")
    };
    private static final ResourceLocation[] KIT_TEXTURES_HOVER = {
            new ResourceLocation(ExampleMod.MODID + ":textures/gui/kits/kit_ar_hover.png"),
            new ResourceLocation(ExampleMod.MODID + ":textures/gui/kits/kit_smg_hover.png"),
            new ResourceLocation(ExampleMod.MODID + ":textures/gui/kits/kit_sg_hover.png"),
            new ResourceLocation(ExampleMod.MODID + ":textures/gui/kits/kit_sr_hover.png")
    };

    public KitSelectorScreen() {
        super(Component.literal("Выберите снаряжение"));
    }

    @Override
    protected void init() {
        super.init();
        int cardWidth = 80;
        int cardHeight = 180;
        int gap = 20;
        int totalWidth = (cardWidth * 4) + (gap * 3);
        int startX = (this.width - totalWidth) / 2;
        int startY = this.height / 2 - cardHeight / 2 + 20;

        for (int i = 0; i < 4; i++) {
            int finalId = i; // ИСПРАВЛЕНО: Объявляем финальный ID для использования в лямбде

            // Логика нового режима магазина
            boolean isShopMode = ClientTeamData.currentGameMode.equals("shop");
            boolean hasKit = ClientKitData.availableKits.containsKey("kit" + i);

            ImageCardButton btn = new ImageCardButton(startX + (cardWidth + gap) * i, startY, cardWidth, cardHeight, finalId, (b) -> {
                PacketHandler.INSTANCE.sendToServer(new PacketSelectKit(finalId));
                this.onClose();
            });

            // В режиме магазина кнопки всегда активны, в других - только если кит настроен
            btn.active = isShopMode || hasKit;
            this.addRenderableWidget(btn);
        }

        this.addRenderableWidget(new ModernButton(this.width / 2 - 50, this.height - 30, 100, 20, Component.literal("Закрыть"), (btn) -> this.onClose()));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int bannerWidth = 200; int bannerHeight = 50;
        guiGraphics.blit(BANNER_TEXTURE, (this.width - bannerWidth) / 2, 10, 0, 0, bannerWidth, bannerHeight, bannerWidth, bannerHeight);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private class ImageCardButton extends Button {
        private final int kitId;
        private float hoverOffset = 0;

        public ImageCardButton(int x, int y, int width, int height, int id, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.kitId = id;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            float targetOffset = (isHovered && active) ? -10f : 0f;
            this.hoverOffset = Mth.lerp(0.15f, this.hoverOffset, targetOffset);

            RenderSystem.enableBlend();
            ResourceLocation texture = (isHovered && active) ? KIT_TEXTURES_HOVER[kitId] : KIT_TEXTURES[kitId];

            if (!active) {
                RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 0.7f);
            } else {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, this.hoverOffset, 0);
            guiGraphics.blit(texture, getX(), getY(), 0, 0, width, height, width, height);
            guiGraphics.pose().popPose();

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
    }
}