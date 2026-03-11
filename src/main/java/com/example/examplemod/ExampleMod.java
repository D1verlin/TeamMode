package com.example.examplemod;

import com.example.examplemod.commands.TeamCommands;
import com.example.examplemod.network.PacketHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// Аннотация указывает Forge, какой ID искать в mods.toml
@Mod(ExampleMod.MODID)
public class ExampleMod {

    // ВАЖНО: Это имя должно в точности совпадать с modId в файле mods.toml
    public static final String MODID = "teammod";

    // Логгер для вывода информации в консоль
    private static final Logger LOGGER = LogUtils.getLogger();

    public ExampleMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // --- РЕГИСТРИРУЕМ НАШИ НОВЫЕ КЛАССЫ ---
        com.example.examplemod.registry.ModBlocks.BLOCKS.register(modEventBus);
        com.example.examplemod.registry.ModItems.ITEMS.register(modEventBus);
        com.example.examplemod.registry.ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);

        // 1. Регистрация метода общей настройки
        modEventBus.addListener(this::commonSetup);

        // 2. Регистрация нашего класса в шине событий Minecraft Forge (нужно для команд и событий сервера)
        MinecraftForge.EVENT_BUS.register(this);

        // 3. Инициализация сетевых пакетов (чтобы работали очки и выбор команды)
        PacketHandler.register();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("MODID: {} загружен. Сеть и компоненты инициализированы.", MODID);
    }

    // Это событие срабатывает, когда сервер запускается
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Сервер TeamMod стартует...");
    }

    // Регистрация команд (например, /tb)
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        TeamCommands.register(event.getDispatcher());
    }

    // Вложенный класс для регистрации КЛИЕНТСКИХ штук (GUI, Оверлеи)
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void registerOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
            // Регистрируем наш HUD (плашку со счетом)
            event.registerAboveAll("score_hud", com.example.examplemod.client.ScoreHud.INSTANCE);

            // НОВОЕ: Регистрируем TAB меню поверх всего остального
            event.registerAboveAll("cs2_tab", com.example.examplemod.client.Cs2TabRenderer.INSTANCE);
        }
        @SubscribeEvent
        public static void registerKeys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
            event.register(com.example.examplemod.client.KeyInit.OPEN_TEAM_MENU);
            event.register(com.example.examplemod.client.KeyInit.OPEN_KIT_EDITOR);
        }
    }
}