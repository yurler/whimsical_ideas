package com.whimsical.ideas.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.whimsical.ideas.network.ModNetwork;
import com.whimsical.ideas.network.MusicForceJoinPacket;
import com.whimsical.ideas.network.MusicRoom;
import com.whimsical.ideas.network.MusicRoomPacket;
import com.whimsical.ideas.server.RoomManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;

@Mod.EventBusSubscriber
public class MusicAdminCommand {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("musicadmin")
                .requires(source -> source.hasPermission(2))

                // 1. 强制解散
                .then(Commands.literal("dissolve")
                        .then(Commands.literal("all")
                                .executes(context -> {
                                    RoomManager.ROOMS.clear();
                                    RoomManager.SERVER_ROOM = null;
                                    ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                                            new MusicRoomPacket(MusicRoomPacket.Action.LEAVE, "", "", "", "", 0, "DISSOLVED"));
                                    context.getSource().sendSuccess(() -> Component.translatable("command.whimsical_ideas.dissolve_all"), true);
                                    return 1;
                                })
                        )
                        .then(Commands.argument("roomName", StringArgumentType.string())
                                .executes(context -> {
                                    String roomName = StringArgumentType.getString(context, "roomName");
                                    if (RoomManager.ROOMS.containsKey(roomName)) {
                                        if (roomName.equals(RoomManager.SERVER_ROOM_NAME)) {
                                            RoomManager.SERVER_ROOM = null;
                                        }
                                        RoomManager.ROOMS.remove(roomName);
                                        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                                                new MusicRoomPacket(MusicRoomPacket.Action.LEAVE, "", "", "", "", 0, "DISSOLVED"));
                                        context.getSource().sendSuccess(() -> Component.translatable("command.whimsical_ideas.dissolve_one", roomName), true);
                                    } else {
                                        context.getSource().sendFailure(Component.translatable("command.whimsical_ideas.room_not_found"));
                                    }
                                    return 1;
                                })
                        )
                )

                // 2. 强制加入
                .then(Commands.literal("join")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("roomName", StringArgumentType.string())
                                        .executes(context -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                            String roomName = StringArgumentType.getString(context, "roomName");
                                            MusicRoom room = RoomManager.ROOMS.get(roomName);

                                            if (room != null) {
                                                for (ServerPlayer player : targets) {
                                                    room.listeners.add(player.getUUID());
                                                    ModNetwork.CHANNEL.sendTo(new MusicForceJoinPacket(room.roomName, room.hostName),
                                                            player.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                                                }
                                                context.getSource().sendSuccess(() -> Component.translatable("command.whimsical_ideas.force_join", roomName), true);
                                            } else {
                                                context.getSource().sendFailure(Component.translatable("command.whimsical_ideas.room_not_found"));
                                            }
                                            return 1;
                                        })
                                )
                        )
                )

                // 3. 强制邀请
                .then(Commands.literal("invite")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                    ServerPlayer admin = context.getSource().getPlayerOrException();

                                    MusicRoom adminRoom = null;
                                    for (MusicRoom r : RoomManager.ROOMS.values()) {
                                        if (r.hostUUID.equals(admin.getUUID())) {
                                            adminRoom = r;
                                            break;
                                        }
                                    }

                                    if (adminRoom != null) {
                                        for (ServerPlayer target : targets) {
                                            adminRoom.invitedPlayers.add(target.getUUID());
                                            target.sendSystemMessage(Component.translatable("command.whimsical_ideas.force_invite", adminRoom.roomName));
                                        }
                                        context.getSource().sendSuccess(() -> Component.translatable("command.whimsical_ideas.invite_success"), true);
                                    } else {
                                        context.getSource().sendFailure(Component.translatable("command.whimsical_ideas.must_be_in_room"));
                                    }
                                    return 1;
                                })
                        )
                )

                // 4. 服务器专属电台管理
                .then(Commands.literal("serverroom")
                        .then(Commands.literal("enable")
                                .executes(context -> {
                                    if (RoomManager.SERVER_ROOM == null) {
                                        RoomManager.createServerRoom();
                                        context.getSource().sendSuccess(() -> Component.translatable("command.whimsical_ideas.server_room_enabled"), true);
                                    } else {
                                        context.getSource().sendFailure(Component.translatable("command.whimsical_ideas.server_room_already_enabled"));
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("disable")
                                .executes(context -> {
                                    if (RoomManager.SERVER_ROOM != null) {
                                        RoomManager.removeServerRoom();
                                        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                                                new MusicRoomPacket(MusicRoomPacket.Action.LEAVE, "", "", "", "", 0, "DISSOLVED"));
                                        context.getSource().sendSuccess(() -> Component.translatable("command.whimsical_ideas.server_room_disabled"), true);
                                    } else {
                                        context.getSource().sendFailure(Component.translatable("command.whimsical_ideas.server_room_already_disabled"));
                                    }
                                    return 1;
                                })
                        )
                        // ★ 新增：配置服务器房间（名称 类型 密码）
                        .then(Commands.literal("config")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .then(Commands.argument("type", StringArgumentType.string())
                                                .then(Commands.argument("password", StringArgumentType.string())
                                                        .executes(context -> {
                                                            if (RoomManager.SERVER_ROOM == null) {
                                                                context.getSource().sendFailure(Component.literal("服务器电台尚未开启。"));
                                                                return 0;
                                                            }

                                                            String name = StringArgumentType.getString(context, "name");
                                                            String type = StringArgumentType.getString(context, "type").toUpperCase();
                                                            String pwd = StringArgumentType.getString(context, "password");

                                                            // 移除旧的记录
                                                            RoomManager.ROOMS.remove(RoomManager.SERVER_ROOM_NAME);

                                                            // 更新属性
                                                            RoomManager.SERVER_ROOM_NAME = name;
                                                            RoomManager.SERVER_ROOM.roomName = name;
                                                            RoomManager.SERVER_ROOM.roomType = type;
                                                            RoomManager.SERVER_ROOM.password = pwd;

                                                            // 重新放入
                                                            RoomManager.ROOMS.put(name, RoomManager.SERVER_ROOM);

                                                            context.getSource().sendSuccess(() -> Component.literal("§a服务器电台配置已更新！名称: " + name + ", 类型: " + type), true);

                                                            // 强制刷新客户端列表
                                                            ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                                                                    new MusicRoomPacket(MusicRoomPacket.Action.REQUEST_LIST, "", "", "", "", 0, ""));
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )
                )
        );
    }
}