package com.gg.SaltSteamPlugin;

import com.codedisaster.steamworks.*;

public class SteamIntegration {
    // 常量定义
    private static final String STEAM_NOT_INITIALIZED_MSG = "Steam integration not initialized";
    private SteamFriends steamFriends;
    private SteamUser steamUser;
    private boolean initialized = false;

    public boolean initialize() {
        try {
            if (!SteamAPI.isSteamRunning()) {
                System.err.println("Steam is not running");
                return false;
            }

            steamFriends = new SteamFriends(new SteamFriendsCallback() {
                @Override
                public void onSetPersonaNameResponse(boolean success, boolean localSuccess, SteamResult result) {

                }

                @Override
                public void onPersonaStateChange(SteamID steamID, SteamFriends.PersonaChange change) {

                }

                @Override
                public void onGameOverlayActivated(boolean active) {

                }

                @Override
                public void onGameLobbyJoinRequested(SteamID steamIDLobby, SteamID steamIDFriend) {

                }

                @Override
                public void onAvatarImageLoaded(SteamID steamID, int image, int width, int height) {

                }

                @Override
                public void onFriendRichPresenceUpdate(SteamID steamIDFriend, int appID) {

                }

                @Override
                public void onGameRichPresenceJoinRequested(SteamID steamIDFriend, String connect) {

                }

                @Override
                public void onGameServerChangeRequested(String server, String password) {

                }
            });

            // 创建SteamUser接口来获取用户信息
            steamUser = new SteamUser(new SteamUserCallback() {
                @Override
                public void onValidateAuthTicket(SteamID steamID, SteamAuth.AuthSessionResponse authSessionResponse, SteamID ownerSteamID) {

                }

                @Override
                public void onMicroTxnAuthorization(int appID, long orderID, boolean authorized) {

                }

                @Override
                public void onEncryptedAppTicket(SteamResult result) {

                }

                @Override
                public void onAuthSessionTicket(SteamAuthTicket authTicket, SteamResult result) {

                }
            });

            initialized = true;
            System.out.println("Steam integration initialized successfully (using existing Steam API)");
            return true;

        } catch (Exception e) {
            System.err.println("Failed to initialize Steam integration: " + e.getMessage());
            return false;
        }
    }

    public boolean setRichPresence(String key, String value) {
        if (!initialized || steamFriends == null) {
            System.err.println(STEAM_NOT_INITIALIZED_MSG);
            return false;
        }

        try {
            System.out.println("Setting rich presence: " + key + " = " + value);

            boolean b = steamFriends.setRichPresence(key, value);
            if (!b) {
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

    public String getRichPresence(SteamID steamID, String key) {
        if (!initialized || steamFriends == null) {
            System.err.println(STEAM_NOT_INITIALIZED_MSG);
            return null;
        }

        try {
            String value = steamFriends.getFriendRichPresence(steamID, key);
            System.out.println("GetRichPresence: " + key + " = " + value);
            return value;

        } catch (Exception e) {
            System.err.println("Exception in getRichPresence: " + e.getMessage());
            return null;
        }
    }

    public void clearRichPresence() {
        if (initialized && steamFriends != null) {
            try {
                steamFriends.clearRichPresence();
                System.out.println("Rich presence cleared");
            } catch (Exception e) {
                System.err.println("Failed to clear rich presence: " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        if (initialized) {
            try {
                // 不调用SteamAPI.shutdown()，因为主应用负责管理Steam API的生命周期
                if (steamFriends != null) {
                    steamFriends.dispose();
                }
                if (steamUser != null) {
                    steamUser.dispose();
                }
                initialized = false;
                steamFriends = null;
                steamUser = null;
                System.out.println("Steam integration shutdown (Steam API remains active)");
            } catch (Exception e) {
                System.err.println("Failed to shutdown Steam integration: " + e.getMessage());
            }
        }
    }


    /**
     * 获取当前用户的Steam ID
     *
     * @return Steam ID，如果未初始化则返回null
     */
    public SteamID getCurrentUserSteamID() {
        if (!initialized || steamUser == null) {
            return null;
        }

        try {
            return steamUser.getSteamID();
        } catch (Exception e) {
            System.err.println("Failed to get current user Steam ID: " + e.getMessage());
            return null;
        }
    }

}