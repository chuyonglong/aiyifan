package com.aiyifan.app.feature.video

enum class PlaybackDestination {
    PICTURE_IN_PICTURE,
    OVERLAY,
    NONE,
}

data class PlaybackCapabilities(
    val hasPlayableMedia: Boolean,
    val isPrepared: Boolean,
    val isPlaying: Boolean,
    val supportsPictureInPicture: Boolean,
    val pictureInPictureEnabled: Boolean,
    val hasOverlayPermission: Boolean,
)

object PlaybackPresentationPolicy {
    fun destinationWhenLeaving(value: PlaybackCapabilities): PlaybackDestination = when {
        !value.hasPlayableMedia || !value.isPrepared || !value.isPlaying -> PlaybackDestination.NONE
        value.supportsPictureInPicture && value.pictureInPictureEnabled -> PlaybackDestination.PICTURE_IN_PICTURE
        value.hasOverlayPermission -> PlaybackDestination.OVERLAY
        else -> PlaybackDestination.NONE
    }
}
