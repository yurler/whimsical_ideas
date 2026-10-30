package com.whimsical.ideas.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MusicStopBroadcastPacket {
    // 这个包不需要携带任何数据，空实现即可
    public static void encode(MusicStopBroadcastPacket msg, FriendlyByteBuf buf) {}
    public static MusicStopBroadcastPacket decode(FriendlyByteBuf buf) { return new MusicStopBroadcastPacket(); }
    public static void handle(MusicStopBroadcastPacket msg, Supplier<NetworkEvent.Context> ctx) {}
}