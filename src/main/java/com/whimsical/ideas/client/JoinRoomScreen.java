package com.whimsical.ideas.client;

import com.whimsical.ideas.network.MusicRoomPacket;
import com.whimsical.ideas.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class JoinRoomScreen extends Screen {
    private final Screen parent;
    private String[] roomList = new String[0];
    private EditBox passwordBox;

    public JoinRoomScreen(Screen parent) {
        super(Component.translatable("gui.whimsical_ideas.join.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        // 初始化密码输入框
        this.passwordBox = new EditBox(this.font, this.width / 2 - 100, this.height - 60, 200, 20, Component.translatable("gui.whimsical_ideas.join.password_hint"));
        this.passwordBox.setMaxLength(16);
        this.addRenderableWidget(this.passwordBox);

        // 向服务端请求最新的房间列表
        ModNetwork.CHANNEL.sendToServer(new MusicRoomPacket(MusicRoomPacket.Action.REQUEST_LIST, "", "", "", "", 0, ""));
    }

    // 由网络包调用，接收服务端返回的房间列表
    public void setRoomList(String data) {
        if (data == null || data.isEmpty()) {
            this.roomList = new String[0];
            return;
        }
        this.roomList = data.split(";");
        this.rebuildWidgets(); // 刷新控件
    }

    @Override
    protected void rebuildWidgets() {
        // 清除旧控件
        this.clearWidgets();

        // 重新添加密码输入框（clearWidgets 会清空它）
        this.passwordBox = new EditBox(this.font, this.width / 2 - 100, this.height - 60, 200, 20, Component.translatable("gui.whimsical_ideas.join.password_hint"));
        this.passwordBox.setMaxLength(16);
        this.addRenderableWidget(this.passwordBox);

        int x = this.width / 2;
        int y = this.height / 2 - 60;

        // 动态生成房间按钮
        for (int i = 0; i < roomList.length; i++) {
            String roomInfo = roomList[i];
            String[] parts = roomInfo.split(":");
            if (parts.length >= 2) {
                String roomName = parts[0];
                String type = parts[1];

                // 为了让长名字不超出屏幕，这里截断一下显示
                String displayName = roomName.length() > 18 ? roomName.substring(0, 15) + "..." : roomName;

                this.addRenderableWidget(Button.builder(Component.literal("[" + type + "] " + displayName), b -> {
                    String pwd = passwordBox.getValue().trim();
                    // 发送加入请求
                    ModNetwork.CHANNEL.sendToServer(new MusicRoomPacket(MusicRoomPacket.Action.JOIN, roomName, pwd, "", "", 0, ""));
                    // ★ 修复：加入后重新 new MusicScreen 以刷新界面
                    this.minecraft.setScreen(new MusicScreen());
                }).bounds(x - 100, y + i * 25, 200, 20).build());
            }
        }

        // 返回按钮
        this.addRenderableWidget(Button.builder(Component.translatable("gui.whimsical_ideas.back"), b ->
                this.minecraft.setScreen(new MusicScreen()) // ★ 修复：返回也刷新
        ).bounds(x - 100, this.height - 30, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);

        gfx.drawCenteredString(this.font, Component.translatable("gui.whimsical_ideas.join.list").getString(), this.width / 2, 30, 0xFFFFFF);
        gfx.drawCenteredString(this.font, Component.translatable("gui.whimsical_ideas.join.hint").getString(), this.width / 2, this.height - 75, 0xAAAAAA);

        if (roomList.length == 0) {
            gfx.drawCenteredString(this.font, Component.translatable("gui.whimsical_ideas.join.empty").getString(), this.width / 2, this.height / 2, 0xAAAAAA);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}