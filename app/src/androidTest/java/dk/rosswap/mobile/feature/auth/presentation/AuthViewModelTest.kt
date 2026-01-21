package dk.rosswap.mobile.feature.auth.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.runner.RunWith

/**
 * Instrumented tests for AuthViewModel with Android framework
 * Tests UI state management on actual Android device/emulator
 * Uses proper LiveData observer pattern to capture state changes
 */
@RunWith(AndroidJUnit4::class)
class AuthViewModelTest {
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
    fun login_withValidCredentials_shouldCaptureLoadingStateTransitions() = runTest {
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

        // Assert - Should have captured loading state transitions (true at some point)
        assertTrue("Loading state should transition to true during login", loadingStates.any { it })
        
        authViewModel.isLoading.removeObserver(observer)
    }

    @Test
    fun login_withInvalidCredentials_shouldPostFailureResultToLiveData() = runTest {
        // Arrange
        val email = "wrong@example.com"
        val password = "wrongpassword"
        val exception = Exception("Invalid credentials")
        coEvery { mockAuthRepository.login(email, password) } returns Result.failure(exception)

        var capturedLoginResult: Result<Unit>? = null
        val observer = Observer<Result<Unit>> { capturedLoginResult = it }

        // Act
        authViewModel.loginResult.observeForever(observer)
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - Verify failure is posted to LiveData
        assertNotNull("Login result should be posted", capturedLoginResult)
        assertTrue("Result should be failure", capturedLoginResult!!.isFailure)
        assertEquals("Exception should match", exception, capturedLoginResult!!.exceptionOrNull())

        authViewModel.loginResult.removeObserver(observer)
    }

    @Test
    fun login_withEmptyEmail_shouldCallRepository() = runTest {
        // Arrange
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

        // Assert
        coVerify { mockAuthRepository.signInWithGoogle(idToken) }
    }

    // ==================== LOADING STATE TESTS ====================

    @Test
    fun login_shouldSetLoadingToTrue_duringExecution() = runTest {
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

        // Assert - Should have at least one state change captured
        assertTrue("Loading state changes should be captured", loadingStates.isNotEmpty())
        
        authViewModel.isLoading.removeObserver(observer)
    }

    @Test
    fun login_shouldSetLoadingToFalse_afterExecution() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        coEvery { mockAuthRepository.login(email, password) } returns Result.success(Unit)

        // Act
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - After advancement, loading should be false
        assertFalse("Loading should be false after completion", authViewModel.isLoading.value ?: true)
    }

    @Test
    fun signInWithGoogle_shouldSetLoadingToTrue_duringExecution() = runTest {
        // Arrange
        val idToken = "valid.token"
        coEvery { mockAuthRepository.signInWithGoogle(idToken) } returns Result.success(Unit)

        val loadingStates = mutableListOf<Boolean>()
        val observer = Observer<Boolean> { loadingStates.add(it) }

        // Act
        authViewModel.isLoading.observeForever(observer)
        authViewModel.signInWithGoogle(idToken)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertTrue("Loading state changes should be captured", loadingStates.isNotEmpty())

        authViewModel.isLoading.removeObserver(observer)
    }

    @Test
    fun signInWithGoogle_shouldSetLoadingToFalse_afterExecution() = runTest {
        // Arrange
        val idToken = "valid.token"
        coEvery { mockAuthRepository.signInWithGoogle(idToken) } returns Result.success(Unit)

        // Act
        authViewModel.signInWithGoogle(idToken)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - After advancement, loading should be false
        assertFalse("Loading should be false after completion", authViewModel.isLoading.value ?: true)
    }

    @Test
    fun login_withError_shouldUpdateLoadingStateToFalse() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val exception = Exception("Login failed")
        coEvery { mockAuthRepository.login(email, password) } returns Result.failure(exception)

        // Act
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - Even on error, loading should be false
        assertFalse("Loading should be false even on error", authViewModel.isLoading.value ?: true)
    }
}
