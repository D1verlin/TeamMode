package com.example.examplemod.world.item;

import com.example.examplemod.world.TeamData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BombItem extends Item {

    public BombItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 60; // 3 секунды удержания (60 тиков)
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW; // Анимация натяжения, чтобы видеть процесс каста
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Проверки выполняем ТОЛЬКО на сервере. Клиенту просто разрешаем запустить анимацию.
        if (!level.isClientSide) {
            TeamData data = TeamData.get((ServerLevel) level);

            // 1. Проверяем включен ли нужный режим
            if (!data.gameMode.equals("defusal")) {
                player.displayClientMessage(Component.literal("§cОшибка: Режим 'defusal' не включен! Введите /tb mode defusal"), true);
                return InteractionResultHolder.fail(stack);
            }

            // 2. Проверяем команду (Команда 1 - Одиночки)
            if (data.getTeamOf(player.getUUID()) != 1) {
                player.displayClientMessage(Component.literal("§cОшибка: Только (Одиночки) могут ставить бомбу!"), true);
                return InteractionResultHolder.fail(stack);
            }

            // 3. Проверяем нахождение в зоне Плэнта А или Б
            if (!data.isPosInBombSite(player.blockPosition())) {
                player.displayClientMessage(Component.literal("§cОшибка: Вы должны находиться внутри Плэнта А или Б!"), true);
                return InteractionResultHolder.fail(stack);
            }
        }

        // Если мы дошли сюда, запускаем каст (очень важно для клиента, чтобы пошла анимация!)
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
        if (!level.isClientSide && entityLiving instanceof Player player) {
            TeamData data = TeamData.get((ServerLevel) level);

            // Перепроверяем, не выбежал ли игрок из зоны за эти 3 секунды
            if (data.isPosInBombSite(player.blockPosition())) {

                // Умная логика установки: ставим туда, куда смотрим, ИЛИ просто в ноги
                BlockPos placePos = player.blockPosition(); // По умолчанию прямо в ноги игроку
                HitResult hit = player.pick(4.0D, 0.0F, false);

                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos lookedPos = ((BlockHitResult)hit).getBlockPos().relative(((BlockHitResult)hit).getDirection());
                    if (level.getBlockState(lookedPos).canBeReplaced()) {
                        placePos = lookedPos;
                    }
                }

                // Окончательная проверка: есть ли место для блока (воздух/трава)?
                if (level.getBlockState(placePos).canBeReplaced()) {
                    level.setBlock(placePos, com.example.examplemod.registry.ModBlocks.BOMB_BLOCK.get().defaultBlockState(), 3);
                    level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("§cБомба заложена! До взрыва 40 секунд!"), false);
                    stack.shrink(1); // Забираем предмет C4
                } else {
                    player.displayClientMessage(Component.literal("§cНет места для установки бомбы! (Блок занят)"), true);
                }

            } else {
                player.displayClientMessage(Component.literal("§cУстановка отменена: вы покинули зону!"), true);
            }
        }
        return stack;
    }
}