package com.aiyifan.app.feature.video

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackPresentationPolicyTest {

    @Test
    fun `active video prefers picture in picture`() {
        assertEquals(
            PlaybackDestination.PICTURE_IN_PICTURE,
            PlaybackPresentationPolicy.destinationWhenLeaving(
                PlaybackCapabilities(
                    hasPlayableMedia = true,
                    isPrepared = true,
                    isPlaying = true,
                    supportsPictureInPicture = true,
                    pictureInPictureEnabled = true,
                    hasOverlayPermission = true,
                ),
            ),
        )
    }

    @Test
    fun `overlay is fallback when picture in picture is unavailable`() {
        assertEquals(
            PlaybackDestination.OVERLAY,
            PlaybackPresentationPolicy.destinationWhenLeaving(
                PlaybackCapabilities(
                    hasPlayableMedia = true,
                    isPrepared = true,
                    isPlaying = true,
                    supportsPictureInPicture = false,
                    pictureInPictureEnabled = false,
                    hasOverlayPermission = true,
                ),
            ),
        )
    }

    @Test
    fun `overlay is fallback when picture in picture is disabled`() {
        assertEquals(
            PlaybackDestination.OVERLAY,
            PlaybackPresentationPolicy.destinationWhenLeaving(
                PlaybackCapabilities(
                    hasPlayableMedia = true,
                    isPrepared = true,
                    isPlaying = true,
                    supportsPictureInPicture = true,
                    pictureInPictureEnabled = false,
                    hasOverlayPermission = true,
                ),
            ),
        )
    }

    @Test
    fun `paused video does not enter picture in picture`() {
        assertEquals(
            PlaybackDestination.NONE,
            PlaybackPresentationPolicy.destinationWhenLeaving(
                PlaybackCapabilities(
                    hasPlayableMedia = true,
                    isPrepared = true,
                    isPlaying = false,
                    supportsPictureInPicture = true,
                    pictureInPictureEnabled = true,
                    hasOverlayPermission = true,
                ),
            ),
        )
    }

    @Test
    fun `in app mini player is used when PiP and overlay are unavailable`() {
        assertEquals(
            PlaybackDestination.IN_APP_MINI_PLAYER,
            PlaybackPresentationPolicy.destinationWhenLeaving(
                PlaybackCapabilities(
                    hasPlayableMedia = true,
                    isPrepared = true,
                    isPlaying = true,
                    supportsPictureInPicture = false,
                    pictureInPictureEnabled = false,
                    hasOverlayPermission = false,
                ),
            ),
        )
    }
}
