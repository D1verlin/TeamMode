package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.world.item.BombItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ExampleMod.MODID);

    // Регистрируем предмет. Разрешаем держать только 1 бомбу в стаке
    public static final RegistryObject<Item> BOMB_ITEM = ITEMS.register("bomba",
            () -> new BombItem(new Item.Properties().stacksTo(1)));
}