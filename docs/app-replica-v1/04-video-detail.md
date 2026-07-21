# 视频详情与互动复刻文档

## 原 APK 依据

- 播放 Activity：`VideoPlayVerticalActivity`。
- 上方播放器容器：`activity_video_base.xml`。
- 下方详情容器：`layout_video_details.xml`。
- 简介页：`layout_introduction.xml`。
- 详情弹层：`layout_video_detail_fragment.xml`。
- 评论页：`layout_comment.xml`。
- 选集页：`layout_anthology_fragment.xml`。

## V1 目标

详情页和播放页合一：上方播放视频，下方展示详情、选集、推荐和评论。用户可以完成播放、选集切换、收藏、点赞、不喜欢、分享、查看详情、评论列表浏览。

## 页面结构

### VideoPlayerActivity

- 根布局：竖向容器。
- 上方：播放器区域，默认高度约 `211dp`。
- 下方：`TabLayout + ViewPager2`。
- v1 tabs：
  - 简介
  - 评论
- 如果视频有多集，简介页内展示选集模块；不单独做第三个 tab。

### 简介页

顶部 up 主区：

- 头像、昵称、粉丝数。
- 关注按钮 v1 可保留，未登录点击跳登录。

视频信息：

- 标题，最多两行。
- 播放量、评分、发布时间。
- 类型、地区、年份。
- “详情”入口，点击打开底部弹层或全屏详情页。

互动区：

- 点赞。
- 不喜欢。
- 收藏。
- 分享。
- 下载按钮 v1 隐藏或置灰。

选集区：

- 有多集时显示标题“选集”。
- 横向 RecyclerView 展示集数。
- 当前播放集高亮。
- 点击集数切换播放源，并写入历史。

推荐区：

- 标题“相关推荐”。
- 使用海报卡或信息流卡。
- 点击推荐项打开新视频。

### 详情弹层

展示字段：

- 标题。
- 更新信息。
- 年份、类型。
- 播放量。
- 分类。
- 导演。
- 主演。
- 简介。

弹层可关闭，关闭后不影响播放状态。

### 评论页

- RecyclerView 分页展示评论。
- 底部评论输入栏固定。
- v1 评论发布可以先要求登录；未登录跳登录。
- 投票入口隐藏。
- 支持评论加载失败重试。

## 数据字段

详情接口返回：

- `mediaKey`
- `title`
- `coverUrl`
- `typeName`
- `director`
- `actor`
- `introduce`
- `playCount`
- `comments`
- `shareCount`
- `publishTime`
- `updateMsg`
- `commentEnabled`
- `episodes`
- `qualities`
- `languages`
- `related`

互动状态：

```kotlin
data class VideoActionState(
    val liked: Boolean,
    val disliked: Boolean,
    val favorited: Boolean,
    val likeCount: Int,
    val dislikeCount: Int,
    val favoriteCount: Int
)
```

评论：

```kotlin
data class Comment(
    val id: Long,
    val content: String,
    val userName: String,
    val avatarUrl: String?,
    val postTime: String,
    val likeCount: Int,
    val liked: Boolean,
    val replies: List<Comment>
)
```

## 交互规则

- 进入页面优先请求视频详情，拿到默认集后开始准备播放。
- 切换集数时播放器重新加载，详情页选集高亮变化。
- 收藏需要登录；游客点击跳登录。
- 点赞/不喜欢互斥。
- 分享 v1 可以调用系统分享文本，内容为视频标题和落地页 URL。
- 评论关闭时隐藏评论 tab 或展示“评论已关闭”。

## V1 不做

- 付费解锁。
- 金币跳广告。
- 真实下载。
- 弹幕发送。
- 投票评论。
- 黑名单和举报完整流程。

## 验收点

- 从首页/搜索进入后能加载视频详情并播放默认集。
- 多集视频能切换集数。
- 收藏状态能持久化到本地和账号接口。
- 评论列表能分页加载。
- 详情弹层字段完整且可关闭。
