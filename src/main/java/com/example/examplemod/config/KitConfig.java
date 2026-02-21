package com.example.examplemod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KitConfig {

    // Файл будет лежать в папке config/teammod_kits.json
    private static final File FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "teammod_kits.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Главное хранилище наборов
    public static final Map<String, List<ItemStack>> KITS = new HashMap<>();

    public static void load() {
        KITS.clear();
        if (!FILE.exists()) return;

        try (FileReader reader = new FileReader(FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) return;

            for (String kitName : json.keySet()) {
                JsonArray arr = json.getAsJsonArray(kitName);
                List<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < arr.size(); i++) {
                    String snbt = arr.get(i).getAsString();
                    CompoundTag tag = TagParser.parseTag(snbt); // Парсим строку обратно в NBT
                    items.add(ItemStack.of(tag));               // Создаем предмет из NBT
                }
                KITS.put(kitName, items);
            }
        } catch (Exception e) {
            System.err.println("Ошибка при загрузке teammod_kits.json");
            e.printStackTrace();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            JsonObject json = new JsonObject();

            for (Map.Entry<String, List<ItemStack>> entry : KITS.entrySet()) {
                JsonArray arr = new JsonArray();
                for (ItemStack stack : entry.getValue()) {
                    CompoundTag tag = new CompoundTag();
                    stack.save(tag); // Сохраняем предмет в NBT
                    arr.add(tag.toString()); // Конвертируем NBT в строку и пишем в JSON
                }
                json.add(entry.getKey(), arr);
            }

            GSON.toJson(json, writer);
        } catch (Exception e) {
            System.err.println("Ошибка при сохранении teammod_kits.json");
            e.printStackTrace();
        }
    }
}