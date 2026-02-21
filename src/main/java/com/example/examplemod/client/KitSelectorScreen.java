package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.PacketHandler;
import com.example.examplemod.network.PacketSelectKit;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class KitSelectorScreen extends Screen {

    private static final ResourceLocation BANNER_TEXTURE = new ResourceLocation(ExampleMod.MODID + ":textures/gui/kit_banner.png");

    // Пути к текстурам карточек (обычные и hover)
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
        int cardWidth = 80;  // Ширина карточки (подберите под ваши текстуры)
        int cardHeight = 180; // Высота карточки
        int gap = 20; // Расстояние между карточками
        int totalWidth = (cardWidth * 4) + (gap * 3);
        int startX = (this.width - totalWidth) / 2;
        int startY = this.height / 2 - cardHeight / 2 + 20; // Чуть ниже центра

        // Создаем 4 карточки
        for (int i = 0; i < 4; i++) {
            int finalId = i;
            // Проверяем, есть ли набор на сервере (иначе кнопка неактивна)
            boolean hasKit = ClientKitData.availableKits.containsKey("kit" + i);

            ImageCardButton btn = new ImageCardButton(startX + (cardWidth + gap) * i, startY, cardWidth, cardHeight, finalId, (b) -> {
                PacketHandler.INSTANCE.sendToServer(new PacketSelectKit(finalId));
                this.onClose();
            });
            btn.active = hasKit; // Делаем неактивной, если кит не настроен админом
            this.addRenderableWidget(btn);
        }

        // Кнопка Закрыть
        this.addRenderableWidget(new ModernButton(this.width / 2 - 50, this.height - 30, 100, 20, Component.literal("Закрыть"), (btn) -> this.onClose()));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        // Рисуем баннер сверху
        int bannerWidth = 200; int bannerHeight = 50;
        guiGraphics.blit(BANNER_TEXTURE, (this.width - bannerWidth) / 2, 10, 0, 0, bannerWidth, bannerHeight, bannerWidth, bannerHeight);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    // === ОБНОВЛЕННЫЙ КЛАСС АНИМИРОВАННОЙ КАРТОЧКИ ===
    private class ImageCardButton extends Button {
        private final int kitId;
        private float hoverOffset = 0;

        public ImageCardButton(int x, int y, int width, int height, int id, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.kitId = id;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // 1. Плавная математика БЕЗ partialTick
            float targetOffset = (isHovered && active) ? -10f : 0f;
            // Коэффициент 0.15f отвечает за скорость (чем меньше, тем плавнее)
            this.hoverOffset = Mth.lerp(0.15f, this.hoverOffset, targetOffset);

            RenderSystem.enableBlend();
            ResourceLocation texture = (isHovered && active) ? KIT_TEXTURES_HOVER[kitId] : KIT_TEXTURES[kitId];

            if (!active) {
                RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 0.7f); // Затемняем неактивные
            } else {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }

            // 2. Идеально плавный сдвиг через матрицы (поддерживает дробные значения)
            guiGraphics.pose().pushPose();
            // Сдвигаем всю сетку рендера вверх на hoverOffset
            guiGraphics.pose().translate(0, this.hoverOffset, 0);

            // Рисуем на обычных координатах getY(), так как сетка уже сдвинута
            guiGraphics.blit(texture, getX(), getY(), 0, 0, width, height, width, height);

            // Возвращаем сетку на место
            guiGraphics.pose().popPose();

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // Сброс цвета
            RenderSystem.disableBlend();
        }
    }
}