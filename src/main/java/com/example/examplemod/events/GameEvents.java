package com.example.examplemod.events;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.PacketHandler;
import com.example.examplemod.network.PacketKillstreak;
import com.example.examplemod.world.KitData;
import com.example.examplemod.world.TeamData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GameEvents {

    // Хранилище серий убийств (UUID -> количество убийств подряд)
    private static final Map<UUID, Integer> killstreaks = new HashMap<>();

    // 1. Событие: Игрок зашел на сервер
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            TeamData teamData = TeamData.get(level);
            PacketHandler.sendScoreToPlayer(
                    player,
                    teamData.score1,
                    teamData.score2,
                    teamData.team1,
                    teamData.team2,
                    teamData.playerKills,
                    teamData.playerDeaths,
                    teamData.gameMode,
                    teamData.zonePos1,
                    teamData.zonePos2,
                    teamData.zoneOwner,
                    teamData.targetScore // Добавлено здесь
            );

            // --- Синхронизация КИТОВ ---
            KitData kitData = KitData.get(level);
            // --- Синхронизация КИТОВ ---
            PacketHandler.sendKitsToPlayer(player, com.example.examplemod.config.KitConfig.KITS);
        }
    }

    // 2. Событие: Кто-то умер
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim && !victim.level().isClientSide()) {
            ServerLevel level = (ServerLevel) victim.level();
            TeamData data = TeamData.get(level);

            // Записываем смерть жертве и прерываем её серию убийств
            data.addDeath(victim.getUUID());
            killstreaks.put(victim.getUUID(), 0);

            // Проверяем убийцу
            Entity source = event.getSource().getEntity();

            // Убеждаемся, что убийца - это игрок, и он не убил сам себя
            if (source instanceof ServerPlayer killer && killer != victim) {
                // Записываем статистику убийце
                data.addKill(killer.getUUID());

                int killerTeam = data.getTeamOf(killer.getUUID());
                int victimTeam = data.getTeamOf(victim.getUUID());

                // Если убит враг из другой команды - даем очко
                if (killerTeam != 0 && victimTeam != 0 && killerTeam != victimTeam) {
                    data.addScore(killerTeam, 1);
                }

                // Увеличиваем серию убийств (Killstreak)
                int currentStreak = killstreaks.getOrDefault(killer.getUUID(), 0) + 1;
                killstreaks.put(killer.getUUID(), currentStreak);

                String titleText = "";
                String subtitleText = "Жертва: " + victim.getName().getString();
                int streakColor = 0xFFFFFF; // Белый по умолчанию

                // Логика названий и цветов серии
                if (currentStreak == 1) {
                    titleText = "ПЕРВАЯ КРОВЬ";
                    streakColor = 0xFFFFFF;
                } else if (currentStreak == 2) {
                    titleText = "ДВОЙНОЕ УБИЙСТВО";
                    streakColor = 0xFFD700; // Желтый
                } else if (currentStreak == 3) {
                    titleText = "ТРОЙНОЕ УБИЙСТВО";
                    streakColor = 0xFFA500; // Оранжевый
                } else if (currentStreak == 4) {
                    titleText = "ДОМИНАЦИЯ";
                    streakColor = 0xFF4500; // Оранжево-красный
                } else if (currentStreak == 5) {
                    titleText = "БУЙСТВО";
                    streakColor = 0xDC143C; // Малиновый
                } else if (currentStreak >= 6) {
                    titleText = "НЕОСТАНОВИМ!";
                    streakColor = 0x8B0000; // Темно-красный
                }

                // Отправляем пакет нашему красивому интерфейсу (видит только убийца)
                PacketHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> killer),
                        new PacketKillstreak(titleText, subtitleText, streakColor)
                );

                // Оповещаем жертву маленьким текстом над хотбаром (параметр true = Action Bar)
                victim.displayClientMessage(Component.literal("§cВас убил " + killer.getName().getString()), true);
            }
        }
    }

    // 3. Событие: Игрок возрождается (Телепортация на базу)
    @SubscribeEvent
    public static void onPlayerRespawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            ServerLevel level = (ServerLevel) player.level();
            TeamData data = TeamData.get(level);

            int teamId = data.getTeamOf(player.getUUID());

            // Получаем нужный список спавнов
            List<BlockPos> teamSpawns = (teamId == 1) ? data.spawns1 : (teamId == 2 ? data.spawns2 : null);

            // Если список существует и в нём есть хотя бы одна точка
            if (teamSpawns != null && !teamSpawns.isEmpty()) {
                // Выбираем случайную точку с помощью встроенного рандомизатора игрока
                BlockPos randomSpawn = teamSpawns.get(player.getRandom().nextInt(teamSpawns.size()));

                // Телепортируем
                player.teleportTo(level, randomSpawn.getX() + 0.5, randomSpawn.getY() + 1, randomSpawn.getZ() + 0.5, player.getYRot(), player.getXRot());
            }
        }
    }

    // 4. Событие: Отключаем голод
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Проверяем, что это конец тика и мы на сервере
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            Player player = event.player;
            // Устанавливаем уровень еды 20 (полный) и насыщенность 5.0 каждый тик
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5.0f);
        }
    }

    // 5. Событие: Применяем игровые правила (GameRules) при загрузке мира
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            // Отключаем регенерацию здоровья от еды
            serverLevel.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(false, serverLevel.getServer());
            // Отключаем ванильные сообщения о смерти в чате ("Стив был убит...")
            serverLevel.getGameRules().getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(false, serverLevel.getServer());
        }
    }

    // 6. Событие: Отключаем выпадение предметов при смерти
    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        // Проверяем, что умерла именно сущность игрока
        if (event.getEntity() instanceof Player) {
            // Полностью отменяем выброс инвентаря в мир
            event.setCanceled(true);
        }
    }

    // Загружаем конфиг китов при старте сервера
    @SubscribeEvent
    public static void onServerStarting(net.minecraftforge.event.server.ServerStartingEvent event) {
        com.example.examplemod.config.KitConfig.load();
    }


    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        if (tickCounter >= 20) { // Каждую 1 секунду
            tickCounter = 0;

            ServerLevel level = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().overworld();
            if (level == null) return;

            TeamData data = TeamData.get(level);

            // Работаем только если включен режим удержания и точки заданы
            if (data.gameMode.equals("domination") && data.zonePos1 != null && data.zonePos2 != null) {

                // Создаем "коробку" из двух заданных точек
                AABB zoneBox = new AABB(data.zonePos1, data.zonePos2).inflate(0.5); // inflate расширяет зону на полблока для точности

                int t1PlayersInZone = 0;
                int t2PlayersInZone = 0;

                // Проверяем всех игроков на сервере
                for (ServerPlayer player : level.players()) {
                    if (player.isAlive() && zoneBox.contains(player.position())) {
                        int teamId = data.getTeamOf(player.getUUID());
                        if (teamId == 1) t1PlayersInZone++;
                        else if (teamId == 2) t2PlayersInZone++;
                    }
                }

                int newOwner = 0; // 0 - ничья/оспаривается, 1 - Одиночки, 2 - Бандосы

                // Логика захвата: если в зоне только одна команда, она захватывает/удерживает её
                if (t1PlayersInZone > 0 && t2PlayersInZone == 0) {
                    newOwner = 1;
                    data.addScore(1, 1); // Даем 1 очко Одиночкам
                } else if (t2PlayersInZone > 0 && t1PlayersInZone == 0) {
                    newOwner = 2;
                    data.addScore(2, 1); // Даем 1 очко Бандосам
                } else if (t1PlayersInZone > 0 && t2PlayersInZone > 0) {
                    // Зона оспаривается (Contested) - очки никому не идут, владелец сбрасывается или остается старым
                    newOwner = 0;
                }

                // Если владелец сменился, синхронизируем данные (для визуала)
                if (data.zoneOwner != newOwner) {
                    data.zoneOwner = newOwner;
                    data.sync();
                }

                // Проверка на победу
                if (data.score1 >= data.targetScore) {
                    level.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal("§a§lОДИНОЧКИ ПОБЕДИЛИ!"), false);
                    resetGame(data, level); // Теперь передаем level
                } else if (data.score2 >= data.targetScore) {
                    level.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal("§c§lБАНДОСЫ ПОБЕДИЛИ!"), false);
                    resetGame(data, level); // Теперь передаем level
                }
            }
        }
    }

    private static void resetGame(TeamData data, ServerLevel level) {
        // Сброс данных
        data.score1 = 0;
        data.score2 = 0;
        data.playerKills.clear();
        data.playerDeaths.clear();
        data.zoneOwner = 0; // Зона снова нейтральна
        data.sync();

        // Респавн всех игроков на их базы
        for (ServerPlayer player : level.players()) {
            if (player.isAlive()) {
                int teamId = data.getTeamOf(player.getUUID());

                // Получаем список спавнов для команды игрока
                java.util.List<BlockPos> teamSpawns = (teamId == 1) ? data.spawns1 : (teamId == 2 ? data.spawns2 : null);

                // Если спавны есть - телепортируем на случайный
                if (teamSpawns != null && !teamSpawns.isEmpty()) {
                    BlockPos randomSpawn = teamSpawns.get(player.getRandom().nextInt(teamSpawns.size()));
                    player.teleportTo(level, randomSpawn.getX() + 0.5, randomSpawn.getY() + 1, randomSpawn.getZ() + 0.5, player.getYRot(), player.getXRot());

                    // Восстанавливаем здоровье и еду для нового раунда
                    player.setHealth(player.getMaxHealth());
                    player.getFoodData().setFoodLevel(20);
                }
            }
        }
    }
}