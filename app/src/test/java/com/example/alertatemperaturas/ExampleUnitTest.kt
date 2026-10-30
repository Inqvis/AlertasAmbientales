package com.example.alertatemperaturas

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    private fun statuses(t: Double, soil: Int, air: Int, light: Int) =
        evaluateSensors(t, soil, air, light).map { it.status }

    @Test
    fun evaluateSensors_checksOptimalWarningAndCriticalThresholds() {
        val ok = SensorStatus.OPTIMAL
        val warn = SensorStatus.WARNING
        val crit = SensorStatus.CRITICAL

        assertEquals(listOf(ok, ok, ok, ok), statuses(22.0, 70, 65, 70))
        assertEquals(listOf(warn, warn, warn, warn), statuses(27.0, 50, 75, 40))
        assertEquals(listOf(warn, warn, warn, warn), statuses(17.0, 85, 55, 69))
        assertEquals(listOf(crit, crit, crit, crit), statuses(8.0, 20, 35, 39))
        assertEquals(listOf(crit, crit, crit, crit), statuses(31.0, 95, 90, 10))
    }
}
