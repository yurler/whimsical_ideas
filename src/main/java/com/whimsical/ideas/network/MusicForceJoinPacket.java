package com.whimsical.ideas.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class MusicForceJoinPacket {
    public String roomName;
    public String hostName;

    public MusicForceJoinPacket(String roomName, String hostName) {
        this.roomName = roomName;
        this.hostName = hostName;
    }

    public static void encode(MusicForceJoinPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.roomName);
        buf.writeUtf(msg.hostName);
    }

    public static MusicForceJoinPacket decode(FriendlyByteBuf buf) {
        return new MusicForceJoinPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(MusicForceJoinPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                com.whimsical.ideas.client.MusicManager.joinRoom(msg.roomName, msg.hostName);
                net.minecraft.client.Minecraft.getInstance().player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§a[系统] 你已被强制加入房间: " + msg.roomName), false);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}