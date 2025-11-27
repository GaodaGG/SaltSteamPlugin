package com.gg.SaltSteamPlugin;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamException;
import com.xuncorp.spw.workshop.api.PluginContext;
import com.xuncorp.spw.workshop.api.SpwPlugin;

import org.jetbrains.annotations.NotNull;

public class MainPlugin extends SpwPlugin {
    public MainPlugin(@NotNull PluginContext pluginContext) {
        super(pluginContext);
    }

    @Override
// ... L17
public void start() {
    super.start();
    
    // 关键修改：增加安全检查和更优雅的错误处理
    try {
        // 1. 尝试加载库。如果主程序已加载成功，这个方法可能安全退出；
        //    如果未加载，它会尝试解压 DLL。
        //    我们保留它以防万一主程序未加载。
        SteamAPI.loadLibraries();
        
        // 2. 尝试初始化 SteamAPI，并检查是否需要重启应用（虽然通常主程序会处理）
        if (SteamAPI.restartAppIfNecessary(3009140)) {
            // 如果需要重启，则退出。但对于插件来说，通常直接失败是更好的选择。
            // 这里我们只需要确保 init() 成功。
        }
        
    } catch (Throwable e) { 
        // 使用 Throwable 捕获所有可能出现的错误，包括 UnsatisfiedLinkError（这是底层的错误）
        // 记录日志，但不抛出致命异常，允许应用继续运行。
        System.err.println("SaltSteamPlugin: 无法加载或初始化 Steam API 库。Steam 功能将禁用。");
        // 如果想把错误信息也输出，可以加上：
        System.err.println("错误详情: " + e.getMessage());
        
        // ⭐ 重点：如果加载失败，直接退出 start 方法，阻止后续 SteamAPI 的调用。
        return;
    }

    Config config = Config.getInstance();

    if (config.isInitAfterStart()) {
        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
            // 确保 MainPluginExtension.initSteamAPI() 内部也包含 try-catch 来处理 SteamAPI.init() 失败的情况
            MainPluginExtension.initSteamAPI(); 
        }).start();
    }
}

    public void stop() {
        super.stop();
        SteamIntegration steamIntegration = new SteamIntegration();
        steamIntegration.initialize();
        steamIntegration.clearRichPresence();
        steamIntegration.shutdown();
    }
}
