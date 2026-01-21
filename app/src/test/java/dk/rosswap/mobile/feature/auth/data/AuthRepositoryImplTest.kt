package dk.rosswap.mobile.feature.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.AuthResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference
import com.google.android.gms.tasks.Task
import dk.rosswap.mobile.core.common.SessionManager
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class AuthRepositoryImplTest {
    private lateinit var authRepository: FirebaseAuthRepository
    private val mockAuth: FirebaseAuth = mockk()
    private val mockFirestore: FirebaseFirestore = mockk()
    private val mockSessionManager: SessionManager = mockk()
    private val mockFirebaseUser: FirebaseUser = mockk()
    private val mockAuthResult: AuthResult = mockk()
    private val mockDocRef: DocumentReference = mockk()

    @Before
    fun setup() {
        authRepository = FirebaseAuthRepository(mockAuth, mockFirestore, mockSessionManager)
    }

    // ==================== LOGIN TESTS ====================

    @Test
    fun login_withValidCredentials_shouldReturnSuccess() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val mockTask: Task<AuthResult> = mockk()
        every { mockTask.await() } returns mockAuthResult
        coEvery { mockAuthResult.user } returns mockFirebaseUser
        every { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.login(email, password)

        // Assert
        assertTrue(result.isSuccess)
    }

    @Test
    fun login_withInvalidEmail_shouldReturnFailure() = runBlocking {
        // Arrange
        val email = "invalid@example.com"
        val password = "password123"
        val exception = FirebaseAuthException("ERROR_USER_NOT_FOUND", "User not found")
        val mockTask: Task<AuthResult> = mockk()
        every { mockTask.await() } throws exception
        every { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.login(email, password)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun login_withWrongPassword_shouldReturnFailure() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "wrongpassword"
        val exception = FirebaseAuthException("ERROR_WRONG_PASSWORD", "Wrong password")
        val mockTask: Task<AuthResult> = mockk()
        every { mockTask.await() } throws exception
        every { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.login(email, password)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun login_whenAuthReturnsNullUser_shouldReturnFailure() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val mockTask: Task<AuthResult> = mockk()
        every { mockTask.await() } returns mockAuthResult
        coEvery { mockAuthResult.user } returns null
        every { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.login(email, password)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun login_withNetworkError_shouldReturnFailure() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val exception = FirebaseAuthException("ERROR_NETWORK_REQUEST_FAILED", "Network error")
        val mockTask: Task<AuthResult> = mockk()
        every { mockTask.await() } throws exception
        every { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.login(email, password)

        // Assert
        assertTrue(result.isFailure)
    }

    // ==================== SIGNUP TESTS ====================

    @Test
    fun signUp_withValidData_shouldReturnSuccess() = runBlocking {
        // Arrange
        val email = "newuser@example.com"
        val password = "password123"
        val name = "Test User"
        val hasConsent = true

        val createUserTask: Task<AuthResult> = mockk()
        val updateProfileTask: Task<Void> = mockk()
        val setDocTask: Task<Void> = mockk()

        every { createUserTask.await() } returns mockAuthResult
        every { mockAuthResult.user } returns mockFirebaseUser
        every { mockFirebaseUser.uid } returns "user123"
        every { mockFirebaseUser.updateProfile(any()) } returns updateProfileTask
        every { updateProfileTask.await() } returns null
        every { mockFirestore.collection("users").document("user123") } returns mockDocRef
        every { mockDocRef.set(any()) } returns setDocTask
        every { setDocTask.await() } returns null
        every { mockAuth.createUserWithEmailAndPassword(email, password) } returns createUserTask

        // Act
        val result = authRepository.signUp(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isSuccess)
    }

    @Test
    fun signUp_withExistingEmail_shouldReturnFailure() = runBlocking {
        // Arrange
        val email = "existing@example.com"
        val password = "password123"
        val name = "Test User"
        val hasConsent = true
        val exception = FirebaseAuthException("ERROR_EMAIL_ALREADY_IN_USE", "Email already in use")

        val mockTask: Task<AuthResult> = mockk()
        every { mockTask.await() } throws exception
        every { mockAuth.createUserWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.signUp(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun signUp_withWeakPassword_shouldReturnFailure() = runBlocking {
        // Arrange
        val email = "newuser@example.com"
        val password = "123"  // Too weak
        val name = "Test User"
        val hasConsent = true
        val exception = FirebaseAuthException("ERROR_WEAK_PASSWORD", "Weak password")

        val mockTask: Task<AuthResult> = mockk()
        every { mockTask.await() } throws exception
        every { mockAuth.createUserWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.signUp(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isFailure)
    }

    @Test
    fun signUp_whenUserDocumentFailsToCreate_shouldReturnFailure() = runBlocking {
        // Arrange
        val email = "newuser@example.com"
        val password = "password123"
        val name = "Test User"
        val hasConsent = true

        val createUserTask: Task<AuthResult> = mockk()
        val updateProfileTask: Task<Void> = mockk()
        val setDocTask: Task<Void> = mockk()
        val firestoreException = Exception("Firestore error")

        every { createUserTask.await() } returns mockAuthResult
        every { mockAuthResult.user } returns mockFirebaseUser
        every { mockFirebaseUser.uid } returns "user123"
        every { mockFirebaseUser.updateProfile(any()) } returns updateProfileTask
        every { updateProfileTask.await() } returns null
        every { mockFirestore.collection("users").document("user123") } returns mockDocRef
        every { mockDocRef.set(any()) } returns setDocTask
        every { setDocTask.await() } throws firestoreException
        every { mockAuth.createUserWithEmailAndPassword(email, password) } returns createUserTask

        // Act
        val result = authRepository.signUp(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(firestoreException, result.exceptionOrNull())
    }

    @Test
    fun signUp_withoutConsent_shouldStoreFalse() = runBlocking {
        // Arrange
        val email = "newuser@example.com"
        val password = "password123"
        val name = "Test User"
        val hasConsent = false

        val createUserTask: Task<AuthResult> = mockk()
        val updateProfileTask: Task<Void> = mockk()
        val setDocTask: Task<Void> = mockk()

        every { createUserTask.await() } returns mockAuthResult
        every { mockAuthResult.user } returns mockFirebaseUser
        every { mockFirebaseUser.uid } returns "user123"
        every { mockFirebaseUser.updateProfile(any()) } returns updateProfileTask
        every { updateProfileTask.await() } returns null
        every { mockFirestore.collection("users").document("user123") } returns mockDocRef
        every { mockDocRef.set(any()) } returns setDocTask
        every { setDocTask.await() } returns null
        every { mockAuth.createUserWithEmailAndPassword(email, password) } returns createUserTask

        // Act
        val result = authRepository.signUp(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isSuccess)
    }
}
