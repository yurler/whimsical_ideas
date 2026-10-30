package com.whimsical.ideas.client;

import com.whimsical.ideas.Config;
import com.whimsical.ideas.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@OnlyIn(Dist.CLIENT)
public class MusicManager {
    public static final List<String> PLAYLIST = new ArrayList<>();
    // ★ 新增：待播放队列
    public static final Queue<String> PLAY_QUEUE = new LinkedList<>();

    public static int currentIndex = 0;
    public static boolean isPlaying = false;
    public static boolean isPaused = false;
    public static int playMode = 0;

    public static float personalVolume = 1.0f;
    public static String currentRoomName = null;
    public static String listeningToHost = null;

    private static int sourceId = -1;
    private static int bufferId = -1;
    private static Thread checkThread = null;
    public static float totalDuration = 0;
    private static long playStartTime = 0;
    private static float offsetWhenPaused = 0;

    private static int remoteSourceId = -1;
    private static int remoteBufferId = -1;
    private static long remoteStartTime = 0;
    private static float remoteTotalDuration = 0;
    private static float remoteOffsetPaused = 0;

    public static void init() {
        MinecraftForge.EVENT_BUS.register(MusicManager.class);
        MinecraftForge.EVENT_BUS.addListener(MusicManager::onConfigReload);
    }

