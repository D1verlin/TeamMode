package com.example.examplemod.commands;

import com.example.examplemod.network.PacketHandler;
import com.example.examplemod.world.KitData;
import com.example.examplemod.world.TeamData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TeamCommands {

    // Основной метод регистрации. Мы вызываем его из главного класса.
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tb") // Главная команда /tm

                // Подкоманда: /tm join <номер>
                .then(Commands.literal("join")
                        .then(Commands.argument("teamId", IntegerArgumentType.integer(1, 2)) // Аргумент: число от 1 до 2
                                .executes(TeamCommands::joinTeam))) // Выполнить метод joinTeam

                // Подкоманда: /tm score
                .then(Commands.literal("score")
                        .executes(TeamCommands::checkScore))

                // Подкоманда: /tm add <команда> <очки> (для тестов)
                .then(Commands.literal("add")
                        .then(Commands.argument("teamId", IntegerArgumentType.integer(1, 2))
                                .then(Commands.argument("points", IntegerArgumentType.integer())
                                        .executes(TeamCommands::addScore))))
                // Подкоманда: /tb setspawn <1 или 2>
                .then(Commands.literal("setspawn")
                        .then(Commands.argument("team", IntegerArgumentType.integer(1, 2))
                                .executes(context -> {
                                    int teamId = IntegerArgumentType.getInteger(context, "team");
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    BlockPos pos = player.blockPosition();
                                    TeamData data = TeamData.get(player.serverLevel());

                                    String teamName = (teamId == 1) ? "ОДИНОЧКИ" : "БАНДОСЫ";
                                    int totalSpawns;

                                    // ДОБАВЛЯЕМ точку в список
                                    if (teamId == 1) {
                                        data.spawns1.add(pos);
                                        totalSpawns = data.spawns1.size();
                                    } else {
                                        data.spawns2.add(pos);
                                        totalSpawns = data.spawns2.size();
                                    }

                                    data.setDirty();
                                    context.getSource().sendSuccess(() -> Component.literal("§a[Система] §fДобавлена точка спавна для " + teamName + " на " + pos.toShortString() + ". Всего точек: " + totalSpawns), true);
                                    return 1;
                                })))

                // ... внутри регистрации команд, после блока setspawn ...

                // Просмотр списка точек спавна команды
                .then(Commands.literal("listspawns")
                        .then(Commands.argument("team", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 2))
                                .executes(context -> {
                                    int teamId = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "team");
                                    com.example.examplemod.world.TeamData data = com.example.examplemod.world.TeamData.get(context.getSource().getLevel());

                                    java.util.List<net.minecraft.core.BlockPos> spawns = (teamId == 1) ? data.spawns1 : data.spawns2;
                                    String teamName = (teamId == 1) ? "ОДИНОЧКИ" : "БАНДОСЫ";

                                    if (spawns.isEmpty()) {
                                        context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§c[Система] §fУ группировки " + teamName + " пока нет точек спавна."), false);
                                        return 1;
                                    }

                                    context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§a[Система] §fТочки спавна (" + teamName + "):"), false);
                                    for (int i = 0; i < spawns.size(); i++) {
                                        net.minecraft.core.BlockPos pos = spawns.get(i);
                                        final int index = i; // Обязательно финальная переменная для лямбды
                                        context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("  §e[" + index + "] §f- " + pos.toShortString()), false);
                                    }
                                    return 1;
                                })))

                // Удаление конкретной точки спавна по индексу
                .then(Commands.literal("delspawn")
                        .then(Commands.argument("team", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 2))
                                .then(Commands.argument("index", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            int teamId = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "team");
                                            int index = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "index");
                                            com.example.examplemod.world.TeamData data = com.example.examplemod.world.TeamData.get(context.getSource().getLevel());

                                            java.util.List<net.minecraft.core.BlockPos> spawns = (teamId == 1) ? data.spawns1 : data.spawns2;
                                            String teamName = (teamId == 1) ? "ОДИНОЧКИ" : "БАНДОСЫ";

                                            if (index >= 0 && index < spawns.size()) {
                                                net.minecraft.core.BlockPos removedPos = spawns.remove(index);
                                                data.setDirty(); // Обязательно сохраняем изменения в файл мира
                                                context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§a[Система] §fТочка §e[" + index + "] §f(" + removedPos.toShortString() + ") успешно удалена у команды " + teamName), true);
                                            } else {
                                                context.getSource().sendFailure(net.minecraft.network.chat.Component.literal("§c[Ошибка] Неверный индекс! У этой команды всего " + spawns.size() + " точек спавна (от 0 до " + (spawns.size() - 1) + ")."));
                                            }
                                            return 1;
                                        }))))

                // Установка режима игры
                .then(Commands.literal("mode")
                        .then(Commands.argument("modeName", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(context -> {
                                    String mode = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "modeName").toLowerCase();
                                    if (!mode.equals("deathmatch") && !mode.equals("domination")) {
                                        context.getSource().sendFailure(net.minecraft.network.chat.Component.literal("§cДоступные режимы: deathmatch, domination"));
                                        return 1;
                                    }
                                    TeamData data = TeamData.get(context.getSource().getLevel());
                                    data.gameMode = mode;
                                    data.sync();
                                    context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§a[Система] §fУстановлен режим: §e" + mode), true);
                                    return 1;
                                })))

                // Установка лимита очков
                .then(Commands.literal("maxscore")
                        .then(Commands.argument("score", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    int score = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "score");
                                    TeamData data = TeamData.get(context.getSource().getLevel());
                                    data.targetScore = score;
                                    data.sync();
                                    context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§a[Система] §fОчков для победы: §e" + score), true);
                                    return 1;
                                })))

                // Установка границ зоны (1 или 2)
                .then(Commands.literal("setzone")
                        .then(Commands.argument("point", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 2))
                                .executes(context -> {
                                    int point = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "point");
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    TeamData data = TeamData.get(player.serverLevel());

                                    if (point == 1) data.zonePos1 = player.blockPosition();
                                    else data.zonePos2 = player.blockPosition();

                                    data.sync();
                                    context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§a[Система] §fТочка зоны " + point + " установлена на " + player.blockPosition().toShortString()), true);
                                    return 1;
                                })))

                .then(Commands.literal("clear")
                        .requires(source -> source.hasPermission(2))
                        // 1. Очистка общего счета команд
                        .then(Commands.literal("score")
                                .executes(context -> {
                                    TeamData data = TeamData.get(context.getSource().getLevel());
                                    data.score1 = 0;
                                    data.score2 = 0;
                                    data.sync(); // Синхронизация с клиентами
                                    context.getSource().sendSuccess(() -> Component.literal("§6[Система] §fСчёт группировок ОДИНОЧКИ и БАНДОСЫ сброшен."), true);
                                    return 1;
                                }))
                        // 2. Очистка статистики конкретного игрока
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
                        // 3. Удаление всех созданных китов
                                .then(Commands.literal("kits")
                                        .executes(context -> {
                                            com.example.examplemod.config.KitConfig.KITS.clear();
                                            com.example.examplemod.config.KitConfig.save(); // Перезаписываем файл как пустой

                                            PacketHandler.sendKitsToAll(com.example.examplemod.config.KitConfig.KITS);
                                            context.getSource().sendSuccess(() -> Component.literal("§6[Система] §fВсе наборы снаряжения успешно удалены из конфигурации."), true);
                                            return 1;
                                        }))
                        // 4. Удаление точек спавна
                        .then(Commands.literal("spawns")
                                .executes(context -> {
                                    TeamData data = TeamData.get(context.getSource().getLevel());
                                    // Очищаем списки
                                    data.spawns1.clear();
                                    data.spawns2.clear();
                                    data.setDirty();
                                    context.getSource().sendSuccess(() -> Component.literal("§6[Система] §fВсе точки респавна группировок успешно удалены."), true);
                                    return 1;
                                }))
                )
        );
    }

    // --- ЛОГИКА КОМАНД ---

    private static int joinTeam(CommandContext<CommandSourceStack> context) {
        try {
            // Получаем игрока, который ввел команду
            ServerPlayer player = context.getSource().getPlayerOrException();
            // Получаем аргумент (номер команды)
            int teamId = IntegerArgumentType.getInteger(context, "teamId");

            // Получаем наши данные мира
            TeamData data = TeamData.get(player.serverLevel());

            // Записываем игрока
            data.joinTeam(teamId, player.getUUID());

            // Пишем ответ в чат
            context.getSource().sendSuccess(() -> Component.literal("Вы вошли в Команду " + teamId), false);
            return 1; // Успех
        } catch (Exception e) {
            return 0; // Ошибка
        }
    }

    private static int checkScore(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            TeamData data = TeamData.get(player.serverLevel());

            String message = "Счет: Команда 1 [" + data.score1 + "] - [" + data.score2 + "] Команда 2";
            context.getSource().sendSuccess(() -> Component.literal(message), false);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int addScore(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            int teamId = IntegerArgumentType.getInteger(context, "teamId");
            int points = IntegerArgumentType.getInteger(context, "points");

            TeamData data = TeamData.get(player.serverLevel());
            data.addScore(teamId, points);

            context.getSource().sendSuccess(() -> Component.literal("Очки добавлены!"), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
    private static int setSpawn(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            int teamId = IntegerArgumentType.getInteger(context, "teamId");

            // Берем текущую позицию игрока (округляем до блока)
            BlockPos pos = player.blockPosition();

            TeamData data = TeamData.get(player.serverLevel());
            data.setSpawn(teamId, pos);
            String teamName = (teamId == 1) ? "ОДИНОЧКИ" : "БАНДОСЫ";
            context.getSource().sendSuccess(() -> Component.literal("Точка спавна команды " + teamName + " установлена на " + pos.toShortString()), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
}