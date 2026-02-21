package com.example.examplemod.network;

import com.example.examplemod.client.ClientKitData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PacketSyncKits {
    public Map<String, List<ItemStack>> kits;

    // Конструктор отправки (принимает карту)
    public PacketSyncKits(Map<String, List<ItemStack>> kits) {
        this.kits = kits;
    }

    // Чтение (Клиент)
    public PacketSyncKits(FriendlyByteBuf buf) {
        kits = new HashMap<>();
        int mapSize = buf.readInt(); // Сколько всего наборов

        for (int i = 0; i < mapSize; i++) {
            String name = buf.readUtf();
            // Читаем список предметов для этого набора
            List<ItemStack> items = buf.readCollection(ArrayList::new, FriendlyByteBuf::readItem);
            kits.put(name, items);
        }
    }

    // Запись (Сервер)
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(kits.size()); // Пишем количество наборов

        for (Map.Entry<String, List<ItemStack>> entry : kits.entrySet()) {
            buf.writeUtf(entry.getKey()); // Пишем имя
            // Пишем список предметов
            buf.writeCollection(entry.getValue(), FriendlyByteBuf::writeItem);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Сохраняем полную карту предметов на клиенте
            ClientKitData.availableKits = kits;
        });
        return true;
    }
}