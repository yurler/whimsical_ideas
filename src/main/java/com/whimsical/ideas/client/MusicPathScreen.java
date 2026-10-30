package com.whimsical.ideas.client;

import com.whimsical.ideas.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MusicPathScreen extends Screen {
    private final Screen parent;
    private EditBox pathBox;

    public MusicPathScreen(Screen parent) {
        super(Component.translatable("gui.whimsical_ideas.path.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.width / 2;
        int y = this.height / 2;

        // 路径输入框
        this.pathBox = new EditBox(this.font, x - 150, y - 20, 300, 20, Component.translatable("gui.whimsical_ideas.path.hint"));
        // 读取当前配置
        this.pathBox.setValue(Config.MUSIC_FOLDER.get());
        this.addRenderableWidget(this.pathBox);

        // 保存按钮
        this.addRenderableWidget(Button.builder(Component.translatable("gui.whimsical_ideas.settings.save"), b -> {
            String newPath = pathBox.getValue().trim();
            // 保存路径
            Config.MUSIC_FOLDER.set(newPath);
            Config.MUSIC_FOLDER.save();

            // 立即刷新播放列表
            MusicManager.loadPlaylist();

            // 返回上一级界面
            this.minecraft.setScreen(parent);
        }).bounds(x - 150, y + 20, 145, 20).build());

        // 取消按钮
        this.addRenderableWidget(Button.builder(Component.translatable("gui.whimsical_ideas.cancel"), b -> {
            this.minecraft.setScreen(parent);
        }).bounds(x + 5, y + 20, 145, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);

        gfx.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        gfx.drawCenteredString(this.font, Component.translatable("gui.whimsical_ideas.path.desc"), this.width / 2, this.height / 2 - 35, 0xAAAAAA);
    }
}