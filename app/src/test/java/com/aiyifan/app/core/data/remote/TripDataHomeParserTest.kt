package com.aiyifan.app.core.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripDataHomeParserTest {

    @Test
    fun `parser maps home cover image url`() {
        val sections = TripDataHomeParser.parse(
            """
                {
                  "data": {
                    "list": [
                      {
                        "name": "Recommended",
                        "list": [
                          {
                            "mediaKey": "media-key",
                            "title": "Feature film",
                            "coverImgUrl": "https://images.example.com/poster.jpg",
                            "videoType": 1
                          }
                        ]
                      }
                    ]
                  }
                }
            """.trimIndent(),
        )

        assertEquals("https://images.example.com/poster.jpg", sections.single().videos.single().coverUrl)
    }

    @Test
    fun `parser treats null text metadata as missing`() {
        val sections = TripDataHomeParser.parse(
            """
                {
                  "data": {
                    "list": [
                      {
                        "name": "推荐",
                        "list": [
                          {
                            "mediaKey": "home-null-metadata",
                            "title": "首页字段清理",
                            "coverImgUrl": "null",
                            "videoType": 1,
                            "contentType": " NULL ",
                            "regional": "null",
                            "actor": "null",
                            "director": "null",
                            "updateStatus": "null"
                          }
                        ]
                      }
                    ]
                  }
                }
            """.trimIndent(),
        )

        val video = sections.single().videos.single()

        assertEquals("", video.coverUrl)
        assertNull(video.contentType)
        assertNull(video.area)
        assertNull(video.actor)
        assertNull(video.director)
        assertNull(video.updateStatus)
    }
}
