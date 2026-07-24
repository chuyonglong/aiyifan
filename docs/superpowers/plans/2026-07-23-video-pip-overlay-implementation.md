# 视频画中画与悬浮窗实施计划

> **供代理执行：**必须按任务逐项执行；使用 subagent-driven-development（推荐）或 executing-plans。所有步骤均使用复选框记录进度。

**目标：**为视频播放器提供横竖屏全屏切换、系统画中画的播放/暂停与缩放，以及可拖动、可缩放的应用内悬浮窗。

**架构：**纯 Kotlin 展示策略决定容器转换；唯一的 VideoPlaybackController 持有 ExoPlayer 与 MediaSession。Activity 承载普通页面、全屏、系统 PiP 和无权限时的应用内迷你播放器；前台服务通过 WindowManager 承载已授权的跨应用悬浮窗。

**技术栈：**Kotlin、AndroidX、Media3 ExoPlayer/Session、Android PiP、前台媒体服务、WindowManager、JUnit 4。

---

## 文件结构

- 修改 app/build.gradle.kts：增加 Media3 Session 与仪器测试依赖。
- 修改 app/src/main/AndroidManifest.xml：声明 PiP、悬浮窗、前台媒体服务权限与服务。
- 新建 app/src/main/java/com/aiyifan/app/feature/video/PlaybackPresentationPolicy.kt：纯状态决策。
- 新建 app/src/main/java/com/aiyifan/app/feature/video/PlayerAttachmentCoordinator.kt：播放器单容器挂载规则。
- 新建 app/src/main/java/com/aiyifan/app/feature/video/VideoPlaybackController.kt：唯一播放器、媒体会话、播放历史。
- 新建 app/src/main/java/com/aiyifan/app/feature/video/FloatingPlayerService.kt：悬浮窗服务、拖动和缩放。
- 新建 app/src/main/res/layout/view_floating_player.xml：悬浮窗界面。
- 修改 app/src/main/res/layout/activity_video_player.xml 与 VideoPlayerActivity.kt：入口和生命周期。
- 新建 app/src/test/java/com/aiyifan/app/feature/video 下的策略、挂载、尺寸与返回行为测试。
- 新建 app/src/androidTest/java/com/aiyifan/app/feature/video/VideoPlayerManifestTest.kt：安装包声明测试。

### 任务 1：建立依赖、权限和清单测试

**文件：**

- 修改：app/build.gradle.kts
- 修改：app/src/main/AndroidManifest.xml
- 新建：app/src/androidTest/java/com/aiyifan/app/feature/video/VideoPlayerManifestTest.kt
- 新建：app/src/main/java/com/aiyifan/app/feature/video/FloatingPlayerService.kt

- [ ] **步骤 1：先编写会失败的清单测试。**

~~~kotlin
@RunWith(AndroidJUnit4::class)
class VideoPlayerManifestTest {
    @Test
    fun videoPlayerDeclaresPictureInPictureSupport() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val info = context.packageManager.getActivityInfo(
            ComponentName(context, VideoPlayerActivity::class.java),
            PackageManager.ComponentInfoFlags.of(0),
        )
        assertTrue(info.flags and ActivityInfo.FLAG_SUPPORTS_PICTURE_IN_PICTURE != 0)
    }

    @Test
    fun floatingPlayerServiceDeclaresMediaPlaybackForegroundType() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val info = context.packageManager.getServiceInfo(
            ComponentName(context, FloatingPlayerService::class.java),
            PackageManager.ComponentInfoFlags.of(0),
        )
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK, info.foregroundServiceType)
    }
}
~~~

- [ ] **步骤 2：运行测试并确认失败。**

运行：./gradlew.bat connectedDebugAndroidTest --tests com.aiyifan.app.feature.video.VideoPlayerManifestTest

预期：因 FloatingPlayerService 或画中画声明不存在而编译失败或测试失败。

- [ ] **步骤 3：加入最小配置使测试通过。**

在 app/build.gradle.kts 增加：

~~~kotlin
implementation("androidx.media3:media3-session:1.4.1")

androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.test:runner:1.6.2")
androidTestImplementation("androidx.test:core-ktx:1.6.1")
~~~

在 AndroidManifest.xml 的 application 前加入：

~~~xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
~~~

将播放器 Activity 调整为：

