# 为 SPW Steam丰富状态进行扩展的插件
## 用法:
1. 打开设置 → 账户 将丰富状态功能关闭
2. 在 SPW 设置 → 创意工坊 → 模组管理 → 右上角导入模组 → 选择 `Steam_Rich_Presence.zip`
3. 设置 → 创意工坊 → 模组管理 启用 "Steam 丰富状态扩展"
4. 修改配置
5. 切歌使插件获取信息

## 可配置项:
- `songFormat`：歌曲信息的格式化字符串。可用占位符：
  - `{title}`：歌曲标题
  - `{artist}`：艺术家
  - `{album}`：专辑名
  - `{albumArtist}`：专辑艺术家
  - `{mainLyrics}`：主歌词文本
  - `{subLyrics}`：翻译歌词文本
  - `{position}`：当前播放位置，格式为 mm:ss
  - `{duration}`：歌曲总时长，格式为 mm:ss

  示例：
  - `{artist} - {title}`
  - `{albumArtist} - {album} - {title}`

  注意：由于 Steam 限制，丰富状态只能以每5秒更新一次

# **不需要解压!!**

## 其他插件:
[临时SPW全屏窗口插件](https://github.com/GaodaGG/SaltFullPlugin)

[Discord 丰富状态](https://github.com/GaodaGG/SaltDiscordPlugin)
