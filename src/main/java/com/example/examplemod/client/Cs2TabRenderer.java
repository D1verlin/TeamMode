package com.example.examplemod.client;

import com.example.examplemod.network.ClientTeamData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Cs2TabRenderer implements IGuiOverlay {

    // Создаем экземпляр (инстанс) для регистрации оверлея
    public static final Cs2TabRenderer INSTANCE = new Cs2TabRenderer();

    private static final int COLOR_BG = 0xCC1e1e1e;
    private static final int COLOR_T1 = 0xFFd19a36;
    private static final int COLOR_T2 = 0xFF5d79ae;
    private static final int COLOR_HEADER = 0xFF888888; // Цвет заголовков колонок

    // 1. Отменяем ванильный TAB (чтобы не было двойной отрисовки)
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.PLAYER_LIST.type()) {
            event.setCanceled(true);
        }
    }

    // 2. Наш кастомный метод отрисовки из IGuiOverlay (выполняется поверх всего)
    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // Рисуем только если игрок удерживает кнопку TAB
        if (mc.options.keyPlayerList.isDown()) {
            guiGraphics.pose().pushPose();
            // Жестко сдвигаем весь TAB на передний план по оси Z (ближе к глазам игрока)
            guiGraphics.pose().translate(0, 0, 500f);

            renderCs2Tab(guiGraphics, screenWidth);

            guiGraphics.pose().popPose();
        }
    }

    private static void renderCs2Tab(GuiGraphics gui, int width) {
        Minecraft mc = Minecraft.getInstance();

        int midX = width / 2;
        int topY = 80;
        int panelWidth = 160;
        int panelHeight = 200;

        // Фон левой панели
        gui.fill(midX - panelWidth - 5, topY, midX - 5, topY + panelHeight, COLOR_BG);
        gui.fill(midX - panelWidth - 5, topY, midX - 5, topY + 2, COLOR_T1);

        // Фон правой панели
        gui.fill(midX + 5, topY, midX + panelWidth + 5, topY + panelHeight, COLOR_BG);
        gui.fill(midX + 5, topY, midX + panelWidth + 5, topY + 2, COLOR_T2);

        // Заголовки команд
        gui.drawCenteredString(mc.font, "ОДИНОЧКИ  (" + ClientTeamData.clientScore1 + ")", midX - panelWidth / 2 - 5, topY + 6, COLOR_T1);
        gui.drawCenteredString(mc.font, "БАНДОСЫ  (" + ClientTeamData.clientScore2 + ")", midX + panelWidth / 2 + 5, topY + 6, COLOR_T2);

        // Заголовки колонок
        drawHeaders(gui, mc, midX - panelWidth - 5, topY + 20, panelWidth);
        drawHeaders(gui, mc, midX + 5, topY + 20, panelWidth);

        // Списки игроков
        int startY = topY + 35;

        int i = 0;
        for (UUID uuid : ClientTeamData.team1Players) {
            if (mc.getConnection() != null && mc.getConnection().getPlayerInfo(uuid) != null) {
                drawPlayerRow(gui, mc, uuid, midX - panelWidth - 5, startY + (i * 14), panelWidth);
                i++;
            }
        }

        i = 0;
        for (UUID uuid : ClientTeamData.team2Players) {
            if (mc.getConnection() != null && mc.getConnection().getPlayerInfo(uuid) != null) {
                drawPlayerRow(gui, mc, uuid, midX + 5, startY + (i * 14), panelWidth);
                i++;
            }
        }
    }

    private static void drawHeaders(GuiGraphics gui, Minecraft mc, int x, int y, int width) {
        gui.drawString(mc.font, "K", x + width - 90, y, COLOR_HEADER);
        gui.drawString(mc.font, "D", x + width - 70, y, COLOR_HEADER);
        gui.drawString(mc.font, "K/D", x + width - 45, y, COLOR_HEADER);
        gui.drawString(mc.font, "MS", x + width - 20, y, COLOR_HEADER);
    }

    private static void drawPlayerRow(GuiGraphics gui, Minecraft mc, UUID uuid, int x, int y, int width) {
        PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
        String name = (info != null) ? info.getProfile().getName() : "Loading...";

        int kills = ClientTeamData.kills.getOrDefault(uuid, 0);
        int deaths = ClientTeamData.deaths.getOrDefault(uuid, 0);
        int ping = (info != null) ? info.getLatency() : 0;

        String kd = "0.0";
        if (deaths == 0) {
            kd = kills + ".0";
        } else {
            float ratio = (float) kills / deaths;
            kd = String.format("%.1f", ratio);
        }

        if (info != null) {
            RenderSystem.setShaderTexture(0, info.getSkinLocation());
            gui.blit(info.getSkinLocation(), x + 4, y, 8, 8, 8.0F, 8.0F, 8, 8, 64, 64);
        }

        gui.drawString(mc.font, name, x + 16, y, 0xFFFFFF);
        gui.drawString(mc.font, String.valueOf(kills), x + width - 90, y, 0xFFFFFF);
        gui.drawString(mc.font, String.valueOf(deaths), x + width - 70, y, 0xFFFFFF);
        gui.drawString(mc.font, kd, x + width - 45, y, 0xFFD700);

        int pingColor = (ping < 50) ? 0x00FF00 : (ping < 150 ? 0xFFFF00 : 0xFF0000);
        gui.drawString(mc.font, String.valueOf(ping), x + width - 20, y, pingColor);
    }
}