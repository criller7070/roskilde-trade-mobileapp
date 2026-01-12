package dk.rosswap.mobile.feature.auth.domain

import dk.rosswap.mobile.core.common.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>

    /**
     * Fetches additional user data from Firestore and enriches the User object.
     * Mirrors the React pattern: listen → fetch → enrich → emit state
     *
     * @param baseUser The base user object (from Firebase Auth)
     * @return Enriched User object with Firestore data
     */
    suspend fun enrichUserWithFirestoreData(baseUser: User): User
}