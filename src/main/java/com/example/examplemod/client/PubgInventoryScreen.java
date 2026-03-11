package com.example.examplemod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;

public class PubgInventoryScreen extends InventoryScreen implements IsHovering {

    private float xMouse;
    private float yMouse;

    // Переменная для скроллинга левой панели
    private float scrollOffs = 0f;

    // Непрозрачные цвета в стиле PUBG
    private static final int COLOR_PANEL_BG = 0xDD111111; // Темный фон панелей
    private static final int COLOR_SLOT_BG = 0x66333333;  // Фон самих слотов
    private static final int COLOR_TEXT_DIM = 0xFFAAAAAA; // Серый текст

    public PubgInventoryScreen(Player player) {
        super(player);
    }

    @Override
    protected void init() {
        this.imageWidth = 430;
        this.imageHeight = 240;
        super.init();
        this.clearWidgets();
        updateSlotPositions(); // Первичная расстановка
    }

    // --- РАССТАНОВКА СЛОТОВ ---
    private void updateSlotPositions() {
        int listIndex = 0;

        for (Slot slot : this.menu.slots) {
            int id = slot.index;

            // 1. ЛЕВАЯ ПАНЕЛЬ: РЮКЗАК (Скроллируемый список)
            if (id >= 9 && id <= 35) {
                int rowY = 30 + (listIndex * 22) - (int) scrollOffs;

                // Скрываем слоты за пределами видимой зоны
                if (rowY < 20 || rowY > 215) {
                    setSlotPosition(slot, -1000, -1000);
                } else {
                    setSlotPosition(slot, 17, rowY + 3);
                }
                listIndex++;
            }
// 2. ЦЕНТРАЛЬНАЯ ПАНЕЛЬ: БРОНЯ И РУКА
            else if (id >= 5 && id <= 8) {
                int relIndex = id - 5;
                setSlotPosition(slot, 155, 40 + relIndex * 35);
            } else if (id == 45) {
                setSlotPosition(slot, -1000, -1000); // СКРЫВАЕМ ВТОРУЮ РУКУ
            }
            // 3. ПРАВАЯ ПАНЕЛЬ: ОРУЖИЕ (Хотбар 36-43)
            else if (id >= 36 && id <= 43) {
                int boxX = 0, boxY = 0, boxW = 0, boxH = 0;

                switch (id - 36) {
                    case 0: boxX = 285; boxY = 30;  boxW = 130; boxH = 40; break; // Основное
                    case 1: boxX = 285; boxY = 75;  boxW = 130; boxH = 40; break; // Вторичное
                    case 2: boxX = 285; boxY = 120; boxW = 130; boxH = 30; break; // Пистолет
                    case 3: boxX = 285; boxY = 155; boxW = 60;  boxH = 35; break; // Нож
                    case 4: boxX = 355; boxY = 155; boxW = 60;  boxH = 35; break; // Медицина
                    case 5: boxX = 285; boxY = 195; boxW = 40;  boxH = 30; break; // Граната 1
                    case 6: boxX = 330; boxY = 195; boxW = 40;  boxH = 30; break; // Граната 2
                    case 7: boxX = 375; boxY = 195; boxW = 40;  boxH = 30; break; // Граната 3
                }

                // Ставим слот ровно по центру нашей панели
                setSlotPosition(slot, boxX + (boxW - 16) / 2, boxY + (boxH - 16) / 2);
            }
            // 4. СКРЫВАЕМ ОСТАЛЬНОЕ
            else {
                setSlotPosition(slot, -1000, -1000);
            }
        }
    }

    // --- ЛОГИКА СКРОЛЛИНГА ---
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            int totalContentHeight = 27 * 22;
            int visibleHeight = 190;
            int maxScroll = Math.max(0, totalContentHeight - visibleHeight);

            this.scrollOffs -= (float) (delta * 15);
            this.scrollOffs = Math.max(0, Math.min(this.scrollOffs, maxScroll));

            updateSlotPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    // --- УМНЫЕ ХИТБОКСЫ ---
    @Override
    public boolean isHovering(Slot slot, double mouseX, double mouseY) {
        int id = slot.index;
        int boxX = slot.x, boxY = slot.y, boxW = 16, boxH = 16;

        if (id >= 9 && id <= 35) { boxX = slot.x - 2; boxY = slot.y - 3; boxW = 120; boxH = 22; }
        else if (id == 36 || id == 37) { boxX = 285; boxW = 130; boxH = 40; boxY = (id==36) ? 30 : 75; }
        else if (id == 38) { boxX = 285; boxY = 120; boxW = 130; boxH = 30; }
        else if (id == 39) { boxX = 285; boxY = 155; boxW = 60; boxH = 35; }
        else if (id == 40) { boxX = 355; boxY = 155; boxW = 60; boxH = 35; }
        else if (id == 41) { boxX = 285; boxY = 195; boxW = 40; boxH = 30; }
        else if (id == 42) { boxX = 330; boxY = 195; boxW = 40; boxH = 30; }
        else if (id == 43) { boxX = 375; boxY = 195; boxW = 40; boxH = 30; }

        return this.isHovering(boxX, boxY, boxW, boxH, mouseX, mouseY);
    }

