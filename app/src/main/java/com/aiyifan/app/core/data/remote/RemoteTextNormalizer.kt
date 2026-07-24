package com.aiyifan.app.core.data.remote

internal object RemoteTextNormalizer {
    fun optional(value: String?): String? =
        value?.trim()?.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
}
