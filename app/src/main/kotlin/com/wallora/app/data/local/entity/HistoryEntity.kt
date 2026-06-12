package com.wallora.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Records wallpapers that have been set, to avoid repeats in rotation. */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val globalKey: String,
    val sourceId: String,
    val id: String,
    val thumbUrl: String,
    val fullUrl: String,
    val width: Int,
    val height: Int,
    val author: String,
    val authorUrl: String,
    val sourcePageUrl: String,
    val colorHint: Int?,
    val tags: String,
    val setAt: Long,
)
