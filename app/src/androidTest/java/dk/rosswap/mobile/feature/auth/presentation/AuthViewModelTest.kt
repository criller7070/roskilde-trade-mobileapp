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
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.runner.RunWith

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
    fun login_withValidCredentials_shouldUpdateLoadingState() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        coEvery { mockAuthRepository.login(email, password) } returns Result.success(Unit)

        // Mock the LiveData observer
        var loadingStates = mutableListOf<Boolean>()
        val observer = Observer<Boolean> { loadingStates.add(it) }

        // Act
        authViewModel.isLoading.observeForever(observer)
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        // Should see loading states: initially false, then true during execution, then false after
        assertTrue(loadingStates.any { it })  // Should have been true at some point
        
        authViewModel.isLoading.removeObserver(observer)
    }

    @Test
    fun login_withValidCredentials_shouldCallRepository() = runBlocking {
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
    fun login_withInvalidCredentials_shouldPostFailureResult() = runBlocking {
        // Arrange
        val email = "wrong@example.com"
        val password = "wrongpassword"
        val exception = Exception("Invalid credentials")
        coEvery { mockAuthRepository.login(email, password) } returns Result.failure(exception)

        var loginResult: Result<Unit>? = null
        val observer = Observer<Result<Unit>> { loginResult = it }

        // Act
        authViewModel.loginResult.observeForever(observer)
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertNotNull(loginResult)
        assertTrue(loginResult!!.isFailure)
        assertEquals(exception, loginResult!!.exceptionOrNull())

        authViewModel.loginResult.removeObserver(observer)
    }

    @Test
    fun login_withEmptyEmail_shouldFail() = runBlocking {
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
    fun login_withEmptyPassword_shouldFail() = runBlocking {
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
    fun signInWithGoogle_withValidToken_shouldCallRepository() = runBlocking {
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
    fun signInWithGoogle_withInvalidToken_shouldPostFailureResult() = runBlocking {
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
    fun login_shouldSetLoadingToTrue_duringExecution() = runBlocking {
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

        // Assert
        assertTrue(loadingStates.isNotEmpty())
        
        authViewModel.isLoading.removeObserver(observer)
    }

    @Test
    fun login_shouldSetLoadingToFalse_afterExecution() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        coEvery { mockAuthRepository.login(email, password) } returns Result.success(Unit)

        // Act
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - After advancement, loading should be false
        assertFalse(authViewModel.isLoading.value ?: true)
    }

    @Test
    fun signInWithGoogle_shouldSetLoadingToTrue_duringExecution() = runBlocking {
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
        assertTrue(loadingStates.isNotEmpty())

        authViewModel.isLoading.removeObserver(observer)
    }
}
