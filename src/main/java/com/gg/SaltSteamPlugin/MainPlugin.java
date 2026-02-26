package com.gg.SaltSteamPlugin;

import com.xuncorp.spw.workshop.api.PluginContext;
import com.xuncorp.spw.workshop.api.SpwPlugin;
import com.xuncorp.spw.workshop.api.WorkshopApi;

import org.jetbrains.annotations.NotNull;

public class MainPlugin extends SpwPlugin {
    public MainPlugin(@NotNull PluginContext pluginContext) {
        super(pluginContext);
        SteamworksCatcher.init(pluginContext.getSpwVersion(), pluginContext.getSpwChannel().name());
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
