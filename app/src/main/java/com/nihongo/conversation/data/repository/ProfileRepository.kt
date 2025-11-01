package com.nihongo.conversation.data.repository

import com.nihongo.conversation.data.local.UserDao
import com.nihongo.conversation.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val userDao: UserDao
) {
    /**
     * Get current user (assumes single user for now)
     */
    fun getCurrentUser(): Flow<User?> {
        return userDao.getUserById(1L)
    }

    /**
     * Get current user immediately
     */
    suspend fun getCurrentUserImmediate(): User? {
        return userDao.getUserById(1L).first()
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(user: User) {
        userDao.updateUser(user)
    }

    /**
     * Create or update user profile
     */
    suspend fun saveProfile(
        name: String,
        avatarId: Int,
        learningGoal: String,
        favoriteScenarios: List<Long>,
        nativeLanguage: String,
        bio: String
    ): Long {
        val currentUser = getCurrentUserImmediate()

        val user = if (currentUser != null) {
            currentUser.copy(
                name = name,
                avatarId = avatarId,
                learningGoal = learningGoal,
                favoriteScenarios = favoriteScenarios.joinToString(","),
                nativeLanguage = nativeLanguage,
                bio = bio
            )
        } else {
            User(
                id = 1L, // Fixed user ID for single user app
                name = name,
                avatarId = avatarId,
                learningGoal = learningGoal,
                favoriteScenarios = favoriteScenarios.joinToString(","),
                nativeLanguage = nativeLanguage,
                bio = bio
            )
        }

        return if (currentUser != null) {
            userDao.updateUser(user)
            user.id
        } else {
            userDao.insertUser(user)
        }
    }

    /**
     * Get favorite scenario IDs
     */
    suspend fun getFavoriteScenarioIds(): List<Long> {
        val user = getCurrentUserImmediate()
        return user?.favoriteScenarios
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()
    }

    /**
     * Check if profile is complete
     */
    suspend fun isProfileComplete(): Boolean {
        val user = getCurrentUserImmediate()
        return user != null && user.name.isNotBlank()
    }

    /**
     * Get personalized AI prompt prefix based on user profile
     */
    suspend fun getPersonalizedPromptPrefix(): String {
        val user = getCurrentUserImmediate() ?: return ""

        val parts = mutableListOf<String>()

        if (user.name.isNotBlank()) {
            parts.add("You are speaking with ${user.name}")
        }

        if (user.learningGoal.isNotBlank()) {
            parts.add("Their learning goal is: ${user.learningGoal}")
        }

        if (user.bio.isNotBlank()) {
            parts.add("About them: ${user.bio}")
        }

        parts.add("Their native language is ${user.nativeLanguage}")

        return if (parts.isNotEmpty()) {
            "\n\nUser Context:\n" + parts.joinToString("\n") + "\n\nTailor your responses to be appropriate for their level and goals.\n"
        } else {
            ""
        }
    }
}
