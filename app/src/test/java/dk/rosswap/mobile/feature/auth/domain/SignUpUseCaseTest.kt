package dk.rosswap.mobile.feature.auth.domain

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for SignUpUseCase
 * Verifies the use case properly delegates sign-up operations to the repository
 * 
 * Note: SignUpUseCase is a thin wrapper around AuthRepository.signUp().
 * It does NOT perform validation - that responsibility belongs to the repository.
 */
class SignUpUseCaseTest {
    private lateinit var signUpUseCase: SignUpUseCase
    private val mockAuthRepository: AuthRepository = mockk()

    @Before
    fun setup() {
        signUpUseCase = SignUpUseCase(mockAuthRepository)
    }

    // ==================== HAPPY PATH ====================

    @Test
    fun invoke_withValidData_shouldDelegateToRepository() = runTest {
        // Arrange - Repository returns success
        val email = "newuser@example.com"
        val password = "password123"
        val name = "Test User"
        val hasConsent = true
        coEvery { mockAuthRepository.signUp(email, password, name, hasConsent) } returns Result.success(Unit)

        // Act
        val result = signUpUseCase(email, password, name, hasConsent)

        // Assert - Use case properly delegates and propagates success
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { mockAuthRepository.signUp(email, password, name, hasConsent) }
    }

    // ==================== ERROR HANDLING ====================

    @Test
    fun invoke_whenRepositoryFails_shouldPropagateFailure() = runTest {
        // Arrange - Repository returns failure
        val email = "newuser@example.com"
        val password = "password123"
        val name = "Test User"
        val hasConsent = true
        val exception = Exception("Email already exists")
        coEvery { mockAuthRepository.signUp(email, password, name, hasConsent) } returns Result.failure(exception)

        // Act
        val result = signUpUseCase(email, password, name, hasConsent)

        // Assert - Use case propagates the repository's failure
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { mockAuthRepository.signUp(email, password, name, hasConsent) }
    }

    @Test
    fun invoke_whenRepositoryThrowsException_shouldPropagateError() = runTest {
        // Arrange - Repository throws an exception
        val email = "newuser@example.com"
        val password = "password123"
        val name = "Test User"
        val hasConsent = true
        val exception = RuntimeException("Network error")
        coEvery { mockAuthRepository.signUp(email, password, name, hasConsent) } throws exception

        // Act & Assert
        try {
            signUpUseCase(email, password, name, hasConsent)
            fail("Should have thrown exception")
        } catch (e: Exception) {
            assertEquals(exception, e)
        }
    }

    // ==================== REUSABILITY ====================

    @Test
    fun invoke_multipleTimesWithDifferentData_shouldWorkCorrectly() = runTest {
        // Arrange - Repository accepts any parameters
        coEvery { mockAuthRepository.signUp(any(), any(), any(), any()) } returns Result.success(Unit)

        // Act - First sign up
        val result1 = signUpUseCase("user1@example.com", "pass123", "User One", true)
        assertTrue(result1.isSuccess)

        // Act - Second sign up
        val result2 = signUpUseCase("user2@example.com", "pass456", "User Two", false)
        assertTrue(result2.isSuccess)

        // Assert - Repository was called exactly twice
        coVerify(exactly = 2) { mockAuthRepository.signUp(any(), any(), any(), any()) }
    }

}
