package com.example.examplemod.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

public class BombBlockEntity extends BlockEntity {
    private int fuseTimer = 800; // 40 секунд (по 20 тиков)
    private int defuseTimer = 0;
    private UUID defuser = null;
    private boolean isDefused = false;

    public BombBlockEntity(BlockPos pos, BlockState state) {
        // Вместо null передаем зарегистрированный тип сущности
        super(com.example.examplemod.registry.ModBlockEntities.BOMB_BE.get(), pos, state);
    }

    public void startDefusing(Player player) {
        if (isDefused) return;
        this.defuser = player.getUUID();
        this.defuseTimer = 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BombBlockEntity entity) {
        if (entity.isDefused) return;

        // Получаем данные команд
        com.example.examplemod.world.TeamData data = com.example.examplemod.world.TeamData.get((net.minecraft.server.level.ServerLevel) level);

        // Логика разминирования
        if (entity.defuser != null) {
            Player player = level.getPlayerByUUID(entity.defuser);
            if (player != null && player.isAlive() && player.distanceToSqr(pos.getCenter()) < 9.0) {
                entity.defuseTimer++;
                if (entity.defuseTimer % 20 == 0) {
                    player.displayClientMessage(Component.literal("§aРазминирование: " + (5 - (entity.defuseTimer / 20)) + " сек..."), false);
                }

                if (entity.defuseTimer >= 100) { // 5 секунд удержания
                    entity.isDefused = true;
                    level.removeBlock(pos, false);
                    level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("§bБомба обезврежена! Спецназ (Бандосы) победили в раунде!"), false);

                    // --- НАЧИСЛЯЕМ ОЧКИ КОМАНДЕ 2 ---
                    data.addScore(2, 1);

                    com.example.examplemod.events.GameEvents.restartDefusalRound((net.minecraft.server.level.ServerLevel) level);
                    return;
                }
            } else {
                if (player != null) player.displayClientMessage(Component.literal("§cРазминирование прервано!"), false);
                entity.defuser = null;
                entity.defuseTimer = 0;
            }
        }

        // Логика таймера взрыва
        entity.fuseTimer--;
        if (entity.fuseTimer <= 0) {
            level.removeBlock(pos, false);
            level.explode(null, pos.getX(), pos.getY(), pos.getZ(), 15.0f, Level.ExplosionInteraction.NONE);
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("§cБомба взорвалась! Террористы (Одиночки) победили в раунде!"), false);

            // --- НАЧИСЛЯЕМ ОЧКИ КОМАНДЕ 1 ---
            data.addScore(1, 1);

            com.example.examplemod.events.GameEvents.restartDefusalRound((net.minecraft.server.level.ServerLevel) level);

        } else if (entity.fuseTimer % 20 == 0) {
            float pitch = entity.fuseTimer < 200 ? 1.5f : 1.0f;
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.get(), SoundSource.BLOCKS, 1.0f, pitch);
        }
    }
}