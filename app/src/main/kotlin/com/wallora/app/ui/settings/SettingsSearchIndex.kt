package com.wallora.app.ui.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dataset
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoFilter
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.wallora.app.R

/** Identifies where a search result should send the user. */
sealed class SettingsTarget {
    /** Navigate to a nested sub-page. */
    data class Page(val route: String) : SettingsTarget()
    /** Open a popup dialog on the master list. */
    data class Popup(val id: SettingsPopupId) : SettingsTarget()
}

enum class SettingsPopupId { THEME, CLEAR_CACHE }

data class SettingsSearchEntry(
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val keywords: List<String>,
    val target: SettingsTarget,
)

val SETTINGS_SEARCH_INDEX: List<SettingsSearchEntry> = listOf(
    // Sources & API keys page
    SettingsSearchEntry(
        titleRes = R.string.settings_sources_title,
        icon = Icons.Outlined.Dataset,
        keywords = listOf(
            "pexels", "unsplash", "wallhaven", "reddit", "source", "provider",
            "enable", "disable", "toggle",
        ),
        target = SettingsTarget.Page(WalloraSettingsRoute.SOURCES),
    ),
    SettingsSearchEntry(
        titleRes = R.string.settings_api_pexels_label,
        icon = Icons.Outlined.Key,
        keywords = listOf("pexels", "api key", "key", "access key", "token"),
        target = SettingsTarget.Page(WalloraSettingsRoute.SOURCES),
    ),
    SettingsSearchEntry(
        titleRes = R.string.settings_api_unsplash_label,
        icon = Icons.Outlined.Key,
        keywords = listOf("unsplash", "api key", "key", "access key", "token"),
        target = SettingsTarget.Page(WalloraSettingsRoute.SOURCES),
    ),
    SettingsSearchEntry(
        titleRes = R.string.settings_api_wallhaven_label,
        icon = Icons.Outlined.Key,
        keywords = listOf("wallhaven", "api key", "key", "access key", "token"),
        target = SettingsTarget.Page(WalloraSettingsRoute.SOURCES),
    ),

    // Categories page
    SettingsSearchEntry(
        titleRes = R.string.settings_categories_title,
        icon = Icons.Outlined.Label,
        keywords = listOf(
            "category", "categories", "nature", "space", "city", "anime", "abstract",
            "minimal", "amoled", "dark", "cars", "art", "animals", "landscapes",
            "keyword", "topic", "subreddit", "reddit",
        ),
        target = SettingsTarget.Page(WalloraSettingsRoute.CATEGORIES),
    ),
    SettingsSearchEntry(
        titleRes = R.string.settings_reddit_section,
        icon = Icons.Outlined.Label,
        keywords = listOf(
            "reddit", "subreddit", "r/", "earthporn", "wallpapers", "spaceporn",
            "amoledbackgrounds", "cityporn", "midjourney", "aiart",
        ),
        target = SettingsTarget.Page(WalloraSettingsRoute.CATEGORIES),
    ),
    SettingsSearchEntry(
        titleRes = R.string.settings_custom_keyword_add,
        icon = Icons.Outlined.Label,
        keywords = listOf("keyword", "custom", "topic", "search term", "tag"),
        target = SettingsTarget.Page(WalloraSettingsRoute.CATEGORIES),
    ),

    // Rotation page
    SettingsSearchEntry(
        titleRes = R.string.settings_rotation_title,
        icon = Icons.Outlined.Loop,
        keywords = listOf(
            "rotate", "rotation", "auto", "change", "schedule", "interval",
            "automatic", "timer", "period",
        ),
        target = SettingsTarget.Page(WalloraSettingsRoute.ROTATION),
    ),
    SettingsSearchEntry(
        titleRes = R.string.settings_rotation_interval,
        icon = Icons.Outlined.Schedule,
        keywords = listOf("interval", "15 minutes", "1 hour", "period", "frequency", "every"),
        target = SettingsTarget.Page(WalloraSettingsRoute.ROTATION),
    ),
    SettingsSearchEntry(
        titleRes = R.string.settings_rotation_times,
        icon = Icons.Outlined.Schedule,
        keywords = listOf("time", "times", "8am", "morning", "specific time", "alarm", "clock"),
        target = SettingsTarget.Page(WalloraSettingsRoute.ROTATION),
    ),
    SettingsSearchEntry(
        titleRes = R.string.settings_wifi_only,
        icon = Icons.Outlined.Wifi,
        keywords = listOf("wifi", "wi-fi", "wireless", "data", "mobile data", "network"),
        target = SettingsTarget.Page(WalloraSettingsRoute.ROTATION),
    ),
    SettingsSearchEntry(
        titleRes = R.string.settings_charging_only,
        icon = Icons.Outlined.AutoAwesome,
        keywords = listOf("charging", "charger", "battery", "power"),
        target = SettingsTarget.Page(WalloraSettingsRoute.ROTATION),
    ),
    SettingsSearchEntry(
        titleRes = R.string.settings_rotation_on_unlock,
        icon = Icons.Outlined.Loop,
        keywords = listOf("unlock", "screen on", "lock screen", "wake"),
        target = SettingsTarget.Page(WalloraSettingsRoute.ROTATION),
    ),

    // Live wallpaper page
    SettingsSearchEntry(
        titleRes = R.string.settings_live_title,
        icon = Icons.Outlined.Wallpaper,
        keywords = listOf("live wallpaper", "parallax", "scroll", "effect", "motion", "animate"),
        target = SettingsTarget.Page(WalloraSettingsRoute.LIVE),
    ),
    SettingsSearchEntry(
        titleRes = R.string.settings_parallax,
        icon = Icons.Outlined.Layers,
        keywords = listOf("parallax", "scroll", "depth", "move", "swipe", "launcher"),
        target = SettingsTarget.Page(WalloraSettingsRoute.LIVE),
    ),
    SettingsSearchEntry(
        titleRes = R.string.settings_filters_section,
        icon = Icons.Outlined.PhotoFilter,
        keywords = listOf(
            "filter", "blur", "brightness", "contrast", "saturation", "adjust",
            "default look", "appearance", "edit",
        ),
        target = SettingsTarget.Page(WalloraSettingsRoute.LIVE),
    ),

    // Remote / change from anywhere page
    SettingsSearchEntry(
        titleRes = R.string.settings_remote_title,
        icon = Icons.Outlined.SettingsRemote,
        keywords = listOf(
            "gesture", "launcher", "nova", "lawnchair", "shortcut", "qs tile",
            "quick settings", "tile", "tasker", "automation", "widget", "long press",
            "change from anywhere", "outside app",
        ),
        target = SettingsTarget.Page(WalloraSettingsRoute.REMOTE),
    ),

    // Theme (popup)
    SettingsSearchEntry(
        titleRes = R.string.settings_appearance_title,
        icon = Icons.Outlined.Palette,
        keywords = listOf("theme", "dark mode", "dark", "light", "system", "appearance", "color"),
        target = SettingsTarget.Popup(SettingsPopupId.THEME),
    ),

    // Clear cache (popup)
    SettingsSearchEntry(
        titleRes = R.string.settings_clear_cache,
        icon = Icons.Outlined.Storage,
        keywords = listOf("cache", "clear", "storage", "disk", "space", "memory", "image cache"),
        target = SettingsTarget.Popup(SettingsPopupId.CLEAR_CACHE),
    ),

    // About page
    SettingsSearchEntry(
        titleRes = R.string.settings_about,
        icon = Icons.Outlined.Info,
        keywords = listOf("about", "version", "developer", "github", "credits", "license", "open source"),
        target = SettingsTarget.Page(WalloraSettingsRoute.ABOUT),
    ),
)

/** Route string constants for settings sub-pages. */
object WalloraSettingsRoute {
    const val SOURCES = "settings/sources"
    const val CATEGORIES = "settings/categories"
    const val ROTATION = "settings/rotation"
    const val LIVE = "settings/live"
    const val REMOTE = "settings/remote"
    const val ABOUT = "settings/about"
}