~~~xml
<activity
    android:name=".feature.video.VideoPlayerActivity"
    android:configChanges="keyboardHidden|orientation|screenSize|smallestScreenSize"
    android:exported="false"
    android:supportsPictureInPicture="true" />
~~~

创建最小服务并在 application 内声明：

~~~kotlin
class FloatingPlayerService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
~~~

~~~xml
<service
    android:name=".feature.video.FloatingPlayerService"
    android:exported="false"
    android:foregroundServiceType="mediaPlayback" />
~~~

- [ ] **步骤 4：重新运行清单测试。**

运行：./gradlew.bat connectedDebugAndroidTest --tests com.aiyifan.app.feature.video.VideoPlayerManifestTest

预期：有连接设备时两项通过；无设备时记录 No connected devices，继续完成本地单元测试和构建。

- [ ] **步骤 5：提交基础配置。**

~~~powershell
git add app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/java/com/aiyifan/app/feature/video/FloatingPlayerService.kt app/src/androidTest/java/com/aiyifan/app/feature/video/VideoPlayerManifestTest.kt
git commit -m "feat: configure video PiP and floating service"
~~~

### 任务 2：以测试实现展示状态策略与挂载协调器

**文件：**

- 新建：app/src/main/java/com/aiyifan/app/feature/video/PlaybackPresentationPolicy.kt
- 新建：app/src/main/java/com/aiyifan/app/feature/video/PlayerAttachmentCoordinator.kt
- 新建：app/src/test/java/com/aiyifan/app/feature/video/PlaybackPresentationPolicyTest.kt
- 新建：app/src/test/java/com/aiyifan/app/feature/video/PlayerAttachmentCoordinatorTest.kt

- [ ] **步骤 1：编写失败的策略和挂载顺序测试。**

~~~kotlin
@Test
fun activeVideoPrefersPiP() {
    assertEquals(
        PlaybackDestination.PICTURE_IN_PICTURE,
        PlaybackPresentationPolicy.destinationWhenLeaving(
            PlaybackCapabilities(true, true, true, true, true, true),
        ),
    )
}

@Test
fun overlayIsFallbackWhenPiPIsUnavailable() {
    assertEquals(
        PlaybackDestination.OVERLAY,
        PlaybackPresentationPolicy.destinationWhenLeaving(
            PlaybackCapabilities(true, true, true, false, false, true),
        ),
    )
}

@Test
fun pausedVideoDoesNotEnterPiP() {
    assertEquals(
        PlaybackDestination.NONE,
        PlaybackPresentationPolicy.destinationWhenLeaving(
            PlaybackCapabilities(true, true, false, true, true, true),
        ),
    )
}
~~~

~~~kotlin
@Test
fun switchingHostDetachesOldHostFirst() {
    val events = mutableListOf<String>()
    val page = RecordingHost("page", events)
    val overlay = RecordingHost("overlay", events)
    val coordinator = PlayerAttachmentCoordinator<String>()

    coordinator.attach("player", page)
    coordinator.attach("player", overlay)

    assertEquals(listOf("page:attach", "page:detach", "overlay:attach"), events)
}
~~~

RecordingHost 实现 PlayerHost<String>，其 attach 与 detach 分别将名称加到 events。

- [ ] **步骤 2：运行测试并确认类型未定义。**

运行：./gradlew.bat testDebugUnitTest --tests com.aiyifan.app.feature.video.PlaybackPresentationPolicyTest --tests com.aiyifan.app.feature.video.PlayerAttachmentCoordinatorTest

预期：编译失败，提示策略和挂载协调器类型未解析。

- [ ] **步骤 3：实现最小策略和协调器。**

~~~kotlin
enum class PlaybackDestination { PICTURE_IN_PICTURE, OVERLAY, NONE }

data class PlaybackCapabilities(
    val hasPlayableMedia: Boolean,
    val isPrepared: Boolean,
    val isPlaying: Boolean,
    val supportsPictureInPicture: Boolean,
    val pictureInPictureEnabled: Boolean,
    val hasOverlayPermission: Boolean,
)

object PlaybackPresentationPolicy {
    fun destinationWhenLeaving(value: PlaybackCapabilities): PlaybackDestination =
        when {
            !value.hasPlayableMedia || !value.isPrepared || !value.isPlaying -> PlaybackDestination.NONE
            value.supportsPictureInPicture && value.pictureInPictureEnabled -> PlaybackDestination.PICTURE_IN_PICTURE
            value.hasOverlayPermission -> PlaybackDestination.OVERLAY
            else -> PlaybackDestination.NONE
        }
}
~~~

