package com.gg.SaltSteamPlugin;

import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;
import com.xuncorp.spw.workshop.api.WorkshopApi;
import com.xuncorp.spw.workshop.api.config.ConfigHelper;
import com.xuncorp.spw.workshop.api.config.ConfigManager;

import java.nio.file.Files;
import java.text.Normalizer;

public class Config {
    private static Config instance;
    ConfigManager configManager = WorkshopApi.manager().createConfigManager();
    ConfigHelper configHelper = configManager.getConfig();
    private ConfigData configData = new ConfigData();

    private Config() {
        loadConfig();
        configWatcher();
    }

    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    private void loadConfig() {
        if (Files.notExists(configHelper.getConfigPath())) {
            // 创建默认配置文件
            saveConfig();
            return;
        }

        configHelper.reload();
        configData.songFormat = configHelper.get("songFormat", "{artist} - {title}");
        System.out.println("配置文件加载成功");
    }

    public void saveConfig() {
        configHelper.set("songFormat", configData.songFormat);
        configHelper.save();
    }

    private void configWatcher() {
        configManager.addConfigChangeListener(configHelper -> {
            loadConfig();
            MainPluginExtension.setRichPresence(
                    formatSongString(
                            MainPluginExtension.getMediaItem(),
                            MainPluginExtension.getLyricsLine(),
                            MainPluginExtension.getPosition(),
                            MainPluginExtension.getDuration()
                    )
            );
        });
    }

    public String formatSongString(PlaybackExtensionPoint.MediaItem mediaItem) {
        return formatSongString(mediaItem, null, null, null);
    }

    public String formatSongString(PlaybackExtensionPoint.LyricsLine lyricsLine) {
        return formatSongString(null, lyricsLine, "", "");
    }

    public String formatSongString(PlaybackExtensionPoint.MediaItem mediaItem, PlaybackExtensionPoint.LyricsLine lyricsLine, String position, String duration) {
        String title = mediaItem != null ? mediaItem.getTitle() : "";
        String artist = mediaItem != null ? mediaItem.getArtist() : "";
        String album = mediaItem != null ? mediaItem.getAlbum() : "";
        String albumArtist = mediaItem != null ? mediaItem.getAlbumArtist() : "";

        String mainLyrics = lyricsLine != null ? lyricsLine.getPureMainText() : "";
        String subLyrics = (lyricsLine != null && lyricsLine.getPureSubText() != null) ? lyricsLine.getPureSubText() : "";

        String replacedString = configData.songFormat
                .replace("{title}", title)
                .replace("{artist}", artist)
                .replace("{album}", album)
                .replace("{albumArtist}", albumArtist)
                .replace("{mainLyrics}", mainLyrics)
                .replace("{subLyrics}", subLyrics)
                .replace("{position}", position != null ? position : "")
                .replace("{duration}", duration != null ? duration : "");
        return Normalizer.normalize(replacedString, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
    }

    public boolean hasLyrics() {
        return configData.songFormat.contains("{mainLyrics}") || configData.songFormat.contains("{subLyrics}");
    }

    public boolean hasPosition() {
        return configData.songFormat.contains("{position}");
    }

    public ConfigData getConfigData() {
        return configData;
    }

    public static class ConfigData {
        public String songFormat = "{artist} - {title}";
    }
}
