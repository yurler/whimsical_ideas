package com.whimsical.ideas.server;

import com.whimsical.ideas.Config;
import com.whimsical.ideas.network.MusicRoom;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoomManager {
    public static final Map<String, MusicRoom> ROOMS = new HashMap<>();

    // 服务器专属房间
    public static MusicRoom SERVER_ROOM = null;
    // 使用翻译键，客户端会自动翻译
    public static String SERVER_ROOM_NAME = "gui.whimsical_ideas.room.server";

    public static void initServerRoom(MinecraftServer server) {
        if (Config.ENABLE_SERVER_ROOM.get()) {
            createServerRoom();
        }
    }

    public static void createServerRoom() {
        // 使用一个固定的 UUID 代表服务器
        UUID serverUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        SERVER_ROOM = new MusicRoom(SERVER_ROOM_NAME, serverUUID, "Server", "PUBLIC", "");
        ROOMS.put(SERVER_ROOM_NAME, SERVER_ROOM);
    }

    public static void removeServerRoom() {
        if (SERVER_ROOM != null) {
            ROOMS.remove(SERVER_ROOM_NAME);
            SERVER_ROOM = null;
        }
    }

    // 获取服务器音乐目录
    public static File getServerMusicDir(MinecraftServer server) {
        return new File(server.getServerDirectory(), "server_music");
    }

    public static void onPlayerLogout(ServerPlayer player) {
        ROOMS.entrySet().removeIf(entry -> {
            MusicRoom room = entry.getValue();
            if (room.hostUUID.equals(player.getUUID())) {
                com.whimsical.ideas.network.ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                        new com.whimsical.ideas.network.MusicRoomPacket(com.whimsical.ideas.network.MusicRoomPacket.Action.LEAVE, "", "", "", "", 0, "DISSOLVED"));
                return true;
            }
            return false;
        });

        for (MusicRoom room : ROOMS.values()) {
            room.listeners.remove(player.getUUID());
            room.invitedPlayers.remove(player.getUUID());
        }
    }
}