package com.gg.SaltSteamPlugin;

import com.codedisaster.steamworks.*;

public class SteamIntegration {
    // 常量定义
    private static final String STEAM_DISPLAY_KEY = "status";
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

            steamFriends.setRichPresence(key, value);
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

    // Steam状态管理功能

    /**
     * 设置Steam个人状态（显示在好友列表中的状态文本）
     *
     * @param status 状态文本
     * @return 是否设置成功
     */
    public boolean setPersonaStatus(String status) {
        if (!initialized || steamFriends == null) {
            System.err.println(STEAM_NOT_INITIALIZED_MSG);
            return false;
        }

        try {
            // 使用Rich Presence来设置状态
            steamFriends.setRichPresence(STEAM_DISPLAY_KEY, status);
            System.out.println("Personal status set to: " + status);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to set personal status: " + e.getMessage());
            return false;
        }
    }

    /**
     * 设置详细的游戏状态信息
     *
     * @param appId         应用ID（0表示非游戏状态）
     * @param gameExtraInfo 游戏额外信息
     * @return 是否设置成功
     */
    public boolean setGameInfo(int appId, String gameExtraInfo) {
        if (!initialized || steamFriends == null) {
            System.err.println(STEAM_NOT_INITIALIZED_MSG);
            return false;
        }

        try {
            if (appId > 0) {
                // 设置为正在游戏状态
                steamFriends.setRichPresence(STEAM_DISPLAY_KEY, "#Status_InGame");
                steamFriends.setRichPresence("game", gameExtraInfo);
            } else {
                // 清除游戏状态
                steamFriends.setRichPresence(STEAM_DISPLAY_KEY, gameExtraInfo);
            }
            System.out.println("Game info set - AppID: " + appId + ", Info: " + gameExtraInfo);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to set game info: " + e.getMessage());
            return false;
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

    /**
     * 获取当前个人状态
     *
     * @return 当前个人状态，如果未初始化则返回null
     */
    public SteamFriends.PersonaState getCurrentPersonaState() {
        if (!initialized || steamFriends == null) {
            return null;
        }

        try {
            SteamID currentUser = getCurrentUserSteamID();
            if (currentUser != null) {
                return steamFriends.getFriendPersonaState(currentUser);
            }
            return null;
        } catch (Exception e) {
            System.err.println("Failed to get current persona state: " + e.getMessage());
            return null;
        }
    }
}