package dk.rosswap.mobile.feature.auth.domain

import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpUseCaseTest {
    private lateinit var signUpUseCase: SignUpUseCase
    private val mockAuth: FirebaseAuth = mockk(relaxed = true)
    private val mockFirestore: FirebaseFirestore = mockk(relaxed = true)

    @Before
    fun setup() {
        // construct use-case but tests below only check input validation which runs before Firebase calls
        signUpUseCase = SignUpUseCase(mockAuth, mockFirestore)
    }

    @Test
    fun invoke_withEmptyName_shouldFailure() = runBlocking {
        // Arrange
        val email = "newuser@example.com"
        val password = "password123"
        val name = ""
        val hasConsent = true

        // Act
        val result = signUpUseCase(name, email, password, hasConsent)

        // Assert
        assertTrue(result.isFailure)
    }

    @Test
    fun invoke_withShortPassword_shouldFailure() = runBlocking {
        val email = "newuser@example.com"
        val password = "123"
        val name = "Test User"
        val hasConsent = true

        val result = signUpUseCase(name, email, password, hasConsent)

        assertTrue(result.isFailure)
    }

    @Test
    fun invoke_withoutAcceptedTerms_shouldFailure() = runBlocking {
        val email = "newuser@example.com"
        val password = "password123"
        val name = "Test User"
        val hasConsent = false

        val result = signUpUseCase(name, email, password, hasConsent)

        assertTrue(result.isFailure)
    }
}
