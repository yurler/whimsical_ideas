package com.whimsical.ideas.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class MusicControlPacket {
    public enum Action { PAUSE, RESUME, STOP }
    private final Action action;
    private final String hostName;

    public MusicControlPacket(Action action, String hostName) {
        this.action = action;
        this.hostName = hostName;
    }

    public Action getAction() { return action; }
    public String getHostName() { return hostName; }

    public static void encode(MusicControlPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeUtf(msg.hostName);
    }

    public static MusicControlPacket decode(FriendlyByteBuf buf) {
        return new MusicControlPacket(buf.readEnum(Action.class), buf.readUtf());
    }

    public static void handle(MusicControlPacket msg, Supplier<NetworkEvent.Context> ctx) {}
}