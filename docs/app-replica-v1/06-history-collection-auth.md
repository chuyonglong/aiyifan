# 历史、收藏与账号基础复刻文档

## 原 APK 依据

- 播放记录：`RecordActivity`、`activity_record.xml`。
- 我的页：`MineFragment`、`layout_mine.xml`。
- 收藏入口：视频详情页收藏按钮、“我的收藏”入口。
- 登录页：`LoginActivity`、`RegisterActivity`、`RetrievePassword` 等。

## V1 目标

实现支撑视频主链路的个人数据能力：

- 游客可浏览和播放。
- 观看历史本地保存。
- 收藏视频需要登录；也可先本地收藏，登录后同步。
- “我的”页展示登录态、历史、收藏、基础设置。
- 登录页完成基础账号登录壳。

## 我的页

布局参考 `layout_mine.xml`，v1 精简：

- 顶部背景图、头像、昵称。
- 未登录：显示默认头像、登录按钮。
- 已登录：显示头像、昵称、简介。
- 四个快捷入口只保留：
  - 离线视频：隐藏。
  - 观看记录：进入历史页。
  - 我的收藏：进入收藏页。
  - 消息：隐藏。
- 上传中心、相册、动态、VIP 提示、任务中心、金币、等级全部隐藏。

## 历史页

原 APK 有剧集、小视频、动态、相册多个 tab。v1 只做“视频”：

- 顶部标题：“观看记录”。
- 右上角“编辑”进入批量选择模式。
- 列表展示封面、标题、集数/类型、观看进度、更新时间。
- 点击继续播放。
- 支持单条删除、批量删除、清空全部。

Room 表：

```kotlin
@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val mediaKey: String,
    val episodeKey: String,
    val title: String,
    val coverUrl: String,
    val videoType: Int,
    val progressMs: Long,
    val durationMs: Long,
    val updatedAt: Long
)
```

## 收藏页

- 标题：“我的收藏”。
- 列表展示收藏的视频。
- 点击进入视频页。
- 支持取消收藏和批量管理。
- 已登录时优先请求服务端收藏列表；无网络时展示本地缓存。

Room 表：

```kotlin
@Entity(tableName = "favorite_video")
data class FavoriteVideoEntity(
    @PrimaryKey val mediaKey: String,
    val title: String,
    val coverUrl: String,
    val videoType: Int,
    val createdAt: Long
)
```

## 登录基础

v1 登录页字段：

- 账号输入。
- 密码输入。
- 登录按钮。
- 用户协议/隐私协议勾选。

登录成功保存：

```kotlin
data class AuthSession(
    val token: String,
    val userId: String,
    val nickname: String,
    val avatarUrl: String?
)
```

未登录拦截：

- 收藏。
- 点赞/不喜欢。
- 评论。
- 关注。

游客可用：

- 首页。
- 分类。
- 搜索。
- 详情。
- 播放。
- 本地观看历史。

## 同步规则

- 播放历史默认本地保存；登录后可调用接口同步，但 v1 可先不做云同步。
- 收藏若未登录，点击收藏直接跳登录；登录成功后回到原视频并执行收藏。
- 退出登录后清除 token，保留本地观看历史。

## V1 不做

- 注册。
- 找回密码。
- 验证码。
- 账号安全。
- 黑名单。
- 私信与系统消息。
- 金币、等级、签到、任务中心。

## 验收点

- 未登录可以正常播放并产生本地观看历史。
- 收藏未登录时跳登录。
- 登录成功后“我的”页显示用户信息。
- 收藏列表可查看、取消收藏。
- 历史列表可继续播放、删除、清空。
