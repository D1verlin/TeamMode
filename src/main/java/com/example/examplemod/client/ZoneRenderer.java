package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.ClientTeamData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ZoneRenderer {

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest(); // Видно сквозь стены
        RenderSystem.disableCull();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        // 1. РЕНДЕР РЕЖИМА "УДЕРЖАНИЕ ЗОНЫ"
        if (ClientTeamData.currentGameMode.equals("domination") && ClientTeamData.clientZonePos1 != null && ClientTeamData.clientZonePos2 != null) {
            AABB box = new AABB(ClientTeamData.clientZonePos1, ClientTeamData.clientZonePos2).inflate(0.01);
            float r, g, b;
            int owner = ClientTeamData.clientZoneOwner;
            if (owner == 1) { r = 1.0f; g = 0.6f; b = 0.0f; }
            else if (owner == 2) { r = 0.2f; g = 0.4f; b = 1.0f; }
            else { r = 0.9f; g = 0.9f; b = 0.9f; }

            renderBox(buffer, tesselator, matrix, box, r, g, b);
        }

        // 2. РЕНДЕР РЕЖИМА "ЗАКЛАДКА БОМБЫ"
        if (ClientTeamData.currentGameMode.equals("defusal")) {
            // Плэнт А (Красная зона)
            if (ClientTeamData.clientSiteAPos1 != null && ClientTeamData.clientSiteAPos2 != null) {
                AABB boxA = new AABB(ClientTeamData.clientSiteAPos1, ClientTeamData.clientSiteAPos2).inflate(0.01);
                renderBox(buffer, tesselator, matrix, boxA, 1.0f, 0.2f, 0.2f); // Красный
            }
            // Плэнт Б (Синяя/Голубая зона)
            if (ClientTeamData.clientSiteBPos1 != null && ClientTeamData.clientSiteBPos2 != null) {
                AABB boxB = new AABB(ClientTeamData.clientSiteBPos1, ClientTeamData.clientSiteBPos2).inflate(0.01);
                renderBox(buffer, tesselator, matrix, boxB, 0.2f, 0.6f, 1.0f); // Голубой
            }
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    // Вспомогательный метод, чтобы не дублировать код отрисовки граней и линий
    private static void renderBox(BufferBuilder buffer, Tesselator tesselator, Matrix4f matrix, AABB box, float r, float g, float b) {
        // Полупрозрачные стенки
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        renderSolidBox(buffer, matrix, box, r, g, b, 0.25f);
        tesselator.end();

        // Яркий каркас (3 слоя для толщины)
        for (float off = -0.01f; off <= 0.01f; off += 0.01f) {
            buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            drawBoxLines(buffer, matrix, box.inflate(off), r, g, b, 0.8f);
            tesselator.end();
        }
    }


    // Метод для отрисовки 6 сторон куба
    private static void renderSolidBox(BufferBuilder buffer, Matrix4f mat, AABB b, float r, float g, float bl, float a) {
        float x1 = (float)b.minX; float y1 = (float)b.minY; float z1 = (float)b.minZ;
        float x2 = (float)b.maxX; float y2 = (float)b.maxY; float z2 = (float)b.maxZ;

        // Низ
        buffer.vertex(mat, x1, y1, z1).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x2, y1, z1).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x2, y1, z2).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x1, y1, z2).color(r, g, bl, a).endVertex();
        // Верх
        buffer.vertex(mat, x1, y2, z1).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x1, y2, z2).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x2, y2, z2).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x2, y2, z1).color(r, g, bl, a).endVertex();
        // Стенки
        buffer.vertex(mat, x1, y1, z1).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x1, y2, z1).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x2, y2, z1).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x2, y1, z1).color(r, g, bl, a).endVertex();

        buffer.vertex(mat, x1, y1, z2).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x2, y1, z2).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x2, y2, z2).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x1, y2, z2).color(r, g, bl, a).endVertex();

        buffer.vertex(mat, x1, y1, z1).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x1, y1, z2).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x1, y2, z2).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x1, y2, z1).color(r, g, bl, a).endVertex();

        buffer.vertex(mat, x2, y1, z1).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x2, y2, z1).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x2, y2, z2).color(r, g, bl, a).endVertex();
        buffer.vertex(mat, x2, y1, z2).color(r, g, bl, a).endVertex();
    }

    // Метод для отрисовки линий ребер
    private static void drawBoxLines(BufferBuilder buffer, Matrix4f mat, AABB b, float r, float g, float bl, float a) {
        float x1 = (float)b.minX; float y1 = (float)b.minY; float z1 = (float)b.minZ;
        float x2 = (float)b.maxX; float y2 = (float)b.maxY; float z2 = (float)b.maxZ;

        // Линии (12 штук)
        line(buffer, mat, x1, y1, z1, x2, y1, z1, r, g, bl, a);
        line(buffer, mat, x2, y1, z1, x2, y1, z2, r, g, bl, a);
        line(buffer, mat, x2, y1, z2, x1, y1, z2, r, g, bl, a);
        line(buffer, mat, x1, y1, z2, x1, y1, z1, r, g, bl, a);
        line(buffer, mat, x1, y2, z1, x2, y2, z1, r, g, bl, a);
        line(buffer, mat, x2, y2, z1, x2, y2, z2, r, g, bl, a);
        line(buffer, mat, x2, y2, z2, x1, y2, z2, r, g, bl, a);
        line(buffer, mat, x1, y2, z2, x1, y2, z1, r, g, bl, a);
        line(buffer, mat, x1, y1, z1, x1, y2, z1, r, g, bl, a);
        line(buffer, mat, x2, y1, z1, x2, y2, z1, r, g, bl, a);
        line(buffer, mat, x2, y1, z2, x2, y2, z2, r, g, bl, a);
        line(buffer, mat, x1, y1, z2, x1, y2, z2, r, g, bl, a);
    }

    private static void line(BufferBuilder b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float bl, float a) {
        b.vertex(m, x1, y1, z1).color(r, g, bl, a).endVertex();
        b.vertex(m, x2, y2, z2).color(r, g, bl, a).endVertex();
    }
}