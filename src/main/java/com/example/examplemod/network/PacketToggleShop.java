package com.example.examplemod.network;

import com.example.examplemod.world.TeamData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketToggleShop {
    public PacketToggleShop() {}
    public PacketToggleShop(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.hasPermissions(2)) {
                TeamData data = TeamData.get(player.serverLevel());

                // Переключаем режим
                if (data.gameMode.equals("shop")) {
                    data.gameMode = "deathmatch";
                } else {
                    data.gameMode = "shop";
                }

                data.sync();
                String status = data.gameMode.equals("shop") ? "§aВКЛЮЧЕН" : "§cВЫКЛЮЧЕН";
                player.displayClientMessage(Component.literal("§6[Система] §fРежим магазина: " + status), true);
            }
        });
        return true;
    }
}