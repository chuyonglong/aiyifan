# 搜索复刻文档

## 原 APK 依据

- 页面：`SearchActivity`。
- 布局：`activity_search.xml`、`layout_search_result.xml`、`item_search_result.xml`。
- 本地对象：`SearchKeywordBean`。
- 原搜索结果支持视频、用户、动态、相册；v1 只保留视频。

## V1 目标

实现视频搜索主链路：

- 搜索输入框、返回、搜索按钮。
- 搜索历史展示、展开/收起、清空。
- 热门搜索展示。
- 输入联想列表。
- 视频搜索结果列表。
- 点击结果播放、点击“查看全部”进入详情/选集。

## 页面结构

### 初始态

- 顶部：返回按钮 + `SearchEditView` 风格输入框 + 搜索按钮。
- 内容：历史搜索标题、历史词 FlowLayout、清空历史、热门搜索列表。
- 历史词最多本地保存 20 条，展示默认前 10 条，点击“展开”展示全部。

### 输入态

- 当输入框非空时，请求 `/search/suggest?keyword=...`。
- 联想结果用纵向 RecyclerView 展示。
- 点击联想词：填入输入框并执行搜索。
- 输入清空：回到初始态。

### 结果态

- v1 不展示多 tab，只展示“视频”结果。
- 列表项参考 `item_search_result.xml`：
  - 左侧海报 `140dp x 187dp`。
  - 右侧标题、年份、类型、分类、导演、主演。
  - 底部“立即播放”按钮。
  - 下载按钮 v1 可隐藏。
- 如果接口返回剧集列表，在卡片下方展示前几集快捷入口和“查看全部”。

## 数据字段

搜索建议：

```kotlin
data class SearchSuggestion(
    val keyword: String
)
```

搜索结果：

```kotlin
data class SearchVideoResult(
    val mediaKey: String,
    val title: String,
    val coverUrl: String,
    val mediaType: String?,
    val year: String?,
    val category: String?,
    val director: String?,
    val actor: String?,
    val updateCount: Int?,
    val updateDate: String?,
    val episodes: List<Episode>
)
```

搜索历史：

```kotlin
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val keyword: String,
    val createdAt: Long
)
```

## 交互规则

- 点击键盘搜索或顶部“搜索”按钮都执行搜索。
- 空关键词点击搜索不请求接口，输入框轻微错误提示即可。
- 搜索成功后把关键词写入本地历史；重复关键词更新 `createdAt`。
- 清空历史需要二次确认。
- 搜索请求要防抖，建议输入联想延迟 300ms。
- 搜索结果分页加载，第一页失败显示重试，后续失败显示底部重试。

## V1 不做

- 用户、动态、相册搜索 tab。
- 搜索短视频推荐区。
- 下载入口。
- 高级筛选。

## 验收点

- 输入关键词能展示联想。
- 搜索后进入视频结果页并保存历史。
- 历史词可点击再次搜索。
- 清空历史后本地不再展示。
- 点击“立即播放”能进入视频播放页。
