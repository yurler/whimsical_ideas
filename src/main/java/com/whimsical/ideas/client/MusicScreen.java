package com.whimsical.ideas.client;

import com.whimsical.ideas.network.MusicQueuePacket;
import com.whimsical.ideas.network.MusicRoomPacket;
import com.whimsical.ideas.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MusicScreen extends Screen {
    public static String clientSyncMode = "FOLLOW";

    public MusicScreen() { super(Component.translatable("gui.whimsical_ideas.music")); }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        MusicManager.loadPlaylist();

        // ==========================================
        // 左侧区域：音乐播放控制
        // ==========================================
        int leftX = centerX - 160;
        int y = centerY;

        // 播放控制按钮
        this.addRenderableWidget(Button.builder(Component.translatable("gui.whimsical_ideas.music.prev"), b -> MusicManager.prev())
                .bounds(leftX, y, 35, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable(MusicManager.isPlaying && !MusicManager.isPaused ? "gui.whimsical_ideas.music.pause" : "gui.whimsical_ideas.music.play"),
                b -> {
                    if (!MusicManager.isPlaying) MusicManager.playCurrent();
                    else MusicManager.togglePause();
                    b.setMessage(Component.translatable(MusicManager.isPlaying && !MusicManager.isPaused ? "gui.whimsical_ideas.music.pause" : "gui.whimsical_ideas.music.play"));
                }).bounds(leftX + 40, y, 35, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.whimsical_ideas.music.next"), b -> MusicManager.next())
                .bounds(leftX + 80, y, 35, 20).build());

        // 播放模式
        this.addRenderableWidget(Button.builder(getModeText(), b -> {
            MusicManager.playMode = (MusicManager.playMode + 1) % 3;
            b.setMessage(getModeText());
        }).bounds(leftX + 120, y, 70, 20).build());

        // 音量滑块
        this.addRenderableWidget(new AbstractSliderButton(leftX, y + 30, 155, 20, Component.translatable("gui.whimsical_ideas.music.volume", (int)(MusicManager.personalVolume * 100)), MusicManager.personalVolume) {
            @Override
            protected void updateMessage() { this.setMessage(Component.translatable("gui.whimsical_ideas.music.volume", (int)(this.value * 100))); }
            @Override
            protected void applyValue() { MusicManager.setPersonalVolume((float) this.value); }
        });

        // 音乐路径设置按钮
        this.addRenderableWidget(Button.builder(Component.translatable("gui.whimsical_ideas.music.path_setting"), b -> {
            this.minecraft.setScreen(new MusicPathScreen(this));
        }).bounds(leftX, y + 55, 120, 20).build());


        // ==========================================
        // 右侧区域：房间管理
        // ==========================================
        int rightX = centerX + 110;
        boolean inRoom = MusicManager.listeningToHost != null;
        boolean isHost = inRoom && MusicManager.listeningToHost.equals(Minecraft.getInstance().player.getName().getString());

        if (inRoom) {
            // 同步开关
            this.addRenderableWidget(Button.builder(getSyncModeText(), b -> {
                clientSyncMode = clientSyncMode.equals("FOLLOW") ? "INDEPENDENT" : "FOLLOW";
                b.setMessage(getSyncModeText());
            }).bounds(rightX, y - 30, 100, 20).build());

            if (isHost) {
                // 房主：设置 + 解散
                this.addRenderableWidget(Button.builder(Component.translatable("gui.whimsical_ideas.music.room.settings"), b -> {
                    this.minecraft.setScreen(new RoomSettingsScreen(this));
                }).bounds(rightX, y, 48, 20).build());

                this.addRenderableWidget(Button.builder(Component.translatable("gui.whimsical_ideas.music.room.dissolve"), b -> {
                    ModNetwork.CHANNEL.sendToServer(new MusicRoomPacket(MusicRoomPacket.Action.LEAVE, "", "", "", "", 0, ""));
                    this.minecraft.setScreen(new MusicScreen());
                }).bounds(rightX + 52, y, 48, 20).build());
            } else {
                // 房客：离开
                this.addRenderableWidget(Button.builder(Component.translatable("gui.whimsical_ideas.music.room.leave"), b -> {
                    ModNetwork.CHANNEL.sendToServer(new MusicRoomPacket(MusicRoomPacket.Action.LEAVE, "", "", "", "", 0, ""));
                    this.minecraft.setScreen(new MusicScreen());
                }).bounds(rightX, y, 100, 20).build());
            }
        } else {
            // 不在房间：创建 + 加入
            this.addRenderableWidget(Button.builder(Component.translatable("gui.whimsical_ideas.music.room.create"), b -> {
                this.minecraft.setScreen(new CreateRoomScreen(this));
            }).bounds(rightX, y - 15, 100, 20).build());

            this.addRenderableWidget(Button.builder(Component.translatable("gui.whimsical_ideas.music.room.join"), b -> {
                this.minecraft.setScreen(new JoinRoomScreen(this));
            }).bounds(rightX, y + 15, 100, 20).build());
        }

        // 如果是房主，加一个清空队列的按钮
        if (inRoom && isHost) {
            this.addRenderableWidget(Button.builder(Component.literal("清空队列"), b -> {
                ModNetwork.CHANNEL.sendToServer(new MusicQueuePacket(MusicQueuePacket.Action.CLEAR, ""));
                MusicManager.PLAY_QUEUE.clear();
            }).bounds(rightX, y + 35, 100, 20).build());
        }
    }

    private Component getModeText() {
        return switch (MusicManager.playMode) {
            case 0 -> Component.translatable("gui.whimsical_ideas.music.mode.sequence");
            case 1 -> Component.translatable("gui.whimsical_ideas.music.mode.loop_list");
            case 2 -> Component.translatable("gui.whimsical_ideas.music.mode.loop_one");
            default -> Component.literal("Unknown");
        };
    }

    private Component getSyncModeText() {
        return Component.translatable(clientSyncMode.equals("FOLLOW") ? "gui.whimsical_ideas.music.sync.on" : "gui.whimsical_ideas.music.sync.off");
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 顶部信息
        if (MusicManager.listeningToHost != null) {
            gfx.drawCenteredString(this.font, Component.translatable("gui.whimsical_ideas.music.room.current", MusicManager.currentRoomName, MusicManager.listeningToHost).getString(), centerX, centerY - 80, 0x00FFFF);
        }

        String song = MusicManager.PLAYLIST.isEmpty() ? Component.translatable("gui.whimsical_ideas.music.song.none").getString() : MusicManager.PLAYLIST.get(MusicManager.currentIndex);
        gfx.drawCenteredString(this.font, Component.translatable("gui.whimsical_ideas.music.song.current", song).getString(), centerX, centerY - 45, 0xFFFF00);

        // 进度条
        int barWidth = 200;
        int barX = centerX - barWidth / 2;
        int barY = centerY - 10;
        gfx.fill(barX, barY, barX + barWidth, barY + 4, 0xFF555555);
        if (MusicManager.isPlaying || MusicManager.listeningToHost != null) {
            int progressWidth = (int) (barWidth * MusicManager.getProgress());
            gfx.fill(barX, barY, barX + progressWidth, barY + 4, 0xFF00FF00);
        }

        // 显示播放队列信息
        if (!MusicManager.PLAY_QUEUE.isEmpty()) {
            gfx.drawCenteredString(this.font, "§b下一首队列: " + MusicManager.PLAY_QUEUE.size() + " 首歌", centerX, centerY + 10, 0xFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}