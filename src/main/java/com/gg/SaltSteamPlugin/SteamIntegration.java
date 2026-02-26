package com.gg.SaltSteamPlugin;

/**
 * Steam 集成层，通过 SteamworksCatcher 从宿主程序获取 Steamworks4k 实例。
 */
public class SteamIntegration {

    private static final String STEAM_NOT_INITIALIZED_MSG = "Steam integration not initialized";

    private Object steamworks;
    private boolean initialized = false;

    /**
     * 初始化：通过 SteamworksCatcher 从宿主获取 Steamworks4k 实例。
     */
    public boolean initialize() {
        try {
            steamworks = SteamworksCatcher.getSteamworks();
            if (steamworks == null) {
                System.err.println("Failed to obtain Steamworks4k instance from host");
                return false;
            }

            if (!SteamworksCatcher.isInitialized(steamworks)) {
                System.err.println("Steamworks4k instance is not initialized");
                return false;
            }

            initialized = true;
            System.out.println("Steam integration initialized successfully (using host Steamworks4k)");
            return true;

        } catch (Exception e) {
            System.err.println("Failed to initialize Steam integration: " + e.getMessage());
            return false;
        }
    }

    public boolean setRichPresence(String key, String value) {
        if (!initialized || steamworks == null) {
            System.err.println(STEAM_NOT_INITIALIZED_MSG);
            return false;
        }

        try {
            System.out.println("Setting rich presence: " + key + " = " + value);

            boolean success = SteamworksCatcher.setRichPresence(steamworks, key, value);
            if (!success) {
                System.err.println("Failed to set rich presence for key: " + key);
                return false;
            }
            System.out.println("SetRichPresence succeeded");
            return true;

        } catch (Exception e) {
            System.err.println("Exception in setRichPresence: " + e.getMessage());
            return false;
        }
    }

    /**
     * 清除 Rich Presence，通过将各 key 设为空字符串实现。
     */
    public void clearRichPresence() {
        if (!initialized || steamworks == null) {
            return;
        }

        try {
            SteamworksCatcher.setRichPresence(steamworks, "song", "");
            SteamworksCatcher.setRichPresence(steamworks, "steam_display", "");
            System.out.println("Rich presence cleared");
        } catch (Exception e) {
            System.err.println("Failed to clear rich presence: " + e.getMessage());
        }
    }

    public void shutdown() {
        initialized = false;
        steamworks = null;
    }

}