package com.gio.guiasclinicas.data.repo

data class RootManifest(
    val guides: List<GuideRef>
)

data class GuideRef(
    val slug: String,
    val title: String
)

data class GuideItem(
    val slug: String,
    val title: String
)