~~~kotlin
interface PlayerHost<T> {
    fun attach(player: T)
    fun detach()
}

class PlayerAttachmentCoordinator<T> {
    private var attachedHost: PlayerHost<T>? = null

    fun attach(player: T, host: PlayerHost<T>) {
        if (attachedHost === host) return
        attachedHost?.detach()
        host.attach(player)
        attachedHost = host
    }

    fun detach() {
        attachedHost?.detach()
        attachedHost = null
    }
}
~~~

- [ ] **步骤 4：运行测试确认通过。**

运行：./gradlew.bat testDebugUnitTest --tests com.aiyifan.app.feature.video.PlaybackPresentationPolicyTest --tests com.aiyifan.app.feature.video.PlayerAttachmentCoordinatorTest

预期：4 项测试通过。

- [ ] **步骤 5：提交策略与协调器。**

~~~powershell
git add app/src/main/java/com/aiyifan/app/feature/video/PlaybackPresentationPolicy.kt app/src/main/java/com/aiyifan/app/feature/video/PlayerAttachmentCoordinator.kt app/src/test/java/com/aiyifan/app/feature/video/PlaybackPresentationPolicyTest.kt app/src/test/java/com/aiyifan/app/feature/video/PlayerAttachmentCoordinatorTest.kt
git commit -m "feat: add video presentation policy"
~~~

### 任务 3：实现共享播放器、媒体会话和播放历史

**文件：**

- 新建：app/src/main/java/com/aiyifan/app/feature/video/VideoPlaybackController.kt
- 修改：app/src/main/java/com/aiyifan/app/core/data/AppGraph.kt
- 新建：app/src/test/java/com/aiyifan/app/feature/video/VideoPlaybackControllerTest.kt

- [ ] **步骤 1：先写会失败的播放器释放幂等测试。**

~~~kotlin
@Test
fun releaseOnlyReachesEngineOnce() {
    val engine = FakePlaybackEngine()
    val controller = VideoPlaybackController(engine, FakeCatalogRepository())

    controller.release()
    controller.release()

    assertEquals(1, engine.releaseCalls)
}
~~~

FakePlaybackEngine 实现生产接口 PlaybackEngine 并记录 release 调用次数；不向生产代码加入仅用于测试的方法。

- [ ] **步骤 2：运行测试并确认控制器类型缺失。**

运行：./gradlew.bat testDebugUnitTest --tests com.aiyifan.app.feature.video.VideoPlaybackControllerTest

预期：编译失败，提示 VideoPlaybackController 与 PlaybackEngine 未解析。

- [ ] **步骤 3：实现最小控制器。**

~~~kotlin
interface PlaybackEngine {
    val isPlaying: Boolean
    val currentPosition: Long
    val duration: Long
    fun setMediaItem(item: MediaItem)
    fun prepare()
    fun play()
    fun pause()
    fun release()
}
~~~

控制器以 ExoPlayer 的生产适配器实现 PlaybackEngine，并创建 MediaSession.Builder(applicationContext, exoPlayer).build()。它提供 prepare(detail, episode)、attach(playerView)、detach()、togglePlayPause()、saveHistory() 和幂等 release()。prepare 拒绝空地址；saveHistory 始终使用：

~~~kotlin
repository.saveHistory(
    detail = activeDetail,
    episode = activeEpisode,
    progressMs = engine.currentPosition.coerceAtLeast(0L),
    durationMs = engine.duration.coerceAtLeast(0L),
)
~~~

AppGraph 增加惰性单例控制器，使用应用级 Context 创建，使 Activity 和 Service 共享同一个播放器。旋转和 PiP 回调不能释放播放器；显式关闭才调用 release。

- [ ] **步骤 4：运行完整单元测试。**

运行：./gradlew.bat testDebugUnitTest

预期：全部单元测试通过。

- [ ] **步骤 5：提交共享播放器。**

~~~powershell
git add app/src/main/java/com/aiyifan/app/feature/video/VideoPlaybackController.kt app/src/main/java/com/aiyifan/app/core/data/AppGraph.kt app/src/test/java/com/aiyifan/app/feature/video/VideoPlaybackControllerTest.kt
git commit -m "feat: share video player across presentation surfaces"
~~~

