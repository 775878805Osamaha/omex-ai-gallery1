package com.omex.gallery

import com.omex.gallery.core.ai.model.ExecutionDelegate
import com.omex.gallery.core.ai.runtime.InterpreterPool
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
@Config(sdk = [35])
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
    fun `test interpreter pool acquisition and fallback`() = kotlinx.coroutines.runBlocking {
        val pool = InterpreterPool(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        val registry = com.omex.gallery.core.ai.registry.ModelRegistry()
        val descriptor = registry.getDescriptor(com.omex.gallery.core.ai.registry.ModelType.MOBILENET_V3_LARGE)
        assertNotNull(descriptor)
        if (descriptor != null) {
            val runner = pool.acquireRunner(descriptor)
            assertNotNull(runner)
            assertTrue(runner.isInitialized())
            pool.releaseRunner(descriptor, runner)
        }
        pool.clearPool()
    }
}
