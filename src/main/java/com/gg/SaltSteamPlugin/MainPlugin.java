package com.gg.SaltSteamPlugin;

import com.xuncorp.spw.workshop.api.PluginContext;
import com.xuncorp.spw.workshop.api.SpwPlugin;
import com.xuncorp.spw.workshop.api.WorkshopApi;

import org.jetbrains.annotations.NotNull;

public class MainPlugin extends SpwPlugin {
    public MainPlugin(@NotNull PluginContext pluginContext) {
        super(pluginContext);
    }

    @Override
    public void stop() {
        super.stop();
        SteamIntegration steamIntegration = new SteamIntegration();
        steamIntegration.initialize();
        steamIntegration.clearRichPresence();
        steamIntegration.shutdown();
    }

    public static void toastFoundClassStatus() {
        Class<?> steamworks = SteamworksCatcher.getSteamworksClass();

        if (steamworks != null) {
            WorkshopApi.ui().toast("✅ 已找到宿主类: " + steamworks.getName(), WorkshopApi.Ui.ToastType.Success);
        } else {
            WorkshopApi.ui().toast("❌ 未找到宿主类", WorkshopApi.Ui.ToastType.Error);
        }
    }
}
