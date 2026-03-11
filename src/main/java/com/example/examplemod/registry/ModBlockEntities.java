package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.world.block.BombBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ExampleMod.MODID);

    // Связываем BombBlockEntity с блоком BOMB_BLOCK
    public static final RegistryObject<BlockEntityType<BombBlockEntity>> BOMB_BE = BLOCK_ENTITIES.register("bomba_be",
            () -> BlockEntityType.Builder.of(BombBlockEntity::new, ModBlocks.BOMB_BLOCK.get()).build(null));
}