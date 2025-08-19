package com.gio.guiasclinicas.data.model

data class RootManifest(
    val guides: List<GuideRef>
)

data class GuideRef(
    val slug: String,
    val title: String
)

// Asegúrate de tener este:
data class GuideItem(
    val slug: String,
    val title: String
)

