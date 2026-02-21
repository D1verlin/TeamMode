package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.ClientTeamData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    private static boolean needToOpenTeamScreen = false;
    private static int teamDelayTimer = 0;

    private static boolean needToOpenKitScreen = false;
    private static int kitDelayTimer = 0;

    // НОВОЕ: Переменная, которая следит, первый ли это заход
    private static boolean firstJoin = true;

    // Сбрасываем переменную при выходе с сервера
    @SubscribeEvent
    public static void onLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
        firstJoin = true;
    }

    // Событие: Игрок зашел в мир
    @SubscribeEvent
    public static void onJoinWorld(EntityJoinLevelEvent event) {
        if (event.getEntity() == Minecraft.getInstance().player) {
            // Запускаем проверку команды ТОЛЬКО при первом заходе, а не при каждом возрождении
            if (firstJoin) {
                needToOpenTeamScreen = true;
                teamDelayTimer = 20;
                firstJoin = false;
            }
        }
    }

    // Событие: Игрок возродился
    @SubscribeEvent
    public static void onRespawn(ClientPlayerNetworkEvent.Clone event) {
        needToOpenKitScreen = true;
        kitDelayTimer = 10;
    }

    // Главный цикл
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {

            // НОВАЯ ЗАЩИТА: Если игрок уже сам открыл какое-то меню или уже смотрит в него - отменяем авто-открытие
            if (Minecraft.getInstance().screen instanceof KitSelectorScreen || Minecraft.getInstance().screen instanceof TeamSelectScreen) {
                needToOpenTeamScreen = false;
                needToOpenKitScreen = false;
            }

            // --- Логика открытия меню КОМАНДЫ ---
            if (needToOpenTeamScreen) {
                if (teamDelayTimer > 0) {
                    teamDelayTimer--;
                } else {
                    needToOpenTeamScreen = false;

                    java.util.UUID myUUID = Minecraft.getInstance().player.getUUID();
                    int myTeam = ClientTeamData.getPlayerTeam(myUUID);

                    if (myTeam == 0) {
                        Minecraft.getInstance().setScreen(new TeamSelectScreen());
                    } else {
                        needToOpenKitScreen = true;
                        kitDelayTimer = 5;
                    }
                }
            }

            // --- Логика открытия меню КИТОВ ---
            if (needToOpenKitScreen) {
                if (kitDelayTimer > 0) {
                    kitDelayTimer--;
                } else {
                    needToOpenKitScreen = false;
                    if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.isAlive()) {
                        Minecraft.getInstance().setScreen(new KitSelectorScreen());
                    }
                }
            }
        }
    }

    // Событие чата: Для открытия редактора админом (.kitadmin)
    @SubscribeEvent
    public static void onChat(ClientChatEvent event) {
        if (event.getMessage().equals(".kitadmin")) {
            Minecraft.getInstance().setScreen(new KitEditorScreen());
            event.setCanceled(true);
        }
    }

    // Обработка клавиш (M и K), если ты оставлял этот метод
    @SubscribeEvent
    public static void onKeyInput(net.minecraftforge.client.event.InputEvent.Key event) {
        if (KeyInit.OPEN_TEAM_MENU.consumeClick()) {
            Minecraft.getInstance().setScreen(new TeamSelectScreen());
        }

        if (KeyInit.OPEN_KIT_EDITOR.consumeClick()) {
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissions(2)) {
                Minecraft.getInstance().setScreen(new KitEditorScreen());
            } else {
                Minecraft.getInstance().setScreen(new KitEditorScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        // Проверяем, что игра пытается нарисовать ник именно ИГРОКА
        if (event.getEntity() instanceof Player targetPlayer) {

            // ВАРИАНТ 1: Скрыть ники ВООБЩЕ У ВСЕХ (как в Таркове или хардкорных модах)
            event.setResult(Event.Result.DENY);

            /* // ВАРИАНТ 2: Скрыть ники ТОЛЬКО У ВРАГОВ (своих видно)
            // Если захочешь использовать этот вариант - удали строку выше и раскомментируй этот блок:

            if (Minecraft.getInstance().player != null) {
                java.util.UUID myUUID = Minecraft.getInstance().player.getUUID();
                java.util.UUID targetUUID = targetPlayer.getUUID();

                int myTeam = ClientTeamData.getPlayerTeam(myUUID);
                int targetTeam = ClientTeamData.getPlayerTeam(targetUUID);

                // Если команды разные (враги) или кто-то без команды - прячем ник
                if (myTeam != targetTeam || myTeam == 0) {
                    event.setResult(Event.Result.DENY);
                }
            }
            */
        }
    }
}