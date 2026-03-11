package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";

    // ИСПРАВЛЕНО: Теперь передаем одной строкой (через двоеточие)
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ExampleMod.MODID + ":main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++, PacketSyncScore.class, PacketSyncScore::toBytes, PacketSyncScore::new, PacketSyncScore::handle);
        INSTANCE.registerMessage(id++, PacketChooseTeam.class, PacketChooseTeam::toBytes, PacketChooseTeam::new, PacketChooseTeam::handle);
        INSTANCE.registerMessage(id++, PacketSyncKits.class, PacketSyncKits::toBytes, PacketSyncKits::new, PacketSyncKits::handle);
        INSTANCE.registerMessage(id++, PacketSaveKit.class, PacketSaveKit::toBytes, PacketSaveKit::new, PacketSaveKit::handle);
        INSTANCE.registerMessage(id++, PacketSelectKit.class, PacketSelectKit::toBytes, PacketSelectKit::new, PacketSelectKit::handle);
        INSTANCE.registerMessage(id++, PacketDeleteKit.class, PacketDeleteKit::toBytes, PacketDeleteKit::new, PacketDeleteKit::handle);
        INSTANCE.registerMessage(id++, PacketToggleShop.class, PacketToggleShop::toBytes, PacketToggleShop::new, PacketToggleShop::handle);
        INSTANCE.registerMessage(id++, PacketKillstreak.class, PacketKillstreak::toBytes, PacketKillstreak::new, PacketKillstreak::handle);
    }

    public static void sendToServer(int teamId) {
        INSTANCE.sendToServer(new PacketChooseTeam(teamId));
    }

    public static void sendScoreUpdate(int s1, int s2, List<UUID> t1, List<UUID> t2,
                                       Map<UUID, Integer> kills, Map<UUID, Integer> deaths,
                                       String mode, BlockPos zp1, BlockPos zp2, int zOwner, int targetScore,
                                       BlockPos sa1, BlockPos sa2, BlockPos sb1, BlockPos sb2) {
        INSTANCE.send(PacketDistributor.ALL.noArg(),
                new PacketSyncScore(s1, s2, t1, t2, kills, deaths, mode, zp1, zp2, zOwner, targetScore, sa1, sa2, sb1, sb2));
    }

    public static void sendScoreToPlayer(ServerPlayer player, int s1, int s2, List<UUID> t1, List<UUID> t2,
                                         Map<UUID, Integer> kills, Map<UUID, Integer> deaths,
                                         String mode, BlockPos zp1, BlockPos zp2, int zOwner, int targetScore,
                                         BlockPos sa1, BlockPos sa2, BlockPos sb1, BlockPos sb2) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new PacketSyncScore(s1, s2, t1, t2, kills, deaths, mode, zp1, zp2, zOwner, targetScore, sa1, sa2, sb1, sb2));
    }

    public static void sendKitsToAll(java.util.Map<String, java.util.List<net.minecraft.world.item.ItemStack>> kits) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketSyncKits(kits));
    }

    public static void sendKitsToPlayer(ServerPlayer player, java.util.Map<String, java.util.List<net.minecraft.world.item.ItemStack>> kits) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PacketSyncKits(kits));
    }
}