package com.example.alertatemperaturas

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun evaluateSensors_checksOptimalWarningAndCriticalThresholds() {
        val optimal = evaluateSensors(tempC = 22.0, soilHumidityPct = 70, airHumidityPct = 65, lightLux = 6000)
        assertEquals(listOf(SensorStatus.OPTIMAL, SensorStatus.OPTIMAL, SensorStatus.OPTIMAL, SensorStatus.OPTIMAL), optimal.map { it.status })

        val warning = evaluateSensors(tempC = 27.0, soilHumidityPct = 50, airHumidityPct = 75, lightLux = 5000)
        assertEquals(listOf(SensorStatus.WARNING, SensorStatus.WARNING, SensorStatus.WARNING, SensorStatus.OPTIMAL), warning.map { it.status })

        val criticalLow = evaluateSensors(tempC = 8.0, soilHumidityPct = 20, airHumidityPct = 35, lightLux = 4000)
        assertEquals(listOf(SensorStatus.CRITICAL, SensorStatus.CRITICAL, SensorStatus.CRITICAL, SensorStatus.CRITICAL), criticalLow.map { it.status })

        val criticalHigh = evaluateSensors(tempC = 31.0, soilHumidityPct = 95, airHumidityPct = 90, lightLux = 4999)
        assertEquals(listOf(SensorStatus.CRITICAL, SensorStatus.CRITICAL, SensorStatus.CRITICAL, SensorStatus.CRITICAL), criticalHigh.map { it.status })
    }
}