    // --- ОТРИСОВКА БАЗОВОГО ФОНА ПАНЕЛЕЙ ---
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(this.leftPos + 10, this.topPos + 10, this.leftPos + 140, this.topPos + 230, COLOR_PANEL_BG);
        guiGraphics.fill(this.leftPos + 145, this.topPos + 10, this.leftPos + 275, this.topPos + 230, COLOR_PANEL_BG);
        guiGraphics.fill(this.leftPos + 280, this.topPos + 10, this.leftPos + 420, this.topPos + 230, COLOR_PANEL_BG);

        guiGraphics.drawString(this.font, "РЮКЗАК", this.leftPos + 15, this.topPos + 15, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "СНАРЯЖЕНИЕ", this.leftPos + 285, this.topPos + 15, 0xFFFFFF, false);

        // Фоны Рюкзака
        for (int id = 9; id <= 35; id++) {
            Slot slot = this.menu.slots.get(id);
            if (slot.x > 0) {
                int bgX = this.leftPos + 15;
                int bgY = this.topPos + slot.y - 3;
                guiGraphics.fill(bgX, bgY, bgX + 120, bgY + 21, COLOR_SLOT_BG);
            }
        }

// Фоны Брони
        for (int id = 5; id <= 8; id++) {
            Slot slot = this.menu.slots.get(id);
            guiGraphics.fill(this.leftPos + slot.x - 2, this.topPos + slot.y - 2, this.leftPos + slot.x + 18, this.topPos + slot.y + 18, COLOR_SLOT_BG);
        }
        // Удали отсюда строку, которая рисовала квадратик подложки для оффхенда!
        guiGraphics.fill(this.leftPos + 153, this.topPos + 178, this.leftPos + 173, this.topPos + 198, COLOR_SLOT_BG);

