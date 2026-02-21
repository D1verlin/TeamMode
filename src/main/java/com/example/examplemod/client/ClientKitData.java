package com.example.examplemod.client;

import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientKitData {
    // Теперь храним Имя -> Список предметов
    public static Map<String, List<ItemStack>> availableKits = new HashMap<>();
}