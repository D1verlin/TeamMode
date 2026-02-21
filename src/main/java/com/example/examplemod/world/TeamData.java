package com.example.examplemod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class TeamData extends SavedData {

    // Списки игроков в командах
    public List<UUID> team1 = new ArrayList<>();
    public List<UUID> team2 = new ArrayList<>();

    // Списки точек возрождения (Множественные спавны)
    public List<BlockPos> spawns1 = new ArrayList<>();
    public List<BlockPos> spawns2 = new ArrayList<>();

    // Счет команд
    public int score1 = 0;
    public int score2 = 0;

    // Хранилище статистики (UUID -> Количество)
    public Map<UUID, Integer> playerKills = new HashMap<>();
    public Map<UUID, Integer> playerDeaths = new HashMap<>();

    // --- НОВЫЕ ПЕРЕМЕННЫЕ ДЛЯ РЕЖИМОВ ---
    public String gameMode = "deathmatch"; // "deathmatch" или "domination"
    public int targetScore = 100; // Очков для победы

    // Координаты зоны удержания (две точки образуют куб/параллелепипед)
    public BlockPos zonePos1 = null;
    public BlockPos zonePos2 = null;
    public int zoneOwner = 0; // 0 - ничья, 1 - Одиночки, 2 - Бандосы
    // ------------------------------------

    public static TeamData load(CompoundTag nbt) {
        TeamData data = new TeamData();
        data.score1 = nbt.getInt("Score1");
        data.score2 = nbt.getInt("Score2");

        // Загрузка игроков команды 1
        data.team1.clear();
        int t1Size = nbt.getInt("Team1Size");
        for (int i = 0; i < t1Size; i++) {
            data.team1.add(nbt.getUUID("Team1_" + i));
        }

        // Загрузка игроков команды 2
        data.team2.clear();
        int t2Size = nbt.getInt("Team2Size");
        for (int i = 0; i < t2Size; i++) {
            data.team2.add(nbt.getUUID("Team2_" + i));
        }

        // Загрузка списка спавнов Одиночек
        if (nbt.contains("Spawns1", Tag.TAG_LIST)) {
            ListTag list1 = nbt.getList("Spawns1", Tag.TAG_COMPOUND);
            for (int i = 0; i < list1.size(); i++) {
                data.spawns1.add(NbtUtils.readBlockPos(list1.getCompound(i)));
            }
        }

        // Загрузка списка спавнов Бандосов
        if (nbt.contains("Spawns2", Tag.TAG_LIST)) {
            ListTag list2 = nbt.getList("Spawns2", Tag.TAG_COMPOUND);
            for (int i = 0; i < list2.size(); i++) {
                data.spawns2.add(NbtUtils.readBlockPos(list2.getCompound(i)));
            }
        }

        // Загрузка статистики
        loadStats(nbt.getCompound("PlayerKills"), data.playerKills);
        loadStats(nbt.getCompound("PlayerDeaths"), data.playerDeaths);

        // --- Загрузка параметров режима ---
        data.gameMode = nbt.contains("GameMode") ? nbt.getString("GameMode") : "deathmatch";
        data.targetScore = nbt.contains("TargetScore") ? nbt.getInt("TargetScore") : 100;
        if (nbt.contains("ZonePos1")) data.zonePos1 = NbtUtils.readBlockPos(nbt.getCompound("ZonePos1"));
        if (nbt.contains("ZonePos2")) data.zonePos2 = NbtUtils.readBlockPos(nbt.getCompound("ZonePos2"));

        return data;
    }

    private static void loadStats(CompoundTag tag, Map<UUID, Integer> map) {
        map.clear();
        for (String key : tag.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                map.put(uuid, tag.getInt(key));
            } catch (Exception ignored) {}
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.putInt("Score1", score1);
        nbt.putInt("Score2", score2);

        // Сохранение игроков команды 1
        nbt.putInt("Team1Size", team1.size());
        for (int i = 0; i < team1.size(); i++) {
            nbt.putUUID("Team1_" + i, team1.get(i));
        }

        // Сохранение игроков команды 2
        nbt.putInt("Team2Size", team2.size());
        for (int i = 0; i < team2.size(); i++) {
            nbt.putUUID("Team2_" + i, team2.get(i));
        }

        // Сохранение списка спавнов Одиночек
        ListTag list1 = new ListTag();
        for (BlockPos pos : spawns1) {
            list1.add(NbtUtils.writeBlockPos(pos));
        }
        nbt.put("Spawns1", list1);

        // Сохранение списка спавнов Бандосов
        ListTag list2 = new ListTag();
        for (BlockPos pos : spawns2) {
            list2.add(NbtUtils.writeBlockPos(pos));
        }
        nbt.put("Spawns2", list2);

        // Сохранение статистики
        nbt.put("PlayerKills", saveStats(playerKills));
        nbt.put("PlayerDeaths", saveStats(playerDeaths));

        // --- Сохранение параметров режима ---
        nbt.putString("GameMode", gameMode);
        nbt.putInt("TargetScore", targetScore);
        if (zonePos1 != null) nbt.put("ZonePos1", NbtUtils.writeBlockPos(zonePos1));
        if (zonePos2 != null) nbt.put("ZonePos2", NbtUtils.writeBlockPos(zonePos2));

        return nbt;
    }

    private CompoundTag saveStats(Map<UUID, Integer> map) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<UUID, Integer> entry : map.entrySet()) {
            tag.putInt(entry.getKey().toString(), entry.getValue());
        }
        return tag;
    }

    public static TeamData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TeamData::load, TeamData::new, "TeamBattleData");
    }

    public boolean joinTeam(int teamId, UUID playerUUID) {
        team1.remove(playerUUID);
        team2.remove(playerUUID);
        if (teamId == 1) team1.add(playerUUID);
        else if (teamId == 2) team2.add(playerUUID);
        sync();
        return true;
    }

    // Метод добавления статистики
    public void addKill(UUID player) {
        playerKills.put(player, playerKills.getOrDefault(player, 0) + 1);
        sync();
    }

    public void addDeath(UUID player) {
        playerDeaths.put(player, playerDeaths.getOrDefault(player, 0) + 1);
        sync();
    }

    public void addScore(int teamId, int points) {
        if (teamId == 1) score1 += points;
        if (teamId == 2) score2 += points;
        sync();
    }

    // Вспомогательный метод для добавления точки спавна
    public void setSpawn(int teamId, BlockPos pos) {
        if (teamId == 1) spawns1.add(pos);
        if (teamId == 2) spawns2.add(pos);
        setDirty();
    }

    public int getTeamOf(UUID playerUUID) {
        if (team1.contains(playerUUID)) return 1;
        if (team2.contains(playerUUID)) return 2;
        return 0;
    }

    public void sync() {
        setDirty();
        com.example.examplemod.network.PacketHandler.sendScoreUpdate(
                score1, score2, team1, team2, playerKills, playerDeaths,
                gameMode, zonePos1, zonePos2, zoneOwner, targetScore // Передаем targetScore
        );
    }
}