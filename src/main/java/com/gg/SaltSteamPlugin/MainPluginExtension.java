package com.gg.SaltSteamPlugin;

import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pf4j.Extension;

import javax.xml.transform.Source;


@Extension
public class MainPluginExtension implements PlaybackExtensionPoint {
    private static final Config config = Config.getInstance();
    private static boolean setRichPresence(String formattedSong) {
        SteamIntegration steamIntegration = new SteamIntegration();
        steamIntegration.initialize();
        boolean song = steamIntegration.setRichPresence("song", formattedSong);
        boolean steamDisplay = steamIntegration.setRichPresence("steam_display", "#ListeningTo");

        System.out.println(steamIntegration.getCurrentUserSteamID());
        steamIntegration.getRichPresence(steamIntegration.getCurrentUserSteamID(), "status");
        steamIntegration.shutdown();
        return song && steamDisplay;
    }

    @Nullable
    @Override
    public String onBeforeLoadLyrics(@NotNull MediaItem mediaItem) {
        if (config.isUseLyric()) {
            return null;
        }

        String formattedSong = config.formatSongString(mediaItem);
        boolean setRichPresence = setRichPresence(formattedSong);
        if (setRichPresence) {
            System.out.println("MediaItem rich presence set successfully: " + formattedSong);
        }
        return null;
    }

    @Override
    public void onLyricsLineUpdated(@Nullable LyricsLine lyricsLine) {
        System.out.println("isUseLyric: " + config.isUseLyric());
        if (lyricsLine == null) {
            return;
        }

        if (!config.isUseLyric()) {
            return;
        }

        String formattedSong = config.formatSongString(lyricsLine);
        boolean setRichPresence = setRichPresence(formattedSong);

        if (setRichPresence) {
            System.out.println("Lyrics rich presence set successfully: " + formattedSong);
        }
    }
}
