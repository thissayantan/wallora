package com.wallora.app.worker

/**
 * Pure helper that computes the next alarm trigger instant.
 *
 * Inputs are kept as primitive types so this can be unit-tested without Android
 * framework dependencies.
 */
object AlarmScheduleCalculator {

    /**
     * Given a list of [times] (each "HH:mm") and a [nowMs] epoch-millis,
     * return the next trigger epoch-millis.
     *
     * Algorithm:
     * 1. Parse each "HH:mm" into minutes-of-day (0–1439).
     * 2. Compute "minutes-of-day" for [nowMs] adjusted to the device's default time zone.
     * 3. Find the next time-of-day that is strictly after [nowMs]-in-day.
     * 4. If none exist today, take the earliest time and advance to tomorrow.
     *
     * @param times           Set of "HH:mm" strings. Duplicates are ignored.
     * @param nowMs           Current epoch milliseconds (injectable for testing).
     * @param timeZoneOffsetMs Millis offset of the local time zone from UTC. Defaults to the
     *                        JVM default. Injected for hermetic tests.
     * @return The next trigger epoch-millis, or null if [times] is empty.
     */
    fun nextTrigger(
        times: Set<String>,
        nowMs: Long,
        timeZoneOffsetMs: Long = java.util.TimeZone.getDefault().getOffset(nowMs).toLong(),
    ): Long? {
        if (times.isEmpty()) return null

        // Parse unique minutes-of-day, ignoring malformed entries
        val minutesOfDay = times
            .mapNotNull { parseMinutesOfDay(it) }
            .distinct()
            .sorted()

        if (minutesOfDay.isEmpty()) return null

        val localMs = nowMs + timeZoneOffsetMs
        val msIntoDay = localMs % MILLIS_PER_DAY
        val nowMinutesOfDay = (msIntoDay / MILLIS_PER_MINUTE).toInt()

        // Start of today (midnight UTC adjusted to local)
        val startOfTodayLocal = localMs - msIntoDay
        val startOfTodayUtc = startOfTodayLocal - timeZoneOffsetMs

        // Find the next time strictly after the current minute-of-day
        val nextToday = minutesOfDay.firstOrNull { it > nowMinutesOfDay }
        return if (nextToday != null) {
            startOfTodayUtc + nextToday * MILLIS_PER_MINUTE
        } else {
            // All times today have passed — first time tomorrow
            val firstTomorrow = minutesOfDay.first()
            startOfTodayUtc + MILLIS_PER_DAY + firstTomorrow * MILLIS_PER_MINUTE
        }
    }

    /**
     * Parse "HH:mm" into minutes-of-day (0–1439).
     * Returns null for invalid input.
     */
    fun parseMinutesOfDay(timeStr: String): Int? {
        val parts = timeStr.split(":")
        if (parts.size != 2) return null
        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null
        if (hours !in 0..23 || minutes !in 0..59) return null
        return hours * 60 + minutes
    }

    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MILLIS_PER_DAY = 86_400_000L
}
