package com.example.examplemod.network;

import com.example.examplemod.client.KillstreakHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketKillstreak {
    private final String title;
    private final String subtitle;
    private final int color;

    public PacketKillstreak(String title, String subtitle, int color) {
        this.title = title;
        this.subtitle = subtitle;
        this.color = color;
    }

    public PacketKillstreak(FriendlyByteBuf buf) {
        this.title = buf.readUtf();
        this.subtitle = buf.readUtf();
        this.color = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(title);
        buf.writeUtf(subtitle);
        buf.writeInt(color);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Запускаем HUD на стороне клиента
            KillstreakHud.trigger(title, subtitle, color);
        });
        context.setPacketHandled(true);
        return true;
    }
}