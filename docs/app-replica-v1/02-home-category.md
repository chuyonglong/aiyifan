# 首页与分类复刻文档

## 原 APK 依据

- 首页：`layout_home.xml`、`HomeFragment`。
- 主框架：`activity_main.xml`、`MainActivity`。
- 分类页：`activity_category.xml`、`CategoryActivity`、`CategoryFilterActivity`。
- 卡片布局：`layout_banner.xml`、`layout_video_item.xml`、`layout_video_list_item.xml`、`layout_guess_like.xml`。

## V1 目标

首页要复刻原 APK 的视频浏览入口：

- 顶部品牌区、搜索入口、扫码图标占位、历史入口。
- 横向分类 tab。
- 分类下的 banner、模块标题、视频海报栅格、视频信息流。
- 更多分类页，左侧一级分类，右侧二级分类。
- 下拉刷新、上拉加载、加载中、空态、失败重试。

## 页面结构

### MainActivity

- 根布局垂直排列：`fragmentContainer` + 底部导航。
- v1 底部导航显示：首页、热门、我的。
- VIP、音乐、资讯/发现先隐藏。
- 点击首页/热门/我的时切换 Fragment，保持 Fragment 状态。

### HomeFragment

顶部栏：

- 左侧 logo。
- 中间搜索框文案：“搜一搜”或原字符串 `searchForIt`。
- 搜索框点击进入 `SearchActivity`。
- 扫码图标保留但置灰，点击提示“暂未开放”。
- 历史图标进入 `HistoryActivity`。
- 上传图标 v1 隐藏。

分类栏：

- 使用 `TabLayout + ViewPager2`。
- 分类来自 `/categories`，本地缓存到 `app_cache`。
- 默认分类加载失败时显示重试页；有缓存时先展示缓存，再后台刷新。

内容区：

- 每个分类页使用 `RecyclerView`。
- 支持多 viewType：banner、模块标题、海报卡、横向列表、信息流大卡、加载更多。
- 点击视频卡进入 `VideoPlayerActivity`，携带 `mediaKey`、`videoType`、可选 `episodeKey`。

### CategoryActivity

- 横向布局：左侧一级分类列表，右侧二级分类/筛选项列表。
- 点击右侧分类进入视频筛选结果页。
- v1 筛选维度建议：类型、地区、年份、排序。

## UI 细节

- 顶部栏高度参考原 APK：`56dp`。
- 首页分类栏高度参考：`40dp`。
- 底部导航高度参考：`52dp`。
- 海报卡图比例接近 `140x187` 或 `fill x 228dp`，标题单行省略。
- Banner 高度参考：`188dp`，底部渐变叠标题。
- 信息流大卡包含用户头像/名称、标题、视频封面、播放按钮、时长、浏览/评论/点赞数。

## 数据字段

首页分类：

- `id`
- `name`
- `type`
- `styleType`
- `url`

视频卡：

- `mediaKey`
- `title`
- `coverUrl`
- `score`
- `playCount`
- `updateStatus`
- `contentType`
- `duration`
- `videoType`

模块：

- `moduleId`
- `title`
- `style`: `banner | poster_grid | feed | horizontal`
- `items`

## 交互状态

- 首次进入：加载分类，成功后加载默认 tab 内容。
- 切换 tab：如果已有缓存立即展示，同时触发刷新。
- 下拉刷新：重载当前分类第一页。
- 上拉加载：追加下一页。
- 无网络有缓存：展示缓存和顶部 toast。
- 无网络无缓存：展示失败页和“点击重试”。
- 空数据：展示空态文案。

## V1 不做

- 上传菜单。
- 扫码真实授权。
- 首页广告真实投放。
- 资讯/发现流。
- 音乐入口。
- VIP 活动动画。

## 验收点

- 冷启动进入首页后能看到分类 tab 和默认内容。
- 点击搜索框进入搜索页。
- 点击历史图标进入观看历史。
- 点击视频卡进入播放详情页。
- 分类页左右列表能联动跳转结果。
- 首页断网时能展示缓存或失败重试。
