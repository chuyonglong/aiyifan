# Video picture-in-picture and floating-window design

## Goal

Extend `VideoPlayerActivity` so that ongoing playback can continue in either Android system picture-in-picture (PiP) or an app-managed floating window. Playback position, selected episode, and play/pause state must survive transitions between the player page, PiP, and the overlay.

## Scope

- Add an explicit full-screen control that switches the player activity between portrait and landscape. Back exits full-screen before leaving the player page.
- Prefer Android system PiP when the user leaves an actively playing video and the device supports PiP.
- Supply play and pause actions in PiP through a MediaSession.
- Support a custom floating window as a fallback when PiP is unavailable or disabled, and when the user deliberately chooses it.
- Request `SYSTEM_ALERT_WINDOW` only when the floating-window path is first used.
- Let the floating window be dragged and resized, with play/pause and close controls.
- Persist history and release the player when playback is closed from any presentation.

## Architecture

### Playback owner

Introduce one playback coordinator as the single owner of the Media3 `ExoPlayer`, selected episode, playback position, and play state. The coordinator exposes attach/detach operations for three presentation surfaces:

1. `VideoPlayerActivity` owns the normal and full-screen page UI.
2. Android PiP reuses the activity and its attached `PlayerView`.
3. A foreground media service owns the app overlay and its attached `PlayerView` while the activity is no longer visible.

The player must never be attached to more than one `PlayerView` at a time. Any surface transition detaches first, attaches the next surface, and then restores the captured playback state.

### System picture-in-picture

The activity declares PiP support in the manifest. When a playable video is currently playing and the activity is about to leave the foreground, it enters PiP with a 16:9 aspect ratio. The MediaSession publishes play/pause actions, so the system PiP controls can pause and resume playback. Android controls PiP resizing; the app does not implement its own resize gesture in this mode.

Entering PiP hides page-only controls and content. Returning from PiP restores the normal activity surface without restarting the stream or losing position. If PiP is unsupported, unavailable, or fails to enter, the app proceeds to the overlay fallback only when overlay permission has already been granted; otherwise it remains on the normal close/background path.

### Floating window

The first explicit request to use the floating window checks `Settings.canDrawOverlays`. If absent, the app opens Android's per-app overlay-permission screen and explains that the feature cannot start until permission is granted. A denial never blocks normal playback or system PiP.

After authorization, a foreground service with media-playback notification owns a `WindowManager` overlay. The window contains a Media3 `PlayerView`, play/pause control, and close control. Drag gestures move the window; a dedicated resize handle changes its width and height within screen-safe minimum and maximum bounds while preserving a 16:9 content ratio. The service keeps playback active after the activity is hidden.

Close stops playback, saves history, removes the window, stops the foreground service, and releases the player. If the permission is revoked or the service fails, the app removes the overlay safely and stops playback with a user-visible message.

## Interaction rules

- Full-screen toggles the activity orientation. Back in landscape returns to portrait; otherwise Back uses normal navigation.
- Leaving an actively playing player page prefers system PiP.
- PiP supports play/pause and system-provided resizing.
- The overlay is selected intentionally by the user or used only after PiP is unavailable/disabled and permission is granted.
- The overlay supports drag, resize, play/pause, and close.
- Paused, unprepared, errored, or streamless media never enters PiP automatically.
- Current episode, progress, and play state survive all valid surface transitions.
- The app records progress when a player surface stops and performs final save/release on explicit close or terminal failure.

## Error handling

- A missing media URL shows the existing playback error and never exposes PiP or overlay controls.
- Failed PiP entry leaves the page usable; it must not release or restart the player.
- Denied overlay permission explains the limitation and keeps PiP and normal playback available.
- Revoked overlay permission, failed service startup, or failed window attachment removes any partial UI, saves progress, and stops playback cleanly.
- The player must be released exactly once after an explicit overlay close, page close without PiP, or terminal failure.

## Tests and acceptance criteria

- Unit tests cover presentation-state decisions: PiP eligibility, overlay permission gating, fallback selection, and full-screen back behavior.
- Instrumented tests validate activity PiP configuration and overlay service intent construction where platform APIs permit.
- Manual device validation verifies PiP play/pause and system resizing, overlay authorization, dragging, ratio-preserving resizing, closing, full-screen transitions, and uninterrupted progress/episode continuity across each transition.
- Existing player loading, episode switching, history, favorite, sharing, related content, and comments continue to work.

## Non-goals

- Custom resizing of Android's system PiP window.
- A background player without an active PiP window or authorized foreground overlay.
- Casting, ads, subtitles, quality selection, or other player features unrelated to PiP and floating windows.
