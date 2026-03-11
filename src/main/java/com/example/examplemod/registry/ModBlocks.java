package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.world.block.BombBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    // Создаем реестр для блоков, привязанный к нашему MODID ("teammod")
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ExampleMod.MODID);

    // Регистрируем сам блок бомбы. Имя "bomba" должно совпадать с названием JSON-файла
    // noOcclusion() нужен, чтобы блок считался прозрачным и сквозь него рендерился пол
    public static final RegistryObject<Block> BOMB_BLOCK = BLOCKS.register("bomba",
            () -> new BombBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));
}