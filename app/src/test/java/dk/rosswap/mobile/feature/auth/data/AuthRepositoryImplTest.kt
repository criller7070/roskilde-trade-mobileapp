package dk.rosswap.mobile.feature.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.AuthResult
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import io.mockk.mockk
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import com.google.firebase.auth.FirebaseUser
import com.google.android.gms.tasks.Task
import io.mockk.every

/**
 * Tests for FirebaseAuthRepository
 * 
 * Verifies the repository correctly implements Firebase authentication operations
 * including login, sign-up, and Google sign-in flows.
 */
class AuthRepositoryImplTest {
    private lateinit var authRepository: FirebaseAuthRepository
    private val mockAuth: FirebaseAuth = mockk()
    private val mockFirestore: FirebaseFirestore = mockk()
    private val mockSessionManager: SessionManager = mockk()
    private val mockFirebaseUser: FirebaseUser = mockk()
    private val mockAuthResult: AuthResult = mockk()

    @Before
    fun setup() {
        authRepository = FirebaseAuthRepository(mockAuth, mockFirestore, mockSessionManager)
    }

    // ==================== INTERFACE COMPLIANCE ====================

    @Test
    fun repository_implements_auth_repository_interface() {
        assertTrue("Repository must implement AuthRepository interface", authRepository is AuthRepository)
    }

    // ==================== LOGIN TESTS ====================

    @Test
    fun login_withValidCredentials_shouldReturnSuccess() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        
        every { mockAuthResult.user } returns mockFirebaseUser
        val mockTask = mockk<Task<AuthResult>> {
            coEvery { await() } returns mockAuthResult
        }
        coEvery { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.login(email, password)

        // Assert
        assertTrue("Login should succeed with valid credentials", result.isSuccess)
    }

    @Test
    fun login_whenFirebaseThrowsException_shouldReturnFailure() = runTest {
        // Arrange
        val email = "wrong@example.com"
        val password = "wrongpass"
        val exception = Exception("User not found")
        
        val mockTask = mockk<Task<AuthResult>> {
            coEvery { await() } throws exception
        }
        coEvery { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.login(email, password)

        // Assert
        assertTrue("Login should fail", result.isFailure)
        assertEquals("Should propagate exception", exception, result.exceptionOrNull())
    }

    @Test
    fun login_whenUserIsNull_shouldReturnFailure() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        
        every { mockAuthResult.user } returns null
        val mockTask = mockk<Task<AuthResult>> {
            coEvery { await() } returns mockAuthResult
        }
        coEvery { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.login(email, password)

        // Assert
        assertTrue("Should fail when user is null", result.isFailure)
    }
}
