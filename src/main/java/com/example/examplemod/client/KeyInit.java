package com.example.examplemod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyInit {
    public static final String CATEGORY = "key.categories.teammod";

    public static final KeyMapping OPEN_TEAM_MENU = new KeyMapping(
            "key.teammod.open_team",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    );

    public static final KeyMapping OPEN_KIT_EDITOR = new KeyMapping(
            "key.teammod.open_editor",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY
    );

    // НОВОЕ: Клавиша для переключения режима магазина (на цифровой клавиатуре)
    public static final KeyMapping TOGGLE_SHOP_MODE = new KeyMapping(
            "key.teammod.toggle_shop",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_MULTIPLY, // Клавиша "*" на Numpad
            CATEGORY
    );
}