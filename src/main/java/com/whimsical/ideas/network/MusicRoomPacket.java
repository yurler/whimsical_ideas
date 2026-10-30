package com.whimsical.ideas.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class MusicRoomPacket {
    public enum Action {
        CREATE, JOIN, LEAVE, REQUEST_LIST, SYNC_LIST, SYNC_MUSIC, UPDATE_ROOM
    }

    public Action action;
    public String roomName;
    public String password;
    public String hostName;
    public String songName;
    public float playTime;
    public String roomType;
    public String syncMode;

    public MusicRoomPacket(Action action, String roomName, String password, String hostName, String songName, float playTime, String roomType) {
        this(action, roomName, password, hostName, songName, playTime, roomType, "FOLLOW");
    }

    public MusicRoomPacket(Action action, String roomName, String password, String hostName, String songName, float playTime, String roomType, String syncMode) {
        this.action = action; this.roomName = roomName; this.password = password;
        this.hostName = hostName; this.songName = songName; this.playTime = playTime;
        this.roomType = roomType; this.syncMode = syncMode;
    }

    public static void encode(MusicRoomPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeUtf(msg.roomName == null ? "" : msg.roomName);
        buf.writeUtf(msg.password == null ? "" : msg.password);
        buf.writeUtf(msg.hostName == null ? "" : msg.hostName);
        buf.writeUtf(msg.songName == null ? "" : msg.songName);
        buf.writeFloat(msg.playTime);
        buf.writeUtf(msg.roomType == null ? "" : msg.roomType);
        buf.writeUtf(msg.syncMode == null ? "FOLLOW" : msg.syncMode);
    }

    public static MusicRoomPacket decode(FriendlyByteBuf buf) {
        return new MusicRoomPacket(buf.readEnum(Action.class), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readFloat(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(MusicRoomPacket msg, Supplier<NetworkEvent.Context> ctx) {}
}