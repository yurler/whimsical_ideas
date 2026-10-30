package com.whimsical.ideas.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class MusicDataPacket {
    public enum Type { REQUEST, RESPONSE }
    public Type type;
    public String fileName;
    public byte[] data;

    public MusicDataPacket(Type type, String fileName, byte[] data) {
        this.type = type;
        this.fileName = fileName;
        this.data = data;
    }

    public static void encode(MusicDataPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);
        buf.writeUtf(msg.fileName, 256);
        if (msg.type == Type.RESPONSE) {
            buf.writeByteArray(msg.data);
        }
    }

    public static MusicDataPacket decode(FriendlyByteBuf buf) {
        Type type = buf.readEnum(Type.class);
        String fileName = buf.readUtf(256);
        byte[] data = type == Type.RESPONSE ? buf.readByteArray() : null;
        return new MusicDataPacket(type, fileName, data);
    }

    public static void handle(MusicDataPacket msg, Supplier<NetworkEvent.Context> ctx) {}
}