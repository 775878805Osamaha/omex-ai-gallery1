package com.example

import com.omex.gallery.core.ai.orchestrator.ExecutionDelegate
import com.omex.gallery.core.ai.orchestrator.InterpreterPool
import com.omex.gallery.core.log.LogCategory
import com.omex.gallery.core.log.OmexLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OmexPerformanceReportTest {

    @Test
    fun `test structured logging and performance metrics`() {
        OmexLogger.i(LogCategory.PERFORMANCE, "TestTag", "Cold startup time: 240ms")
        OmexLogger.d(LogCategory.AI, "TestTag", "MobileNetV3 inference time: 14ms")
        OmexLogger.w(LogCategory.DATABASE, "TestTag", "Batch insert completed in 12ms")

        val recentLogs = OmexLogger.getRecentLogs(10)
        assertTrue(recentLogs.isNotEmpty())

        val exportString = OmexLogger.formatLogsForExport()
        assertTrue(exportString.contains("PERFORMANCE"))
        assertTrue(exportString.contains("Cold startup time"))
    }

    @Test
    fun `test interpreter benchmark latency tracker`() {
        val pool = InterpreterPool(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        pool.recordInferenceLatency("mobilenet_v3.tflite", 15)
        pool.recordInferenceLatency("mobilenet_v3.tflite", 25)

        val benchmark = pool.getBenchmark("mobilenet_v3.tflite")
        assertNotNull(benchmark)
        assertEquals("mobilenet_v3.tflite", benchmark?.modelName)
        assertEquals(20L, benchmark?.avgLatencyMs)
        assertEquals(2, benchmark?.inferenceCount)
        assertEquals(ExecutionDelegate.CPU, benchmark?.delegateUsed)
    }
}
