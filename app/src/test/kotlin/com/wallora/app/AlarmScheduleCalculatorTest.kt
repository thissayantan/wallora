package com.wallora.app

import com.wallora.app.worker.AlarmScheduleCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AlarmScheduleCalculator].
 * All tests use timeZoneOffsetMs = 0 (UTC) to stay hermetic.
 */
class AlarmScheduleCalculatorTest {

    // ── parseMinutesOfDay ─────────────────────────────────────────────────────

    @Test
    fun `parseMinutesOfDay valid midnight`() = assertEquals(0, AlarmScheduleCalculator.parseMinutesOfDay("00:00"))

    @Test
    fun `parseMinutesOfDay valid noon`() = assertEquals(720, AlarmScheduleCalculator.parseMinutesOfDay("12:00"))

    @Test
    fun `parseMinutesOfDay valid 23h59m`() = assertEquals(1439, AlarmScheduleCalculator.parseMinutesOfDay("23:59"))

    @Test
    fun `parseMinutesOfDay rejects hour 24`() = assertNull(AlarmScheduleCalculator.parseMinutesOfDay("24:00"))

    @Test
    fun `parseMinutesOfDay rejects minute 60`() = assertNull(AlarmScheduleCalculator.parseMinutesOfDay("12:60"))

    @Test
    fun `parseMinutesOfDay rejects malformed string`() = assertNull(AlarmScheduleCalculator.parseMinutesOfDay("noon"))

    // ── nextTrigger — empty input ─────────────────────────────────────────────

    @Test
    fun `nextTrigger returns null for empty times`() {
        assertNull(AlarmScheduleCalculator.nextTrigger(emptySet(), nowMs = 0L, timeZoneOffsetMs = 0L))
    }

    // ── nextTrigger — same day ────────────────────────────────────────────────

    /** now = 10:00 UTC; next alarm = 14:00 UTC → same day */
    @Test
    fun `nextTrigger returns next alarm same day`() {
        val nowMs = hoursToMs(10)
        val result = AlarmScheduleCalculator.nextTrigger(
            times = setOf("14:00"),
            nowMs = nowMs,
            timeZoneOffsetMs = 0L,
        )
        assertEquals(hoursToMs(14), result)
    }

    /** now = 10:00; alarms at 08:00 + 14:00 → picks 14:00 today */
    @Test
    fun `nextTrigger skips passed alarm and picks upcoming one`() {
        val nowMs = hoursToMs(10)
        val result = AlarmScheduleCalculator.nextTrigger(
            times = setOf("08:00", "14:00"),
            nowMs = nowMs,
            timeZoneOffsetMs = 0L,
        )
        assertEquals(hoursToMs(14), result)
    }

    // ── nextTrigger — rolls to tomorrow ──────────────────────────────────────

    /** now = 22:00; single alarm at 08:00 → next is tomorrow 08:00 */
    @Test
    fun `nextTrigger rolls to next day when all alarms passed`() {
        val nowMs = hoursToMs(22)
        val result = AlarmScheduleCalculator.nextTrigger(
            times = setOf("08:00"),
            nowMs = nowMs,
            timeZoneOffsetMs = 0L,
        )
        assertEquals(hoursToMs(24 + 8), result)
    }

    /** now = 23:59; alarms at 08:00 + 20:00 → next is tomorrow 08:00 */
    @Test
    fun `nextTrigger all alarms today passed — picks earliest tomorrow`() {
        val nowMs = minutesToMs(23 * 60 + 59)
        val result = AlarmScheduleCalculator.nextTrigger(
            times = setOf("08:00", "20:00"),
            nowMs = nowMs,
            timeZoneOffsetMs = 0L,
        )
        assertEquals(hoursToMs(24 + 8), result)
    }

    // ── nextTrigger — exactness ───────────────────────────────────────────────

    /** now exactly equals an alarm time — should pick that alarm on the SAME call */
    @Test
    fun `nextTrigger skips alarm at exact current minute`() {
        val nowMs = hoursToMs(8) // exactly 08:00
        val result = AlarmScheduleCalculator.nextTrigger(
            times = setOf("08:00", "12:00"),
            nowMs = nowMs,
            timeZoneOffsetMs = 0L,
        )!!
        // 08:00 is NOT strictly > nowMs (it equals), so result should be 12:00
        assertTrue("Expected trigger after now", result > nowMs)
    }

    // ── nextTrigger — duplicates ignored ─────────────────────────────────────

    @Test
    fun `nextTrigger deduplicates equal times`() {
        val nowMs = hoursToMs(6)
        val r1 = AlarmScheduleCalculator.nextTrigger(setOf("14:00"), nowMs, 0L)
        val r2 = AlarmScheduleCalculator.nextTrigger(setOf("14:00", "14:00"), nowMs, 0L)
        assertEquals(r1, r2)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun hoursToMs(h: Int) = h * 3_600_000L
    private fun minutesToMs(m: Int) = m * 60_000L
}
