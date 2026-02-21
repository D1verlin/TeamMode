package com.example.examplemod.network;

import com.example.examplemod.world.KitData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import java.util.List;
import java.util.function.Supplier;

public class PacketSelectKit {
    private final int kitId; // Теперь число

    public PacketSelectKit(int id) {
        this.kitId = id;
    }

    public PacketSelectKit(FriendlyByteBuf buf) {
        this.kitId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(kitId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // Берем предметы напрямую из конфига
                List<ItemStack> items = com.example.examplemod.config.KitConfig.KITS.get("kit" + kitId);

                if (items != null && !items.isEmpty()) {
                    player.getInventory().clearContent();
                    for (ItemStack stack : items) {
                        ItemStack copy = stack.copy();
                        EquipmentSlot slotType = Mob.getEquipmentSlotForItem(copy);
                        if (slotType.getType() == EquipmentSlot.Type.ARMOR && player.getItemBySlot(slotType).isEmpty()) {
                            player.setItemSlot(slotType, copy);
                        } else if (slotType == EquipmentSlot.OFFHAND && player.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) {
                            player.setItemSlot(EquipmentSlot.OFFHAND, copy);
                        } else {
                            player.getInventory().add(copy);
                        }
                    }
                    player.containerMenu.broadcastChanges();
                    player.inventoryMenu.broadcastChanges();
                }
            }
        });
        return true;
    }
}