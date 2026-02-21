package com.example.examplemod.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KitData extends SavedData {

    // Храним наборы: Имя -> Список предметов
    public Map<String, List<ItemStack>> kits = new HashMap<>();

    public static KitData load(CompoundTag nbt) {
        KitData data = new KitData();
        CompoundTag kitsTag = nbt.getCompound("Kits");

        for (String key : kitsTag.getAllKeys()) {
            ListTag listTag = kitsTag.getList(key, 10); // 10 = CompoundTag
            List<ItemStack> items = new ArrayList<>();

            for (int i = 0; i < listTag.size(); i++) {
                items.add(ItemStack.of(listTag.getCompound(i)));
            }
            data.kits.put(key, items);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        CompoundTag kitsTag = new CompoundTag();

        for (Map.Entry<String, List<ItemStack>> entry : kits.entrySet()) {
            ListTag listTag = new ListTag();
            for (ItemStack stack : entry.getValue()) {
                CompoundTag itemTag = new CompoundTag();
                stack.save(itemTag);
                listTag.add(itemTag);
            }
            kitsTag.put(entry.getKey(), listTag);
        }

        nbt.put("Kits", kitsTag);
        return nbt;
    }

    public static KitData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(KitData::load, KitData::new, "TeamBattleKits");
    }

    // Сохранить инвентарь игрока как Кит
    public void saveKit(String name, Inventory inventory) {
        List<ItemStack> items = new ArrayList<>();
        // Сохраняем основной инвентарь и броню
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
        kits.put(name, items);
        setDirty();
    }

    public void deleteKit(String name) {
        kits.remove(name);
        setDirty(); // Сохраняем изменения на диск
    }
}