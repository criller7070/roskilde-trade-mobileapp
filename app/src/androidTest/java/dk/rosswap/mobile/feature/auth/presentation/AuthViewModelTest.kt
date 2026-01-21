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
 * Instrumented tests for AuthViewModel with Android framework integration
 * 
 * These tests run on Android device/emulator and verify ViewModel behavior
 * with Android-specific features (LiveData, actual lifecycle, etc.).
 * 
 * Note: Most state management tests are in unit tests. This file focuses on
 * Android-specific integration that requires real Android context.
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
        
        val authStateFlow = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
        coEvery { mockSessionManager.authState } returns authStateFlow

        authViewModel = AuthViewModel(mockSessionManager, mockAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== ANDROID LIVEDATA INTEGRATION ====================

    @Test
    fun loginResult_liveData_should_notify_observers_on_success() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        coEvery { mockAuthRepository.login(email, password) } returns Result.success(Unit)
        
        var observedResult: Result<Unit>? = null
        val observer = Observer<Result<Unit>> { observedResult = it }

        // Act
        authViewModel.loginResult.observeForever(observer)
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - LiveData observer pattern verified on Android
        assertNotNull("Observer should be notified", observedResult)
        assertTrue("Result should be success", observedResult?.isSuccess == true)

        authViewModel.loginResult.removeObserver(observer)
    }

    @Test
    fun isLoading_liveData_should_notify_observers_during_operation() = runTest {
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

        // Assert - Multiple state transitions captured via observer
        assertTrue("Observer should capture state changes", loadingStates.size > 0)

        authViewModel.isLoading.removeObserver(observer)
    }

    @Test
    fun multiple_observers_should_all_receive_updates() = runTest {
        // Arrange - Multiple observers on same LiveData
        val email = "test@example.com"
        val password = "password123"
        coEvery { mockAuthRepository.login(email, password) } returns Result.success(Unit)
        
        val result1 = mutableListOf<Boolean>()
        val result2 = mutableListOf<Boolean>()
        val observer1 = Observer<Boolean> { result1.add(it) }
        val observer2 = Observer<Boolean> { result2.add(it) }

        // Act
        authViewModel.isLoading.observeForever(observer1)
        authViewModel.isLoading.observeForever(observer2)
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - Both observers receive updates
        assertTrue("Observer 1 should be notified", result1.isNotEmpty())
        assertTrue("Observer 2 should be notified", result2.isNotEmpty())

        authViewModel.isLoading.removeObserver(observer1)
        authViewModel.isLoading.removeObserver(observer2)
    }

    @Test
    fun observer_removal_should_stop_receiving_updates() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        coEvery { mockAuthRepository.login(email, password) } returns Result.success(Unit)
        
        val results = mutableListOf<Boolean>()
        val observer = Observer<Boolean> { results.add(it) }

        // Act - Add and remove observer
        authViewModel.isLoading.observeForever(observer)
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()
        val countBefore = results.size
        
        authViewModel.isLoading.removeObserver(observer)
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()
        val countAfter = results.size

        // Assert - No new updates after removal
        assertEquals("Observer should not receive updates after removal", countBefore, countAfter)
    }
}
