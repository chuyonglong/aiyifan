# 视频搜索页 APK 对齐 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让视频搜索结果的布局、信息层级和剧集快捷入口对齐参考 APK，同时保持当前搜索、回退和播放链路。

**Architecture:** `VideoSummary` 保存搜索响应可用的剧集预览。纯 Kotlin 的 `SearchResultPresentation` 将可缺失的领域字段转为稳定的 UI 文本和可见性，适配器只渲染该展示模型；`SearchActivity` 只控制加载、成功、空和失败状态。

**Tech Stack:** Kotlin、Android View Binding、RecyclerView、Glide、Kotlin Coroutines、JUnit 4、XML Drawable。

---

## File Structure

- `app/src/main/java/com/aiyifan/app/core/model/Models.kt`: 保存剧集预览数据。
- `app/src/main/java/com/aiyifan/app/core/data/remote/RemoteCatalogRepository.kt`: 解析搜索响应内嵌剧集。
- `app/src/main/java/com/aiyifan/app/core/ui/SearchResultPresentation.kt`: 格式化卡片字段和页面状态。
- `app/src/main/java/com/aiyifan/app/core/ui/SearchAdapters.kt`: 渲染卡片及剧集快捷项。
- `app/src/main/java/com/aiyifan/app/feature/search/SearchActivity.kt`: 渲染搜索状态。
- `app/src/main/res/layout/activity_search.xml`: 消除成功状态的固定顶部提示行。
- `app/src/main/res/layout/item_search_result.xml`: 视频卡片和剧集容器。
- `app/src/main/res/drawable/bg_episode_preview.xml`: 剧集快捷项背景。
- `app/src/test/java/com/aiyifan/app/core/ui/SearchResultPresentationTest.kt`: 展示字段与状态的单测。
- `app/src/test/java/com/aiyifan/app/core/data/remote/RemoteCatalogRepositorySearchTest.kt`: 剧集解析测试。

### Task 1: 建立剧集预览和展示模型

**Files:**
- Modify: `app/src/main/java/com/aiyifan/app/core/model/Models.kt`
- Modify: `app/src/main/java/com/aiyifan/app/core/data/remote/RemoteCatalogRepository.kt`
- Create: `app/src/main/java/com/aiyifan/app/core/ui/SearchResultPresentation.kt`
- Create: `app/src/test/java/com/aiyifan/app/core/ui/SearchResultPresentationTest.kt`
- Modify: `app/src/test/java/com/aiyifan/app/core/data/remote/RemoteCatalogRepositorySearchTest.kt`

- [ ] **Step 1: 写入失败的展示模型测试**

```kotlin
@Test
fun `presentation groups metadata and limits episode previews`() {
    val result = SearchResultPresentation.from(
        VideoSummary(
            mediaKey = "demo", title = "示例剧集", coverUrl = "", videoType = 1,
            year = "2026", mediaType = "电视剧", contentType = "剧情", area = "中国大陆",
            actor = "主演甲", director = "导演乙",
            episodePreviews = (1..8).map { Episode("ep-$it", "$it", it, null) },
        ),
    )

    assertEquals("2026 / 电视剧", result.primaryMeta)
    assertEquals("剧情 / 中国大陆", result.secondaryMeta)
    assertEquals("导演：导演乙\n主演：主演甲", result.credits)
    assertEquals(listOf("1", "2", "3", "4", "5", "6"), result.episodeLabels)
    assertTrue(result.showEpisodePreviews)
}

@Test
fun `presentation hides empty metadata and episode previews`() {
    val result = SearchResultPresentation.from(
        VideoSummary("demo", title = "示例", coverUrl = "", videoType = 1),
    )

    assertEquals("", result.primaryMeta)
    assertEquals("", result.secondaryMeta)
    assertEquals("", result.credits)
    assertTrue(result.episodeLabels.isEmpty())
    assertFalse(result.showEpisodePreviews)
}
```

- [ ] **Step 2: 验证测试因尚未实现的类型而失败**

Run: `./gradlew.bat testDebugUnitTest --tests com.aiyifan.app.core.ui.SearchResultPresentationTest`

Expected: FAIL，提示 `SearchResultPresentation` 和 `episodePreviews` 未定义。

- [ ] **Step 3: 实现最小模型与 JSON 解析**

在 `VideoSummary` 最后追加默认字段，确保既有构造调用无需修改：

```kotlin
val episodePreviews: List<Episode> = emptyList(),
```

创建展示模型：

```kotlin
data class SearchResultPresentation(
    val primaryMeta: String,
    val secondaryMeta: String,
    val credits: String,
    val episodeLabels: List<String>,
    val showEpisodePreviews: Boolean,
) {
    companion object {
        fun from(video: VideoSummary) = SearchResultPresentation(
            primaryMeta = listOfNotNull(video.year, video.mediaType).joinToString(" / "),
            secondaryMeta = listOfNotNull(video.contentType, video.area, video.updateStatus).joinToString(" / "),
            credits = listOfNotNull(
                video.director?.takeIf(String::isNotBlank)?.let { "导演：$it" },
                video.actor?.takeIf(String::isNotBlank)?.let { "主演：$it" },
            ).joinToString("\n"),
            episodeLabels = video.episodePreviews.take(6).map(Episode::episodeTitle),
            showEpisodePreviews = video.episodePreviews.isNotEmpty(),
        )
    }
}
```

