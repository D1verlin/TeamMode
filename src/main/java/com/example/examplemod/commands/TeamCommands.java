package com.example.examplemod.commands;

import com.example.examplemod.network.PacketHandler;
import com.example.examplemod.world.TeamData;
import com.example.examplemod.config.KitConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class TeamCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tb")

                // /tb join <teamId> - Присоединиться к команде
                .then(Commands.literal("join")
                        .then(Commands.argument("teamId", IntegerArgumentType.integer(1, 2))
                                .executes(TeamCommands::joinTeam)))
                // /tb nightvision - Переключить ночное зрение
                .then(Commands.literal("nightvision")
                        .requires(source -> source.hasPermission(2))
                        .executes(TeamCommands::toggleNightVision))
                // /tb score - Проверить текущий счет
                .then(Commands.literal("score")
                        .executes(TeamCommands::checkScore))

                // /tb add <teamId> <points> - Добавить очки вручную
                .then(Commands.literal("add")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("teamId", IntegerArgumentType.integer(1, 2))
                                .then(Commands.argument("points", IntegerArgumentType.integer())
                                        .executes(TeamCommands::addScore))))

                // /tb setspawn <teamId> - Установить точку возрождения
                .then(Commands.literal("setspawn")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("team", IntegerArgumentType.integer(1, 2))
                                .executes(TeamCommands::setSpawn)))

                // /tb listspawns <teamId> - Список всех точек спавна
                .then(Commands.literal("listspawns")
                        .then(Commands.argument("team", IntegerArgumentType.integer(1, 2))
                                .executes(TeamCommands::listSpawns)))

                // /tb delspawn <teamId> <index> - Удалить точку по номеру
                .then(Commands.literal("delspawn")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("team", IntegerArgumentType.integer(1, 2))
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                        .executes(TeamCommands::delSpawn))))

                        // /tb mode <deathmatch|domination|defusal>
                        .then(Commands.literal("mode")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("modeName", StringArgumentType.word())
                                        .executes(context -> {
                                            String mode = StringArgumentType.getString(context, "modeName").toLowerCase();

                                            if (!mode.equals("deathmatch") && !mode.equals("domination") && !mode.equals("defusal")) {
                                                context.getSource().sendFailure(Component.literal("§cДоступные режимы: deathmatch, domination, defusal"));
                                                return 1;
                                            }

                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            TeamData data = TeamData.get(player.serverLevel());
                                            data.gameMode = mode;
                                            data.sync();
                                            context.getSource().sendSystemMessage(Component.literal("§a[Система] §fУстановлен режим: §e" + mode));

                                            // НОВОЕ: Если включили закладку бомбы - стартуем раунд
                                            if (mode.equals("defusal")) {
                                                com.example.examplemod.events.GameEvents.restartDefusalRound(player.serverLevel());
                                            }

                                            return 1;
                                        })))

                // /tb maxscore <score> - Лимит очков для победы
                .then(Commands.literal("maxscore")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("score", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    int score = IntegerArgumentType.getInteger(context, "score");
                                    TeamData data = TeamData.get(context.getSource().getLevel());
                                    data.targetScore = score;
                                    data.sync();
                                    context.getSource().sendSuccess(() -> Component.literal("§a[Система] §fОчков для победы: §e" + score), true);
                                    return 1;
                                })))

                // /tb setzone <1|2> - Установка границ зоны
                .then(Commands.literal("setzone")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("point", IntegerArgumentType.integer(1, 2))
                                .executes(context -> {
                                    int point = IntegerArgumentType.getInteger(context, "point");
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    TeamData data = TeamData.get(player.serverLevel());
                                    if (point == 1) data.zonePos1 = player.blockPosition();
                                    else data.zonePos2 = player.blockPosition();
                                    data.sync();
                                    context.getSource().sendSuccess(() -> Component.literal("§a[Система] §fТочка зоны " + point + " установлена на " + player.blockPosition().toShortString()), true);
                                    return 1;
                                })))

                // /tb setsitea <1|2> - Установка границ Плэнта А
                .then(Commands.literal("setsitea")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("point", IntegerArgumentType.integer(1, 2))
                                .executes(context -> {
                                    int point = IntegerArgumentType.getInteger(context, "point");
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    TeamData data = TeamData.get(player.serverLevel());
                                    if (point == 1) data.siteAPos1 = player.blockPosition();
                                    else data.siteAPos2 = player.blockPosition();
                                    data.setDirty();
                                    context.getSource().sendSystemMessage(Component.literal("§a[Система] §fТочка Плэнта А " + point + " установлена на " + player.blockPosition().toShortString()));
                                    return 1;
                                })))

                // /tb setsiteb <1|2> - Установка границ Плэнта Б
                .then(Commands.literal("setsiteb")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("point", IntegerArgumentType.integer(1, 2))
                                .executes(context -> {
                                    int point = IntegerArgumentType.getInteger(context, "point");
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    TeamData data = TeamData.get(player.serverLevel());
                                    if (point == 1) data.siteBPos1 = player.blockPosition();
                                    else data.siteBPos2 = player.blockPosition();
                                    data.setDirty();
                                    context.getSource().sendSystemMessage(Component.literal("§a[Система] §fТочка Плэнта Б " + point + " установлена на " + player.blockPosition().toShortString()));
                                    return 1;
                                })))

                // Блок команд очистки
                .then(Commands.literal("clear")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("score")
                                .executes(context -> {
                                    TeamData data = TeamData.get(context.getSource().getLevel());
                                    data.score1 = 0; data.score2 = 0;
                                    data.sync();
                                    context.getSource().sendSuccess(() -> Component.literal("§6[Система] §fСчёт группировок сброшен."), true);
                                    return 1;
                                }))
                        .then(Commands.literal("player")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                            TeamData data = TeamData.get(context.getSource().getLevel());
                                            data.playerKills.remove(target.getUUID());
                                            data.playerDeaths.remove(target.getUUID());
                                            data.sync();
                                            context.getSource().sendSuccess(() -> Component.literal("§6[Система] §fСтатистика игрока " + target.getName().getString() + " обнулена."), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("kits")
                                .executes(context -> {
                                    KitConfig.KITS.clear();
                                    KitConfig.save();
                                    PacketHandler.sendKitsToAll(KitConfig.KITS);
                                    context.getSource().sendSuccess(() -> Component.literal("§6[Система] §fВсе наборы снаряжения удалены."), true);
                                    return 1;
                                }))
                        .then(Commands.literal("spawns")
                                .executes(context -> {
                                    TeamData data = TeamData.get(context.getSource().getLevel());
                                    data.spawns1.clear(); data.spawns2.clear();
                                    data.setDirty();
                                    context.getSource().sendSuccess(() -> Component.literal("§6[Система] §fВсе точки респавна удалены."), true);
                                    return 1;
                                })))
        );
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ---

    private static int joinTeam(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            int teamId = IntegerArgumentType.getInteger(context, "teamId");
            TeamData data = TeamData.get(player.serverLevel());
            data.joinTeam(teamId, player.getUUID());
            context.getSource().sendSuccess(() -> Component.literal("Вы вошли в Команду " + teamId), false);
            return 1;
        } catch (Exception e) { return 0; }
    }

    private static int checkScore(CommandContext<CommandSourceStack> context) {
        try {
            TeamData data = TeamData.get(context.getSource().getLevel());
            String message = "Счет: Команда 1 [" + data.score1 + "] - [" + data.score2 + "] Команда 2";
            context.getSource().sendSuccess(() -> Component.literal(message), false);
            return 1;
        } catch (Exception e) { return 0; }
    }

    private static int addScore(CommandContext<CommandSourceStack> context) {
        try {
            int teamId = IntegerArgumentType.getInteger(context, "teamId");
            int points = IntegerArgumentType.getInteger(context, "points");
            TeamData data = TeamData.get(context.getSource().getLevel());
            data.addScore(teamId, points);
            context.getSource().sendSuccess(() -> Component.literal("Очки добавлены!"), true);
            return 1;
        } catch (Exception e) { return 0; }
    }

    private static int setSpawn(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            int teamId = IntegerArgumentType.getInteger(context, "team");
            TeamData data = TeamData.get(player.serverLevel());
            BlockPos pos = player.blockPosition();
            data.setSpawn(teamId, pos);
            String teamName = (teamId == 1) ? "ОДИНОЧКИ" : "БАНДОСЫ";
            context.getSource().sendSystemMessage( Component.literal("Точка спавна команды " + teamName + " установлена на " + pos.toShortString()));
            return 1;
        } catch (Exception e) { return 0; }
    }

    private static int listSpawns(CommandContext<CommandSourceStack> context) {
        int teamId = IntegerArgumentType.getInteger(context, "team");
        TeamData data = TeamData.get(context.getSource().getLevel());
        List<BlockPos> spawns = (teamId == 1) ? data.spawns1 : data.spawns2;
        String teamName = (teamId == 1) ? "ОДИНОЧКИ" : "БАНДОСЫ";
        if (spawns.isEmpty()) {
            context.getSource().sendSystemMessage( Component.literal("§c[Система] §fУ группировки " + teamName + " нет точек спавна."));
            return 1;
        }
        context.getSource().sendSystemMessage( Component.literal("§a[Система] §fТочки спавна (" + teamName + "):"));
        for (int i = 0; i < spawns.size(); i++) {
            final int index = i;
            context.getSource().sendSystemMessage( Component.literal("  §e[" + index + "] §f- " + spawns.get(index).toShortString()));
        }
        return 1;
    }
    private static int toggleNightVision(CommandContext<CommandSourceStack> context) {
        try {
            TeamData data = TeamData.get(context.getSource().getLevel());
            data.globalNightVision = !data.globalNightVision;
            data.setDirty();
            String state = data.globalNightVision ? "§aВКЛЮЧЕНО" : "§cВЫКЛЮЧЕНО";
            context.getSource().sendSuccess(() -> Component.literal("§a[Система] §fБесконечное ночное зрение: " + state), true);
            return 1;
        } catch (Exception e) { return 0; }
    }
    private static int delSpawn(CommandContext<CommandSourceStack> context) {
        int teamId = IntegerArgumentType.getInteger(context, "team");
        int index = IntegerArgumentType.getInteger(context, "index");
        TeamData data = TeamData.get(context.getSource().getLevel());
        List<BlockPos> spawns = (teamId == 1) ? data.spawns1 : data.spawns2;
        if (index >= 0 && index < spawns.size()) {
            BlockPos removedPos = spawns.remove(index);
            data.setDirty();
            context.getSource().sendSystemMessage( Component.literal("§a[Система] §fТочка [" + index + "] (" + removedPos.toShortString() + ") удалена."));
        } else {
            context.getSource().sendFailure(Component.literal("§c[Ошибка] Неверный индекс."));
        }
        return 1;
    }
}