        // Персонаж
        InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, this.leftPos + 210, this.topPos + 220, 65, (this.leftPos + 210) - this.xMouse, (this.topPos + 220 - 80) - this.yMouse, this.minecraft.player);
    }

    // --- ОТРИСОВКА СПИСКОВ И ИКОНОК ОРУЖИЯ С МАСШТАБОМ ---
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (Slot slot : this.menu.slots) {
            int id = slot.index;

            // --- 1. СПИСОК РЮКЗАКА ---
            if (id >= 9 && id <= 35 && slot.x > 0) {
                if (slot.hasItem()) {
                    ItemStack stack = slot.getItem();
                    String name = stack.getHoverName().getString();
                    if (this.font.width(name) > 65) {
                        name = this.font.plainSubstrByWidth(name, 60) + "...";
                    }
                    guiGraphics.drawString(this.font, name, slot.x + 22, slot.y + 4, 0xFFFFFF, false);

                    if (stack.getCount() > 1) {
                        String countTxt = String.valueOf(stack.getCount());
                        int txtW = this.font.width(countTxt);
                        guiGraphics.drawString(this.font, countTxt, 15 + 115 - txtW, slot.y + 4, COLOR_TEXT_DIM, false);
                    }
                }
            }

            // --- 2. ПАНЕЛИ ОРУЖИЯ С МАСШТАБИРОВАННЫМИ ИКОНКАМИ ---
            else if (id >= 36 && id <= 43) {
                int boxX = 0, boxY = 0, boxW = 0, boxH = 0;
                float scale = 1.0f;
                int hotbarIndex = id - 36;

                // Настраиваем координаты и масштабы
                switch (hotbarIndex) {
                    case 0: boxX = 285; boxY = 30;  boxW = 130; boxH = 40; scale = 2.0f; break; // Основное
                    case 1: boxX = 285; boxY = 75;  boxW = 130; boxH = 40; scale = 2.0f; break; // Вторичное
                    case 2: boxX = 285; boxY = 120; boxW = 130; boxH = 30; scale = 1.5f; break; // Пистолет
                    case 3: boxX = 285; boxY = 155; boxW = 60;  boxH = 35; scale = 1.5f; break; // Нож
                    case 4: boxX = 355; boxY = 155; boxW = 60;  boxH = 35; scale = 1.5f; break; // Медицина
                    case 5: boxX = 285; boxY = 195; boxW = 40;  boxH = 30; scale = 1.2f; break; // Граната 1
                    case 6: boxX = 330; boxY = 195; boxW = 40;  boxH = 30; scale = 1.2f; break; // Граната 2
                    case 7: boxX = 375; boxY = 195; boxW = 40;  boxH = 30; scale = 1.2f; break; // Граната 3
                }

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 150);

                // Маскируем стандартную иконку (которую майнкрафт уже нарисовал мелкой)
                guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, COLOR_SLOT_BG);
                guiGraphics.drawString(this.font, String.valueOf(hotbarIndex + 1), boxX + 2, boxY + 2, COLOR_TEXT_DIM, false);

                // Рендер увеличенной иконки
                ItemStack stack = slot.getItem();
                if (!stack.isEmpty()) {
                    guiGraphics.pose().pushPose();
                    // Сдвигаем центр с учетом масштаба (базовый размер иконки 16x16)
                    guiGraphics.pose().translate(boxX + boxW / 2f - (8 * scale), boxY + boxH / 2f - (8 * scale), 250);
                    guiGraphics.pose().scale(scale, scale, 1.0f);
                    guiGraphics.renderItem(stack, 0, 0);
                    guiGraphics.pose().popPose();

                    // ТЕКСТ (Патроны или количество)
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, 0, 300); // Поверх предмета

                    CompoundTag tag = stack.getTag();
                    boolean isTaczGun = tag != null && tag.contains("GunId");

                    if (isTaczGun) {
                        // Читаем NBT от TaCZ
                        String ammoTxt = tag.contains("GunCurrentAmmoCount") ? String.valueOf(tag.getInt("GunCurrentAmmoCount")) : "0";
                        if (tag.contains("GunFireMode")) {
                            String mode = tag.getString("GunFireMode");
                            if (!mode.isEmpty()) ammoTxt += " [" + mode.substring(0, 1) + "]";
                        }
                        int textW = this.font.width(ammoTxt);
                        guiGraphics.drawString(this.font, ammoTxt, boxX + boxW - textW - 4, boxY + 4, 0xFFD700, false);
                    } else if (stack.getCount() > 1 && id > 38) {
                        String countText = String.valueOf(stack.getCount());
                        int textW = this.font.width(countText);
                        guiGraphics.drawString(this.font, countText, boxX + boxW - textW - 3, boxY + boxH - 10, 0xFFFFFF, false);
                    }
                    guiGraphics.pose().popPose();
                }

                // ЭФФЕКТ ХОВЕРА
                if (this.hoveredSlot == slot) {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, 0, 350);
                    RenderSystem.disableDepthTest();
                    RenderSystem.colorMask(true, true, true, false);
                    guiGraphics.fillGradient(boxX, boxY, boxX + boxW, boxY + boxH, 0x40FFFFFF, 0x40FFFFFF);
                    RenderSystem.colorMask(true, true, true, true);
                    RenderSystem.enableDepthTest();
                    guiGraphics.pose().popPose();
                }

                guiGraphics.pose().popPose();
            }

            // --- 3. ХОВЕР ДЛЯ СПИСКА И БРОНИ ---
            else if (this.hoveredSlot == slot && slot.x > 0) {
                int bgX = (id >= 9 && id <= 35) ? slot.x - 2 : slot.x;
                int bgY = (id >= 9 && id <= 35) ? slot.y - 3 : slot.y;
                int bgW = (id >= 9 && id <= 35) ? 120 : 16;
                int bgH = (id >= 9 && id <= 35) ? 21 : 16;

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 250);
                RenderSystem.disableDepthTest();
                RenderSystem.colorMask(true, true, true, false);
                guiGraphics.fillGradient(bgX, bgY, bgX + bgW, bgY + bgH, 0x40FFFFFF, 0x40FFFFFF);
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.enableDepthTest();
                guiGraphics.pose().popPose();
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.xMouse = (float)mouseX;
        this.yMouse = (float)mouseY;
    }

    // --- РЕФЛЕКСИЯ ДЛЯ КООРДИНАТ СЛОТОВ ---
    private void setSlotPosition(Slot slot, int newX, int newY) {
        try {
            Field xField = getSlotField("x", "f_40220_"); xField.setAccessible(true); xField.setInt(slot, newX);
            Field yField = getSlotField("y", "f_40221_"); yField.setAccessible(true); yField.setInt(slot, newY);
        } catch (Exception e) { e.printStackTrace(); }
    }
    private Field getSlotField(String devName, String srgName) throws NoSuchFieldException {
        try { return Slot.class.getDeclaredField(devName); } catch (NoSuchFieldException e) { return Slot.class.getDeclaredField(srgName); }
    }
}