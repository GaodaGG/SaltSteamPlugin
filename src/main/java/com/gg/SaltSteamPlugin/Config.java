package com.gg.SaltSteamPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {
    private static final String CONFIG_FILE = "config.json";
    private static Config instance;
    private ConfigData configData;

    public static class ConfigData {
        public RichPresenceFormat richPresenceFormat = new RichPresenceFormat();
        public boolean useLyric = true;

        public static class RichPresenceFormat {
            public String songFormat = "{artist} - {title}";
        }
    }

    private Config() {
        loadConfig();
    }

    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    private void loadConfig() {
        Path configPath = Paths.get(CONFIG_FILE);
        Gson gson = new Gson();

        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                configData = gson.fromJson(reader, ConfigData.class);
                System.out.println("配置文件加载成功");
            } catch (IOException e) {
                System.err.println("读取配置文件失败: " + e.getMessage());
                configData = new ConfigData();
            }
        } else {
            // 创建默认配置文件
            configData = new ConfigData();
            saveConfig();
        }
    }

    public void saveConfig() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = Files.newBufferedWriter(Paths.get(CONFIG_FILE))) {
            gson.toJson(configData, writer);
            System.out.println("配置文件保存成功");
        } catch (IOException e) {
            System.err.println("保存配置文件失败: " + e.getMessage());
        }
    }

    public String formatSongString(PlaybackExtensionPoint.MediaItem mediaItem) {
        return configData.richPresenceFormat.songFormat
                .replace("{artist}", mediaItem.getArtist())
                .replace("{title}", mediaItem.getTitle())
                .replace("{album}", mediaItem.getAlbum())
                .replace("{albumArtist}", mediaItem.getAlbumArtist());
    }

    public String formatSongString(PlaybackExtensionPoint.LyricsLine lyricsLine) {

        return configData.richPresenceFormat.songFormat
                .replace("{mainLyrics}", lyricsLine.getPureMainText())
                .replace("{subLyrics}", lyricsLine.getPureSubText() != null ? lyricsLine.getPureSubText() : " ");
    }

    public boolean isUseLyric() {
        return configData.useLyric;
    }

    public ConfigData getConfigData() {
        return configData;
    }
}
