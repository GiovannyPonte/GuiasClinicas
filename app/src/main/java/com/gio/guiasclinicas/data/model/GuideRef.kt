package com.gio.guiasclinicas.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GuideRef(
    val slug: String,
    val title: String,
    val manifestPath: String
)
