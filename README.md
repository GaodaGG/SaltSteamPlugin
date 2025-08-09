# 为 SPW Steam丰富状态进行扩展的插件
## 用法:
1. 将Plugin复制到%appdata%\Salt Player for Windows\workshop
2. 打开SPW 设置 - 账户 将丰富状态功能关闭
3. 修改 %appdata%\Salt Player for Windows\workshop 下的config.json文件

## 可配置项:
- songFormat: 歌曲格式化字符串，支持 {title}, {artist}, {album}, {albumArtist}, {mainLyrics}, {subLyrics}
> 示例: "{artist} - {title}" 或 "{albumArtist} - {album} - {title}"
> 
> 注意: {mainLyrics} 为 主歌词文本，{subLyrics} 为翻译歌词文本
- useLyric: 当songFormat中包含歌词相关占位符时，要将此设为true

# **不需要解压!!**

## 其他插件:
[临时SPW全屏窗口插件](https://github.com/GaodaGG/SaltFullPlugin)

[Discord 丰富状态](https://github.com/GaodaGG/SaltDiscordPlugin)
