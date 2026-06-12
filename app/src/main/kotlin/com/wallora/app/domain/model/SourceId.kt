package com.wallora.app.domain.model

/** Identifier for a wallpaper source. */
enum class SourceId(val displayName: String) {
    PEXELS("Pexels"),
    WALLHAVEN("Wallhaven"),
    REDDIT("Reddit"),
    UNSPLASH("Unsplash"),
}
