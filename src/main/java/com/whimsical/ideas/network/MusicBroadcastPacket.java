package com.whimsical.ideas.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class MusicBroadcastPacket {
    private final double x, y, z;
    private final String fileName;
    private final float volume;
    private final float playTime;
    private final String hostName;

    public MusicBroadcastPacket(double x, double y, double z, String fileName, float volume, float playTime, String hostName) {
        this.x = x; this.y = y; this.z = z; this.fileName = fileName; this.volume = volume; this.playTime = playTime; this.hostName = hostName;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public String getFileName() { return fileName; }
    public float getVolume() { return volume; }
    public float getPlayTime() { return playTime; }
    public String getHostName() { return hostName; }

    public static void encode(MusicBroadcastPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x); buf.writeDouble(msg.y); buf.writeDouble(msg.z);
        buf.writeUtf(msg.fileName, 256); buf.writeFloat(msg.volume); buf.writeFloat(msg.playTime); buf.writeUtf(msg.hostName, 32);
    }

    public static MusicBroadcastPacket decode(FriendlyByteBuf buf) {
        return new MusicBroadcastPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readUtf(256), buf.readFloat(), buf.readFloat(), buf.readUtf(32));
    }
    public static void handle(MusicBroadcastPacket msg, Supplier<NetworkEvent.Context> ctx) {}
}