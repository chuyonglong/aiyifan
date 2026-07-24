package com.aiyifan.app.feature.home

import com.aiyifan.app.R
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeCategoryAppearanceTest {

    @Test
    fun `selected category uses accent background and white text`() {
        assertEquals(
            CategoryChipAppearance(
                backgroundRes = R.drawable.bg_chip_selected,
                textColorRes = R.color.white,
            ),
            HomeCategoryAppearance.forSelection(isSelected = true),
        )
    }

    @Test
    fun `unselected category uses neutral background and primary text`() {
        assertEquals(
            CategoryChipAppearance(
                backgroundRes = R.drawable.bg_chip,
                textColorRes = R.color.text_primary,
            ),
            HomeCategoryAppearance.forSelection(isSelected = false),
        )
    }
}
