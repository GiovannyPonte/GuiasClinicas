package com.gio.guiasclinicas.data.model

data class RootManifest(
    val guides: List<GuideRef>
)

data class GuideRef(
    val slug: String,
    val title: String,
    val folder: String,
    val manifestPath: String
)

data class GuideItem(
    val slug: String,
    val title: String
)