### 任务 4：实现前台悬浮播放器

**文件：**

- 修改：app/src/main/java/com/aiyifan/app/feature/video/FloatingPlayerService.kt
- 新建：app/src/main/java/com/aiyifan/app/feature/video/FloatingWindowSizePolicy.kt
- 新建：app/src/test/java/com/aiyifan/app/feature/video/FloatingWindowSizePolicyTest.kt
- 新建：app/src/main/res/layout/view_floating_player.xml
- 新建：app/src/main/res/drawable/bg_floating_player.xml
- 新建：app/src/main/res/drawable/ic_close.xml

- [ ] **步骤 1：编写失败的比例缩放测试。**

~~~kotlin
@Test
fun resizePreservesSixteenByNineRatio() {
    assertEquals(
        FloatingWindowSize(width = 480, height = 270),
        FloatingWindowSizePolicy.resize(requestedWidth = 500, minWidth = 240, maxWidth = 480),
    )
}
~~~

- [ ] **步骤 2：运行测试并确认尺寸策略不存在。**

运行：./gradlew.bat testDebugUnitTest --tests com.aiyifan.app.feature.video.FloatingWindowSizePolicyTest

预期：编译失败，提示 FloatingWindowSize 与 FloatingWindowSizePolicy 未解析。

- [ ] **步骤 3：实现尺寸策略、布局与服务。**

~~~kotlin
data class FloatingWindowSize(val width: Int, val height: Int)

object FloatingWindowSizePolicy {
    fun resize(requestedWidth: Int, minWidth: Int, maxWidth: Int): FloatingWindowSize {
        val width = requestedWidth.coerceIn(minWidth, maxWidth)
        return FloatingWindowSize(width, (width * 9f / 16f).roundToInt())
    }
}
~~~

悬浮布局包含 16:9 的 PlayerView、右上角关闭按钮、底部播放/暂停按钮、右下角 24dp 缩放手柄。服务启动顺序固定为：

~~~kotlin
startForeground(NOTIFICATION_ID, buildMediaNotification())
val root = LayoutInflater.from(this).inflate(R.layout.view_floating_player, null)
controller.attach(root.findViewById(R.id.floatingPlayerView))
windowManager.addView(root, layoutParams)
~~~

拖动时记录 ACTION_DOWN 的触点和窗口 x/y；ACTION_MOVE 更新 WindowManager.LayoutParams 并调用 updateViewLayout。缩放手柄只计算宽度并调用尺寸策略。播放/暂停调用 controller.togglePlayPause()；关闭依次执行 saveHistory、release、安全移除视图、stopForeground(STOP_FOREGROUND_REMOVE)、stopSelf。移除前检查视图已经添加，并捕获 IllegalArgumentException 后继续清理服务。

- [ ] **步骤 4：运行尺寸与完整单元测试。**

运行：./gradlew.bat testDebugUnitTest

预期：新增尺寸测试和全部既有测试通过。

- [ ] **步骤 5：提交悬浮播放器。**

~~~powershell
git add app/src/main/java/com/aiyifan/app/feature/video/FloatingPlayerService.kt app/src/main/java/com/aiyifan/app/feature/video/FloatingWindowSizePolicy.kt app/src/main/res/layout/view_floating_player.xml app/src/main/res/drawable/bg_floating_player.xml app/src/main/res/drawable/ic_close.xml app/src/test/java/com/aiyifan/app/feature/video/FloatingWindowSizePolicyTest.kt
git commit -m "feat: add resizable floating video player"
~~~

### 任务 5：整合播放页全屏、系统画中画和悬浮窗权限

**文件：**

- 修改：app/src/main/java/com/aiyifan/app/feature/video/VideoPlayerActivity.kt
- 修改：app/src/main/res/layout/activity_video_player.xml
- 新建：app/src/main/res/drawable/ic_fullscreen.xml
- 新建：app/src/main/res/drawable/ic_fullscreen_exit.xml
- 新建：app/src/main/res/drawable/ic_picture_in_picture.xml
- 新建：app/src/test/java/com/aiyifan/app/feature/video/VideoPlayerBackBehaviorTest.kt

- [ ] **步骤 1：先写会失败的全屏返回测试。**

