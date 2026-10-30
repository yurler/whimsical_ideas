package com.whimsical.ideas.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class MusicQueuePacket {
    public enum Action { ADD, CLEAR, SYNC, NEXT }
    public Action action;
    public String fileName;

    public MusicQueuePacket(Action action, String fileName) {
        this.action = action;
        this.fileName = fileName;
    }

    public static void encode(MusicQueuePacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeUtf(msg.fileName == null ? "" : msg.fileName);
    }

    public static MusicQueuePacket decode(FriendlyByteBuf buf) {
        return new MusicQueuePacket(buf.readEnum(Action.class), buf.readUtf());
    }

    public static void handle(MusicQueuePacket msg, Supplier<NetworkEvent.Context> ctx) {}
}