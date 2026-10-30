package com.whimsical.ideas.client;

import com.whimsical.ideas.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MusicConfigScreen extends Screen {

    private final Screen parent;
    private EditBox folderBox;

    public MusicConfigScreen(Screen parent) {
        super(Component.translatable("whimsical_ideas.configuration.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.width / 2;
        int y = this.height / 2;

        this.folderBox = new EditBox(this.font, x - 100, y - 30, 200, 20, Component.literal("Path"));
        this.folderBox.setValue(Config.MUSIC_FOLDER.get());
        this.addRenderableWidget(this.folderBox);

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.whimsical_ideas.config.save"),
                b -> {
                    Config.MUSIC_FOLDER.set(folderBox.getValue());
                    Config.MUSIC_FOLDER.save();
                    this.minecraft.setScreen(parent);
                }
        ).bounds(x - 100, y + 20, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.whimsical_ideas.config.cancel"),
                b -> this.minecraft.setScreen(parent)
        ).bounds(x - 100, y + 45, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        super.render(gfx, mouseX, mouseY, partialTick);
    }
}