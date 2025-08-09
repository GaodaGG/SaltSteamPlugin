package com.gg.SaltSteamPlugin;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamException;
import org.pf4j.Plugin;

public class MainPlugin extends Plugin {
    @Override
    public void start() {
        super.start();
        try {
            SteamAPI.loadLibraries();
        } catch (SteamException e) {
            throw new RuntimeException(e);
        }
    }
}