    private static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == Config.SPEC) {
            if (isPlaying && !isPaused && sourceId != -1) AL10.alSourcef(sourceId, AL10.AL_GAIN, Config.MUSIC_VOLUME.get().floatValue());
            loadPlaylist();
        }
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        leaveRoom();
        stopLocal();
        PLAY_QUEUE.clear();
    }

    public static void joinRoom(String roomName, String hostName) {
        currentRoomName = roomName;
        listeningToHost = hostName;
        Minecraft.getInstance().player.displayClientMessage(Component.translatable("gui.whimsical_ideas.room.joined", roomName), false);
    }

    public static void leaveRoom() {
        currentRoomName = null;
        stopAllRemoteMusic();
    }

    public static File getMusicDir() {
        String pathStr = Config.MUSIC_FOLDER.get();
        File dir = new File(pathStr);
        if (!dir.isAbsolute()) dir = new File(Minecraft.getInstance().gameDirectory, pathStr);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static void loadPlaylist() {
        PLAYLIST.clear();
        File[] files = getMusicDir().listFiles((d, name) -> name.toLowerCase().endsWith(".ogg"));
        if (files != null) for (File f : files) PLAYLIST.add(f.getName());
        if (currentIndex >= PLAYLIST.size()) currentIndex = 0;
    }

    public static void playCurrent() {
        if (PLAYLIST.isEmpty()) return;
        stopLocal();
        File file = new File(getMusicDir(), PLAYLIST.get(currentIndex));
        if (!file.exists()) return;
        isPlaying = true;
        isPaused = false;
        offsetWhenPaused = 0;

        new Thread(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer channels = stack.mallocInt(1);
                IntBuffer sampleRate = stack.mallocInt(1);
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                ByteBuffer fileBuffer = MemoryUtil.memAlloc(fileBytes.length);
                fileBuffer.put(fileBytes); fileBuffer.flip();
                ShortBuffer rawAudio = STBVorbis.stb_vorbis_decode_memory(fileBuffer, channels, sampleRate);
                MemoryUtil.memFree(fileBuffer);
                if (rawAudio == null) { isPlaying = false; return; }
                int format = channels.get(0) == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
                int bytesPerFrame = channels.get(0) * 2;
                float duration = (float) rawAudio.remaining() / (sampleRate.get(0) * bytesPerFrame);

                Minecraft.getInstance().execute(() -> {
                    try {
                        totalDuration = duration;
                        bufferId = AL10.alGenBuffers();
                        AL10.alBufferData(bufferId, format, rawAudio, sampleRate.get(0));
                        MemoryUtil.memFree(rawAudio);
                        sourceId = AL10.alGenSources();
                        AL10.alSourcei(sourceId, AL10.AL_BUFFER, bufferId);
                        AL10.alSourcef(sourceId, AL10.AL_GAIN, Config.MUSIC_VOLUME.get().floatValue());
                        AL10.alSourcei(sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
                        AL10.alSourcePlay(sourceId);
                        playStartTime = System.currentTimeMillis();

                        checkThread = new Thread(() -> {
                            try {
                                while (isPlaying && sourceId != -1) {
                                    int state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
                                    if (state == AL10.AL_STOPPED && !isPaused) {
                                        Minecraft.getInstance().execute(MusicManager::onMusicEnd);
                                        break;
                                    }
                                    Thread.sleep(500);
                                }
                            } catch (Exception ignored) {}
                        });
                        checkThread.setDaemon(true);
                        checkThread.start();
                    } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
        }).start();
    }

    // ★ 处理队列和播放模式
    private static void onMusicEnd() {
        if (!isPlaying) return;

        // 1. 优先检查队列
        if (!PLAY_QUEUE.isEmpty()) {
            String nextSong = PLAY_QUEUE.poll();
            // 同步给房间内其他人（如果是房主）
            if (listeningToHost == null && currentRoomName != null) {
                ModNetwork.CHANNEL.sendToServer(new MusicQueuePacket(MusicQueuePacket.Action.NEXT, nextSong));
                ModNetwork.CHANNEL.sendToServer(new MusicRoomPacket(MusicRoomPacket.Action.SYNC_MUSIC, "", "", "", nextSong, 0, "", "FOLLOW"));
            }

            // 将队列中的歌切换为当前播放
            if (PLAYLIST.contains(nextSong)) {
                currentIndex = PLAYLIST.indexOf(nextSong);
                playCurrent();
                return;
            }
        }

        // 2. 检查循环模式
        if (playMode == 2) {
            playCurrent();
        } else if (playMode == 1) {
            if (!PLAYLIST.isEmpty()) {
                currentIndex = (currentIndex + 1) % PLAYLIST.size();
                playCurrent();
            }
        } else {
            if (currentIndex < PLAYLIST.size() - 1) {
                currentIndex++;
                playCurrent();
            } else {
                stopLocal();
            }
        }
    }

    public static void togglePause() {
        if (sourceId == -1) return;
        if (isPaused) {
            AL10.alSourcePlay(sourceId);
            playStartTime = System.currentTimeMillis() - (long)(offsetWhenPaused * 1000);
            isPaused = false;
            if (currentRoomName != null) ModNetwork.CHANNEL.sendToServer(new MusicControlPacket(MusicControlPacket.Action.RESUME, ""));
        } else {
            offsetWhenPaused = getProgress() * totalDuration;
            AL10.alSourcePause(sourceId);
            isPaused = true;
            if (currentRoomName != null) ModNetwork.CHANNEL.sendToServer(new MusicControlPacket(MusicControlPacket.Action.PAUSE, ""));
        }
    }

    public static void stopLocal() {
        isPlaying = false;
        isPaused = false;
        try {
            if (sourceId != -1) { AL10.alSourceStop(sourceId); AL10.alDeleteSources(sourceId); sourceId = -1; }
            if (bufferId != -1) { AL10.alDeleteBuffers(bufferId); bufferId = -1; }
        } catch (Exception ignored) {}
        if (checkThread != null && checkThread.isAlive()) { checkThread.interrupt(); checkThread = null; }
    }

    public static void next() {
        if (!PLAYLIST.isEmpty()) {
            currentIndex = (currentIndex + 1) % PLAYLIST.size();
            if (isPlaying) playCurrent();
        }
    }
    public static void prev() {
        if (!PLAYLIST.isEmpty()) {
            currentIndex = (currentIndex - 1 + PLAYLIST.size()) % PLAYLIST.size();
            if (isPlaying) playCurrent();
        }
    }

    public static float getProgress() {
        if (listeningToHost != null) {
            if (remoteSourceId == -1 || remoteTotalDuration <= 0) return 0;
            long elapsedMs = System.currentTimeMillis() - remoteStartTime;
            return Math.min((elapsedMs / 1000.0f) / remoteTotalDuration, 1.0f);
        } else {
            if (sourceId == -1 || totalDuration <= 0 || isPaused) {
                return totalDuration <= 0 ? 0 : offsetWhenPaused / totalDuration;
            }
            long elapsedMs = System.currentTimeMillis() - playStartTime;
            return Math.min((elapsedMs / 1000.0f) / totalDuration, 1.0f);
        }
    }

    // ★ 局域网文件流：请求音乐文件
    public static void requestMusicFile(String fileName) {
        ModNetwork.CHANNEL.sendToServer(new MusicDataPacket(MusicDataPacket.Type.REQUEST, fileName, null));
        Minecraft.getInstance().player.displayClientMessage(Component.literal("§e[网络] 正在请求音乐文件: " + fileName), false);
    }

    // ★ 接收文件流并缓存
    public static void receiveMusicData(MusicDataPacket msg) {
        if (msg.type == MusicDataPacket.Type.REQUEST) {
            // 收到请求：如果本地有，就发给对方
            File file = new File(getMusicDir(), msg.fileName);
            if (file.exists()) {
                try {
                    byte[] data = Files.readAllBytes(file.toPath());
                    // 借用 fileName 传递目标名:文件名
                    ModNetwork.CHANNEL.sendToServer(new MusicDataPacket(MusicDataPacket.Type.RESPONSE, "TargetPlayer:" + msg.fileName, data));
                } catch (Exception ignored) {}
            }
        } else if (msg.type == MusicDataPacket.Type.RESPONSE) {
            // 收到文件，存入本地缓存
            try {
                File cacheDir = new File(getMusicDir(), "cache/" + listeningToHost);
                if (!cacheDir.exists()) cacheDir.mkdirs();
                File out = new File(cacheDir, msg.fileName);
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    fos.write(msg.data);
                }
                Minecraft.getInstance().player.displayClientMessage(Component.literal("§a[网络] 音乐同步完成: " + msg.fileName), false);
                // 自动播放刚下载好的歌
                playRemoteMusic(listeningToHost, msg.fileName, 1.0f, 0);
            } catch (Exception e) {
                Minecraft.getInstance().player.displayClientMessage(Component.literal("§c[网络] 音乐同步失败。"), false);
            }
        }
    }

    // ★ 处理队列网络包
    public static void handleQueuePacket(MusicQueuePacket msg) {
        if (msg.action == MusicQueuePacket.Action.ADD) {
            PLAY_QUEUE.add(msg.fileName);
            Minecraft.getInstance().player.displayClientMessage(Component.literal("§a[队列] 已添加: " + msg.fileName), false);
        } else if (msg.action == MusicQueuePacket.Action.CLEAR) {
            PLAY_QUEUE.clear();
        } else if (msg.action == MusicQueuePacket.Action.NEXT) {
            PLAY_QUEUE.poll(); // 同步播放进度
        }
    }

    public static void playRemoteMusic(String hostName, String fileName, float baseVolume, float playTime) {
        stopAllRemoteMusic();
        if (isPlaying) stopLocal();
        listeningToHost = hostName;

        File cacheDir = new File(getMusicDir(), "cache/" + hostName);
        if (!cacheDir.exists()) cacheDir.mkdirs();
        File file = new File(cacheDir, fileName);

        // ★ 如果本地没有这首歌，触发网络下载
        if (!file.exists()) {
            requestMusicFile(fileName);
            return;
        }

        new Thread(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer channels = stack.mallocInt(1);
                IntBuffer sampleRate = stack.mallocInt(1);
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                ByteBuffer fileBuffer = MemoryUtil.memAlloc(fileBytes.length);
                fileBuffer.put(fileBytes); fileBuffer.flip();
                ShortBuffer rawAudio = STBVorbis.stb_vorbis_decode_memory(fileBuffer, channels, sampleRate);
                MemoryUtil.memFree(fileBuffer);
                if (rawAudio == null) return;
                int format = channels.get(0) == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;

                int bytesPerFrame = channels.get(0) * 2;
                float duration = (float) rawAudio.remaining() / (sampleRate.get(0) * bytesPerFrame);

                Minecraft.getInstance().execute(() -> {
                    try {
                        remoteTotalDuration = duration;
                        remoteBufferId = AL10.alGenBuffers();
                        AL10.alBufferData(remoteBufferId, format, rawAudio, sampleRate.get(0));
                        MemoryUtil.memFree(rawAudio);
                        remoteSourceId = AL10.alGenSources();
                        AL10.alSourcei(remoteSourceId, AL10.AL_BUFFER, remoteBufferId);
                        AL10.alSourcei(remoteSourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
                        AL10.alSourcef(remoteSourceId, AL10.AL_GAIN, baseVolume * personalVolume);

                        if (playTime > 0) {
                            AL11.alSourcef(remoteSourceId, AL11.AL_SEC_OFFSET, playTime);
                            remoteStartTime = System.currentTimeMillis() - (long)(playTime * 1000);
                        } else {
                            remoteStartTime = System.currentTimeMillis();
                        }

                        AL10.alSourcePlay(remoteSourceId);
                    } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
        }).start();
    }

    public static void pauseRemoteMusic() { if (remoteSourceId != -1) { AL10.alSourcePause(remoteSourceId); remoteOffsetPaused = getProgress() * remoteTotalDuration; } }
    public static void resumeRemoteMusic() { if (remoteSourceId != -1) { AL10.alSourcePlay(remoteSourceId); remoteStartTime = System.currentTimeMillis() - (long)(remoteOffsetPaused * 1000); } }

    public static void stopAllRemoteMusic() {
        listeningToHost = null;
        try {
            if (remoteSourceId != -1) { AL10.alSourceStop(remoteSourceId); AL10.alDeleteSources(remoteSourceId); remoteSourceId = -1; }
            if (remoteBufferId != -1) { AL10.alDeleteBuffers(remoteBufferId); remoteBufferId = -1; }
        } catch (Exception ignored) {}
    }

    public static void setPersonalVolume(float vol) {
        personalVolume = vol;
        if (remoteSourceId != -1) AL10.alSourcef(remoteSourceId, AL10.AL_GAIN, Config.MUSIC_VOLUME.get().floatValue() * personalVolume);
    }
}