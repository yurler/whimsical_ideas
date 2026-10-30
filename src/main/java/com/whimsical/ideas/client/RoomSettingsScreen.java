package com.whimsical.ideas.client;

import com.whimsical.ideas.network.MusicRoomPacket;
import com.whimsical.ideas.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RoomSettingsScreen extends Screen {
    private final Screen parent;
    private EditBox passwordBox;
    private String currentType;

    public RoomSettingsScreen(Screen parent) {
        super(Component.translatable("gui.whimsical_ideas.settings.title"));
        this.parent = parent;
        this.currentType = "PUBLIC"; // 实际应用中应从本地缓存拿
    }

    @Override
    protected void init() {
        super.init();
        int x = this.width / 2;
        int y = this.height / 2;

        this.passwordBox = new EditBox(this.font, x - 100, y - 20, 200, 20, Component.translatable("gui.whimsical_ideas.settings.password"));
        this.passwordBox.setMaxLength(16);
        this.addRenderableWidget(this.passwordBox);

        this.addRenderableWidget(Button.builder(getTypeText(), b -> {
            if (currentType.equals("PUBLIC")) currentType = "PRIVATE";
            else if (currentType.equals("PRIVATE")) currentType = "HIDDEN";
            else currentType = "PUBLIC";
            b.setMessage(getTypeText());
            this.passwordBox.visible = !currentType.equals("PUBLIC");
        }).bounds(x - 100, y + 20, 200, 20).build());

        this.passwordBox.visible = !currentType.equals("PUBLIC");

        this.addRenderableWidget(Button.builder(Component.translatable("gui.whimsical_ideas.settings.save"), b -> {
            String pwd = currentType.equals("PUBLIC") ? "" : passwordBox.getValue().trim();
            ModNetwork.CHANNEL.sendToServer(new MusicRoomPacket(MusicRoomPacket.Action.UPDATE_ROOM, MusicManager.currentRoomName, pwd, "", "", 0, currentType));
            this.minecraft.setScreen(parent);
        }).bounds(x - 100, y + 60, 95, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.whimsical_ideas.back"), b -> this.minecraft.setScreen(parent))
                .bounds(x + 5, y + 60, 95, 20).build());
    }

    private Component getTypeText() {
        String key = "gui.whimsical_ideas.create.type." + currentType.toLowerCase();
        return Component.translatable("gui.whimsical_ideas.create.type", Component.translatable(key));
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);
        gfx.drawCenteredString(this.font, Component.translatable("gui.whimsical_ideas.settings.password").getString(), this.width / 2, this.height / 2 - 35, 0xFFFFFF);
    }
}