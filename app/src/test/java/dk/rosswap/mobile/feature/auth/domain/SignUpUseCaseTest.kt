package dk.rosswap.mobile.feature.auth.domain

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class SignUpUseCaseTest {
    private lateinit var signUpUseCase: SignUpUseCase
    private val mockAuthRepository: AuthRepository = mockk()

    @Before
    fun setup() {
        signUpUseCase = SignUpUseCase(mockAuthRepository)
    }

    @Test
    fun invoke_withValidData_shouldCallRepositorySignUp() = runBlocking {
        // Arrange
        val email = "newuser@example.com"
        val password = "password123"
        val name = "Test User"
        val hasConsent = true
        coEvery { mockAuthRepository.signUp(email, password, name, hasConsent) } returns Result.success(Unit)

        // Act
        val result = signUpUseCase(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isSuccess)
        coVerify { mockAuthRepository.signUp(email, password, name, hasConsent) }
    }

    @Test
    fun invoke_whenRepositoryFails_shouldReturnFailure() = runBlocking {
        // Arrange
        val email = "newuser@example.com"
        val password = "password123"
        val name = "Test User"
        val hasConsent = true
        val exception = Exception("Repository error")
        coEvery { mockAuthRepository.signUp(email, password, name, hasConsent) } returns Result.failure(exception)

        // Act
        val result = signUpUseCase(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun invoke_withEmptyEmail_shouldFailure() = runBlocking {
        // Arrange
        val email = ""
        val password = "password123"
        val name = "Test User"
        val hasConsent = true
        val exception = IllegalArgumentException("Email cannot be empty")
        coEvery { mockAuthRepository.signUp(email, password, name, hasConsent) } returns Result.failure(exception)

        // Act
        val result = signUpUseCase(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isFailure)
    }

    @Test
    fun invoke_withEmptyPassword_shouldFailure() = runBlocking {
        // Arrange
        val email = "newuser@example.com"
        val password = ""
        val name = "Test User"
        val hasConsent = true
        val exception = IllegalArgumentException("Password cannot be empty")
        coEvery { mockAuthRepository.signUp(email, password, name, hasConsent) } returns Result.failure(exception)

        // Act
        val result = signUpUseCase(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isFailure)
    }

    @Test
    fun invoke_withEmptyName_shouldFailure() = runBlocking {
        // Arrange
        val email = "newuser@example.com"
        val password = "password123"
        val name = ""
        val hasConsent = true
        val exception = IllegalArgumentException("Name cannot be empty")
        coEvery { mockAuthRepository.signUp(email, password, name, hasConsent) } returns Result.failure(exception)

        // Act
        val result = signUpUseCase(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isFailure)
    }

    @Test
    fun invoke_multipleTimesWithDifferentData_shouldWorkCorrectly() = runBlocking {
        // Arrange
        coEvery { mockAuthRepository.signUp(any(), any(), any(), any()) } returns Result.success(Unit)

        // Act & Assert - First sign up
        val result1 = signUpUseCase("user1@example.com", "pass123", "User One", true)
        assertTrue(result1.isSuccess)

        // Act & Assert - Second sign up
        val result2 = signUpUseCase("user2@example.com", "pass456", "User Two", false)
        assertTrue(result2.isSuccess)

        coVerify(exactly = 2) { mockAuthRepository.signUp(any(), any(), any(), any()) }
    }
}
