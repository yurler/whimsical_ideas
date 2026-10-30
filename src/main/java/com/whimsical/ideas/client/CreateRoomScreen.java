package com.whimsical.ideas.client;

import com.whimsical.ideas.network.MusicRoomPacket;
import com.whimsical.ideas.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CreateRoomScreen extends Screen {
    private final Screen parent;
    private EditBox nameBox;
    private EditBox passwordBox;
    private String selectedType = "PUBLIC"; // PUBLIC, PRIVATE, HIDDEN

    public CreateRoomScreen(Screen parent) {
        super(Component.translatable("gui.whimsical_ideas.create.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.width / 2;
        int y = this.height / 2;

        // 房间名称输入框
        this.nameBox = new EditBox(this.font, x - 100, y - 60, 200, 20, Component.translatable("gui.whimsical_ideas.create.name"));

        // 使用语言文件生成默认名称，比如 "玩家的音乐房间"
        this.nameBox.setValue(
                Component.translatable("gui.whimsical_ideas.create.default_name", Minecraft.getInstance().player.getName().getString()).getString()
        );
        this.addRenderableWidget(this.nameBox);

        // 密码输入框
        this.passwordBox = new EditBox(this.font, x - 100, y - 20, 200, 20, Component.translatable("gui.whimsical_ideas.create.password"));
        this.passwordBox.setMaxLength(16);
        this.addRenderableWidget(this.passwordBox);

        // 房间类型切换按钮
        this.addRenderableWidget(Button.builder(getTypeText(), b -> {
            if (selectedType.equals("PUBLIC")) selectedType = "PRIVATE";
            else if (selectedType.equals("PRIVATE")) selectedType = "HIDDEN";
            else selectedType = "PUBLIC";
            b.setMessage(getTypeText());
            // 公共房间隐藏密码框
            this.passwordBox.visible = !selectedType.equals("PUBLIC");
        }).bounds(x - 100, y + 20, 200, 20).build());

        // 初始化时如果是公共房间，隐藏密码框
        this.passwordBox.visible = !selectedType.equals("PUBLIC");

        // 确认创建按钮
        this.addRenderableWidget(Button.builder(Component.translatable("gui.whimsical_ideas.create.confirm"), b -> {
            String name = nameBox.getValue().trim();
            String pwd = selectedType.equals("PUBLIC") ? "" : passwordBox.getValue().trim();
            if (!name.isEmpty()) {
                ModNetwork.CHANNEL.sendToServer(new MusicRoomPacket(MusicRoomPacket.Action.CREATE, name, pwd, "", "", 0, selectedType));
                // ★ 修复：创建后重新 new MusicScreen 以刷新界面
                this.minecraft.setScreen(new MusicScreen());
            }
        }).bounds(x - 100, y + 60, 95, 20).build());

        // 取消按钮
        this.addRenderableWidget(Button.builder(Component.translatable("gui.whimsical_ideas.cancel"), b ->
                this.minecraft.setScreen(new MusicScreen()) // ★ 修复：返回时刷新
        ).bounds(x + 5, y + 60, 95, 20).build());
    }

    private Component getTypeText() {
        String key = "gui.whimsical_ideas.create.type." + selectedType.toLowerCase();
        return Component.translatable("gui.whimsical_ideas.create.type", Component.translatable(key));
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);

        gfx.drawCenteredString(this.font, Component.translatable("gui.whimsical_ideas.create.name").getString(), this.width / 2, this.height / 2 - 75, 0xFFFFFF);
        if (!selectedType.equals("PUBLIC")) {
            gfx.drawCenteredString(this.font, Component.translatable("gui.whimsical_ideas.create.password").getString(), this.width / 2, this.height / 2 - 35, 0xFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}