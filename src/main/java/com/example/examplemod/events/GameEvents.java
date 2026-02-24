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
                    teamData.targetScore
            );

            // --- Синхронизация КИТОВ ---
            KitData kitData = KitData.get(level);
            PacketHandler.sendKitsToPlayer(player, com.example.examplemod.config.KitConfig.KITS);
        }
    }

    // 2. Событие: Кто-то умер
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim && !victim.level().isClientSide()) {
            ServerLevel level = (ServerLevel) victim.level();
            TeamData data = TeamData.get(level);

            data.addDeath(victim.getUUID());
            killstreaks.put(victim.getUUID(), 0);

            Entity source = event.getSource().getEntity();

            if (source instanceof ServerPlayer killer && killer != victim) {
                data.addKill(killer.getUUID());

                int killerTeam = data.getTeamOf(killer.getUUID());
                int victimTeam = data.getTeamOf(victim.getUUID());

                if (killerTeam != 0 && victimTeam != 0 && killerTeam != victimTeam) {
                    data.addScore(killerTeam, 1);
                }

                int currentStreak = killstreaks.getOrDefault(killer.getUUID(), 0) + 1;
                killstreaks.put(killer.getUUID(), currentStreak);

                String titleText = "";
                String subtitleText = "Жертва: " + victim.getName().getString();
                int streakColor = 0xFFFFFF;

                if (currentStreak == 1) {
                    titleText = "ПЕРВАЯ КРОВЬ";
                    streakColor = 0xFFFFFF;
                } else if (currentStreak == 2) {
                    titleText = "ДВОЙНОЕ УБИЙСТВО";
                    streakColor = 0xFFD700;
                } else if (currentStreak == 3) {
                    titleText = "ТРОЙНОЕ УБИЙСТВО";
                    streakColor = 0xFFA500;
                } else if (currentStreak == 4) {
                    titleText = "ДОМИНАЦИЯ";
                    streakColor = 0xFF4500;
                } else if (currentStreak == 5) {
                    titleText = "БУЙСТВО";
                    streakColor = 0xDC143C;
                } else if (currentStreak >= 6) {
                    titleText = "НЕОСТАНОВИМ!";
                    streakColor = 0x8B0000;
                }

                PacketHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> killer),
                        new PacketKillstreak(titleText, subtitleText, streakColor)
                );

                victim.displayClientMessage(Component.literal("§cВас убил " + killer.getName().getString()), true);
            }
        }
    }

    // 3. Событие: Игрок возрождается
    @SubscribeEvent
    public static void onPlayerRespawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            ServerLevel level = (ServerLevel) player.level();
            TeamData data = TeamData.get(level);

            // 1. Телепортация на базу
            int teamId = data.getTeamOf(player.getUUID());
            java.util.List<net.minecraft.core.BlockPos> teamSpawns = (teamId == 1) ? data.spawns1 : (teamId == 2 ? data.spawns2 : null);
            if (teamSpawns != null && !teamSpawns.isEmpty()) {
                net.minecraft.core.BlockPos randomSpawn = teamSpawns.get(player.getRandom().nextInt(teamSpawns.size()));
                player.teleportTo(level, randomSpawn.getX() + 0.5, randomSpawn.getY() + 1, randomSpawn.getZ() + 0.5, player.getYRot(), player.getXRot());
            }

            // 2. Автоматическая выдача предметов в режиме SHOP
            if (data.gameMode.equals("shop")) {
                giveShopKit(player, data, teamId);
            }
        }
    }

    private static void giveShopKit(ServerPlayer player, TeamData data, int teamId) {
        player.getInventory().clearContent();

        // Загружаем кит с ID 4 для Одиночек и ID 5 для Бандосов
        String kitKey = (teamId == 1) ? "kit4" : (teamId == 2 ? "kit5" : null);

        if (kitKey != null) {
            java.util.List<net.minecraft.world.item.ItemStack> items = com.example.examplemod.config.KitConfig.KITS.get(kitKey);
            if (items != null && !items.isEmpty()) {
                // Проходимся по всем сохраненным слотам и восстанавливаем их позицию
                for (int i = 0; i < items.size() && i < player.getInventory().getContainerSize(); i++) {
                    net.minecraft.world.item.ItemStack stack = items.get(i);
                    if (!stack.isEmpty()) {
                        player.getInventory().setItem(i, stack.copy());
                    }
                }
            }
        }
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
    }

    // 4. Событие: Отключаем голод
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            Player player = event.player;
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5.0f);
        }
    }

    // 5. Событие: Применяем игровые правила
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(false, serverLevel.getServer());
            serverLevel.getGameRules().getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(false, serverLevel.getServer());
        }
    }

    // 6. Событие: Отключаем выпадение предметов при смерти
    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCanceled(true);
        }
    }

    // Загружаем конфиг китов при старте сервера
    @SubscribeEvent
    public static void onServerStarting(net.minecraftforge.event.server.ServerStartingEvent event) {
        com.example.examplemod.config.KitConfig.load();
    }

    private static int tickCounter = 0;
    private static int glowTimer = 0; // Таймер для отсчета свечения

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        glowTimer++;

        // Логика свечения: 1200 тиков = 60 секунд
        if (glowTimer >= 1200) {
            glowTimer = 0;
            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    if (player.isAlive()) {
                        // Выдаем эффект свечения: 100 тиков (5 секунд), уровень 0, без партиклов
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.GLOWING, 100, 0, false, false
                        ));
                        // Оповещаем игрока сообщением над хотбаром
                        player.displayClientMessage(Component.literal("§c[Радар] Позиции всех игроков раскрыты на 5 секунд!"), true);
                    }
                }
            }
        }

        if (tickCounter >= 20) { // Каждую 1 секунду
            tickCounter = 0;

            ServerLevel level = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().overworld();
            if (level == null) return;

            TeamData data = TeamData.get(level);

            if (data.gameMode.equals("domination") && data.zonePos1 != null && data.zonePos2 != null) {
                AABB zoneBox = new AABB(data.zonePos1, data.zonePos2).inflate(0.5);

                int t1PlayersInZone = 0;
                int t2PlayersInZone = 0;

                for (ServerPlayer player : level.players()) {
                    if (player.isAlive() && zoneBox.contains(player.position())) {
                        int teamId = data.getTeamOf(player.getUUID());
                        if (teamId == 1) t1PlayersInZone++;
                        else if (teamId == 2) t2PlayersInZone++;
                    }
                }

                int newOwner = 0;

                if (t1PlayersInZone > 0 && t2PlayersInZone == 0) {
                    newOwner = 1;
                    data.addScore(1, 1);
                } else if (t2PlayersInZone > 0 && t1PlayersInZone == 0) {
                    newOwner = 2;
                    data.addScore(2, 1);
                } else if (t1PlayersInZone > 0 && t2PlayersInZone > 0) {
                    newOwner = 0;
                }

                if (data.zoneOwner != newOwner) {
                    data.zoneOwner = newOwner;
                    data.sync();
                }

                if (data.score1 >= data.targetScore) {
                    level.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal("§a§lОДИНОЧКИ ПОБЕДИЛИ!"), false);
                    resetGame(data, level);
                } else if (data.score2 >= data.targetScore) {
                    level.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal("§c§lБАНДОСЫ ПОБЕДИЛИ!"), false);
                    resetGame(data, level);
                }
            }
        }
    }

    private static void resetGame(TeamData data, ServerLevel level) {
        data.score1 = 0;
        data.score2 = 0;
        data.playerKills.clear();
        data.playerDeaths.clear();
        data.zoneOwner = 0;
        data.sync();

        // Сбрасываем таймер свечения, чтобы он не сработал сразу после старта нового матча
        glowTimer = 0;

        for (ServerPlayer player : level.players()) {
            if (player.isAlive()) {
                int teamId = data.getTeamOf(player.getUUID());
                java.util.List<BlockPos> teamSpawns = (teamId == 1) ? data.spawns1 : (teamId == 2 ? data.spawns2 : null);

                if (teamSpawns != null && !teamSpawns.isEmpty()) {
                    BlockPos randomSpawn = teamSpawns.get(player.getRandom().nextInt(teamSpawns.size()));
                    player.teleportTo(level, randomSpawn.getX() + 0.5, randomSpawn.getY() + 1, randomSpawn.getZ() + 0.5, player.getYRot(), player.getXRot());
                    player.setHealth(player.getMaxHealth());
                    player.getFoodData().setFoodLevel(20);
                }
            }
        }
    }
}