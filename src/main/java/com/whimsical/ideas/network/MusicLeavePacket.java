package com.whimsical.ideas.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class MusicLeavePacket {
    public static void encode(MusicLeavePacket msg, FriendlyByteBuf buf) {}
    public static MusicLeavePacket decode(FriendlyByteBuf buf) { return new MusicLeavePacket(); }
    public static void handle(MusicLeavePacket msg, Supplier<NetworkEvent.Context> ctx) {}
}