package com.wallora.app.domain.model

/**
 * Parameters for the pre-apply wallpaper editor.
 *
 * All float values are normalized:
 * - [blur]: 0.0 (none) – 25.0 (max).
 * - [brightness]: -1.0 (black) – +1.0 (white). 0.0 = unchanged.
 * - [contrast]: 0.0 (flat gray) – 2.0 (max contrast). 1.0 = unchanged.
 * - [saturation]: 0.0 (grayscale) – 2.0 (max saturation). 1.0 = unchanged.
 * - [panX]: horizontal pan offset in normalized image widths (-1.0 left – +1.0 right). 0 = center.
 * - [panY]: vertical pan offset in normalized image heights. 0 = center.
 */
data class EditParams(
    val blur: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
) {
    companion object {
        val Default = EditParams()

        fun isDefault(params: EditParams): Boolean = params == Default
    }
}
