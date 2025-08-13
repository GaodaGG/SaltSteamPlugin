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

        Config config = Config.getInstance();

        if (config.isInitAfterStart()) {
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {}
                MainPluginExtension.initSteamAPI();
            }).start();
        }
    }

    @Override
    public void stop() {
        super.stop();
        SteamIntegration steamIntegration = new SteamIntegration();
        steamIntegration.initialize();
        steamIntegration.clearRichPresence();
        steamIntegration.shutdown();
    }
}