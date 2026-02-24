package com.example.examplemod.network;

import com.example.examplemod.config.KitConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketSaveKit {
    private final int kitId;

    public PacketSaveKit(int id) {
        this.kitId = id;
    }

    public PacketSaveKit(FriendlyByteBuf buf) {
        this.kitId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(kitId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.hasPermissions(2)) {

                // Собираем ВСЕ слоты инвентаря админа (включая пустые), чтобы сохранить их индексы
                List<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    // Копируем предмет (пустой предмет скопируется как ItemStack.EMPTY)
                    items.add(stack.copy());
                }

                // Сохраняем в глобальный конфиг
                KitConfig.KITS.put("kit" + kitId, items);
                KitConfig.save(); // Записываем в файл

                // Отправляем обновленные данные всем
                PacketHandler.sendKitsToAll(KitConfig.KITS);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}