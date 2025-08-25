package com.gg.SaltSteamPlugin;

import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;
import com.xuncorp.spw.workshop.api.WorkshopApi;
import com.xuncorp.spw.workshop.api.config.ConfigHelper;
import com.xuncorp.spw.workshop.api.config.ConfigManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;

public class Config {
    private static final String CONFIG_FILE = "config.json";
    private static Config instance;
    ConfigManager configManager = WorkshopApi.manager().createConfigManager("Steam 丰富状态扩展");
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

    @NotNull
    private static Path getConfigPath() {
        return Path.of(System.getenv("APPDATA") + "/Salt Player for Windows/workshop/", CONFIG_FILE);
    }

    private void loadConfig() {
        if (Files.notExists(configHelper.getConfigPath())) {
            // 创建默认配置文件
            saveConfig();
            return;
        }

        configHelper.reload();
        configData.songFormat = configHelper.get("songFormat", "{artist} - {title}");
        configData.initAfterStart = configHelper.get("initAfterStart", false);
        System.out.println("配置文件加载成功");
    }

    public void saveConfig() {
        configHelper.set("songFormat", configData.songFormat);
        configHelper.set("initAfterStart", configData.initAfterStart);
        configHelper.save();
    }

    private void configWatcher() {
        configManager.addConfigChangeListener(configHelper -> {
            loadConfig();
            MainPluginExtension.setRichPresence(formatSongString(MainPluginExtension.getMediaItem(), MainPluginExtension.getLyricsLine()));
        });
    }

    public String formatSongString(PlaybackExtensionPoint.MediaItem mediaItem) {
        return formatSongString(mediaItem, null);
    }

    public String formatSongString(PlaybackExtensionPoint.LyricsLine lyricsLine) {
        return formatSongString(null, lyricsLine);
    }

    public String formatSongString(PlaybackExtensionPoint.MediaItem mediaItem, PlaybackExtensionPoint.LyricsLine lyricsLine) {
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
                .replace("{subLyrics}", subLyrics);
        return Normalizer.normalize(replacedString, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
    }

    public boolean hasLyrics() {
        return configData.songFormat.contains("{mainLyrics}") || configData.songFormat.contains("{subLyrics}");
    }

    public boolean isInitAfterStart() {
        return configData.initAfterStart;
    }

    public ConfigData getConfigData() {
        return configData;
    }

    public static class ConfigData {
        public String songFormat = "{artist} - {title}";
        public boolean initAfterStart = false;
    }
}
