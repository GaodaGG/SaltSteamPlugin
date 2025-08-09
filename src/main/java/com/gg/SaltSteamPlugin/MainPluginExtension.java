package com.gg.SaltSteamPlugin;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamException;
import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pf4j.Extension;


@Extension
public class MainPluginExtension implements PlaybackExtensionPoint {
    private static final Config config = Config.getInstance();
    private static boolean isInitialized = false;
    private static MediaItem mediaItem = null;
    private static LyricsLine lyricsLine = null;

    public static boolean setRichPresence(String formattedSong) {
        initSteamAPI();

        SteamIntegration steamIntegration = new SteamIntegration();
        steamIntegration.initialize();
        boolean song = steamIntegration.setRichPresence("song", formattedSong);
        boolean steamDisplay = steamIntegration.setRichPresence("steam_display", "#ListeningTo");

        steamIntegration.shutdown();
        return song && steamDisplay;
    }

    private static void initSteamAPI() {
        if (!isInitialized) {
            try {
                SteamAPI.init();
            } catch (SteamException e) {
                throw new RuntimeException(e);
            }

            isInitialized = true;
        }
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

    @Nullable
    @Override
    public String onBeforeLoadLyrics(@NotNull MediaItem mediaItem) {
        setMediaItem(mediaItem);

//        if (config.isUseLyric()) {
//            return null;
//        }
//
//        String formattedSong = config.formatSongString(mediaItem);
//        boolean setRichPresence = setRichPresence(formattedSong);
//        if (setRichPresence) {
//            System.out.println("MediaItem rich presence set successfully: " + formattedSong);
//        }
        return null;
    }

    @Override
    public void onLyricsLineUpdated(@Nullable LyricsLine lyricsLine) {
        setLyricsLine(lyricsLine);
        if (!config.isUseLyric()) {
            return;
        }

        String formattedSong = config.formatSongString(getMediaItem(), lyricsLine);
        boolean setRichPresence = setRichPresence(formattedSong);

        if (setRichPresence) {
            System.out.println("Lyrics rich presence set successfully: " + formattedSong);
        }
    }
}
