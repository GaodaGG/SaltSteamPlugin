package com.gg.SaltSteamPlugin;

import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pf4j.Extension;

import javax.swing.*;
import java.awt.*;


@Extension
public class MainPluginExtension implements PlaybackExtensionPoint {

    @Override
    public void onStateChanged(@NotNull State state) {
        switch (state) {
            case State.Ready:
                System.out.println("Playback paused.");
                break;
            case State.Ended:
                System.out.println("Playback resumed.");
                break;
            default:
                System.out.println("Unknown playback state: " + state);
        }
    }

    @Override
    public void onIsPlayingChanged(boolean b) {
        if (b) {
            System.out.println("Playback is now playing.");
        } else {
            System.out.println("Playback is paused.");
        }
    }

    @Override
    public void onSeekTo(long l) {
    }

    @Nullable
    @Override
    public String updateLyrics(@NotNull MediaItem mediaItem) {
        SteamIntegration steamIntegration = new SteamIntegration();
        steamIntegration.initialize();
        steamIntegration.setRichPresence("song", mediaItem.getArtist() + " - " + mediaItem.getTitle());
        steamIntegration.setRichPresence("steam_display", "#ListeningTo");

        System.out.println(steamIntegration.getCurrentUserSteamID());
        steamIntegration.getRichPresence(steamIntegration.getCurrentUserSteamID(), "status");
        steamIntegration.shutdown();

        return null; // Return null to indicate no lyrics available
    }
}
