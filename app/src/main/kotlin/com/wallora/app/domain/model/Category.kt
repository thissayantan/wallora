package com.wallora.app.domain.model

/**
 * Curated wallpaper categories. Each category maps to source-specific queries/subreddits.
 *
 * [pexelsQuery] — search term for Pexels curated endpoint.
 * [wallhavenCategories] — Wallhaven categories bitmask string (general=001, anime=010, people=100).
 * [wallhavenQuery] — additional query for Wallhaven.
 * [subreddits] — list of SFW wallpaper subreddits for Reddit.
 * [unsplashQuery] — search term for Unsplash.
 */
enum class Category(
    val displayName: String,
    val pexelsQuery: String,
    val wallhavenQuery: String,
    val wallhavenCategories: String = "111",  // general+anime+people
    val subreddits: List<String>,
    val unsplashQuery: String,
) {
    NATURE(
        displayName = "Nature",
        pexelsQuery = "nature landscape",
        wallhavenQuery = "nature",
        wallhavenCategories = "100", // general only
        subreddits = listOf("EarthPorn", "NatureIsFuckingLit"),
        unsplashQuery = "nature",
    ),
    LANDSCAPES(
        displayName = "Landscapes",
        pexelsQuery = "landscape scenery",
        wallhavenQuery = "landscape",
        subreddits = listOf("EarthPorn", "LandscapePhotography"),
        unsplashQuery = "landscape",
    ),
    SPACE(
        displayName = "Space",
        pexelsQuery = "space galaxy nebula",
        wallhavenQuery = "space galaxy",
        subreddits = listOf("spaceporn", "astrophotography"),
        unsplashQuery = "space galaxy",
    ),
    ANIMALS(
        displayName = "Animals",
        pexelsQuery = "animals wildlife",
        wallhavenQuery = "animals wildlife",
        subreddits = listOf("NatureIsFuckingLit", "wildlifephotography"),
        unsplashQuery = "animals wildlife",
    ),
    TECHNOLOGY(
        displayName = "Technology",
        pexelsQuery = "technology abstract digital",
        wallhavenQuery = "technology",
        subreddits = listOf("wallpaper", "wallpapers"),
        unsplashQuery = "technology",
    ),
    ARCHITECTURE(
        displayName = "Architecture",
        pexelsQuery = "architecture building",
        wallhavenQuery = "architecture",
        subreddits = listOf("ArchitecturePorn", "wallpaper"),
        unsplashQuery = "architecture",
    ),
    CITY(
        displayName = "City",
        pexelsQuery = "city urban skyline",
        wallhavenQuery = "cityscape",
        subreddits = listOf("CityPorn", "urbanporn"),
        unsplashQuery = "cityscape",
    ),
    MINIMAL(
        displayName = "Minimal",
        pexelsQuery = "minimal simple clean",
        wallhavenQuery = "minimalist",
        subreddits = listOf("MinimalWallpaper", "Minimalism"),
        unsplashQuery = "minimal",
    ),
    ABSTRACT(
        displayName = "Abstract",
        pexelsQuery = "abstract colorful",
        wallhavenQuery = "abstract",
        subreddits = listOf("wallpaper", "wallpapers"),
        unsplashQuery = "abstract",
    ),
    AMOLED(
        displayName = "Dark/AMOLED",
        pexelsQuery = "dark black amoled",
        wallhavenQuery = "dark black",
        subreddits = listOf("Amoledbackgrounds", "darkwallpaper"),
        unsplashQuery = "dark minimal",
    ),
    ART(
        displayName = "Art",
        pexelsQuery = "digital art illustration",
        wallhavenQuery = "art",
        subreddits = listOf("ImaginaryLandscapes", "SpecArt"),
        unsplashQuery = "art illustration",
    ),
    CARS(
        displayName = "Cars",
        pexelsQuery = "cars automotive",
        wallhavenQuery = "cars automotive",
        subreddits = listOf("carporn", "wallpaper"),
        unsplashQuery = "cars automotive",
    ),
    ANIME(
        displayName = "Anime",
        pexelsQuery = "anime illustration",
        wallhavenQuery = "anime",
        wallhavenCategories = "010",
        subreddits = listOf("Animewallpaper", "Amoledbackgrounds"),
        unsplashQuery = "anime illustration",
    ),
    AI_ART(
        displayName = "AI Art",
        pexelsQuery = "digital art vibrant colorful",
        wallhavenQuery = "digital art illustration",
        subreddits = listOf("AIArt", "midjourney", "StableDiffusion"),
        unsplashQuery = "digital art colorful",
    ),
}
