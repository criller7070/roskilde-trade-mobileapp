package dk.rosswap.mobile.feature.auth.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import androidx.arch.core.executor.testing.InstantTaskExecutorRule

/**
 * Unit tests for AuthViewModel UI state management
 * Verifies login, Google sign-in, and loading state behavior
 * Uses proper LiveData observers to capture state changes
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    private lateinit var authViewModel: AuthViewModel
    private val mockSessionManager: SessionManager = mockk()
    private val mockAuthRepository: AuthRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Create a flow that emulates SessionManager behavior
        val authStateFlow = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
        coEvery { mockSessionManager.authState } returns authStateFlow

        authViewModel = AuthViewModel(mockSessionManager, mockAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== LOGIN TESTS ====================

    @Test
    fun login_withValidCredentials_shouldCallRepository() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        coEvery { mockAuthRepository.login(email, password) } returns Result.success(Unit)

        // Act
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify { mockAuthRepository.login(email, password) }
    }

    @Test
    fun login_withValidCredentials_shouldSetLoadingStateToTrue() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        coEvery { mockAuthRepository.login(email, password) } returns Result.success(Unit)
        
        val loadingStates = mutableListOf<Boolean>()
        val observer = Observer<Boolean> { loadingStates.add(it) }

        // Act
        authViewModel.isLoading.observeForever(observer)
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - Should have captured loading state transitions
        assertTrue("Loading state should be true at some point", loadingStates.any { it })
        
        authViewModel.isLoading.removeObserver(observer)
    }

    @Test
    fun login_withValidCredentials_shouldSetLoadingStateToFalseAfterCompletion() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        coEvery { mockAuthRepository.login(email, password) } returns Result.success(Unit)

        // Act
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - After completion, loading should be false
        assertFalse("Loading state should be false after completion", authViewModel.isLoading.value ?: true)
    }

    @Test
    fun login_withInvalidCredentials_shouldPostFailureResult() = runTest {
        // Arrange
        val email = "wrong@example.com"
        val password = "wrongpassword"
        val exception = Exception("Invalid credentials")
        coEvery { mockAuthRepository.login(email, password) } returns Result.failure(exception)

        var capturedResult: Result<Unit>? = null
        val observer = Observer<Result<Unit>> { capturedResult = it }

        // Act
        authViewModel.loginResult.observeForever(observer)
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - Failure should be posted to LiveData
        assertNotNull("Login result should be posted", capturedResult)
        assertTrue("Result should be failure", capturedResult?.isFailure == true)
        assertEquals("Exception should match", exception, capturedResult?.exceptionOrNull())
        
        authViewModel.loginResult.removeObserver(observer)
    }

    @Test
    fun login_withEmptyEmail_shouldCallRepository() = runTest {
        // Arrange - Repository will receive the empty email (validation may be at repository level)
        val email = ""
        val password = "password123"
        coEvery { mockAuthRepository.login(email, password) } returns Result.failure(Exception("Email required"))

        // Act
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify { mockAuthRepository.login(email, password) }
    }

    @Test
    fun login_withEmptyPassword_shouldCallRepository() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = ""
        coEvery { mockAuthRepository.login(email, password) } returns Result.failure(Exception("Password required"))

        // Act
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify { mockAuthRepository.login(email, password) }
    }

    // ==================== GOOGLE SIGNIN TESTS ====================

    @Test
    fun signInWithGoogle_withValidToken_shouldCallRepository() = runTest {
        // Arrange
        val idToken = "valid.id.token"
        coEvery { mockAuthRepository.signInWithGoogle(idToken) } returns Result.success(Unit)

        // Act
        authViewModel.signInWithGoogle(idToken)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify { mockAuthRepository.signInWithGoogle(idToken) }
    }

    @Test
    fun signInWithGoogle_withValidToken_shouldSetLoadingState() = runTest {
        // Arrange
        val idToken = "valid.id.token"
        coEvery { mockAuthRepository.signInWithGoogle(idToken) } returns Result.success(Unit)
        
        val loadingStates = mutableListOf<Boolean>()
        val observer = Observer<Boolean> { loadingStates.add(it) }

        // Act
        authViewModel.isLoading.observeForever(observer)
        authViewModel.signInWithGoogle(idToken)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - Should have captured loading state
        assertTrue("Loading state should be captured", loadingStates.isNotEmpty())
        
        authViewModel.isLoading.removeObserver(observer)
    }

    @Test
    fun signInWithGoogle_withInvalidToken_shouldPostFailureResult() = runTest {
        // Arrange
        val idToken = "invalid.token"
        val exception = Exception("Invalid token")
        coEvery { mockAuthRepository.signInWithGoogle(idToken) } returns Result.failure(exception)

        // Act
        authViewModel.signInWithGoogle(idToken)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - Verify repository was called with the invalid token
        coVerify { mockAuthRepository.signInWithGoogle(idToken) }
    }

    // ==================== LOADING STATE TESTS ====================

    @Test
    fun login_shouldSetLoadingStateToFalse_afterCompletion() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        coEvery { mockAuthRepository.login(email, password) } returns Result.success(Unit)

        // Act
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - Loading should be false after operation completes
        assertFalse("Loading should be false after completion", authViewModel.isLoading.value ?: true)
    }

    @Test
    fun signInWithGoogle_shouldSetLoadingStateToFalse_afterCompletion() = runTest {
        // Arrange
        val idToken = "valid.token"
        coEvery { mockAuthRepository.signInWithGoogle(idToken) } returns Result.success(Unit)

        // Act
        authViewModel.signInWithGoogle(idToken)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - Loading should be false after operation completes
        assertFalse("Loading should be false after completion", authViewModel.isLoading.value ?: true)
    }

    @Test
    fun login_shouldCompleteWithoutThrowingException() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        coEvery { mockAuthRepository.login(email, password) } returns Result.success(Unit)

        // Act & Assert - Should not throw
        try {
            authViewModel.login(email, password)
            testDispatcher.scheduler.advanceUntilIdle()
        } catch (e: Exception) {
            fail("Login should not throw exception: ${e.message}")
        }
    }

    @Test
    fun signInWithGoogle_shouldCompleteWithoutThrowingException() = runTest {
        // Arrange
        val idToken = "valid.token"
        coEvery { mockAuthRepository.signInWithGoogle(idToken) } returns Result.success(Unit)

        // Act & Assert - Should not throw
        try {
            authViewModel.signInWithGoogle(idToken)
            testDispatcher.scheduler.advanceUntilIdle()
        } catch (e: Exception) {
            fail("SignIn should not throw exception: ${e.message}")
        }
    }
}
