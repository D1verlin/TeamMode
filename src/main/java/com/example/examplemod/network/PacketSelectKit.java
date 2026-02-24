package com.example.examplemod.network;

import com.example.examplemod.world.TeamData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class PacketSelectKit {
    private final int kitId;

    public PacketSelectKit(int id) {
        this.kitId = id;
    }

    public PacketSelectKit(FriendlyByteBuf buf) {
        this.kitId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(kitId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                TeamData data = TeamData.get(player.serverLevel());
                player.getInventory().clearContent(); // Очищаем инвентарь перед выдачей

                // Определяем, какой кит выдавать
                int targetKitId = kitId;

                // В режиме магазина подменяем ID кита на командный
                if (data.gameMode.equals("shop")) {
                    int teamId = data.getTeamOf(player.getUUID());
                    targetKitId = (teamId == 1) ? 4 : 5;
                }

                // Загружаем кит из сохраненной конфигурации
                List<ItemStack> items = com.example.examplemod.config.KitConfig.KITS.get("kit" + targetKitId);

                if (items != null && !items.isEmpty()) {
                    // Проходимся по всем сохраненным слотам
                    for (int i = 0; i < items.size() && i < player.getInventory().getContainerSize(); i++) {
                        ItemStack stack = items.get(i);
                        // Если предмет в этом слоте был не пустой, кладем его в тот же самый слот
                        if (!stack.isEmpty()) {
                            player.getInventory().setItem(i, stack.copy());
                        }
                    }
                }

                // Синхронизация изменений с клиентом
                player.containerMenu.broadcastChanges();
                player.inventoryMenu.broadcastChanges();
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}