~~~kotlin
@Test
fun fullScreenBackExitsFullScreen() {
    assertEquals(
        VideoPlayerBackAction.EXIT_FULL_SCREEN,
        VideoPlayerBackBehavior.action(isFullScreen = true),
    )
}
~~~

- [ ] **步骤 2：运行测试并确认返回行为类型缺失。**

运行：./gradlew.bat testDebugUnitTest --tests com.aiyifan.app.feature.video.VideoPlayerBackBehaviorTest

预期：编译失败，提示 VideoPlayerBackBehavior 与 VideoPlayerBackAction 未解析。

- [ ] **步骤 3：实现返回行为与 Activity 集成。**

在 PlaybackPresentationPolicy.kt 加入：

~~~kotlin
enum class VideoPlayerBackAction { EXIT_FULL_SCREEN, NAVIGATE_UP }

object VideoPlayerBackBehavior {
    fun action(isFullScreen: Boolean): VideoPlayerBackAction =
        if (isFullScreen) VideoPlayerBackAction.EXIT_FULL_SCREEN else VideoPlayerBackAction.NAVIGATE_UP
}
~~~

在 playerContainer 内新增 48dp 的 fullScreenButton 和 floatingWindowButton；图标使用全屏和画中画矢量资源，contentDescription 分别为“全屏播放”“开启悬浮窗”。保留 playerView id。

~~~kotlin
private fun setFullScreen(enabled: Boolean) {
    requestedOrientation = if (enabled) {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    } else {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    binding.fullScreenButton.setImageResource(
        if (enabled) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen,
    )
}
~~~

全屏时返回键只调用 setFullScreen(false)；否则保存进度，若控制器未被 PiP 或悬浮服务持有则 release，再正常导航。onUserLeaveHint 使用展示策略：PiP 路径调用 enterPictureInPictureMode，并传入 PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()；悬浮路径使用 ContextCompat.startForegroundService 启动服务后 moveTaskToBack(true)。悬浮按钮先检查 Settings.canDrawOverlays(this)：未授权打开 Settings.ACTION_MANAGE_OVERLAY_PERMISSION，已授权才启动服务。onPictureInPictureModeChanged 只隐藏/恢复详情内容，绝不重建、暂停或释放共享播放器；onStop 只保存进度。

- [ ] **步骤 4：运行所有单元测试。**

运行：./gradlew.bat testDebugUnitTest

预期：返回行为和全部既有测试通过。

- [ ] **步骤 5：提交播放页整合。**

~~~powershell
git add app/src/main/java/com/aiyifan/app/feature/video/PlaybackPresentationPolicy.kt app/src/main/java/com/aiyifan/app/feature/video/VideoPlayerActivity.kt app/src/main/res/layout/activity_video_player.xml app/src/main/res/drawable/ic_fullscreen.xml app/src/main/res/drawable/ic_fullscreen_exit.xml app/src/main/res/drawable/ic_picture_in_picture.xml app/src/test/java/com/aiyifan/app/feature/video/VideoPlayerBackBehaviorTest.kt
git commit -m "feat: integrate PiP fullscreen and floating video controls"
~~~

### 任务 6：验证功能并记录验收

**文件：**

- 修改：docs/app-replica-v1/05-player.md

- [ ] **步骤 1：运行全部本地自动化测试。**

运行：./gradlew.bat testDebugUnitTest

预期：BUILD SUCCESSFUL，全部单元测试通过。

- [ ] **步骤 2：构建调试包。**

运行：./gradlew.bat assembleDebug

预期：BUILD SUCCESSFUL，生成 app/build/outputs/apk/debug/app-debug.apk。

- [ ] **步骤 3：在真机逐项验收。**

验证正在播放时按 Home 进入系统 PiP；系统 PiP 可播放、暂停和缩放；横屏全屏按返回键恢复竖屏；首次开启悬浮窗打开系统授权页；授权后悬浮窗可拖动、保持 16:9 比例缩放、播放、暂停和关闭；页面、PiP、悬浮窗间切换时选集与进度不变；关闭后历史页显示最新进度。

- [ ] **步骤 4：记录验收。**

在 docs/app-replica-v1/05-player.md 末尾追加“画中画与悬浮窗验收”，逐条记录结果、测试设备、Android 版本和构建版本。

- [ ] **步骤 5：提交验收记录。**

~~~powershell
git add docs/app-replica-v1/05-player.md
git commit -m "docs: record video PiP verification"
~~~
