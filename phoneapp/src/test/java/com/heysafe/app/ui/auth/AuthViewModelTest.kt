package com.heysafe.app.ui.auth

import com.heysafe.app.data.auth.AuthRepository
import com.heysafe.app.data.auth.AuthResult
import com.heysafe.app.data.auth.AuthUser
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthViewModelTest {
    @Before fun setup() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tear() = Dispatchers.resetMain()

    @Test
    fun `signIn updates state to Authenticated on success`() = runTest {
        val repo = mockk<AuthRepository>()
        coEvery { repo.signIn("a", "b") } returns AuthResult.Success(AuthUser("uid", "a"))
        val vm = AuthViewModel(repo)
        vm.signIn("a", "b")
        assertTrue(vm.state.value is AuthUiState.Authenticated)
    }

    @Test
    fun `signIn updates state to Error on failure`() = runTest {
        val repo = mockk<AuthRepository>()
        coEvery { repo.signIn(any(), any()) } returns AuthResult.Error("bad creds")
        val vm = AuthViewModel(repo)
        vm.signIn("x", "y")
        val s = vm.state.value
        assertTrue(s is AuthUiState.Error)
        assertEquals("bad creds", s.message)
    }
}
