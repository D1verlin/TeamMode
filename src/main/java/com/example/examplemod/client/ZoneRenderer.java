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
        if (!ClientTeamData.currentGameMode.equals("domination")
                || ClientTeamData.clientZonePos1 == null
                || ClientTeamData.clientZonePos2 == null) {
            return;
        }

        // Рендерим на этапе после частиц, чтобы зона была видна поверх всего
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {

            Minecraft mc = Minecraft.getInstance();
            PoseStack poseStack = event.getPoseStack();
            Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

            // Создаем область захвата
            AABB box = new AABB(ClientTeamData.clientZonePos1, ClientTeamData.clientZonePos2).inflate(0.01);

            // Цвета фракций
            float r, g, b;
            int owner = ClientTeamData.clientZoneOwner;
            if (owner == 1) { r = 1.0f; g = 0.6f; b = 0.0f; } // Оранжевый (Одиночки)
            else if (owner == 2) { r = 0.2f; g = 0.4f; b = 1.0f; } // Синий (Бандосы)
            else { r = 0.9f; g = 0.9f; b = 0.9f; } // Белый (Нейтрал)

            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            Matrix4f matrix = poseStack.last().pose();

            // --- НАСТРОЙКИ КРАСИВОГО РЕНДЕРА ---
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            // ВАЖНО: Если хочешь, чтобы зону НЕ было видно сквозь стены,
            // замени disableDepthTest() на enableDepthTest()
            RenderSystem.disableDepthTest();

            RenderSystem.disableCull(); // Чтобы видеть стенки изнутри и снаружи

            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.getBuilder();

            // 1. РИСУЕМ ПЛОТНЫЕ ГРАНИ (ПОЛУПРОЗРАЧНЫЙ КУБ)
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            renderSolidBox(buffer, matrix, box, r, g, b, 0.25f); // Прозрачность стенок 25%
            tesselator.end();

            // 2. РИСУЕМ ЯРКИЙ КАРКАС (ЛИНИИ) ДЛЯ ЧЕТКОСТИ
            // Рисуем 3 слоя со смещением для имитации толщины
            for (float off = -0.01f; off <= 0.01f; off += 0.01f) {
                buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                drawBoxLines(buffer, matrix, box.inflate(off), r, g, b, 0.8f);
                tesselator.end();
            }

            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            poseStack.popPose();
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