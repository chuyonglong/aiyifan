# 爱壹帆 APK 功能分析

分析对象：`C:\goWork\aiyifan\6_base.apk`

反编译方式：使用 `apktool d` 读取 Manifest、XML 布局、字符串资源和 smali 类名。当前目录没有原 Android 工程源码。

## 应用定位

原 APK 是一个内容播放平台，应用名为“爱壹帆”，包名为 `com.cqcsy.ifvod`。主体验围绕在线视频浏览、搜索、详情、播放、用户互动和个人中心展开，同时集成了 VIP、支付、音乐、上传、投屏、扫码、消息等大量功能。

## 主要功能模块

### 启动与主框架

- `SplashActivity` 是启动页，`MainActivity` 是主框架。
- `activity_main.xml` 使用底部 RadioGroup 导航，主内容通过 `FrameLayout fragmentContainer` 承载 Fragment。
- 底部导航包含：首页、资讯/发现、VIP、音乐、热门、我的，其中音乐按钮在布局中默认 `gone`。
- 原 APK 支持深链：`ppde://com.ppde.ppcd`。

### 首页与分类

- 首页布局为 `layout_home.xml`。
- 顶部包含 logo、搜索入口、扫码入口、播放记录入口、上传菜单入口。
- 主体是 `TabLayout + ViewPager`，分类数据来自 `NavigationBarBean`，缓存 key 为 `homeNavigationBar`。
- 首页分类可进入左右栏分类页：`CategoryActivity` / `activity_category.xml`。
- 内容卡片包含 banner、海报栅格、信息流大卡、推荐模块、分类筛选入口。

### 视频详情与播放

- 播放页入口为 `VideoPlayVerticalActivity`，布局包含 `activity_video_base.xml` 和 `layout_video_details.xml`。
- 播放器区域固定在上方，原始高度约 `211dp`，底部内容容器承载详情/评论等内容。
- 播放器使用自定义 `LiteVideoPlayer`，包含播放/暂停、进度、全屏、清晰度、倍速、字幕、语言、选集、弹幕、错误、加载、重播、广告层和投屏层。
- 视频详情包含 up 主、标题、播放量、时间、类型、详情弹层、点赞、不喜欢、收藏、分享、下载、选集、相关推荐、评论。

### 搜索

- 搜索页为 `SearchActivity` / `activity_search.xml`。
- 初始状态展示搜索历史、清空历史、热门搜索、输入联想列表。
- 搜索结果使用 `TabLayout + ViewPager`，原包支持视频、用户、动态、相册等结果页。
- v1 只实现视频结果，其他 tab 暂不展示。

### 历史与收藏

- 播放记录入口为 `RecordActivity` / `activity_record.xml`。
- 原包记录类型包含剧集、小视频、动态、相册。
- v1 只实现普通视频观看历史和视频收藏。
- 收藏入口在“我的”页和视频详情页都有体现。

### 账号基础

- 登录相关页面包括 `LoginActivity`、`RegisterActivity`、`ResetPassword`、`RetrievePassword`、验证码与账号安全页面。
- 原包支持手机/邮箱绑定、注册、找回密码、账号安全、黑名单等。
- v1 建议只做游客态 + 基础登录态壳：未登录可浏览/播放，收藏/评论等需要登录时弹登录。

### 非 v1 主链路

以下功能在原 APK 中存在，但第一个版本暂不接入：

- VIP、会员购买、代充、兑换、支付、账单地址。
- 音乐首页、歌单、排行、歌手、音乐播放、音乐 VIP。
- 上传中心、短视频上传、图片裁剪、相册发布。
- 社区动态、关注、粉丝、私信、聊天、投诉。
- 离线下载、投屏、扫码授权、系统消息、任务中心、签到、金币、等级。
- 广告真实投放、支付风控、第三方地图定位。

## V1 范围

v1 目标是做“视频主链路”：

- 首页分类与内容流。
- 分类筛选和分类列表。
- 搜索历史、热搜、联想、视频搜索结果。
- 视频详情、选集、相关推荐、评论列表。
- 视频播放、播放控制、播放进度记录。
- 基础收藏、观看历史、游客/登录态分流。

付费相关入口在 v1 中隐藏或显示为“暂未开放”，不接入真实支付、会员、代充、广告跳过或金币消费。

## 复刻原则

- 技术栈固定为 Kotlin + XML View，不使用 Compose。
- UI 结构优先参考原 APK 的 XML 布局名、控件层级和尺寸。
- 不直接复用可能受版权限制的原包资源。图标可用用户指定来源：iconfont 或 Google Material Symbols Rounded。
- 网络接口不要绑定原线上域名；新项目定义自己的 Retrofit API 与数据模型。
- v1 以可用主链路为第一目标，社交、付费、上传、音乐逐步拆到后续版本。
