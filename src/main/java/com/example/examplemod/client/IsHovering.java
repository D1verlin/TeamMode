package com.example.examplemod.client;

import net.minecraft.world.inventory.Slot;

public interface IsHovering {
    // --- УМНЫЕ ХИТБОКСЫ ---
    boolean isHovering(Slot slot, double mouseX, double mouseY);
}
