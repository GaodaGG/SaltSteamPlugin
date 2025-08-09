package com.gg.SaltSteamPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.*;

public class Config {
    private static final String CONFIG_FILE = "config.json";
    private static Config instance;
    private ConfigData configData;

    private Config() {
        loadConfig();
        new Thread(this::configWatcher).start();
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
        Path configPath = getConfigPath();
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
        Path configPath = getConfigPath();
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            gson.toJson(configData, writer);
            System.out.println("配置文件保存成功");
        } catch (IOException e) {
            System.err.println("保存配置文件失败: " + e.getMessage());
        }
    }

    private void configWatcher() {
        try {
            Path configPath = getConfigPath().getParent();
            WatchService watcher = FileSystems.getDefault().newWatchService();
            configPath.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) {
                WatchKey key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();

                    if (fileName.toString().equals(CONFIG_FILE)) {
                        System.out.println("配置文件已修改，重新加载配置");
                        loadConfig();

                        MainPluginExtension.setRichPresence(formatSongString(MainPluginExtension.getMediaItem(), MainPluginExtension.getLyricsLine()));
                    }
                }

                key.reset();
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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

        return configData.songFormat
                .replace("{title}", title)
                .replace("{artist}", artist)
                .replace("{album}", album)
                .replace("{albumArtist}", albumArtist)
                .replace("{mainLyrics}", mainLyrics)
                .replace("{subLyrics}", subLyrics);
    }


    public ConfigData getConfigData() {
        return configData;
    }

    public static class ConfigData {
        public String songFormat = "{artist} - {title}";
    }
}
