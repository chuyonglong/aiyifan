package com.aiyifan.app.feature.home

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.aiyifan.app.R

data class CategoryChipAppearance(
    @param:DrawableRes val backgroundRes: Int,
    @param:ColorRes val textColorRes: Int,
)

object HomeCategoryAppearance {
    fun forSelection(isSelected: Boolean): CategoryChipAppearance =
        if (isSelected) {
            CategoryChipAppearance(R.drawable.bg_chip_selected, R.color.white)
        } else {
            CategoryChipAppearance(R.drawable.bg_chip, R.color.text_primary)
        }
}
