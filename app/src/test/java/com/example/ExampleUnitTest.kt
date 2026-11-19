package com.example

import com.example.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun formatTaka_formatsCorrectly() {
        assertEquals("৳ 1,250", Formatters.formatTaka(1250.0))
        assertEquals("+৳ 500", Formatters.formatTaka(500.0, showSign = true))
        assertEquals("-৳ 250.5", Formatters.formatTaka(-250.5, showSign = true))
    }

    @Test
    fun shiftMonthKey_shiftsAcrossYearBoundaries() {
        assertEquals("2026-12", Formatters.shiftMonthKey("2026-11", 1))
        assertEquals("2027-01", Formatters.shiftMonthKey("2026-12", 1))
        assertEquals("2025-12", Formatters.shiftMonthKey("2026-01", -1))
    }
}
