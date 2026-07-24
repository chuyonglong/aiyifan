package com.aiyifan.app.core.data.remote

import org.junit.Assert.assertEquals
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
}
