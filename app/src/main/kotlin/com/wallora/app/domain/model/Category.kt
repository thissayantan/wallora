package com.wallora.app.domain.model

/**
 * Curated wallpaper categories. Each category maps to source-specific queries/subreddits.
 *
 * [pexelsQuery] — search term for Pexels curated endpoint.
 * [wallhavenQuery] — additional query for Wallhaven.
 * [wallhavenCategories] — Wallhaven categories bitmask string (general=100, anime=010, people=001).
 * [subreddits] — list of SFW wallpaper subreddits for Reddit.
 * [unsplashQuery] — search term for Unsplash.
 * [pixabayQuery] — search term for Pixabay.
 */
enum class Category(
    val displayName: String,
    val pexelsQuery: String,
    val wallhavenQuery: String,
    val wallhavenCategories: String = "111", // general+anime+people
    val subreddits: List<String>,
    val unsplashQuery: String,
    val pixabayQuery: String,
) {
    // ---- Vibrant / high-saturation leads — shown first in the chip row ----
    VIBRANT(
        displayName = "Vibrant",
        pexelsQuery = "vibrant colorful wallpaper",
        wallhavenQuery = "colorful vibrant",
        wallhavenCategories = "100",
        subreddits = listOf("wallpaper", "wallpapers"),
        unsplashQuery = "vibrant colorful",
        pixabayQuery = "vibrant colorful",
    ),
    ABSTRACT(
        displayName = "Abstract",
        pexelsQuery = "abstract colorful",
        wallhavenQuery = "abstract",
        subreddits = listOf("wallpaper", "wallpapers"),
        unsplashQuery = "abstract",
        pixabayQuery = "abstract colorful",
    ),
    NEON(
        displayName = "Neon",
        pexelsQuery = "neon lights glow",
        wallhavenQuery = "neon",
        wallhavenCategories = "100",
        subreddits = listOf("Cyberpunk", "outrun"),
        unsplashQuery = "neon",
        pixabayQuery = "neon",
    ),
    GRADIENT(
        displayName = "Gradient",
        pexelsQuery = "gradient colorful background",
        wallhavenQuery = "gradient",
        wallhavenCategories = "100",
        subreddits = listOf("wallpaper", "wallpapers"),
        unsplashQuery = "gradient",
        pixabayQuery = "gradient",
    ),
    SPACE(
        displayName = "Space",
        pexelsQuery = "space galaxy nebula",
        wallhavenQuery = "space galaxy",
        subreddits = listOf("spaceporn", "astrophotography"),
        unsplashQuery = "space galaxy",
        pixabayQuery = "space galaxy nebula",
    ),
    AI_ART(
        displayName = "AI Art",
        pexelsQuery = "digital art vibrant colorful",
        wallhavenQuery = "digital art illustration",
        subreddits = listOf("AIArt", "midjourney", "StableDiffusion"),
        unsplashQuery = "digital art colorful",
        pixabayQuery = "digital art colorful",
    ),
    // ---- Topical / subject categories ----
    NATURE(
        displayName = "Nature",
        pexelsQuery = "nature landscape",
        wallhavenQuery = "nature",
        wallhavenCategories = "100",
        subreddits = listOf("EarthPorn", "NatureIsFuckingLit"),
        unsplashQuery = "nature",
        pixabayQuery = "nature landscape",
    ),
    LANDSCAPES(
        displayName = "Landscapes",
        pexelsQuery = "landscape scenery",
        wallhavenQuery = "landscape",
        subreddits = listOf("EarthPorn", "LandscapePhotography"),
        unsplashQuery = "landscape",
        pixabayQuery = "landscape scenery",
    ),
    CITY(
        displayName = "City",
        pexelsQuery = "city urban skyline",
        wallhavenQuery = "cityscape",
        subreddits = listOf("CityPorn", "urbanporn"),
        unsplashQuery = "cityscape",
        pixabayQuery = "city skyline",
    ),
    ARCHITECTURE(
        displayName = "Architecture",
        pexelsQuery = "architecture building",
        wallhavenQuery = "architecture",
        subreddits = listOf("ArchitecturePorn", "wallpaper"),
        unsplashQuery = "architecture",
        pixabayQuery = "architecture",
    ),
    ANIMALS(
        displayName = "Animals",
        pexelsQuery = "animals wildlife",
        wallhavenQuery = "animals wildlife",
        subreddits = listOf("NatureIsFuckingLit", "wildlifephotography"),
        unsplashQuery = "animals wildlife",
        pixabayQuery = "animals wildlife",
    ),
    CARS(
        displayName = "Cars",
        pexelsQuery = "cars automotive",
        wallhavenQuery = "cars automotive",
        subreddits = listOf("carporn", "wallpaper"),
        unsplashQuery = "cars automotive",
        pixabayQuery = "cars automotive",
    ),
    ANIME(
        displayName = "Anime",
        pexelsQuery = "anime illustration",
        wallhavenQuery = "anime",
        wallhavenCategories = "010",
        subreddits = listOf("Animewallpaper", "Amoledbackgrounds"),
        unsplashQuery = "anime illustration",
        pixabayQuery = "anime",
    ),
    ART(
        displayName = "Art",
        pexelsQuery = "digital art illustration",
        wallhavenQuery = "art",
        subreddits = listOf("ImaginaryLandscapes", "SpecArt"),
        unsplashQuery = "art illustration",
        pixabayQuery = "art illustration",
    ),
    TECHNOLOGY(
        displayName = "Technology",
        pexelsQuery = "technology abstract digital",
        wallhavenQuery = "technology",
        subreddits = listOf("wallpaper", "wallpapers"),
        unsplashQuery = "technology",
        pixabayQuery = "technology",
    ),
    // ---- Muted / dark — kept but moved to end so they don't dominate the default view ----
    MINIMAL(
        displayName = "Minimal",
        pexelsQuery = "minimal simple clean",
        wallhavenQuery = "minimalist",
        subreddits = listOf("MinimalWallpaper", "Minimalism"),
        unsplashQuery = "minimal",
        pixabayQuery = "minimal",
    ),
    AMOLED(
        displayName = "Dark/AMOLED",
        pexelsQuery = "dark black amoled",
        wallhavenQuery = "dark black",
        subreddits = listOf("Amoledbackgrounds", "darkwallpaper"),
        unsplashQuery = "dark minimal",
        pixabayQuery = "dark amoled",
    ),
}
