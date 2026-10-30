package com.whimsical.ideas.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

@Mod.EventBusSubscriber
public class MusicInviteCommand {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("music")
                .then(Commands.literal("invite")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                    ServerPlayer sender = context.getSource().getPlayerOrException();

                                    Component inviteMsg = Component.literal("§a[系统] §e" + sender.getName().getString() + " §a邀请你一起听歌！ ")
                                            .append(Component.literal("§b[✔ 接受]")
                                                    .withStyle(style -> style
                                                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/music accept " + sender.getName().getString()))
                                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击接受邀请")))
                                                    )
                                            )
                                            .append(Component.literal("  "))
                                            .append(Component.literal("§c[✖ 拒绝]")
                                                    .withStyle(style -> style
                                                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/music deny " + sender.getName().getString()))
                                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击拒绝邀请")))
                                                    )
                                            );

                                    for (ServerPlayer target : targets) target.sendSystemMessage(inviteMsg);
                                    context.getSource().sendSuccess(() -> Component.literal("§a邀请已发送！"), false);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("accept")
                        .then(Commands.argument("host", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer host = EntityArgument.getPlayer(context, "host");
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    player.sendSystemMessage(Component.literal("§a已接受 " + host.getName().getString() + " 的邀请。正在尝试同步..."));
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("deny")
                        .then(Commands.argument("host", EntityArgument.player())
                                .executes(context -> {
                                    context.getSource().sendSystemMessage(Component.literal("§c你拒绝了邀请。"));
                                    return 1;
                                })
                        )
                )
        );
    }
}