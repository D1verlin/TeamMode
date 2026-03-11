package com.example.examplemod.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class PacketSyncScore {
    private final int score1;
    private final int score2;
    private final List<UUID> team1;
    private final List<UUID> team2;
    private final Map<UUID, Integer> kills;
    private final Map<UUID, Integer> deaths;
    private final String gameMode;
    private final BlockPos zonePos1;
    private final BlockPos zonePos2;
    private final int zoneOwner;
    private final int targetScore;

    // --- ПЛЭНТЫ А и Б ---
    private final BlockPos siteAPos1, siteAPos2, siteBPos1, siteBPos2;

    public PacketSyncScore(int s1, int s2, List<UUID> t1, List<UUID> t2, Map<UUID, Integer> kills, Map<UUID, Integer> deaths,
                           String gameMode, BlockPos zp1, BlockPos zp2, int zOwner, int targetScore,
                           BlockPos sa1, BlockPos sa2, BlockPos sb1, BlockPos sb2) {
        this.score1 = s1; this.score2 = s2;
        this.team1 = t1; this.team2 = t2;
        this.kills = kills; this.deaths = deaths;
        this.gameMode = gameMode;
        this.zonePos1 = zp1; this.zonePos2 = zp2;
        this.zoneOwner = zOwner; this.targetScore = targetScore;
        this.siteAPos1 = sa1; this.siteAPos2 = sa2;
        this.siteBPos1 = sb1; this.siteBPos2 = sb2;
    }

    public PacketSyncScore(FriendlyByteBuf buf) {
        this.score1 = buf.readInt(); this.score2 = buf.readInt();
        int t1Size = buf.readInt(); this.team1 = new ArrayList<>(t1Size);
        for (int i = 0; i < t1Size; i++) this.team1.add(buf.readUUID());
        int t2Size = buf.readInt(); this.team2 = new ArrayList<>(t2Size);
        for (int i = 0; i < t2Size; i++) this.team2.add(buf.readUUID());
        int kSize = buf.readInt(); this.kills = new HashMap<>(kSize);
        for(int i = 0; i < kSize; i++) this.kills.put(buf.readUUID(), buf.readInt());
        int dSize = buf.readInt(); this.deaths = new HashMap<>(dSize);
        for(int i = 0; i < dSize; i++) this.deaths.put(buf.readUUID(), buf.readInt());

        this.gameMode = buf.readUtf();
        this.zonePos1 = buf.readBoolean() ? buf.readBlockPos() : null;
        this.zonePos2 = buf.readBoolean() ? buf.readBlockPos() : null;
        this.zoneOwner = buf.readInt();
        this.targetScore = buf.readInt();

        // Читаем Плэнты
        this.siteAPos1 = buf.readBoolean() ? buf.readBlockPos() : null;
        this.siteAPos2 = buf.readBoolean() ? buf.readBlockPos() : null;
        this.siteBPos1 = buf.readBoolean() ? buf.readBlockPos() : null;
        this.siteBPos2 = buf.readBoolean() ? buf.readBlockPos() : null;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(score1); buf.writeInt(score2);
        buf.writeInt(team1.size()); for (UUID id : team1) buf.writeUUID(id);
        buf.writeInt(team2.size()); for (UUID id : team2) buf.writeUUID(id);
        buf.writeInt(kills.size()); for(Map.Entry<UUID, Integer> e : kills.entrySet()) { buf.writeUUID(e.getKey()); buf.writeInt(e.getValue()); }
        buf.writeInt(deaths.size()); for(Map.Entry<UUID, Integer> e : deaths.entrySet()) { buf.writeUUID(e.getKey()); buf.writeInt(e.getValue()); }

        buf.writeUtf(gameMode);
        buf.writeBoolean(zonePos1 != null); if (zonePos1 != null) buf.writeBlockPos(zonePos1);
        buf.writeBoolean(zonePos2 != null); if (zonePos2 != null) buf.writeBlockPos(zonePos2);
        buf.writeInt(zoneOwner); buf.writeInt(targetScore);

        // Пишем Плэнты
        buf.writeBoolean(siteAPos1 != null); if (siteAPos1 != null) buf.writeBlockPos(siteAPos1);
        buf.writeBoolean(siteAPos2 != null); if (siteAPos2 != null) buf.writeBlockPos(siteAPos2);
        buf.writeBoolean(siteBPos1 != null); if (siteBPos1 != null) buf.writeBlockPos(siteBPos1);
        buf.writeBoolean(siteBPos2 != null); if (siteBPos2 != null) buf.writeBlockPos(siteBPos2);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ClientTeamData.clientScore1 = score1; ClientTeamData.clientScore2 = score2;
            ClientTeamData.team1Players = team1; ClientTeamData.team2Players = team2;
            ClientTeamData.kills = kills; ClientTeamData.deaths = deaths;
            ClientTeamData.currentGameMode = gameMode;
            ClientTeamData.clientZonePos1 = zonePos1; ClientTeamData.clientZonePos2 = zonePos2;
            ClientTeamData.clientZoneOwner = zoneOwner; ClientTeamData.clientTargetScore = targetScore;

            // Сохраняем Плэнты на клиенте
            ClientTeamData.clientSiteAPos1 = siteAPos1; ClientTeamData.clientSiteAPos2 = siteAPos2;
            ClientTeamData.clientSiteBPos1 = siteBPos1; ClientTeamData.clientSiteBPos2 = siteBPos2;
        });
        context.setPacketHandled(true);
        return true;
    }
}