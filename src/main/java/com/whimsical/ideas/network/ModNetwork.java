package com.whimsical.ideas.network;

import com.whimsical.ideas.WhimsicalIdeas;
import com.whimsical.ideas.client.JoinRoomScreen;
import com.whimsical.ideas.client.MusicManager;
import com.whimsical.ideas.client.MusicScreen;
import com.whimsical.ideas.server.RoomManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

public class ModNetwork {
    public static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(WhimsicalIdeas.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, CraftPlushiePacket.class, CraftPlushiePacket::encode, CraftPlushiePacket::decode, CraftPlushiePacket::handle);
        CHANNEL.registerMessage(id++, SkinUploadPacket.class, SkinUploadPacket::encode, SkinUploadPacket::decode, SkinUploadPacket::handle);
        CHANNEL.registerMessage(id++, MusicRoomPacket.class, MusicRoomPacket::encode, MusicRoomPacket::decode, ModNetwork::handleMusicRoom);
        CHANNEL.registerMessage(id++, MusicControlPacket.class, MusicControlPacket::encode, MusicControlPacket::decode, ModNetwork::handleMusicControl);
        CHANNEL.registerMessage(id++, MusicStopBroadcastPacket.class, MusicStopBroadcastPacket::encode, MusicStopBroadcastPacket::decode, ModNetwork::handleMusicStop);
        CHANNEL.registerMessage(id++, MusicForceJoinPacket.class, MusicForceJoinPacket::encode, MusicForceJoinPacket::decode, MusicForceJoinPacket::handle);
        // 新增注册
        CHANNEL.registerMessage(id++, MusicDataPacket.class, MusicDataPacket::encode, MusicDataPacket::decode, ModNetwork::handleMusicData);
        CHANNEL.registerMessage(id++, MusicQueuePacket.class, MusicQueuePacket::encode, MusicQueuePacket::decode, ModNetwork::handleMusicQueue);
    }

    public static void handleMusicData(MusicDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;
                if (msg.type == MusicDataPacket.Type.REQUEST) {
                    // 客户端请求文件，服务端或房主读取文件发回
                    // 这里假设文件在房主本地，但服务端无法直接读房主的文件。
                    // 所以此逻辑应转发给房主，由房主发回给该客户端。
                    for (MusicRoom room : RoomManager.ROOMS.values()) {
                        if (room.listeners.contains(sender.getUUID())) {
                            ServerPlayer host = sender.server.getPlayerList().getPlayer(room.hostUUID);
                            if (host != null) {
                                CHANNEL.sendTo(new MusicDataPacket(MusicDataPacket.Type.REQUEST, msg.fileName, null), host.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                            }
                            break;
                        }
                    }
                } else if (msg.type == MusicDataPacket.Type.RESPONSE) {
                    // 房主发回了文件，转发给请求者
                    ServerPlayer target = sender.server.getPlayerList().getPlayerByName(msg.fileName.split(":")[0]); // 借用fileName传目标名
                    if (target != null) {
                        CHANNEL.sendTo(new MusicDataPacket(MusicDataPacket.Type.RESPONSE, msg.fileName.split(":")[1], msg.data), target.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                    }
                }
            } else {
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    MusicManager.receiveMusicData(msg);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void handleMusicQueue(MusicQueuePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                ServerPlayer sender = ctx.get().getSender();
                if (sender != null) {
                    for (MusicRoom r : RoomManager.ROOMS.values()) {
                        if (r.hostUUID.equals(sender.getUUID())) {
                            // 同步给房间内所有人
                            for (UUID listenerUUID : r.listeners) {
                                ServerPlayer listener = sender.server.getPlayerList().getPlayer(listenerUUID);
                                if (listener != null) {
                                    CHANNEL.sendTo(new MusicQueuePacket(msg.action, msg.fileName), listener.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                                }
                            }
                        }
                    }
                }
            } else {
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    MusicManager.handleQueuePacket(msg);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // 保持之前的 handleMusicRoom, handleMusicControl, handleMusicStop 不变...
    public static void handleMusicRoom(MusicRoomPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                if (sender == null) return;
                switch (msg.action) {
                    case CREATE:
                        if (!RoomManager.ROOMS.containsKey(msg.roomName)) {
                            MusicRoom room = new MusicRoom(msg.roomName, sender.getUUID(), sender.getName().getString(), msg.roomType, msg.password);
                            room.listeners.add(sender.getUUID());
                            RoomManager.ROOMS.put(msg.roomName, room);
                            sender.sendSystemMessage(Component.translatable("gui.whimsical_ideas.room.create_success", msg.roomName));
                            CHANNEL.sendTo(new MusicRoomPacket(MusicRoomPacket.Action.SYNC_MUSIC, room.roomName, "", room.hostName, "", 0, "", "JOIN_ACK"), sender.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                            syncRoomList();
                        } else {
                            sender.sendSystemMessage(Component.translatable("gui.whimsical_ideas.room.name_exists"));
                        }
                        break;
                    case JOIN:
                        MusicRoom roomToJoin = RoomManager.ROOMS.get(msg.roomName);
                        if (roomToJoin != null) {
                            boolean canJoin = roomToJoin.roomType.equals("PUBLIC") || roomToJoin == RoomManager.SERVER_ROOM ||
                                    (roomToJoin.roomType.equals("PRIVATE") && (roomToJoin.password.equals(msg.password) || roomToJoin.invitedPlayers.contains(sender.getUUID()))) ||
                                    (roomToJoin.roomType.equals("HIDDEN") && roomToJoin.invitedPlayers.contains(sender.getUUID()));
                            if (canJoin) {
                                roomToJoin.listeners.add(sender.getUUID());
                                sender.sendSystemMessage(Component.translatable("gui.whimsical_ideas.room.joined", roomToJoin.roomName));
                                CHANNEL.sendTo(new MusicRoomPacket(MusicRoomPacket.Action.SYNC_MUSIC, roomToJoin.roomName, "", roomToJoin.hostName, "", 0, "", "JOIN_ACK"), sender.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                                if (roomToJoin == RoomManager.SERVER_ROOM) playServerMusic(sender);
                            } else {
                                sender.sendSystemMessage(Component.translatable("gui.whimsical_ideas.room.wrong_password"));
                            }
                        }
                        break;
                    case LEAVE:
                        // ... (与之前代码一致)
                        for (MusicRoom r : RoomManager.ROOMS.values()) {
                            if (r.hostUUID.equals(sender.getUUID())) {
                                RoomManager.ROOMS.remove(r.roomName);
                                CHANNEL.send(PacketDistributor.ALL.noArg(), new MusicRoomPacket(MusicRoomPacket.Action.LEAVE, "", "", "", "", 0, "DISSOLVED"));
                                break;
                            } else if (r.listeners.contains(sender.getUUID())) {
                                r.listeners.remove(sender.getUUID());
                                CHANNEL.sendTo(new MusicRoomPacket(MusicRoomPacket.Action.LEAVE, "", "", "", "", 0, ""), sender.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                                sender.sendSystemMessage(Component.translatable("gui.whimsical_ideas.room.left"));
                                break;
                            }
                        }
                        break;
                    case UPDATE_ROOM:
                        // ... (与之前代码一致)
                        break;
                    case REQUEST_LIST:
                        syncRoomList();
                        break;
                    case SYNC_MUSIC:
                        for (MusicRoom r : RoomManager.ROOMS.values()) {
                            if (r.hostUUID.equals(sender.getUUID())) {
                                for (UUID listenerUUID : r.listeners) {
                                    ServerPlayer listener = sender.server.getPlayerList().getPlayer(listenerUUID);
                                    if (listener != null) {
                                        CHANNEL.sendTo(new MusicRoomPacket(MusicRoomPacket.Action.SYNC_MUSIC, r.roomName, "", r.hostName, msg.songName, msg.playTime, "", msg.syncMode), listener.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                                    }
                                }
                            }
                        }
                        break;
                }
            } else {
                // ... (客户端处理逻辑保持之前)
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    if (msg.action == MusicRoomPacket.Action.SYNC_MUSIC) {
                        if (msg.syncMode.equals("JOIN_ACK")) {
                            MusicManager.joinRoom(msg.roomName, msg.hostName);
                        } else if (!msg.songName.isEmpty()) {
                            MusicManager.playRemoteMusic(msg.hostName, msg.songName, 1.0f, msg.playTime);
                        }
                    } else if (msg.action == MusicRoomPacket.Action.LEAVE) {
                        if (msg.roomType.equals("DISSOLVED")) Minecraft.getInstance().player.displayClientMessage(Component.translatable("gui.whimsical_ideas.room.dissolved"), false);
                        MusicManager.leaveRoom();
                    } else if (msg.action == MusicRoomPacket.Action.SYNC_LIST) {
                        if (Minecraft.getInstance().screen instanceof JoinRoomScreen screen) screen.setRoomList(msg.roomName);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void handleMusicControl(MusicControlPacket msg, Supplier<NetworkEvent.Context> ctx) {
        // ... (保持不变)
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                ServerPlayer sender = ctx.get().getSender();
                if (sender != null) {
                    for (MusicRoom r : RoomManager.ROOMS.values()) {
                        if (r.hostUUID.equals(sender.getUUID())) {
                            for (UUID listenerUUID : r.listeners) {
                                ServerPlayer listener = sender.server.getPlayerList().getPlayer(listenerUUID);
                                if (listener != null) {
                                    CHANNEL.sendTo(new MusicControlPacket(msg.getAction(), r.hostName), listener.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                                }
                            }
                        }
                    }
                }
            } else {
                if (FMLEnvironment.dist == Dist.CLIENT && MusicScreen.clientSyncMode.equals("FOLLOW")) {
                    if (msg.getAction() == MusicControlPacket.Action.PAUSE) MusicManager.pauseRemoteMusic();
                    else if (msg.getAction() == MusicControlPacket.Action.RESUME) MusicManager.resumeRemoteMusic();
                    else if (msg.getAction() == MusicControlPacket.Action.STOP) MusicManager.stopAllRemoteMusic();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void handleMusicStop(MusicStopBroadcastPacket msg, Supplier<NetworkEvent.Context> ctx) {
        // ... (保持不变)
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient() && FMLEnvironment.dist == Dist.CLIENT) {
                MusicManager.stopAllRemoteMusic();
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void syncRoomList() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, MusicRoom> entry : RoomManager.ROOMS.entrySet()) {
            MusicRoom room = entry.getValue();
            if (!room.roomType.equals("HIDDEN")) {
                sb.append(room.roomName).append(":").append(room.roomType).append(";");
            }
        }
        CHANNEL.send(PacketDistributor.ALL.noArg(), new MusicRoomPacket(MusicRoomPacket.Action.SYNC_LIST, sb.toString(), "", "", "", 0, ""));
    }

    private static void playServerMusic(ServerPlayer player) {
        File musicDir = RoomManager.getServerMusicDir(player.server);
        if (!musicDir.exists()) {
            player.sendSystemMessage(Component.translatable("gui.whimsical_ideas.server_room.no_folder"));
            return;
        }
        File[] files = musicDir.listFiles((d, name) -> name.toLowerCase().endsWith(".ogg"));
        if (files == null || files.length == 0) {
            player.sendSystemMessage(Component.translatable("gui.whimsical_ideas.server_room.empty"));
            return;
        }
        File song = files[new Random().nextInt(files.length)];
        CHANNEL.sendTo(new MusicRoomPacket(MusicRoomPacket.Action.SYNC_MUSIC, RoomManager.SERVER_ROOM_NAME, "", "Server", song.getName(), 0, "", "FOLLOW"), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        player.sendSystemMessage(Component.translatable("gui.whimsical_ideas.server_room.playing", song.getName()));
    }
}