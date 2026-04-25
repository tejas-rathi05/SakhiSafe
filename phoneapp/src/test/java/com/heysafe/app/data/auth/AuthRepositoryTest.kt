package com.heysafe.app.data.auth

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRepositoryTest {
    @Test
    fun `signIn returns Success on valid credentials`() = runTest {
        val backend = mockk<AuthBackend>()
        coEvery { backend.signIn("a@b.c", "pw") } returns AuthUser("uid1", "a@b.c")
        val repo = AuthRepository(backend)
        val result = repo.signIn("a@b.c", "pw")
        assertTrue(result is AuthResult.Success)
        assertEquals("uid1", result.user.uid)
    }

    @Test
    fun `signIn returns Error on backend failure`() = runTest {
        val backend = mockk<AuthBackend>()
        coEvery { backend.signIn(any(), any()) } throws RuntimeException("nope")
        val repo = AuthRepository(backend)
        val result = repo.signIn("x", "y")
        assertTrue(result is AuthResult.Error)
        assertEquals("nope", result.message)
    }

    @Test
    fun `signUp returns Success and creates user doc`() = runTest {
        val backend = mockk<AuthBackend>()
        coEvery { backend.signUp("a@b.c", "pw", "Karan") } returns AuthUser("uid2", "a@b.c")
        val repo = AuthRepository(backend)
        val result = repo.signUp("a@b.c", "pw", "Karan")
        assertTrue(result is AuthResult.Success)
        assertEquals("uid2", result.user.uid)
    }
}
