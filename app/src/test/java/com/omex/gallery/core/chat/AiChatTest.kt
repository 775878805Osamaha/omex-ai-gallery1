package com.omex.gallery.core.chat

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.omex.gallery.core.ai.chat.LocalFallbackAiChatEngine
import com.omex.gallery.core.ai.genai.GenerativeModelRepository
import com.omex.gallery.core.data.local.AppDatabase
import com.omex.gallery.core.data.local.ChatDao
import com.omex.gallery.core.data.local.ChatMessageEntity
import com.omex.gallery.core.data.local.ChatSessionEntity
import com.omex.gallery.ui.feature_chat.AiChatViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AiChatTest {

    private lateinit var db: AppDatabase
    private lateinit var chatDao: ChatDao
    private lateinit var modelRepository: GenerativeModelRepository
    private lateinit var fallbackEngine: LocalFallbackAiChatEngine
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        chatDao = db.chatDao()
        modelRepository = GenerativeModelRepository(context)
        fallbackEngine = LocalFallbackAiChatEngine()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test GenerativeModelRepository detects uninstalled status by default`() {
        val info = modelRepository.modelInfo.value
        assertFalse(info.isInstalled)
        assertEquals("gemma-2b-it-cpu-int4.task", info.modelFileName)
        assertNotNull(info.modelDirectory)
    }

    @Test
    fun `test ChatDao insert and query sessions`() = runBlocking {
        val session = ChatSessionEntity(title = "محادثة اختبارية")
        val sessionId = chatDao.insertSession(session)

        assertTrue(sessionId > 0)
        val fetched = chatDao.getSessionById(sessionId)
        assertNotNull(fetched)
        assertEquals("محادثة اختبارية", fetched?.title)

        val allSessions = chatDao.getAllSessions().first()
        assertEquals(1, allSessions.size)
    }

    @Test
    fun `test ChatDao insert and query messages`() = runBlocking {
        val session = ChatSessionEntity(title = "محادثة رسائل")
        val sessionId = chatDao.insertSession(session)

        val userMsg = ChatMessageEntity(
            sessionId = sessionId,
            role = "user",
            content = "مرحبا يا مساعد"
        )
        val assistantMsg = ChatMessageEntity(
            sessionId = sessionId,
            role = "assistant",
            content = "أهلاً بك! كيف يمكنني مساعدتك اليوم؟"
        )

        chatDao.insertMessage(userMsg)
        chatDao.insertMessage(assistantMsg)

        val messages = chatDao.getMessagesForSession(sessionId).first()
        assertEquals(2, messages.size)
        assertEquals("user", messages[0].role)
        assertEquals("assistant", messages[1].role)
    }

    @Test
    fun `test LocalFallbackAiChatEngine output`() = runBlocking {
        assertFalse(fallbackEngine.isAvailable())
        val response = fallbackEngine.sendMessage("اختبار").first()
        assertTrue(response.contains("غير متوفر"))
    }

    @Test
    fun `test AiChatViewModel session management and message flow`() = runBlocking {
        val viewModel = AiChatViewModel(chatDao, fallbackEngine, modelRepository)

        val session = ChatSessionEntity(title = "محادثة اختبارية")
        val sessionId = chatDao.insertSession(session)

        viewModel.selectSession(sessionId)

        val userMsg = ChatMessageEntity(
            sessionId = sessionId,
            role = "user",
            content = "ما هو الطقس اليوم؟"
        )
        chatDao.insertMessage(userMsg)

        val messages = chatDao.getMessagesForSession(sessionId).first()
        assertTrue(messages.any { it.content == "ما هو الطقس اليوم؟" })

        chatDao.deleteMessagesForSession(sessionId)
        val clearedMessages = chatDao.getMessagesForSession(sessionId).first()
        assertTrue(clearedMessages.isEmpty())
    }
}
