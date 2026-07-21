# V1 架构与工程约定

## 目标

用 Kotlin + XML View 复刻爱壹帆的第一版视频主链路。v1 需要让用户完成“打开 App -> 浏览首页 -> 搜索/分类 -> 打开视频详情 -> 播放 -> 记录历史/收藏”的闭环。

## 推荐工程结构

```text
app/src/main/
  AndroidManifest.xml
  java/com/aiyifan/app/
    AiyifanApp.kt
    core/
      network/
      database/
      navigation/
      ui/
      player/
    feature/
      splash/
      main/
      home/
      category/
      search/
      video/
      history/
      collection/
      auth/
      mine/
  res/
    layout/
    drawable/
    mipmap/
    values/
```

## 技术选型

- 语言：Kotlin。
- UI：XML + ViewBinding。
- 页面：Activity + Fragment，主框架使用单 `MainActivity` + 多 Fragment。
- 网络：Retrofit + OkHttp + Kotlin Coroutines。
- 图片：Glide。
- 播放器：Media3 ExoPlayer。
- 本地数据：Room，保存搜索历史、播放历史、收藏、登录会话缓存。
- 下拉刷新/加载更多：可选 SmartRefreshLayout 或自研轻量组件。

## 主导航

v1 主界面保留 4 个底部 tab：

- 首页：视频分类、推荐、热门内容。
- 热门：排行榜/热门专题，可先用首页接口的热门频道数据。
- 我的：游客态、登录入口、历史、收藏、设置。
- VIP：v1 不接入，建议隐藏。若产品需要保留入口，点击展示“暂未开放”。

原 APK 中“资讯/发现、音乐、VIP”不进入 v1 主链路。

## 关键数据模型

### Category

```kotlin
data class Category(
    val id: String,
    val name: String,
    val type: Int,
    val styleType: Int,
    val url: String?
)
```

对应 APK 线索：`NavigationBarBean` 包含 `categoryId`、`name`、`type`、`styleType`、`url`。

### VideoSummary

```kotlin
data class VideoSummary(
    val mediaKey: String,
    val title: String,
    val coverUrl: String,
    val videoType: Int,
    val contentType: String?,
    val score: String?,
    val playCount: Int,
    val updateStatus: String?,
    val duration: String?,
    val year: String?,
    val area: String?
)
```

### VideoDetail

```kotlin
data class VideoDetail(
    val mediaKey: String,
    val title: String,
    val coverUrl: String,
    val typeName: String?,
    val director: String?,
    val actor: String?,
    val introduce: String?,
    val playCount: Int,
    val comments: Int,
    val shareCount: Int,
    val publishTime: String?,
    val updateMsg: String?,
    val commentEnabled: Boolean,
    val episodes: List<Episode>,
    val qualities: List<PlaybackQuality>,
    val languages: List<PlaybackLanguage>,
    val related: List<VideoSummary>
)
```

### Episode / Playback

```kotlin
data class Episode(
    val episodeKey: String,
    val episodeTitle: String,
    val uniqueId: Int,
    val mediaUrl: String?,
    val resolution: String?,
    val lang: String?,
    val duration: String?,
    val watchProgressMs: Long
)

data class PlaybackQuality(
    val resolution: String,
    val description: String,
    val mediaUrl: String,
    val isDefault: Boolean
)

data class PlaybackLanguage(
    val mediaKey: String,
    val name: String
)
```

## 网络接口

v1 自定义接口，不绑定原 APK 的 `ppt.{Host}` 动态域名。

```text
GET /app/config
GET /categories
GET /home?categoryId={id}&page={page}
GET /category/{id}/filters
GET /videos?categoryId={id}&filters=...&page={page}
GET /search/suggest?keyword={keyword}
GET /search/hot
GET /search/videos?keyword={keyword}&page={page}
GET /videos/{mediaKey}
GET /videos/{mediaKey}/comments?page={page}
POST /videos/{mediaKey}/like
POST /videos/{mediaKey}/dislike
POST /videos/{mediaKey}/favorite
DELETE /videos/{mediaKey}/favorite
```

## 本地数据库

Room 表：

- `search_history`：keyword、createdAt。
- `watch_history`：mediaKey、episodeKey、title、coverUrl、videoType、progressMs、durationMs、updatedAt。
- `favorite_video`：mediaKey、title、coverUrl、videoType、createdAt。
- `app_cache`：首页分类、配置缓存。
- `auth_session`：token、userId、nickname、avatarUrl，可空。

## 登录策略

- 游客可以浏览首页、分类、搜索、详情和播放。
- 收藏、点赞、评论需要登录；未登录时跳转 `LoginActivity`。
- v1 可以只实现账号密码登录表单和登录态保存；注册、找回密码、验证码页面先做空壳或隐藏入口。

## 付费策略

- 所有支付/VIP 页面不实现。
- 原 APK 中带 `isVip`、`goldOpenNumber`、`adGold` 的字段在 v1 只作为展示参考，不阻断播放。
- 如果接口返回付费内容，v1 显示“该内容暂未开放”，不发起购买流程。
