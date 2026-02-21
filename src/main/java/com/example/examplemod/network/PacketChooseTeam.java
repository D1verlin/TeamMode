package com.example.examplemod.network;

import com.example.examplemod.world.TeamData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class PacketChooseTeam {
    private final int teamId;

    public PacketChooseTeam(int teamId) {
        this.teamId = teamId;
    }

    public PacketChooseTeam(FriendlyByteBuf buf) {
        this.teamId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(teamId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                TeamData data = TeamData.get(level);

                // Присоединяем игрока к команде
                data.joinTeam(teamId, player.getUUID());

                // НОВОЕ: Получаем нужный список спавнов
                List<BlockPos> teamSpawns = (teamId == 1) ? data.spawns1 : data.spawns2;

                // Если список существует и в нём есть хотя бы одна точка
                if (teamSpawns != null && !teamSpawns.isEmpty()) {
                    // Выбираем случайную точку с помощью встроенного рандомизатора игрока
                    BlockPos randomSpawn = teamSpawns.get(player.getRandom().nextInt(teamSpawns.size()));

                    // Телепортируем игрока
                    player.teleportTo(level, randomSpawn.getX() + 0.5, randomSpawn.getY() + 1, randomSpawn.getZ() + 0.5, player.getYRot(), player.getXRot());
                }
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}