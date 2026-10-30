package com.whimsical.ideas;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<String> MUSIC_FOLDER = BUILDER
            .comment("The folder path for music.")
            .translation("whimsical_ideas.config.music_folder")
            .define("musicFolder", "config/whimsical_ideas/music");

    public static final ForgeConfigSpec.DoubleValue MUSIC_VOLUME = BUILDER
            .comment("Music playback volume (0.0 to 1.0)")
            .translation("whimsical_ideas.config.music_volume")
            .defineInRange("musicVolume", 0.5D, 0.0D, 1.0D);

    // ★ 新增：服务器专属音乐房间开关
    public static final ForgeConfigSpec.BooleanValue ENABLE_SERVER_ROOM = BUILDER
            .comment("Enable the official server music room.")
            .translation("whimsical_ideas.config.enable_server_room")
            .define("enableServerRoom", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}