在 `RemoteCatalogRepository` 新增私有 `parseEpisodePreviews(items: JSONArray?)`，复用 `parseEpisodes` 的字段规则并返回最多六项。在 `parseSearchVideos` 构造摘要时传入：

```kotlin
episodePreviews = parseEpisodePreviews(item.optJSONArray("episodes")),
```

不得改动请求 URL、参数、目录回退或详情接口。

- [ ] **Step 4: 扩展远程解析测试并验证通过**

在既有嵌套列表测试的 JSON 中加入：

```json
"episodes": [
  {"episodeKey":"episode-1","episodeTitle":"01","episodeId":1},
  {"episodeKey":"episode-2","episodeTitle":"02","episodeId":2}
]
```

追加断言：

```kotlin
assertEquals(listOf("01", "02"), results.single().episodePreviews.map { it.episodeTitle })
```

Run: `./gradlew.bat testDebugUnitTest --tests com.aiyifan.app.core.ui.SearchResultPresentationTest --tests com.aiyifan.app.core.data.remote.RemoteCatalogRepositorySearchTest`

Expected: BUILD SUCCESSFUL。

- [ ] **Step 5: 提交数据和展示模型**

```powershell
git add app/src/main/java/com/aiyifan/app/core/model/Models.kt app/src/main/java/com/aiyifan/app/core/ui/SearchResultPresentation.kt app/src/main/java/com/aiyifan/app/core/data/remote/RemoteCatalogRepository.kt app/src/test/java/com/aiyifan/app/core/ui/SearchResultPresentationTest.kt app/src/test/java/com/aiyifan/app/core/data/remote/RemoteCatalogRepositorySearchTest.kt
git commit -m "feat(search): 补充剧集预览数据"
```

### Task 2: 重建视频卡片和剧集入口

**Files:**
- Modify: `app/src/main/java/com/aiyifan/app/core/ui/SearchAdapters.kt`
- Modify: `app/src/main/res/layout/item_search_result.xml`
- Create: `app/src/main/res/drawable/bg_episode_preview.xml`
- Modify: `app/src/test/java/com/aiyifan/app/core/ui/SearchResultPresentationTest.kt`

- [ ] **Step 1: 为剧集可见性写入失败测试**

```kotlin
@Test
fun `episode previews are visible only when the result has episodes`() {
    val withoutEpisodes = VideoSummary("empty", title = "空", coverUrl = "", videoType = 1)
    val withEpisodes = withoutEpisodes.copy(
        episodePreviews = listOf(Episode("ep-1", "01", 1, null)),
    )

    assertFalse(SearchResultPresentation.from(withoutEpisodes).showEpisodePreviews)
    assertTrue(SearchResultPresentation.from(withEpisodes).showEpisodePreviews)
}
```

- [ ] **Step 2: 运行测试，确认失败后实现卡片资源和布局**

Run: `./gradlew.bat testDebugUnitTest --tests com.aiyifan.app.core.ui.SearchResultPresentationTest`

Expected: FAIL，因为剧集可见性尚未实现或不正确。

创建 `bg_episode_preview.xml`：

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#F0F2F5" />
    <corners android:radius="4dp" />
</shape>
```

将 `item_search_result.xml` 改为纵向根节点：上半部分保留 `140dp x 187dp` 海报和右侧信息，下半部分为初始隐藏的 `episodePreviewRow`。信息区使用 `title`、`primaryMeta`、`secondaryMeta`、`credits` 四个 TextView；底部保留全宽 `playButton`。新增的剧集区域必须有以下 ID：

```xml
<LinearLayout android:id="@+id/episodePreviewRow" android:visibility="gone">
    <LinearLayout android:id="@+id/episodePreviewContainer" />
    <TextView android:id="@+id/viewAllButton" android:text="查看全部" />
