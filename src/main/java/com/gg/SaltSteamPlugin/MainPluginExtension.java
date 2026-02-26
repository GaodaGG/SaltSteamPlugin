package com.gg.SaltSteamPlugin;

import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pf4j.Extension;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;


@Extension
public class MainPluginExtension implements PlaybackExtensionPoint {
    private static final Config config = Config.getInstance();
    private static MediaItem mediaItem = null;
    private static LyricsLine lyricsLine = null;
    private static String position = "00:00";
    private static String duration = "00:00";

    private static final Logger logger = Logger.getLogger(MainPluginExtension.class.getName());

    public static boolean setRichPresence(String formattedSong) {
        SteamIntegration steamIntegration = new SteamIntegration();
        steamIntegration.initialize();
        boolean song = steamIntegration.setRichPresence("song", formattedSong);
        boolean steamDisplay = steamIntegration.setRichPresence("steam_display", "#ListeningTo");

        steamIntegration.shutdown();
        return song && steamDisplay;
    }


    public static MediaItem getMediaItem() {
        return mediaItem;
    }

    private static void setMediaItem(MediaItem mediaItem) {
        MainPluginExtension.mediaItem = mediaItem;
    }

    public static LyricsLine getLyricsLine() {
        return lyricsLine;
    }

    private static void setLyricsLine(LyricsLine lyricsLine) {
        MainPluginExtension.lyricsLine = lyricsLine;
    }

    public static String getPosition() {
        return position;
    }

    public static void setPosition(String position) {
        MainPluginExtension.position = position;
    }

    public static String getDuration() {
        return duration;
    }

    public static void setDuration(String duration) {
        MainPluginExtension.duration = duration;
    }

    public static void setDuration(MediaItem mediaItem) {
        int durationSeconds = getDurationSeconds(mediaItem.getPath());
        String duration = String.format("%02d:%02d", (durationSeconds / 60), (durationSeconds % 60));
        setDuration(duration);
    }

    /**
     * 获取音频文件时长（秒）
     */
    private static int getDurationSeconds(String audioPath) {
        try {
            File audioFile = new File(audioPath);
            AudioFile f = AudioFileIO.read(audioFile);
            AudioHeader header = f.getAudioHeader();
            return header.getTrackLength();
        } catch (Exception e) {
            logger.warning("读取音频文件失败: " + e.getMessage());
            return 0;
        }
    }

    @Nullable
    @Override
    public String onBeforeLoadLyrics(@NotNull MediaItem mediaItem) {
        setMediaItem(mediaItem);
        setDuration(mediaItem);

        String formattedSong = config.formatSongString(mediaItem);
        boolean setRichPresence = setRichPresence(formattedSong);
        if (setRichPresence) {
            logger.log(Level.INFO, "Lyrics rich presence set successfully: {}", formattedSong);
        }
        return null;
    }

    @Override
    public void onLyricsLineUpdated(@Nullable LyricsLine lyricsLine) {
        setLyricsLine(lyricsLine);

        if (!config.hasLyrics()) {
            return;
        }

        String formattedSong = config.formatSongString(getMediaItem(), lyricsLine, getPosition(), getDuration());
        boolean setRichPresence = setRichPresence(formattedSong);
        if (setRichPresence) {
            logger.log(Level.INFO, "Lyrics rich presence set successfully: {}", formattedSong);
        }
    }

    @Override
    public void onPositionUpdated(long position) {
        String pos = String.format("%02d:%02d", (position / 1000) / 60, (position / 1000) % 60);
        setPosition(pos);

        if (!config.hasPosition()) {
            return;
        }

        String formattedSong = config.formatSongString(getMediaItem(), lyricsLine, getPosition(), getDuration());
        boolean setRichPresence = setRichPresence(formattedSong);
        if (setRichPresence) {
            logger.log(Level.INFO, "Lyrics rich presence set successfully: {}", formattedSong);
        }
    }
}
