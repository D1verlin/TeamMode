package com.example.examplemod.network;

import com.example.examplemod.world.KitData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketDeleteKit {
    private final String kitName;

    public PacketDeleteKit(String name) {
        this.kitName = name;
    }

    public PacketDeleteKit(FriendlyByteBuf buf) {
        this.kitName = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(kitName);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            // Проверка прав (2 = оператор/админ)
            if (player != null && player.hasPermissions(2)) {
                KitData data = KitData.get(player.serverLevel());

                // Удаляем кит
                data.deleteKit(kitName);

                // Сразу же отправляем всем обновленный список, чтобы кнопка исчезла и у других
                PacketHandler.sendKitsToAll(data.kits);
            }
        });
        return true;
    }
}