package dk.rosswap.mobile.feature.auth.domain

import dk.rosswap.mobile.core.common.User
import javax.inject.Inject

/**
 * Use case for enriching user data with Firestore information.
 * Mirrors the React pattern: fetch → enrich → emit state
 */
class EnrichUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(baseUser: User): User {
        return authRepository.enrichUserWithFirestoreData(baseUser)
    }
}
