package dk.rosswap.mobile.feature.auth.data

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import com.google.android.gms.tasks.Task
import io.mockk.every

/**
 * Tests for FirebaseAuthRepository data layer
 * Verifies login, signup, and Google sign-in behavior with mocked Firebase services
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

    // ==================== REPOSITORY SETUP TESTS ====================

    @Test
    fun repository_implements_auth_repository_interface() {
        assertTrue(authRepository is AuthRepository)
    }

    @Test
    fun repository_is_firebase_implementation() {
        assertEquals("FirebaseAuthRepository", authRepository::class.simpleName)
    }

    // ==================== LOGIN TESTS ====================

    @Test
    fun login_withValidCredentials_shouldReturnSuccess() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        every { mockAuthResult.user } returns mockFirebaseUser
        val mockTask = mockk<Task<AuthResult>>()
        coEvery { mockTask.await() } returns mockAuthResult
        coEvery { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.login(email, password)

        // Assert
        assertTrue(result.isSuccess)
        coVerify { mockAuth.signInWithEmailAndPassword(email, password) }
    }

    @Test
    fun login_whenFirebaseThrowsException_shouldReturnFailure() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "wrongpassword"
        val exception = Exception("Invalid credentials")
        val mockTask = mockk<Task<AuthResult>>()
        coEvery { mockTask.await() } throws exception
        coEvery { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.login(email, password)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun login_whenUserIsNull_shouldReturnFailure() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        every { mockAuthResult.user } returns null  // No user returned
        val mockTask = mockk<Task<AuthResult>>()
        coEvery { mockTask.await() } returns mockAuthResult
        coEvery { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.login(email, password)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    // ==================== SIGNUP TESTS ====================

    @Test
    fun signUp_withValidData_shouldReturnSuccess() = runBlocking {
        // Arrange
        val email = "newuser@example.com"
        val password = "password123"
        val name = "Test User"
        val hasConsent = true
        
        // Mock Firebase Auth creation
        every { mockAuthResult.user } returns mockFirebaseUser
        val mockAuthTask = mockk<Task<AuthResult>>()
        coEvery { mockAuthTask.await() } returns mockAuthResult
        coEvery { mockAuth.createUserWithEmailAndPassword(email, password) } returns mockAuthTask
        
        // Mock profile update
        val mockProfileTask = mockk<Task<Void>>()
        coEvery { mockProfileTask.await() } returns null
        coEvery { mockFirebaseUser.updateProfile(any()) } returns mockProfileTask
        
        // Mock Firestore set
        val mockFirestoreTask = mockk<Task<Void>>()
        coEvery { mockFirestoreTask.await() } returns null
        coEvery { mockFirestore.collection("users").document(any()).set(any()) } returns mockFirestoreTask

        // Act
        val result = authRepository.signUp(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isSuccess)
        coVerify { mockAuth.createUserWithEmailAndPassword(email, password) }
        coVerify { mockFirebaseUser.updateProfile(any()) }
        coVerify { mockFirestore.collection("users").document(any()).set(any()) }
    }

    @Test
    fun signUp_whenAuthCreationFails_shouldReturnFailure() = runBlocking {
        // Arrange
        val email = "newuser@example.com"
        val password = "password123"
        val name = "Test User"
        val hasConsent = true
        val exception = Exception("Email already in use")
        
        val mockAuthTask = mockk<Task<AuthResult>>()
        coEvery { mockAuthTask.await() } throws exception
        coEvery { mockAuth.createUserWithEmailAndPassword(email, password) } returns mockAuthTask

        // Act
        val result = authRepository.signUp(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun signUp_whenUserIsNull_shouldReturnFailure() = runBlocking {
        // Arrange
        val email = "newuser@example.com"
        val password = "password123"
        val name = "Test User"
        val hasConsent = true
        
        every { mockAuthResult.user } returns null  // No user returned
        val mockAuthTask = mockk<Task<AuthResult>>()
        coEvery { mockAuthTask.await() } returns mockAuthResult
        coEvery { mockAuth.createUserWithEmailAndPassword(email, password) } returns mockAuthTask

        // Act
        val result = authRepository.signUp(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is Exception)
    }

    // ==================== GOOGLE SIGNIN TESTS ====================

    @Test
    fun signInWithGoogle_withValidToken_shouldReturnSuccess() = runBlocking {
        // Arrange
        val idToken = "valid.id.token"
        every { mockAuthResult.user } returns mockFirebaseUser
        every { mockAuthResult.additionalUserInfo?.isNewUser } returns false
        
        val mockGoogleAuthTask = mockk<Task<AuthResult>>()
        coEvery { mockGoogleAuthTask.await() } returns mockAuthResult
        coEvery { mockAuth.signInWithCredential(any()) } returns mockGoogleAuthTask
        
        // Mock Firestore operations for existing user
        val mockDocTask = mockk<Task<com.google.firebase.firestore.DocumentSnapshot>>()
        coEvery { mockDocTask.await() } returns mockk {
            every { exists() } returns true
            every { data } returns emptyMap()
        }
        coEvery { mockFirestore.collection("users").document(any()).get() } returns mockDocTask

        // Act
        val result = authRepository.signInWithGoogle(idToken)

        // Assert
        assertTrue(result.isSuccess)
        coVerify { mockAuth.signInWithCredential(any()) }
    }

    @Test
    fun signInWithGoogle_withInvalidToken_shouldReturnFailure() = runBlocking {
        // Arrange
        val idToken = "invalid.token"
        val exception = Exception("Invalid ID token")
        
        val mockGoogleAuthTask = mockk<Task<AuthResult>>()
        coEvery { mockGoogleAuthTask.await() } throws exception
        coEvery { mockAuth.signInWithCredential(any()) } returns mockGoogleAuthTask

        // Act
        val result = authRepository.signInWithGoogle(idToken)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun signInWithGoogle_whenUserIsNull_shouldReturnFailure() = runBlocking {
        // Arrange
        val idToken = "valid.id.token"
        every { mockAuthResult.user } returns null
        
        val mockGoogleAuthTask = mockk<Task<AuthResult>>()
        coEvery { mockGoogleAuthTask.await() } returns mockAuthResult
        coEvery { mockAuth.signInWithCredential(any()) } returns mockGoogleAuthTask

        // Act
        val result = authRepository.signInWithGoogle(idToken)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is Exception)
    }
}
