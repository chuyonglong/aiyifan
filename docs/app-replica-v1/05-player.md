# 播放器复刻文档

## 原 APK 依据

- 播放器容器：`layout_video_player.xml`。
- 顶部控制：`layout_video_top.xml`。
- 底部控制：`layout_video_bottom.xml`。
- 播放页：`activity_video_base.xml`。
- 原 APK 使用 `LiteVideoPlayer`、`LiteVideoAdPlayer`、`SubtitleView`、弹幕 View、错误/加载/重播 include。

## V1 目标

使用 Media3 ExoPlayer 实现稳定的视频播放体验，复刻原 APK 的控制层结构：

- 竖屏播放窗口。
- 播放/暂停。
- 当前时间、总时长、进度拖动。
- 全屏/退出全屏。
- 清晰度、倍速、字幕、语言、选集。
- 加载中、错误、重播状态。
- 自动保存播放进度。

## 页面结构

播放器根布局：

- 黑色背景容器。
- Surface/PlayerView。
- 中央播放按钮。
- 顶部渐变控制栏：返回、标题、投屏占位、更多设置。
- 底部渐变控制栏：播放、当前时间、SeekBar、总时长、全屏。
- 横屏底部扩展栏：字幕、语言、倍速、清晰度、选集。
- 加载层、错误层、重播层。

## 播放状态

```kotlin
sealed class PlayerUiState {
    data object Idle : PlayerUiState()
    data object Loading : PlayerUiState()
    data class Ready(val durationMs: Long, val positionMs: Long) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
    data object Ended : PlayerUiState()
}
```

## 控制规则

- 点击视频区域切换控制栏显示/隐藏。
- 控制栏显示后 3 秒无操作自动隐藏。
- 拖动进度时暂停自动隐藏，松手后 seek。
- 横竖屏切换保持播放进度和当前集。
- 退出页面前保存播放进度。
- 播放结束：
  - 有下一集：自动进入下一集。
  - 无下一集：展示重播按钮。

## 清晰度、语言、字幕、倍速

清晰度：

- 使用底部弹窗或横屏菜单展示。
- 字段来自 `PlaybackQuality`。
- 切换后保持当前播放进度。

语言：

- 使用 `PlaybackLanguage`。
- 切换语言本质上重新加载对应 `mediaKey` 或播放源。

字幕：

- v1 支持外挂字幕 URL 或关闭字幕。
- 原 APK 有 `SubtitleView`，v1 使用 Media3 subtitle 支持。

倍速：

- 默认选项：`0.75x`、`1.0x`、`1.25x`、`1.5x`、`2.0x`。
- 原 APK 上倍速可能带 VIP 标识，v1 不做 VIP 限制。

## 播放历史

每 5 秒保存一次：

- `mediaKey`
- `episodeKey`
- `title`
- `coverUrl`
- `videoType`
- `progressMs`
- `durationMs`
- `updatedAt`

当播放进度超过总时长 95% 时标记为看完，但保留记录。

## 错误处理

- 播放 URL 为空：展示“视频加载失败”。
- 网络错误：展示错误层，点击重试重新 prepare。
- 解码失败：展示错误层，并上报日志。
- 切换清晰度失败：恢复原播放源和进度。

## V1 不做

- 广告播放器。
- 弹幕渲染和发送。
- 截图保存。
- 投屏/DLNA。
- 后台小窗。
- VIP 清晰度限制。

## 验收点

- 进入视频页能自动准备并播放。
- 播放/暂停/拖动进度可靠。
- 全屏切换后不重头播放。
- 切换清晰度和倍速生效。
- 退出再进入同一视频能恢复进度。
- 播放错误时能重试。
