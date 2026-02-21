package com.example.examplemod.network;

import net.minecraft.core.BlockPos;

import java.util.*;

public class ClientTeamData {
    public static int clientScore1 = 0;
    public static int clientScore2 = 0;

    public static List<UUID> team1Players = new ArrayList<>();
    public static List<UUID> team2Players = new ArrayList<>();

    // Новые карты
    public static Map<UUID, Integer> kills = new HashMap<>();
    public static Map<UUID, Integer> deaths = new HashMap<>();

    public static int clientTargetScore = 100;

    public static void set(int s1, int s2, List<UUID> t1, List<UUID> t2, Map<UUID, Integer> k, Map<UUID, Integer> d) {
        clientScore1 = s1;
        clientScore2 = s2;
        team1Players = t1;
        team2Players = t2;
        kills = k;
        deaths = d;
    }

    public static int getPlayerTeam(UUID uuid) {
        if (team1Players.contains(uuid)) return 1;
        if (team2Players.contains(uuid)) return 2;
        return 0;
    }

    public static Map<UUID, Integer> clientKills = new HashMap<>();
    public static Map<UUID, Integer> clientDeaths = new HashMap<>();

    // --- НОВЫЕ ПЕРЕМЕННЫЕ ДЛЯ ЗОНЫ ---
    public static String currentGameMode = "deathmatch";
    // Координаты могут быть null, если зона не задана
    public static BlockPos clientZonePos1 = null;
    public static BlockPos clientZonePos2 = null;
    public static int clientZoneOwner = 0; // 0 - ничья, 1 - Одиночки, 2 - Бандосы

}