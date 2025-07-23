package com.gg.SaltSteamPlugin;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamException;
import org.pf4j.Plugin;

public class MainPlugin extends Plugin {
    @Override
    public void start() {
        super.start();
        steamAPI();
    }

    private void steamAPI() {
        try {
            SteamAPI.loadLibraries();
        } catch (SteamException e) {
            throw new RuntimeException(e);
        }
        new Thread(() -> {
            try {
                while (!SteamAPI.isSteamRunning(true)) {
                    Thread.sleep(5000);
                    SteamAPI.init();
                    System.out.println("Steam is running: " + SteamAPI.isSteamRunning());
                    System.out.println("Steam is initialized: " + SteamAPI.isSteamRunning(true));
                }
            } catch (InterruptedException | SteamException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}