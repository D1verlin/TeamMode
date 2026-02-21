package com.example.examplemod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyInit {
    // Категория настроек в меню управления
    public static final String CATEGORY = "key.categories.teammod";

    // Клавиша M (Меню команды)
    public static final KeyMapping OPEN_TEAM_MENU = new KeyMapping(
            "key.teammod.open_team", // Название в файле перевода
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M, // Кнопка по умолчанию
            CATEGORY
    );

    // Клавиша K (Редактор китов)
    public static final KeyMapping OPEN_KIT_EDITOR = new KeyMapping(
            "key.teammod.open_editor",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, // Кнопка по умолчанию
            CATEGORY
    );
}