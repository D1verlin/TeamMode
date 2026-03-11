package com.example.examplemod.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BombBlock extends BaseEntityBlock {

    // Хитбокс (бомба плоская, лежит на земле) - можешь подогнать под размеры своей модели
    private static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 4.0D, 12.0D);

    public BombBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL; // Чтобы рендерилась твоя bomba.json
    }

    // Нажатие ПКМ по бомбе - начало разминирования
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BombBlockEntity bomb) {

            com.example.examplemod.world.TeamData data = com.example.examplemod.world.TeamData.get((net.minecraft.server.level.ServerLevel) level);

            // Разминировать могут только Бандосы (Команда 2 / Спецназ)
            if (data.getTeamOf(player.getUUID()) != 2) {
                player.displayClientMessage(Component.literal("§cТолько спецназ (Бандосы) может разминировать бомбу!"), true);
                return InteractionResult.FAIL;
            }

            bomb.startDefusing(player);
            player.displayClientMessage(Component.literal("§eРазминирование начато... Стойте рядом!"), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BombBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Заменили null на реальный тип из реестра
        return level.isClientSide ? null : createTickerHelper(type, com.example.examplemod.registry.ModBlockEntities.BOMB_BE.get(), BombBlockEntity::serverTick);
    }
}