</LinearLayout>
```

不要显示原 APK 的下载入口，因为项目无对应功能；卡片和按钮圆角不超过 `6dp`。

- [ ] **Step 3: 更新适配器的绑定逻辑**

`ResultViewHolder.bind` 调用 `SearchResultPresentation.from(item)`，仅在对应文本非空时显示元数据和演职员。动态生成剧集项：

```kotlin
binding.episodePreviewRow.isVisible = presentation.showEpisodePreviews
binding.episodePreviewContainer.removeAllViews()
presentation.episodeLabels.forEach { label ->
    binding.episodePreviewContainer.addView(TextView(binding.root.context).apply {
        text = label
        gravity = Gravity.CENTER
        setBackgroundResource(R.drawable.bg_episode_preview)
        setOnClickListener { onClick(item) }
    }, LinearLayout.LayoutParams(dpToPx(40), dpToPx(32)).apply {
        marginEnd = dpToPx(6)
    })
}
binding.viewAllButton.setOnClickListener { onClick(item) }
```

实现仅供适配器使用的 `dpToPx`。卡片、播放按钮、集数按钮和“查看全部”都复用既有 `onClick(item)` 并跳转 `VideoPlayerActivity`，不改播放器 Intent 协议。

- [ ] **Step 4: 验证测试和编译**

Run: `./gradlew.bat testDebugUnitTest --tests com.aiyifan.app.core.ui.SearchResultPresentationTest`

Expected: BUILD SUCCESSFUL。

Run: `./gradlew.bat assembleDebug`

Expected: BUILD SUCCESSFUL，View Binding 生成的新字段均被正确引用。

- [ ] **Step 5: 提交卡片改造**

```powershell
git add app/src/main/java/com/aiyifan/app/core/ui/SearchAdapters.kt app/src/main/res/layout/item_search_result.xml app/src/main/res/drawable/bg_episode_preview.xml app/src/test/java/com/aiyifan/app/core/ui/SearchResultPresentationTest.kt
git commit -m "feat(search): 对齐视频结果卡片"
```

### Task 3: 收敛结果页状态并完成验证

**Files:**
- Modify: `app/src/main/java/com/aiyifan/app/core/ui/SearchResultPresentation.kt`
- Modify: `app/src/main/java/com/aiyifan/app/feature/search/SearchActivity.kt`
- Modify: `app/src/main/res/layout/activity_search.xml`
- Modify: `app/src/test/java/com/aiyifan/app/core/ui/SearchResultPresentationTest.kt`

- [ ] **Step 1: 写入失败的搜索页面状态测试**

```kotlin
@Test
fun `success has no message while empty and failure show messages`() {
    assertTrue(SearchPageState.Success.showResults)
    assertFalse(SearchPageState.Success.showMessage)
    assertTrue(SearchPageState.Empty.showMessage)
    assertTrue(SearchPageState.Failure.showRetry)
}
```

- [ ] **Step 2: 运行状态测试，确认失败**

Run: `./gradlew.bat testDebugUnitTest --tests com.aiyifan.app.core.ui.SearchResultPresentationTest`

Expected: FAIL，`SearchPageState` 未定义。

- [ ] **Step 3: 实现状态模型并更新 Activity 和布局**

在 `SearchResultPresentation.kt` 定义：

```kotlin
enum class SearchPageState(
    val showResults: Boolean,
    val showMessage: Boolean,
    val showRetry: Boolean,
) {
    Loading(true, true, false),
    Success(true, false, false),
    Empty(true, true, false),
    Failure(true, true, true),
}
```

在 `activity_search.xml` 中将 `searchStatus` 移入覆盖层或内容区域，使成功结果没有固定 `44dp` 顶部行。它必须居中且不与 RecyclerView 同时遮挡可点击卡片。

在 `SearchActivity` 新增如下单一入口，并将查询回调改为 `Loading`、`Success`、`Empty`、`Failure` 四种调用：

```kotlin
private fun renderSearchState(state: SearchPageState, results: List<VideoSummary> = emptyList()) {
    binding.resultsContainer.isVisible = state.showResults
    binding.searchStatus.isVisible = state.showMessage
    binding.resultRecycler.isVisible = state != SearchPageState.Loading
    resultAdapter.submitList(results)
    binding.searchStatus.text = when (state) {
        SearchPageState.Loading -> "正在搜索..."
        SearchPageState.Empty -> "没有找到相关视频"
        SearchPageState.Failure -> "搜索失败，点击重试"
        SearchPageState.Success -> ""
    }
}
```

保留现有 300ms 联想防抖、历史存储、空输入校验、目录回退和 `searchStatus` 点击重试。

- [ ] **Step 4: 运行完整验证和设备对照**

Run: `./gradlew.bat test`

Expected: BUILD SUCCESSFUL，0 个失败测试。

Run: `./gradlew.bat lint`

Expected: BUILD SUCCESSFUL，未产生新的 lint error。

Run: `./gradlew.bat assembleDebug`

Expected: BUILD SUCCESSFUL，生成 `app/build/outputs/apk/debug/app-debug.apk`。

Run:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.aiyifan.app/.feature.search.SearchActivity
```

逐项检查初始历史与热门词、输入联想、成功结果、空结果、失败重试、有剧集预览和无剧集预览卡片，以及“立即播放”和“查看全部”是否进入播放器。

- [ ] **Step 5: 提交页面状态改造**

```powershell
git add app/src/main/java/com/aiyifan/app/core/ui/SearchResultPresentation.kt app/src/main/java/com/aiyifan/app/feature/search/SearchActivity.kt app/src/main/res/layout/activity_search.xml app/src/test/java/com/aiyifan/app/core/ui/SearchResultPresentationTest.kt
git commit -m "feat(search): 优化搜索结果状态"
```

## Plan Self-Review

- 顶部搜索、初始与联想、结果信息层级、剧集入口、错误处理和验证均有对应任务。
- 不包含用户、动态、相册、下载、播放器协议或搜索接口变更。
- 类型、方法名、资源 ID 与测试引用保持一致。
