# 为 SPW Steam丰富状态进行扩展的插件
## 用法:
1. 将Plugin复制到%appdata%\Salt Player for Windows\workshop
2. 打开设置 → 账户 将丰富状态功能关闭
3. 设置 → 创意工坊 → 模组管理 启用 "Steam 丰富状态扩展"
4. 修改配置
5. 重启SPW

## 可配置项:
- songFormat: 歌曲格式化字符串，支持 {title}, {artist}, {album}, {albumArtist}, {mainLyrics}, {subLyrics}
> 示例: "{artist} - {title}" 或 "{albumArtist} - {album} - {title}"
> 
> 注意: {mainLyrics} 为 主歌词文本，{subLyrics} 为翻译歌词文本
- initAfterStart: 为true时，插件将会在SPW启动后三秒再初始化Steam SDK
> 这可以避免SPW启动后，应用内显示steam无法连接的问题

# **不需要解压!!**

## 其他插件:
[临时SPW全屏窗口插件](https://github.com/GaodaGG/SaltFullPlugin)

[Discord 丰富状态](https://github.com/GaodaGG/SaltDiscordPlugin)
