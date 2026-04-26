package com.heysafe.app.data.alerts

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AlertsRepositoryTest {
    @Test fun `create returns alert id from backend`() = runTest {
        val backend = mockk<AlertsBackend>()
        coEvery { backend.create(any()) } returns "alert123"
        val repo = AlertsRepository(backend)
        val r = repo.create(Alert(userId = "u", userName = "K", triggerSource = "heuristic"))
        val id = r.getOrNull()
        assertNotNull(id); assertEquals("alert123", id)
    }

    @Test fun `setAudioBase64 forwards to backend`() = runTest {
        val backend = mockk<AlertsBackend>(relaxed = true)
        val repo = AlertsRepository(backend)
        val r = repo.setAudioBase64("alertX", "base64payload==")
        assertTrue(r.isSuccess)
        coVerify { backend.setAudioBase64("alertX", "base64payload==") }
    }

    @Test fun `resolve forwards to backend`() = runTest {
        val backend = mockk<AlertsBackend>(relaxed = true)
        val repo = AlertsRepository(backend)
        val r = repo.resolve("alertX")
        assertTrue(r.isSuccess)
        coVerify { backend.resolve("alertX") }
    }
}
