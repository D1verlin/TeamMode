package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.ClientTeamData;
import com.example.examplemod.network.PacketToggleShop;
import com.example.examplemod.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.ScreenEvent;
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

    private static boolean firstJoin = true;

    @SubscribeEvent
    public static void onLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
        firstJoin = true;
    }

    @SubscribeEvent
    public static void onJoinWorld(EntityJoinLevelEvent event) {
        if (event.getEntity() == Minecraft.getInstance().player) {
            if (firstJoin) {
                needToOpenTeamScreen = true;
                teamDelayTimer = 20;
                firstJoin = false;
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(ClientPlayerNetworkEvent.Clone event) {
        needToOpenKitScreen = true;
        kitDelayTimer = 10;
    }

    // НОВОЕ: Перехват открытия стандартного инвентаря
    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        // Проверяем, что открывается ванильный инвентарь и это НЕ наш кастомный инвентарь
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen && !(event.getScreen() instanceof PubgInventoryScreen)) {
            Minecraft mc = Minecraft.getInstance();
            // Проверяем, что игрок не в креативе
            if (mc.player != null && !mc.player.isCreative()) {
                // Подменяем экран
                event.setNewScreen(new PubgInventoryScreen(mc.player));
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {

            if (ClientTeamData.currentGameMode.equals("shop")) {
                if (Minecraft.getInstance().screen instanceof KitSelectorScreen) {
                    Minecraft.getInstance().setScreen(null);
                }
            }
            if (Minecraft.getInstance().screen instanceof KitSelectorScreen || Minecraft.getInstance().screen instanceof TeamSelectScreen) {
                needToOpenTeamScreen = false;
                needToOpenKitScreen = false;
            }

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

    @SubscribeEvent
    public static void onChat(ClientChatEvent event) {
        if (event.getMessage().equals(".kitadmin")) {
            Minecraft.getInstance().setScreen(new KitEditorScreen());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(net.minecraftforge.client.event.InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (KeyInit.OPEN_TEAM_MENU.consumeClick()) {
            mc.setScreen(new TeamSelectScreen());
        }

        if (KeyInit.OPEN_KIT_EDITOR.consumeClick()) {
            mc.setScreen(new KitEditorScreen());
        }

        if (KeyInit.TOGGLE_SHOP_MODE.consumeClick()) {
            if (mc.player.hasPermissions(2)) {
                PacketHandler.INSTANCE.sendToServer(new PacketToggleShop());
            }
        }
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (event.getEntity() instanceof Player targetPlayer) {
            event.setResult(Event.Result.DENY);
        }
    }
}