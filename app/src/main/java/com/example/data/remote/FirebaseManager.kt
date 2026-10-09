package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.models.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

// Safe query extension that prevents hanging on Realtime Database reads
private suspend fun Query.safeGet(timeoutMs: Long = 8000L): DataSnapshot? {
    return try {
        withTimeoutOrNull(timeoutMs) {
            get().await()
        }
    } catch (e: Exception) {
        Log.w("FirebaseManager", "safeGet error: ${e.message}")
        null
    }
}

class FirebaseManager(private val context: Context? = null) {

    companion object {
        private const val TAG = "FirebaseManager"
        private const val DB_URL = "https://deciplane-tracker-default-rtdb.firebaseio.com"

        @Volatile
        private var isInitialized = false
        @Volatile
        var initializationError: String? = null
            private set

        fun initialize(context: Context): Result<Unit> {
            if (isInitialized) return Result.success(Unit)
            return synchronized(this) {
                if (isInitialized) return@synchronized Result.success(Unit)
                try {
                    val appContext = context.applicationContext
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyD3ov3TudrN_3LURDmA0wb2INkJ9tboblo")
                        .setApplicationId("1:534378051121:web:3a4dab87ed46e235064710")
                        .setProjectId("deciplane-tracker")
                        .setStorageBucket("deciplane-tracker.firebasestorage.app")
                        .setGcmSenderId("534378051121")
                        .setDatabaseUrl(DB_URL)
                        .build()

                    if (FirebaseApp.getApps(appContext).isEmpty()) {
                        FirebaseApp.initializeApp(appContext, options)
                        Log.d(TAG, "FirebaseApp initialized for project: deciplane-tracker (appId: 1:534378051121:web:3a4dab87ed46e235064710)")
                    } else {
                        Log.d(TAG, "FirebaseApp already initialized, using default instance.")
                    }

                    try {
                        val app = FirebaseApp.getInstance()
                        FirebaseDatabase.getInstance(app, DB_URL).setPersistenceEnabled(true)
                    } catch (e: Exception) {
                        Log.d(TAG, "Database persistence configuration: ${e.message}")
                    }

                    isInitialized = true
                    initializationError = null
                    Result.success(Unit)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize FirebaseApp: ${e.message}", e)
                    initializationError = e.message
                    Result.failure(e)
                }
            }
        }

        fun parseFirebaseAuthError(tag: String, e: Throwable): String {
            val errorCode = if (e is FirebaseAuthException) e.errorCode else "UNKNOWN"
            val rawMsg = e.message ?: "Authentication error"
            val customData = if (e is FirebaseAuthMultiFactorException) e.resolver?.toString() else "null"
            val credential = if (e is FirebaseAuthUserCollisionException) e.updatedCredential?.toString() else "null"

            Log.e(
                TAG,
                """
                |=== FIREBASE AUTHENTICATION ERROR [$tag] ===
                |  error.code: $errorCode
                |  error.message: $rawMsg
                |  error.customData: $customData
                |  error.credential: $credential
                |==============================================
                """.trimMargin(),
                e
            )

            return when {
                errorCode == "ERROR_INVALID_CUSTOM_TOKEN" || rawMsg.contains("auth/invalid-custom-token") ->
                    "Invalid authentication token (auth/invalid-custom-token)."
                errorCode == "ERROR_CUSTOM_TOKEN_MISMATCH" || rawMsg.contains("auth/custom-token-mismatch") ->
                    "Authentication token mismatch (auth/custom-token-mismatch)."
                errorCode == "ERROR_INVALID_CREDENTIAL" || rawMsg.contains("auth/invalid-credential") || rawMsg.contains("The supplied auth credential is incorrect") ->
                    "Invalid credentials or account not found. If you don't have an account yet, tap below to create one (auth/invalid-credential)."
                errorCode == "ERROR_WRONG_PASSWORD" || rawMsg.contains("auth/wrong-password") ->
                    "Incorrect password. Please try again or reset your password (auth/wrong-password)."
                errorCode == "ERROR_USER_NOT_FOUND" || rawMsg.contains("auth/user-not-found") ->
                    "No account found with this email. Please switch to Sign Up to create one (auth/user-not-found)."
                errorCode == "ERROR_USER_DISABLED" || rawMsg.contains("auth/user-disabled") ->
                    "This account has been disabled. Please contact support (auth/user-disabled)."
                errorCode == "ERROR_EMAIL_ALREADY_IN_USE" || rawMsg.contains("auth/email-already-in-use") ->
                    "This email address is already in use by another account (auth/email-already-in-use)."
                errorCode == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" || rawMsg.contains("auth/account-exists-with-different-credential") ->
                    "An account already exists with the same email using a different sign-in method (auth/account-exists-with-different-credential)."
                errorCode == "ERROR_OPERATION_NOT_ALLOWED" || rawMsg.contains("auth/operation-not-allowed") ->
                    "Email/Password sign-in is disabled in your Firebase console. Enable it under Firebase Auth -> Sign-in method (auth/operation-not-allowed)."
                errorCode == "ERROR_WEAK_PASSWORD" || rawMsg.contains("auth/weak-password") ->
                    "The password is too weak. Please use at least 6 characters (auth/weak-password)."
                errorCode == "ERROR_INVALID_EMAIL" || rawMsg.contains("auth/invalid-email") ->
                    "The email address is formatted incorrectly (auth/invalid-email)."
                errorCode == "ERROR_MISSING_EMAIL" || rawMsg.contains("auth/missing-email") ->
                    "Please enter an email address (auth/missing-email)."
                errorCode == "ERROR_TOO_MANY_REQUESTS" || rawMsg.contains("auth/too-many-requests") ->
                    "Too many requests. Please wait a moment and try again (auth/too-many-requests)."
                errorCode == "ERROR_NETWORK_REQUEST_FAILED" || rawMsg.contains("auth/network-request-failed") ->
                    "Network error. Please verify your internet connection (auth/network-request-failed)."
                else -> {
                    val codeStr = if (errorCode != "UNKNOWN") " ($errorCode)" else ""
                    "${e.localizedMessage ?: "Authentication failed."}$codeStr"
                }
            }
        }
    }

    // Lazy / background initialization via companion object initialize()

    private val app: FirebaseApp?
        get() = try {
            FirebaseApp.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseApp.getInstance() error: ${e.message}", e)
            null
        }

    private val auth: FirebaseAuth?
        get() = try {
            val fApp = app
            if (fApp != null) FirebaseAuth.getInstance(fApp) else FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseAuth instance error: ${e.message}", e)
            null
        }

    private val database: FirebaseDatabase?
        get() = try {
            val fApp = app
            if (fApp != null) FirebaseDatabase.getInstance(fApp, DB_URL) else FirebaseDatabase.getInstance(DB_URL)
        } catch (e: Exception) {
            try {
                FirebaseDatabase.getInstance()
            } catch (e2: Exception) {
                Log.e(TAG, "FirebaseDatabase instance error: ${e2.message}", e2)
                null
            }
        }

    fun getCurrentFirebaseUser(): FirebaseUser? = auth?.currentUser

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val authInstance = auth
        if (authInstance == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        // Emit current user immediately so observers do not wait for the async callback
        trySend(authInstance.currentUser)

        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            trySend(fbAuth.currentUser)
        }
        authInstance.addAuthStateListener(listener)
        awaitClose { authInstance.removeAuthStateListener(listener) }
    }

    // --- Authentication ---
    suspend fun signUpWithEmail(email: String, pass: String): Result<String> {
        return try {
            val cleanEmail = email.trim().lowercase()
            val authInstance = auth ?: throw IllegalStateException("Firebase Auth is not available")
            val authResult = authInstance.createUserWithEmailAndPassword(cleanEmail, pass).await()
            val uid = authResult.user?.uid ?: throw IllegalStateException("No user ID generated after signup")
            Log.d(TAG, "Sign up successful for user: $uid")
            Result.success(uid)
        } catch (e: Exception) {
            val formattedMessage = parseFirebaseAuthError("signUpWithEmail", e)
            Result.failure(Exception(formattedMessage, e))
        }
    }

    suspend fun loginWithEmail(email: String, pass: String): Result<String> {
        return try {
            val cleanEmail = email.trim().lowercase()
            val authInstance = auth ?: throw IllegalStateException("Firebase Auth is not available")
            val authResult = authInstance.signInWithEmailAndPassword(cleanEmail, pass).await()
            val uid = authResult.user?.uid ?: throw IllegalStateException("Sign in failed: no user ID")
            Log.d(TAG, "Login successful for user: $uid")
            Result.success(uid)
        } catch (e: Exception) {
            val formattedMessage = parseFirebaseAuthError("loginWithEmail", e)
            Result.failure(Exception(formattedMessage, e))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            val cleanEmail = email.trim().lowercase()
            val authInstance = auth ?: throw IllegalStateException("Firebase Auth is not available")
            authInstance.sendPasswordResetEmail(cleanEmail).await()
            Log.d(TAG, "Password reset email sent to: $cleanEmail")
            Result.success(Unit)
        } catch (e: Exception) {
            val formattedMessage = parseFirebaseAuthError("sendPasswordReset", e)
            Result.failure(Exception(formattedMessage, e))
        }
    }

    fun logout() {
        try {
            auth?.signOut()
            Log.d(TAG, "Firebase user logged out successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Logout error: ${e.message}", e)
        }
    }

    // --- Username Validation (Realtime Database) ---
    suspend fun isUsernameAvailable(username: String, currentUid: String): Boolean {
        val db = database ?: return true
        return try {
            val cleanUsername = username.trim().lowercase()
            val snapshot = db.reference.child("usernames").child(cleanUsername).safeGet(4000L)
            if (snapshot != null && snapshot.exists()) {
                val existingUid = snapshot.getValue(String::class.java)
                if (existingUid != null && existingUid != currentUid) return false
            }
            val publicSnap = db.reference.child("publicUsers").child(cleanUsername).safeGet(4000L)
            if (publicSnap != null && publicSnap.exists()) {
                val existingUid = publicSnap.child("uid").getValue(String::class.java)
                if (existingUid != null && existingUid != currentUid) return false
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Username check error: ${e.message}. Proceeding with local validation.")
            true
        }
    }

    suspend fun releaseOldUsername(oldUsername: String, currentUid: String) {
        val db = database ?: return
        try {
            val cleanOld = oldUsername.trim().lowercase()
            if (cleanOld.isNotBlank()) {
                // Delete old public profile index immediately
                try {
                    db.reference.child("publicUsers").child(cleanOld).removeValue().await()
                    Log.d(TAG, "Removed old public profile index: @$cleanOld")
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing old public profile: ${e.message}")
                }

                // Delete old username reverse index
                try {
                    val snapshot = db.reference.child("usernames").child(cleanOld).safeGet(4000L)
                    if (snapshot != null && snapshot.exists()) {
                        val existingUid = snapshot.getValue(String::class.java)
                        if (existingUid == currentUid) {
                            db.reference.child("usernames").child(cleanOld).removeValue().await()
                            Log.d(TAG, "Released old username: @$cleanOld")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error releasing old username index: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing old username: ${e.message}")
        }
    }

    // --- User Profile (Realtime Database: users/$uid/profile & leaderboard/$uid) ---
    suspend fun syncUserProfileToCloud(user: UserAccount) {
        if (user.uid.startsWith("guest_")) return
        val db = database ?: return
        try {
            val userRef = db.reference.child("users").child(user.uid)
            val profileMap = hashMapOf<String, Any?>(
                "uid" to user.uid,
                "email" to user.email,
                "username" to user.username,
                "username_lower" to user.username.lowercase(),
                "displayName" to user.displayName,
                "bio" to user.bio,
                "photoUrl" to user.photoUrl,
                "disciplineScore" to user.disciplineScore,
                "currentStreak" to user.currentStreak,
                "bestStreak" to user.bestStreak,
                "completionRate" to user.completionRate.toDouble(),
                "isProfileComplete" to user.isProfileComplete,
                "publicProfileEnabled" to user.publicProfileEnabled,
                "leaderboardEnabled" to user.leaderboardEnabled,
                "createdAt" to user.createdAt,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            userRef.child("profile").updateChildren(profileMap).await()

            // Save username reverse index for uniqueness check
            if (user.username.isNotBlank()) {
                db.reference.child("usernames").child(user.username.lowercase()).setValue(user.uid).await()
            }

            // Sync user streak & progress nodes
            userRef.child("streaks").updateChildren(
                mapOf(
                    "currentStreak" to user.currentStreak,
                    "bestStreak" to user.bestStreak,
                    "updatedAt" to ServerValue.TIMESTAMP
                )
            ).await()

            userRef.child("progress").updateChildren(
                mapOf(
                    "disciplineScore" to user.disciplineScore,
                    "completionRate" to user.completionRate.toDouble(),
                    "updatedAt" to ServerValue.TIMESTAMP
                )
            ).await()

            // Sync or clean up public leaderboard record based on optional participation
            syncLeaderboardEntry(user)
        } catch (e: Exception) {
            Log.w(TAG, "Profile sync error: ${e.message}")
        }
    }

    suspend fun syncLeaderboardEntry(user: UserAccount) {
        val db = database ?: return
        if (user.uid.startsWith("guest_") || user.username.isBlank()) return
        try {
            val leaderboardRef = db.reference.child("leaderboard").child(user.uid)
            if (user.leaderboardEnabled && user.isProfileComplete) {
                val leaderboardMap = hashMapOf<String, Any?>(
                    "uid" to user.uid,
                    "username" to user.username.trim().lowercase(),
                    "displayName" to user.displayName.ifBlank { user.username },
                    "photoUrl" to user.photoUrl,
                    "disciplineScore" to user.disciplineScore,
                    "currentStreak" to user.currentStreak,
                    "leaderboardEnabled" to true,
                    "updatedAt" to ServerValue.TIMESTAMP
                )
                leaderboardRef.updateChildren(leaderboardMap).await()
                Log.d(TAG, "Synced leaderboard entry for @${user.username} with score ${user.disciplineScore}")
            } else {
                leaderboardRef.removeValue().await()
                Log.d(TAG, "Removed user ${user.uid} from leaderboard (participation is OFF)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Leaderboard sync error: ${e.message}")
        }
    }

    suspend fun fetchGlobalLeaderboard(limit: Int = 100): List<LeaderboardEntry> {
        val db = database ?: return emptyList()
        return try {
            val snapshot = db.reference.child("leaderboard")
                .orderByChild("disciplineScore")
                .limitToLast(limit)
                .safeGet(4000L) ?: return emptyList()

            if (!snapshot.exists()) return emptyList()

            val rawList = mutableListOf<LeaderboardEntry>()
            for (child in snapshot.children) {
                val uid = child.child("uid").getValue(String::class.java) ?: child.key ?: continue
                val isEnabled = child.child("leaderboardEnabled").getValue(Boolean::class.java) ?: true
                if (!isEnabled) continue

                val score = child.child("disciplineScore").getValue(Long::class.java)?.toInt() ?: 0
                val streak = child.child("currentStreak").getValue(Long::class.java)?.toInt() ?: 0
                val username = child.child("username").getValue(String::class.java) ?: ""
                val displayName = child.child("displayName").getValue(String::class.java) ?: username
                val photoUrl = child.child("photoUrl").getValue(String::class.java) ?: ""
                val updatedAt = child.child("updatedAt").getValue(Long::class.java) ?: 0L

                if (username.isNotBlank()) {
                    rawList.add(
                        LeaderboardEntry(
                            uid = uid,
                            username = username,
                            displayName = displayName,
                            photoUrl = photoUrl,
                            disciplineScore = score,
                            currentStreak = streak,
                            leaderboardEnabled = true,
                            updatedAt = updatedAt
                        )
                    )
                }
            }

            // Rank users deterministically according to Step 5:
            // 1. Discipline Score descending
            // 2. Current streak descending
            // 3. Username deterministic tie-breaker
            val sortedList = rawList.sortedWith(
                compareByDescending<LeaderboardEntry> { it.disciplineScore }
                    .thenByDescending { it.currentStreak }
                    .thenBy { it.username.lowercase() }
            )

            sortedList.mapIndexed { index, entry ->
                entry.copy(rank = index + 1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch leaderboard error: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchUserLeaderboardRank(uid: String, score: Int, streak: Int): Int? {
        val db = database ?: return null
        return try {
            // Retrieve all leaderboard participants to compute exact deterministic rank
            val topList = fetchGlobalLeaderboard(500)
            val match = topList.find { it.uid == uid }
            match?.rank
        } catch (e: Exception) {
            null
        }
    }

    suspend fun toggleLeaderboardSetting(userId: String, enabled: Boolean) {
        val db = database ?: return
        try {
            db.reference.child("users").child(userId).child("profile").child("leaderboardEnabled").setValue(enabled).await()
            if (!enabled) {
                db.reference.child("leaderboard").child(userId).removeValue().await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Toggle leaderboard setting error: ${e.message}")
        }
    }

    suspend fun fetchUserProfile(uid: String): UserAccount? {
        val db = database ?: return null
        return try {
            val snapshot = db.reference.child("users").child(uid).child("profile").safeGet(3500L) ?: return null
            if (!snapshot.exists()) return null

            UserAccount(
                uid = snapshot.child("uid").getValue(String::class.java) ?: uid,
                email = snapshot.child("email").getValue(String::class.java) ?: "",
                username = snapshot.child("username").getValue(String::class.java) ?: "",
                displayName = snapshot.child("displayName").getValue(String::class.java) ?: "",
                bio = snapshot.child("bio").getValue(String::class.java) ?: "",
                photoUrl = snapshot.child("photoUrl").getValue(String::class.java) ?: "",
                createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis(),
                disciplineScore = (snapshot.child("disciplineScore").getValue(Long::class.java)?.toInt()) ?: 0,
                currentStreak = (snapshot.child("currentStreak").getValue(Long::class.java)?.toInt()) ?: 0,
                bestStreak = (snapshot.child("bestStreak").getValue(Long::class.java)?.toInt()) ?: 0,
                completionRate = (snapshot.child("completionRate").getValue(Double::class.java)?.toFloat()) ?: 0f,
                isProfileComplete = snapshot.child("isProfileComplete").getValue(Boolean::class.java) ?: false,
                publicProfileEnabled = snapshot.child("publicProfileEnabled").getValue(Boolean::class.java) ?: false,
                leaderboardEnabled = snapshot.child("leaderboardEnabled").getValue(Boolean::class.java) ?: false
            )
        } catch (e: Exception) {
            Log.w(TAG, "Fetch profile error: ${e.message}")
            null
        }
    }

    // --- Public Profile & Discovery System (/publicUsers/{username}) ---
    suspend fun syncPublicUserProfile(user: UserAccount, achievements: List<Achievement> = emptyList()) {
        if (user.uid.startsWith("guest_")) {
            return
        }
        val cleanUsername = user.username.trim().lowercase()
        if (cleanUsername.isBlank()) {
            return
        }
        val db = database ?: return
        val currentFirebaseUser = getCurrentFirebaseUser() ?: return
        if (currentFirebaseUser.uid != user.uid) {
            Log.w(TAG, "Sync public profile skipped: signed in as ${currentFirebaseUser.uid} but profile is ${user.uid}")
            return
        }

        val publicRef = db.reference.child("publicUsers").child(cleanUsername)

        if (!user.publicProfileEnabled) {
            // When OFF: user is not discoverable and cannot be opened in public profile view
            try {
                publicRef.removeValue().await()
                Log.d(TAG, "Removed public profile from discovery for @$cleanUsername (publicProfileEnabled is OFF)")
            } catch (e: Exception) {
                Log.w(TAG, "Notice: removing public profile from discovery: ${e.message}")
            }
            return
        }

        // When ON: store ONLY approved public fields.
        // NO email, NO password, NO habit names, NO water records, NO focus session details, NO night reviews, NO notes!
        val publicAchievementsMap = hashMapOf<String, Any>()
        achievements.filter { it.isUnlocked }.forEach { ach ->
            publicAchievementsMap[ach.id] = hashMapOf(
                "id" to ach.id,
                "title" to ach.title,
                "description" to ach.description,
                "icon" to ach.icon,
                "unlockedAt" to (ach.unlockedAt ?: 0L)
            )
        }

        val publicData = hashMapOf<String, Any?>(
            "uid" to user.uid,
            "username" to cleanUsername,
            "displayName" to user.displayName.ifBlank { cleanUsername },
            "bio" to user.bio,
            "photoUrl" to user.photoUrl,
            "photoURL" to user.photoUrl,
            "publicProfileEnabled" to true,
            "currentStreak" to user.currentStreak,
            "streak" to user.currentStreak,
            "bestStreak" to user.bestStreak,
            "disciplineScore" to user.disciplineScore,
            "updatedAt" to ServerValue.TIMESTAMP
        )
        if (publicAchievementsMap.isNotEmpty()) {
            publicData["publicAchievements"] = publicAchievementsMap
        }

        try {
            publicRef.setValue(publicData).await()
            Log.d(TAG, "Synced approved public profile for @$cleanUsername (uid=${user.uid})")
        } catch (e: Exception) {
            Log.w(TAG, "Notice: public profile write failed for @$cleanUsername: ${e.message}")
        }
    }

    suspend fun searchPublicUsers(query: String): List<PublicUserProfile> {
        val db = database ?: return emptyList()
        val cleanQuery = query.trim().trimStart('@').trim().lowercase()

        return try {
            val resultsMap = linkedMapOf<String, PublicUserProfile>()

            if (cleanQuery.isBlank()) {
                // If query is blank, fetch community athletes with public profile enabled
                val topSnap = db.reference.child("publicUsers")
                    .orderByChild("disciplineScore")
                    .limitToLast(20)
                    .safeGet(8000L)

                if (topSnap != null && topSnap.exists()) {
                    for (child in topSnap.children) {
                        val isPublic = child.child("publicProfileEnabled").getValue(Boolean::class.java) ?: false
                        if (!isPublic) continue
                        val profile = parsePublicUserProfile(child) ?: continue
                        resultsMap[profile.uid] = profile
                    }
                }
                return resultsMap.values.sortedByDescending { it.disciplineScore }
            }

            // 1. Direct single lookup for exact @username or username
            val directSnap = db.reference.child("publicUsers").child(cleanQuery).safeGet(8000L)
            if (directSnap != null && directSnap.exists()) {
                val isPublic = directSnap.child("publicProfileEnabled").getValue(Boolean::class.java) ?: false
                if (isPublic) {
                    val profile = parsePublicUserProfile(directSnap)
                    if (profile != null) {
                        resultsMap[profile.uid] = profile
                    }
                }
            }

            // 2. Prefix search on normalized username index
            val prefixSnap = db.reference.child("publicUsers")
                .orderByKey()
                .startAt(cleanQuery)
                .endAt(cleanQuery + "\uf8ff")
                .limitToFirst(20)
                .safeGet(8000L)

            if (prefixSnap != null && prefixSnap.exists()) {
                for (child in prefixSnap.children) {
                    val isPublic = child.child("publicProfileEnabled").getValue(Boolean::class.java) ?: false
                    if (!isPublic) continue
                    val profile = parsePublicUserProfile(child) ?: continue
                    resultsMap[profile.uid] = profile
                }
            }

            resultsMap.values.sortedWith(
                compareByDescending<PublicUserProfile> { it.username.equals(cleanQuery, ignoreCase = true) }
                    .thenByDescending { it.disciplineScore }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Search public users notice: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchPublicProfileByUsername(username: String): PublicUserProfile? {
        val db = database ?: return null
        val cleanUsername = username.trim().trimStart('@').trim().lowercase()
        if (cleanUsername.isBlank()) return null
        return try {
            val snapshot = db.reference.child("publicUsers").child(cleanUsername).safeGet(8000L) ?: return null
            if (!snapshot.exists()) return null
            val isPublic = snapshot.child("publicProfileEnabled").getValue(Boolean::class.java) ?: false
            if (!isPublic) return null // Strict: Cannot be opened if publicProfileEnabled is false
            parsePublicUserProfile(snapshot)
        } catch (e: Exception) {
            Log.w(TAG, "Fetch public profile error: ${e.message}")
            null
        }
    }

    private fun parsePublicUserProfile(snapshot: DataSnapshot): PublicUserProfile? {
        val uid = snapshot.child("uid").getValue(String::class.java) ?: return null
        val username = snapshot.child("username").getValue(String::class.java) ?: snapshot.key ?: ""
        val displayName = snapshot.child("displayName").getValue(String::class.java) ?: username
        val bio = snapshot.child("bio").getValue(String::class.java) ?: ""
        val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)
            ?: snapshot.child("photoURL").getValue(String::class.java) ?: ""
        val publicProfileEnabled = snapshot.child("publicProfileEnabled").getValue(Boolean::class.java) ?: false
        val streak = snapshot.child("currentStreak").getValue(Long::class.java)?.toInt()
            ?: snapshot.child("streak").getValue(Long::class.java)?.toInt() ?: 0
        val bestStreak = snapshot.child("bestStreak").getValue(Long::class.java)?.toInt() ?: streak
        val disciplineScore = snapshot.child("disciplineScore").getValue(Long::class.java)?.toInt() ?: 0
        val updatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: 0L

        val achievementsList = mutableListOf<PublicAchievement>()
        val achSnap = snapshot.child("publicAchievements")
        if (achSnap.exists()) {
            for (aChild in achSnap.children) {
                val id = aChild.child("id").getValue(String::class.java) ?: aChild.key ?: ""
                val title = aChild.child("title").getValue(String::class.java) ?: ""
                val desc = aChild.child("description").getValue(String::class.java) ?: ""
                val icon = aChild.child("icon").getValue(String::class.java) ?: "emoji_events"
                val unlockedAt = aChild.child("unlockedAt").getValue(Long::class.java)
                if (title.isNotBlank()) {
                    achievementsList.add(PublicAchievement(id, title, desc, icon, unlockedAt))
                }
            }
        }

        return PublicUserProfile(
            uid = uid,
            username = username,
            displayName = displayName,
            bio = bio,
            photoUrl = photoUrl,
            publicProfileEnabled = publicProfileEnabled,
            streak = streak,
            bestStreak = bestStreak,
            disciplineScore = disciplineScore,
            publicAchievements = achievementsList,
            updatedAt = updatedAt
        )
    }

    // --- Habits (Realtime Database: users/$userId/habits/$habitId) ---
    suspend fun syncHabitToCloud(userId: String, habit: Habit) {
        val db = database ?: return
        try {
            val habitMap = hashMapOf<String, Any?>(
                "id" to habit.id,
                "userId" to habit.userId,
                "title" to habit.title,
                "description" to habit.description,
                "category" to habit.category.name,
                "iconName" to habit.iconName,
                "colorHex" to habit.colorHex,
                "reminderTime" to habit.reminderTime,
                "repeatDays" to habit.repeatDays,
                "goalValue" to habit.goalValue,
                "goalUnit" to habit.goalUnit,
                "isPaused" to habit.isPaused,
                "paused" to habit.isPaused,
                "active" to !habit.isPaused,
                "durationMinutes" to habit.durationMinutes,
                "startDate" to habit.startDate,
                "endDate" to habit.endDate,
                "difficulty" to habit.difficulty.name,
                "notes" to habit.notes,
                "goalType" to habit.goalType.name,
                "isMorningRoutine" to habit.isMorningRoutine,
                "createdAt" to habit.createdAt,
                "orderIndex" to habit.orderIndex,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            db.reference.child("users").child(userId).child("habits").child(habit.id).updateChildren(habitMap).await()
        } catch (e: Exception) {
            Log.w(TAG, "Habit sync error: ${e.message}")
        }
    }

    suspend fun deleteHabitFromCloud(userId: String, habitId: String) {
        val db = database ?: return
        try {
            db.reference.child("users").child(userId).child("habits").child(habitId).removeValue().await()
        } catch (e: Exception) {
            Log.w(TAG, "Habit deletion error: ${e.message}")
        }
    }

    suspend fun fetchUserHabits(userId: String): List<Habit> {
        val db = database ?: return emptyList()
        return try {
            val snapshot = db.reference.child("users").child(userId).child("habits").safeGet(3000L) ?: return emptyList()
            if (!snapshot.exists()) return emptyList()

            val habits = mutableListOf<Habit>()
            for (child in snapshot.children) {
                val categoryStr = child.child("category").getValue(String::class.java) ?: "HEALTH"
                val cat = try { HabitCategory.valueOf(categoryStr) } catch (e: Exception) { HabitCategory.HEALTH }

                val diffStr = child.child("difficulty").getValue(String::class.java) ?: "MEDIUM"
                val diff = try { HabitDifficulty.valueOf(diffStr) } catch (e: Exception) { HabitDifficulty.MEDIUM }

                val goalTypeStr = child.child("goalType").getValue(String::class.java) ?: "ONCE"
                val gType = try { HabitGoalType.valueOf(goalTypeStr) } catch (e: Exception) { HabitGoalType.ONCE }

                val isPausedVal = child.child("isPaused").getValue(Boolean::class.java)
                    ?: child.child("paused").getValue(Boolean::class.java)
                    ?: false

                val habit = Habit(
                    id = child.child("id").getValue(String::class.java) ?: child.key ?: "",
                    userId = child.child("userId").getValue(String::class.java) ?: userId,
                    title = child.child("title").getValue(String::class.java) ?: "",
                    description = child.child("description").getValue(String::class.java) ?: "",
                    category = cat,
                    iconName = child.child("iconName").getValue(String::class.java) ?: "check_circle",
                    colorHex = child.child("colorHex").getValue(Long::class.java) ?: 0xFF10B981,
                    reminderTime = child.child("reminderTime").getValue(String::class.java),
                    repeatDays = child.child("repeatDays").getValue(String::class.java) ?: "1,2,3,4,5,6,7",
                    goalValue = child.child("goalValue").getValue(Long::class.java)?.toInt() ?: 1,
                    goalUnit = child.child("goalUnit").getValue(String::class.java) ?: "times",
                    isPaused = isPausedVal,
                    createdAt = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis(),
                    orderIndex = child.child("orderIndex").getValue(Long::class.java)?.toInt() ?: 0,
                    durationMinutes = child.child("durationMinutes").getValue(Long::class.java)?.toInt() ?: 0,
                    startDate = child.child("startDate").getValue(String::class.java),
                    endDate = child.child("endDate").getValue(String::class.java),
                    difficulty = diff,
                    notes = child.child("notes").getValue(String::class.java) ?: "",
                    goalType = gType,
                    isMorningRoutine = child.child("isMorningRoutine").getValue(Boolean::class.java) ?: false,
                    updatedAt = child.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                )
                if (habit.id.isNotBlank() && habit.title.isNotBlank()) {
                    habits.add(habit)
                }
            }
            habits
        } catch (e: Exception) {
            Log.w(TAG, "Fetch habits error: ${e.message}")
            emptyList()
        }
    }

    // --- Habit Completions (Realtime Database: users/$userId/completions/$habitId_$date) ---
    suspend fun syncCompletionToCloud(userId: String, completion: HabitCompletion) {
        val db = database ?: return
        try {
            val key = "${completion.habitId}_${completion.date}"
            val compMap = hashMapOf<String, Any?>(
                "habitId" to completion.habitId,
                "userId" to completion.userId,
                "date" to completion.date,
                "status" to completion.status.name,
                "completedAt" to completion.completedAt,
                "numericProgress" to completion.numericProgress,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            db.reference.child("users").child(userId).child("completions").child(key).updateChildren(compMap).await()
        } catch (e: Exception) {
            Log.w(TAG, "Completion sync error: ${e.message}")
        }
    }

    suspend fun deleteCompletionFromCloud(userId: String, habitId: String, date: String) {
        val db = database ?: return
        try {
            val key = "${habitId}_${date}"
            db.reference.child("users").child(userId).child("completions").child(key).removeValue().await()
        } catch (e: Exception) {
            Log.w(TAG, "Completion deletion error: ${e.message}")
        }
    }

    suspend fun fetchUserCompletions(userId: String): List<HabitCompletion> {
        val db = database ?: return emptyList()
        return try {
            val snapshot = db.reference.child("users").child(userId).child("completions").safeGet(3000L) ?: return emptyList()
            if (!snapshot.exists()) return emptyList()

            val list = mutableListOf<HabitCompletion>()
            for (child in snapshot.children) {
                val statusStr = child.child("status").getValue(String::class.java) ?: "COMPLETED"
                val status = try { CompletionStatus.valueOf(statusStr) } catch (e: Exception) { CompletionStatus.COMPLETED }

                val comp = HabitCompletion(
                    habitId = child.child("habitId").getValue(String::class.java) ?: "",
                    userId = child.child("userId").getValue(String::class.java) ?: userId,
                    date = child.child("date").getValue(String::class.java) ?: "",
                    status = status,
                    completedAt = child.child("completedAt").getValue(Long::class.java) ?: System.currentTimeMillis(),
                    numericProgress = child.child("numericProgress").getValue(Long::class.java)?.toInt() ?: 1
                )
                if (comp.habitId.isNotBlank() && comp.date.isNotBlank()) {
                    list.add(comp)
                }
            }
            list
        } catch (e: Exception) {
            Log.w(TAG, "Fetch completions error: ${e.message}")
            emptyList()
        }
    }

    // --- Water Records (Realtime Database: users/$userId/water/$date) ---
    suspend fun syncWaterToCloud(userId: String, record: WaterRecord) {
        val db = database ?: return
        try {
            val map = hashMapOf<String, Any?>(
                "userId" to record.userId,
                "date" to record.date,
                "currentMl" to record.currentMl,
                "targetMl" to record.targetMl,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            db.reference.child("users").child(userId).child("water").child(record.date).updateChildren(map).await()
        } catch (e: Exception) {
            Log.w(TAG, "Water sync error: ${e.message}")
        }
    }

    suspend fun syncWaterEntryToCloud(userId: String, entry: WaterEntry) {
        val db = database ?: return
        try {
            val map = hashMapOf<String, Any?>(
                "id" to entry.id,
                "userId" to entry.userId,
                "date" to entry.date,
                "amountMl" to entry.amountMl,
                "timestamp" to entry.timestamp
            )
            db.reference.child("users").child(userId).child("water").child(entry.date)
                .child("entries").child(entry.id).updateChildren(map).await()
        } catch (e: Exception) {
            Log.w(TAG, "Water entry sync error: ${e.message}")
        }
    }

    suspend fun deleteWaterEntryFromCloud(userId: String, date: String, entryId: String) {
        val db = database ?: return
        try {
            db.reference.child("users").child(userId).child("water").child(date)
                .child("entries").child(entryId).removeValue().await()
        } catch (e: Exception) {
            Log.w(TAG, "Delete water entry error: ${e.message}")
        }
    }

    // --- Water Goal Setting (Realtime Database: users/$userId/settings/waterGoal) ---
    suspend fun saveWaterGoalSetting(userId: String, goalMl: Int) {
        if (userId.startsWith("guest_")) return
        val db = database ?: return
        try {
            // Save selected goal permanently at /users/{uid}/settings/waterGoal
            db.reference.child("users").child(userId).child("settings").child("waterGoal").setValue(goalMl).await()
            // Also update /users/{userId}/water/goalMl for backwards compatibility
            db.reference.child("users").child(userId).child("water").child("goalMl").setValue(goalMl).await()
            Log.d(TAG, "Permanent water goal ($goalMl ml) saved to users/$userId/settings/waterGoal")
        } catch (e: Exception) {
            Log.w(TAG, "Save water goal error: ${e.message}")
        }
    }

    suspend fun fetchWaterGoalSetting(userId: String): Int? {
        if (userId.startsWith("guest_")) return null
        val db = database ?: return null
        return try {
            // Primary path: /users/{uid}/settings/waterGoal
            val snap = db.reference.child("users").child(userId).child("settings").child("waterGoal").safeGet(2000L)
            if (snap != null && snap.exists()) {
                snap.getValue(Long::class.java)?.toInt() ?: snap.child("goalMl").getValue(Long::class.java)?.toInt()
            } else {
                // Fallback to legacy path if present
                val legacySnap = db.reference.child("users").child(userId).child("water").child("goalMl").safeGet(2000L)
                legacySnap?.getValue(Long::class.java)?.toInt()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch water goal setting error: ${e.message}")
            null
        }
    }

    suspend fun saveDefaultWaterGoalToCloud(userId: String, goalMl: Int) {
        saveWaterGoalSetting(userId, goalMl)
    }

    suspend fun fetchDefaultWaterGoal(userId: String): Int? {
        return fetchWaterGoalSetting(userId)
    }

    suspend fun fetchUserWaterData(userId: String): Pair<List<WaterRecord>, List<WaterEntry>> {
        val db = database ?: return Pair(emptyList(), emptyList())
        return try {
            val snapshot = db.reference.child("users").child(userId).child("water").safeGet(3000L) ?: return Pair(emptyList(), emptyList())
            if (!snapshot.exists()) return Pair(emptyList(), emptyList())

            val records = mutableListOf<WaterRecord>()
            val entries = mutableListOf<WaterEntry>()

            for (child in snapshot.children) {
                val key = child.key ?: continue
                if (key == "goalMl" || key == "updatedAt") continue

                val date = child.child("date").getValue(String::class.java) ?: key
                val currentMl = child.child("currentMl").getValue(Long::class.java)?.toInt() ?: 0
                val targetMl = child.child("targetMl").getValue(Long::class.java)?.toInt() ?: 0

                records.add(WaterRecord(userId = userId, date = date, currentMl = currentMl, targetMl = targetMl))

                val entriesNode = child.child("entries")
                if (entriesNode.exists()) {
                    for (entryChild in entriesNode.children) {
                        val entryId = entryChild.child("id").getValue(String::class.java) ?: entryChild.key ?: ""
                        val amount = entryChild.child("amountMl").getValue(Long::class.java)?.toInt() ?: 0
                        val timestamp = entryChild.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                        if (entryId.isNotBlank() && amount > 0) {
                            entries.add(
                                WaterEntry(
                                    id = entryId,
                                    userId = userId,
                                    date = date,
                                    amountMl = amount,
                                    timestamp = timestamp
                                )
                            )
                        }
                    }
                }
            }
            Pair(records, entries)
        } catch (e: Exception) {
            Log.w(TAG, "Fetch water data error: ${e.message}")
            Pair(emptyList(), emptyList())
        }
    }

    suspend fun fetchUserWaterRecords(userId: String): List<WaterRecord> {
        return fetchUserWaterData(userId).first
    }

    // --- Water Reminders Settings (Realtime Database: users/$userId/settings/waterReminders) ---
    suspend fun syncWaterReminderSettings(userId: String, settings: WaterReminderSettings) {
        val db = database ?: return
        try {
            val map = hashMapOf<String, Any?>(
                "enabled" to settings.isEnabled,
                "intervalMinutes" to settings.intervalMinutes,
                "startTime" to settings.startTime,
                "endTime" to settings.endTime,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            db.reference.child("users").child(userId).child("settings").child("waterReminders").updateChildren(map).await()
            Log.d(TAG, "Water reminder settings synced for user: $userId")
        } catch (e: Exception) {
            Log.w(TAG, "Water reminder settings sync error: ${e.message}")
        }
    }

    suspend fun fetchWaterReminderSettings(userId: String): WaterReminderSettings? {
        val db = database ?: return null
        return try {
            val snap = db.reference.child("users").child(userId).child("settings").child("waterReminders").safeGet(2000L) ?: return null
            if (!snap.exists()) return null
            WaterReminderSettings(
                isEnabled = snap.child("enabled").getValue(Boolean::class.java) ?: false,
                intervalMinutes = snap.child("intervalMinutes").getValue(Long::class.java)?.toInt() ?: 60,
                startTime = snap.child("startTime").getValue(String::class.java) ?: "08:00",
                endTime = snap.child("endTime").getValue(String::class.java) ?: "21:00"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Fetch water reminder settings error: ${e.message}")
            null
        }
    }

    // --- Focus Sessions (Realtime Database: users/$userId/focusSessions/$sessionId) ---
    suspend fun syncFocusSessionToCloud(userId: String, session: FocusSession) {
        val db = database ?: return
        try {
            val map = hashMapOf<String, Any?>(
                "id" to session.id,
                "sessionId" to session.id,
                "userId" to session.userId,
                "durationMinutes" to session.durationMinutes,
                "completedMinutes" to session.completedMinutes,
                "completedAt" to session.timestamp,
                "timestamp" to session.timestamp,
                "linkedHabitId" to session.habitId,
                "linkedHabitTitle" to session.habitTitle,
                "habitId" to session.habitId,
                "habitTitle" to session.habitTitle,
                "completed" to session.isCompleted,
                "isCompleted" to session.isCompleted,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            // Primary path specified: /users/{uid}/focusSessions/{sessionId}
            db.reference.child("users").child(userId).child("focusSessions").child(session.id).updateChildren(map).await()
            // Mirror path for legacy compatibility
            db.reference.child("users").child(userId).child("focus_sessions").child(session.id).updateChildren(map).await()
        } catch (e: Exception) {
            Log.w(TAG, "Focus sync error: ${e.message}")
        }
    }

    suspend fun fetchUserFocusSessions(userId: String): List<FocusSession> {
        val db = database ?: return emptyList()
        return try {
            val focusSessionsRef = db.reference.child("users").child(userId).child("focusSessions")
            val snapshot = focusSessionsRef.safeGet(2500L)
            if (snapshot != null && snapshot.exists() && snapshot.childrenCount > 0) {
                return parseFocusSessionsSnapshot(snapshot, userId)
            }
            // Fallback to legacy focus_sessions
            val legacyRef = db.reference.child("users").child(userId).child("focus_sessions")
            val legacySnapshot = legacyRef.safeGet(2500L)
            if (legacySnapshot != null && legacySnapshot.exists()) {
                return parseFocusSessionsSnapshot(legacySnapshot, userId)
            }
            emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Fetch focus sessions error: ${e.message}")
            emptyList()
        }
    }

    private fun parseFocusSessionsSnapshot(snapshot: DataSnapshot, userId: String): List<FocusSession> {
        val list = mutableListOf<FocusSession>()
        for (child in snapshot.children) {
            val isCompleted = child.child("completed").getValue(Boolean::class.java)
                ?: child.child("isCompleted").getValue(Boolean::class.java)
                ?: true
            val durationMinutes = child.child("durationMinutes").getValue(Long::class.java)?.toInt() ?: 25
            val completedMinutes = child.child("completedMinutes").getValue(Long::class.java)?.toInt() ?: durationMinutes
            val timestamp = child.child("completedAt").getValue(Long::class.java)
                ?: child.child("timestamp").getValue(Long::class.java)
                ?: System.currentTimeMillis()
            val linkedHabitId = child.child("linkedHabitId").getValue(String::class.java)
                ?: child.child("habitId").getValue(String::class.java)
            val linkedHabitTitle = child.child("linkedHabitTitle").getValue(String::class.java)
                ?: child.child("habitTitle").getValue(String::class.java)
                ?: "Deep Work"

            val session = FocusSession(
                id = child.child("sessionId").getValue(String::class.java)
                    ?: child.child("id").getValue(String::class.java)
                    ?: child.key ?: "",
                userId = child.child("userId").getValue(String::class.java) ?: userId,
                habitId = linkedHabitId,
                habitTitle = linkedHabitTitle,
                durationMinutes = durationMinutes,
                completedMinutes = completedMinutes,
                timestamp = timestamp,
                isCompleted = isCompleted
            )
            if (session.id.isNotBlank()) {
                list.add(session)
            }
        }
        return list
    }

    // --- Achievements (Realtime Database: users/$userId/achievements) ---
    suspend fun syncAchievementsToCloud(userId: String, achievements: List<Achievement>) {
        val db = database ?: return
        try {
            val map = hashMapOf<String, Any>()
            for (ach in achievements) {
                map[ach.id] = mapOf(
                    "id" to ach.id,
                    "title" to ach.title,
                    "description" to ach.description,
                    "icon" to ach.icon,
                    "isUnlocked" to ach.isUnlocked,
                    "unlockedAt" to ach.unlockedAt,
                    "requirementType" to ach.requirementType,
                    "requirementValue" to ach.requirementValue
                )
            }
            db.reference.child("users").child(userId).child("achievements").updateChildren(map).await()
        } catch (e: Exception) {
            Log.w(TAG, "Achievements sync error: ${e.message}")
        }
    }

    suspend fun fetchUserAchievements(userId: String): List<Achievement> {
        val db = database ?: return emptyList()
        return try {
            val snapshot = db.reference.child("users").child(userId).child("achievements").safeGet(2500L) ?: return emptyList()
            if (!snapshot.exists()) return emptyList()

            val list = mutableListOf<Achievement>()
            for (child in snapshot.children) {
                val ach = Achievement(
                    id = child.child("id").getValue(String::class.java) ?: child.key ?: "",
                    title = child.child("title").getValue(String::class.java) ?: "",
                    description = child.child("description").getValue(String::class.java) ?: "",
                    icon = child.child("icon").getValue(String::class.java) ?: "emoji_events",
                    isUnlocked = child.child("isUnlocked").getValue(Boolean::class.java) ?: false,
                    unlockedAt = child.child("unlockedAt").getValue(Long::class.java),
                    requirementType = child.child("requirementType").getValue(String::class.java) ?: "streak",
                    requirementValue = child.child("requirementValue").getValue(Long::class.java)?.toInt() ?: 1
                )
                if (ach.id.isNotBlank()) {
                    list.add(ach)
                }
            }
            list
        } catch (e: Exception) {
            Log.w(TAG, "Fetch achievements error: ${e.message}")
            emptyList()
        }
    }

    // --- Streaks & Progress (Realtime Database: users/$userId/streaks and users/$userId/progress) ---
    suspend fun syncStreaksToCloud(userId: String, currentStreak: Int, bestStreak: Int) {
        val db = database ?: return
        try {
            val map = hashMapOf<String, Any>(
                "currentStreak" to currentStreak,
                "bestStreak" to bestStreak,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            db.reference.child("users").child(userId).child("streaks").updateChildren(map).await()
        } catch (e: Exception) {
            Log.w(TAG, "Streaks sync error: ${e.message}")
        }
    }

    suspend fun syncProgressToCloud(userId: String, disciplineScore: Int, completionRate: Float) {
        val db = database ?: return
        try {
            val map = hashMapOf<String, Any>(
                "disciplineScore" to disciplineScore,
                "completionRate" to completionRate,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            db.reference.child("users").child(userId).child("progress").updateChildren(map).await()
        } catch (e: Exception) {
            Log.w(TAG, "Progress sync error: ${e.message}")
        }
    }

    suspend fun syncDisciplineScoreToCloud(
        userId: String,
        score: Int,
        breakdown: Map<String, Any> = emptyMap()
    ) {
        val db = database ?: return
        try {
            val map = hashMapOf<String, Any>(
                "score" to score,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            map.putAll(breakdown)
            db.reference.child("users").child(userId).child("disciplineScore").updateChildren(map).await()
            db.reference.child("users").child(userId).child("progress").child("disciplineScore").setValue(score).await()
        } catch (e: Exception) {
            Log.w(TAG, "Discipline score sync error: ${e.message}")
        }
    }

    suspend fun fetchDisciplineScoreFromCloud(userId: String): Map<String, Any>? {
        val db = database ?: return null
        return try {
            val snapshot = db.reference.child("users").child(userId).child("disciplineScore").safeGet(2500L)
            if (snapshot != null && snapshot.exists()) {
                val map = mutableMapOf<String, Any>()
                for (child in snapshot.children) {
                    child.value?.let { map[child.key ?: ""] = it }
                }
                map
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Fetch discipline score error: ${e.message}")
            null
        }
    }

    // --- Settings & Reminders (Realtime Database: users/$userId/settings) ---
    suspend fun syncSettingsToCloud(userId: String, settings: Map<String, Any?>) {
        val db = database ?: return
        try {
            val mapWithTimestamp = settings.toMutableMap()
            mapWithTimestamp["updatedAt"] = ServerValue.TIMESTAMP
            db.reference.child("users").child(userId).child("settings").updateChildren(mapWithTimestamp).await()
            Log.d(TAG, "Settings synced for user: $userId")
        } catch (e: Exception) {
            Log.w(TAG, "Settings sync error: ${e.message}")
        }
    }

    suspend fun fetchUserSettings(userId: String): Map<String, Any?>? {
        val db = database ?: return null
        return try {
            val snapshot = db.reference.child("users").child(userId).child("settings").safeGet(2000L) ?: return null
            if (!snapshot.exists()) return null
            val result = mutableMapOf<String, Any?>()
            for (child in snapshot.children) {
                result[child.key ?: ""] = child.value
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "Fetch settings error: ${e.message}")
            null
        }
    }

    // --- Realtime Database User Listener ---
    fun attachUserDataListener(userId: String, onDataChanged: () -> Unit): ValueEventListener? {
        val db = database ?: return null
        return try {
            val ref = db.reference.child("users").child(userId).child("profile")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onDataChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "User data listener cancelled: ${error.message}")
                }
            }
            ref.addValueEventListener(listener)
            listener
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach user data listener: ${e.message}", e)
            null
        }
    }

    fun removeUserDataListener(userId: String, listener: ValueEventListener?) {
        if (listener == null) return
        val db = database ?: return
        try {
            db.reference.child("users").child(userId).child("profile").removeEventListener(listener)
        } catch (e: Exception) {
            Log.w(TAG, "Error removing user data listener: ${e.message}")
        }
    }

    // --- Alarm Settings & Night Reviews (Prepared for Phase 2, clean RTDB structure) ---
    suspend fun syncAlarmSettingsToCloud(userId: String, settings: Map<String, Any>) {
        val db = database ?: return
        try {
            db.reference.child("users").child(userId).child("alarm_settings").updateChildren(settings).await()
        } catch (e: Exception) {
            Log.w(TAG, "Alarm settings sync error: ${e.message}")
        }
    }

    suspend fun syncNightReviewToCloud(userId: String, entry: NightReviewEntry) {
        val db = database ?: return
        try {
            val map = hashMapOf<String, Any?>(
                "date" to entry.date,
                "completedHabits" to entry.completedHabits,
                "totalHabits" to entry.totalHabits,
                "completionPercentage" to entry.completionPercentage,
                "streak" to entry.streak,
                "waterIntake" to entry.waterIntake,
                "waterGoal" to entry.waterGoal,
                "focusMinutes" to entry.focusMinutes,
                "wentWell" to entry.wentWell,
                "improvement" to entry.improvement,
                "reflection" to entry.reflection,
                "rating" to entry.rating,
                "createdAt" to entry.createdAt,
                "updatedAt" to entry.updatedAt
            )
            // Primary path specified: /users/{uid}/nightReviews/{YYYY-MM-DD}/
            db.reference.child("users").child(userId).child("nightReviews").child(entry.date).updateChildren(map).await()
            // Also keep night_reviews updated for backward compatibility
            db.reference.child("users").child(userId).child("night_reviews").child(entry.date).updateChildren(map).await()
            Log.d(TAG, "Successfully synced nightReview to /users/$userId/nightReviews/${entry.date}")
        } catch (e: Exception) {
            Log.w(TAG, "Night review sync error: ${e.message}")
        }
    }

    suspend fun deleteNightReviewFromCloud(userId: String, date: String) {
        val db = database ?: return
        try {
            db.reference.child("users").child(userId).child("nightReviews").child(date).removeValue().await()
            db.reference.child("users").child(userId).child("night_reviews").child(date).removeValue().await()
            Log.d(TAG, "Successfully deleted nightReview for /users/$userId/nightReviews/$date")
        } catch (e: Exception) {
            Log.w(TAG, "Delete night review error: ${e.message}")
        }
    }

    suspend fun fetchNightReview(userId: String, date: String): NightReviewEntry? {
        val db = database ?: return null
        return try {
            var snap = db.reference.child("users").child(userId).child("nightReviews").child(date).safeGet(2000L)
            if (snap == null || !snap.exists()) {
                snap = db.reference.child("users").child(userId).child("night_reviews").child(date).safeGet(2000L)
            }
            if (snap == null || !snap.exists()) return null
            parseNightReviewSnapshot(date, snap, userId)
        } catch (e: Exception) {
            Log.w(TAG, "Fetch night review error: ${e.message}")
            null
        }
    }

    suspend fun fetchAllNightReviews(userId: String): List<NightReviewEntry> {
        val db = database ?: return emptyList()
        return try {
            val list = mutableListOf<NightReviewEntry>()
            val snap = db.reference.child("users").child(userId).child("nightReviews").safeGet(2500L)
            if (snap != null && snap.exists()) {
                for (child in snap.children) {
                    val dateKey = child.key ?: continue
                    list.add(parseNightReviewSnapshot(dateKey, child, userId))
                }
            } else {
                // Fall back to night_reviews
                val legacySnap = db.reference.child("users").child(userId).child("night_reviews").safeGet(2000L)
                if (legacySnap != null && legacySnap.exists()) {
                    for (child in legacySnap.children) {
                        val dateKey = child.key ?: continue
                        list.add(parseNightReviewSnapshot(dateKey, child, userId))
                    }
                }
            }
            list.sortedByDescending { it.date }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch all night reviews error: ${e.message}")
            emptyList()
        }
    }

    private fun parseNightReviewSnapshot(date: String, snap: DataSnapshot, userId: String): NightReviewEntry {
        val dateVal = snap.child("date").getValue(String::class.java) ?: date
        val completedHabits = snap.child("completedHabits").getValue(Long::class.java)?.toInt() ?: 0
        val totalHabits = snap.child("totalHabits").getValue(Long::class.java)?.toInt() ?: 0
        val completionPercentage = snap.child("completionPercentage").getValue(Long::class.java)?.toInt() ?: 0
        val streak = snap.child("streak").getValue(Long::class.java)?.toInt() ?: 0
        val waterIntake = snap.child("waterIntake").getValue(Long::class.java)?.toInt() ?: 0
        val waterGoal = snap.child("waterGoal").getValue(Long::class.java)?.toInt() ?: 0
        val focusMinutes = snap.child("focusMinutes").getValue(Long::class.java)?.toInt() ?: 0
        val wentWell = snap.child("wentWell").getValue(String::class.java)
            ?: snap.child("wins").getValue(String::class.java) ?: ""
        val improvement = snap.child("improvement").getValue(String::class.java)
            ?: snap.child("struggles").getValue(String::class.java) ?: ""
        val reflection = snap.child("reflection").getValue(String::class.java)
            ?: snap.child("tomorrowPriority").getValue(String::class.java) ?: ""
        val rating = snap.child("rating").getValue(Long::class.java)?.toInt()
            ?: snap.child("score").getValue(Long::class.java)?.toInt() ?: 5
        val createdAt = snap.child("createdAt").getValue(Long::class.java)
            ?: snap.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
        val updatedAt = snap.child("updatedAt").getValue(Long::class.java) ?: createdAt

        return NightReviewEntry(
            date = dateVal,
            userId = userId,
            completedHabits = completedHabits,
            totalHabits = totalHabits,
            completionPercentage = completionPercentage,
            streak = streak,
            waterIntake = waterIntake,
            waterGoal = waterGoal,
            focusMinutes = focusMinutes,
            wentWell = wentWell,
            improvement = improvement,
            reflection = reflection,
            rating = rating,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    // --- Master Reminder Settings (/users/{uid}/settings/reminders/) ---
    suspend fun syncReminderSettings(userId: String, settings: ReminderSettings) {
        val db = database ?: return
        try {
            val waterMap = hashMapOf<String, Any?>(
                "enabled" to settings.waterReminders.isEnabled,
                "intervalMinutes" to settings.waterReminders.intervalMinutes,
                "startTime" to settings.waterReminders.startTime,
                "endTime" to settings.waterReminders.endTime,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            val nightMap = hashMapOf<String, Any?>(
                "enabled" to settings.nightReview.isEnabled,
                "time" to settings.nightReview.time,
                "updatedAt" to ServerValue.TIMESTAMP
            )

            val habitsMap = hashMapOf<String, Any>()
            settings.habitReminders.forEach { (habitId, isEnabled) ->
                val time = settings.habitReminderTimes[habitId] ?: "07:30"
                habitsMap[habitId] = hashMapOf(
                    "enabled" to isEnabled,
                    "time" to time
                )
            }

            val masterMap = hashMapOf<String, Any?>(
                "waterReminders" to waterMap,
                "nightReview" to nightMap,
                "habits" to habitsMap,
                "updatedAt" to ServerValue.TIMESTAMP
            )

            // Primary path specified by user: /users/{uid}/settings/reminders/
            db.reference.child("users").child(userId).child("settings").child("reminders").updateChildren(masterMap).await()

            // Also keep legacy /settings/waterReminders in sync
            db.reference.child("users").child(userId).child("settings").child("waterReminders").updateChildren(waterMap).await()

            Log.d(TAG, "Synced reminder settings to /users/$userId/settings/reminders/")
        } catch (e: Exception) {
            Log.w(TAG, "Sync reminder settings error: ${e.message}")
        }
    }

    suspend fun fetchReminderSettings(userId: String): ReminderSettings? {
        val db = database ?: return null
        return try {
            val snap = db.reference.child("users").child(userId).child("settings").child("reminders").safeGet(2500L)
            if (snap != null && snap.exists()) {
                val waterSnap = snap.child("waterReminders")
                val waterSettings = WaterReminderSettings(
                    isEnabled = waterSnap.child("enabled").getValue(Boolean::class.java) ?: false,
                    intervalMinutes = waterSnap.child("intervalMinutes").getValue(Long::class.java)?.toInt() ?: 60,
                    startTime = waterSnap.child("startTime").getValue(String::class.java) ?: "08:00",
                    endTime = waterSnap.child("endTime").getValue(String::class.java) ?: "21:00"
                )

                val nightSnap = snap.child("nightReview")
                val nightSettings = NightReviewReminderSettings(
                    isEnabled = nightSnap.child("enabled").getValue(Boolean::class.java) ?: false,
                    time = nightSnap.child("time").getValue(String::class.java) ?: "21:30"
                )

                val habitReminders = mutableMapOf<String, Boolean>()
                val habitTimes = mutableMapOf<String, String>()
                val habitsSnap = snap.child("habits")
                for (hChild in habitsSnap.children) {
                    val habitId = hChild.key ?: continue
                    val enabled = hChild.child("enabled").getValue(Boolean::class.java) ?: false
                    val time = hChild.child("time").getValue(String::class.java) ?: "07:30"
                    habitReminders[habitId] = enabled
                    habitTimes[habitId] = time
                }

                ReminderSettings(
                    waterReminders = waterSettings,
                    nightReview = nightSettings,
                    habitReminders = habitReminders,
                    habitReminderTimes = habitTimes
                )
            } else {
                // Fall back to water reminder settings if present
                val water = fetchWaterReminderSettings(userId)
                if (water != null) {
                    ReminderSettings(waterReminders = water)
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch reminder settings error: ${e.message}")
            null
        }
    }

    // --- Centralized Smart Notifications (users/$userId/notifications/$notificationId) ---
    suspend fun saveNotificationToCloud(userId: String, notification: AppNotification) {
        if (userId.startsWith("guest_")) return
        val db = database ?: return
        try {
            val notifMap = hashMapOf<String, Any?>(
                "id" to notification.id,
                "userId" to notification.userId,
                "title" to notification.title,
                "message" to notification.message,
                "type" to notification.type.name,
                "targetDestination" to notification.targetDestination,
                "relatedEntityId" to notification.relatedEntityId,
                "timestamp" to notification.timestamp,
                "isRead" to notification.isRead,
                "scheduledDate" to notification.scheduledDate,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            db.reference.child("users").child(userId).child("notifications").child(notification.id).setValue(notifMap).await()
        } catch (e: Exception) {
            Log.w(TAG, "Save notification to cloud error: ${e.message}")
        }
    }

    suspend fun updateNotificationReadStateInCloud(userId: String, notificationId: String, isRead: Boolean) {
        if (userId.startsWith("guest_")) return
        val db = database ?: return
        try {
            db.reference.child("users").child(userId).child("notifications").child(notificationId).child("isRead").setValue(isRead).await()
        } catch (e: Exception) {
            Log.w(TAG, "Update notification read state error: ${e.message}")
        }
    }

    suspend fun markAllNotificationsAsReadInCloud(userId: String) {
        if (userId.startsWith("guest_")) return
        val db = database ?: return
        try {
            val snapshot = db.reference.child("users").child(userId).child("notifications").safeGet(3000L) ?: return
            val updates = hashMapOf<String, Any?>()
            for (child in snapshot.children) {
                updates["${child.key}/isRead"] = true
            }
            if (updates.isNotEmpty()) {
                db.reference.child("users").child(userId).child("notifications").updateChildren(updates).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Mark all notifications read error: ${e.message}")
        }
    }

    suspend fun deleteNotificationFromCloud(userId: String, notificationId: String) {
        if (userId.startsWith("guest_")) return
        val db = database ?: return
        try {
            db.reference.child("users").child(userId).child("notifications").child(notificationId).removeValue().await()
        } catch (e: Exception) {
            Log.w(TAG, "Delete notification error: ${e.message}")
        }
    }

    suspend fun fetchUserNotifications(userId: String): List<AppNotification> {
        if (userId.startsWith("guest_")) return emptyList()
        val db = database ?: return emptyList()
        return try {
            val snapshot = db.reference.child("users").child(userId).child("notifications").safeGet(3500L) ?: return emptyList()
            if (!snapshot.exists()) return emptyList()

            val list = mutableListOf<AppNotification>()
            for (child in snapshot.children) {
                val id = child.child("id").getValue(String::class.java) ?: child.key ?: continue
                val title = child.child("title").getValue(String::class.java) ?: ""
                val message = child.child("message").getValue(String::class.java) ?: ""
                val typeStr = child.child("type").getValue(String::class.java) ?: "SYSTEM"
                val type = try { NotificationType.valueOf(typeStr) } catch (e: Exception) { NotificationType.SYSTEM }
                val targetDestination = child.child("targetDestination").getValue(String::class.java) ?: ""
                val relatedEntityId = child.child("relatedEntityId").getValue(String::class.java)
                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                val isRead = child.child("isRead").getValue(Boolean::class.java) ?: false
                val scheduledDate = child.child("scheduledDate").getValue(String::class.java) ?: ""

                list.add(AppNotification(id, userId, title, message, type, targetDestination, relatedEntityId, timestamp, isRead, scheduledDate))
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch notifications error: ${e.message}")
            emptyList()
        }
    }

    // --- Challenge Alarms (/users/{uid}/alarms/{alarmId}) ---
    suspend fun syncAlarmToCloud(userId: String, alarm: ChallengeAlarm) {
        if (userId.startsWith("guest_")) return
        val db = database ?: return
        try {
            val map = mapOf(
                "id" to alarm.id,
                "userId" to userId,
                "name" to alarm.name,
                "time" to alarm.time,
                "repeatDays" to alarm.repeatDays,
                "enabled" to alarm.isEnabled,
                "challengeType" to alarm.challengeType.name,
                "target" to alarm.target,
                "linkedHabitId" to alarm.linkedHabitId,
                "soundUri" to alarm.soundUri,
                "soundTitle" to alarm.soundTitle,
                "videoUri" to alarm.videoUri,
                "vibration" to alarm.vibration,
                "snoozeMinutes" to alarm.snoozeMinutes,
                "createdAt" to alarm.createdAt,
                "updatedAt" to alarm.updatedAt
            )
            db.reference.child("users").child(userId).child("alarms").child(alarm.id).setValue(map).await()
        } catch (e: Exception) {
            Log.w(TAG, "Sync alarm to cloud error: ${e.message}")
        }
    }

    suspend fun deleteAlarmFromCloud(userId: String, alarmId: String) {
        if (userId.startsWith("guest_")) return
        val db = database ?: return
        try {
            db.reference.child("users").child(userId).child("alarms").child(alarmId).removeValue().await()
        } catch (e: Exception) {
            Log.w(TAG, "Delete alarm from cloud error: ${e.message}")
        }
    }

    suspend fun fetchAlarmsFromCloud(userId: String): List<ChallengeAlarm> {
        if (userId.startsWith("guest_")) return emptyList()
        val db = database ?: return emptyList()
        return try {
            val snapshot = db.reference.child("users").child(userId).child("alarms").safeGet(4000L) ?: return emptyList()
            if (!snapshot.exists()) return emptyList()
            val list = mutableListOf<ChallengeAlarm>()
            for (child in snapshot.children) {
                val id = child.child("id").getValue(String::class.java) ?: child.key ?: continue
                val name = child.child("name").getValue(String::class.java) ?: "Wake-Up Warrior"
                val time = child.child("time").getValue(String::class.java) ?: "06:30"
                val repeatDays = child.child("repeatDays").getValue(String::class.java) ?: "1,2,3,4,5,6,7"
                val isEnabled = child.child("enabled").getValue(Boolean::class.java) ?: true
                val typeStr = child.child("challengeType").getValue(String::class.java) ?: "PUSH_UPS"
                val type = try { ChallengeType.valueOf(typeStr) } catch (e: Exception) { ChallengeType.PUSH_UPS }
                val target = child.child("target").getValue(Long::class.java)?.toInt() ?: type.defaultTarget
                val linkedHabitId = child.child("linkedHabitId").getValue(String::class.java)
                val soundUri = child.child("soundUri").getValue(String::class.java) ?: ""
                val soundTitle = child.child("soundTitle").getValue(String::class.java) ?: "DayForge Default Sound"
                val videoUri = child.child("videoUri").getValue(String::class.java) ?: ""
                val vibration = child.child("vibration").getValue(Boolean::class.java) ?: true
                val snoozeMinutes = child.child("snoozeMinutes").getValue(Long::class.java)?.toInt() ?: 5
                val createdAt = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                val updatedAt = child.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                list.add(ChallengeAlarm(id, userId, name, time, repeatDays, isEnabled, type, target, linkedHabitId, soundUri, soundTitle, videoUri, vibration, snoozeMinutes, createdAt, updatedAt))
            }
            list.sortedBy { it.time }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch alarms error: ${e.message}")
            emptyList()
        }
    }

    // --- Challenge History (/users/{uid}/challengeHistory/{challengeId}) ---
    suspend fun syncChallengeHistoryToCloud(userId: String, entry: ChallengeHistoryEntry) {
        if (userId.startsWith("guest_")) return
        val db = database ?: return
        try {
            val map = mapOf(
                "id" to entry.id,
                "userId" to userId,
                "alarmId" to entry.alarmId,
                "alarmName" to entry.alarmName,
                "challengeType" to entry.challengeType.name,
                "target" to entry.target,
                "completedAmount" to entry.completedAmount,
                "status" to entry.status,
                "linkedHabitId" to entry.linkedHabitId,
                "startedAt" to entry.startedAt,
                "completedAt" to entry.completedAt,
                "durationSeconds" to entry.durationSeconds,
                "date" to entry.date
            )
            db.reference.child("users").child(userId).child("challengeHistory").child(entry.id).setValue(map).await()
        } catch (e: Exception) {
            Log.w(TAG, "Sync challenge history error: ${e.message}")
        }
    }

    suspend fun fetchChallengeHistoryFromCloud(userId: String): List<ChallengeHistoryEntry> {
        if (userId.startsWith("guest_")) return emptyList()
        val db = database ?: return emptyList()
        return try {
            val snapshot = db.reference.child("users").child(userId).child("challengeHistory").safeGet(4000L) ?: return emptyList()
            if (!snapshot.exists()) return emptyList()
            val list = mutableListOf<ChallengeHistoryEntry>()
            for (child in snapshot.children) {
                val id = child.child("id").getValue(String::class.java) ?: child.key ?: continue
                val alarmId = child.child("alarmId").getValue(String::class.java)
                val alarmName = child.child("alarmName").getValue(String::class.java) ?: ""
                val typeStr = child.child("challengeType").getValue(String::class.java) ?: "PUSH_UPS"
                val type = try { ChallengeType.valueOf(typeStr) } catch (e: Exception) { ChallengeType.PUSH_UPS }
                val target = child.child("target").getValue(Long::class.java)?.toInt() ?: 15
                val completedAmount = child.child("completedAmount").getValue(Long::class.java)?.toInt() ?: target
                val status = child.child("status").getValue(String::class.java) ?: "COMPLETED"
                val linkedHabitId = child.child("linkedHabitId").getValue(String::class.java)
                val startedAt = child.child("startedAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                val completedAt = child.child("completedAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                val durationSeconds = child.child("durationSeconds").getValue(Long::class.java)?.toInt() ?: 0
                val date = child.child("date").getValue(String::class.java) ?: ""

                list.add(ChallengeHistoryEntry(id, userId, alarmId, alarmName, type, target, completedAmount, status, linkedHabitId, startedAt, completedAt, durationSeconds, date))
            }
            list.sortedByDescending { it.completedAt }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch challenge history error: ${e.message}")
            emptyList()
        }
    }

    // --- Focus Settings (Step 20) ---
    suspend fun syncFocusSettings(userId: String, settings: FocusSettings) {
        val db = database ?: return
        try {
            val map = mapOf(
                "durationMinutes" to settings.durationMinutes,
                "linkedHabitId" to (settings.linkedHabitId ?: ""),
                "restrictionEnabled" to settings.restrictionEnabled,
                "restrictedApps" to settings.restrictedApps,
                "relatedAppPackage" to (settings.relatedAppPackage ?: ""),
                "relatedAppName" to (settings.relatedAppName ?: ""),
                "autoOpenRelatedApp" to settings.autoOpenRelatedApp,
                "updatedAt" to settings.updatedAt
            )
            db.reference.child("users").child(userId).child("focusSettings").setValue(map).await()
            Log.d(TAG, "Synced focus settings to /users/$userId/focusSettings/")
        } catch (e: Exception) {
            Log.w(TAG, "Sync focus settings error: ${e.message}")
        }
    }

    suspend fun fetchFocusSettings(userId: String): FocusSettings? {
        val db = database ?: return null
        return try {
            val snap = db.reference.child("users").child(userId).child("focusSettings").safeGet(2500L)
            if (snap != null && snap.exists()) {
                val duration = snap.child("durationMinutes").getValue(Long::class.java)?.toInt() ?: 25
                val habitId = snap.child("linkedHabitId").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                val restrictionEnabled = snap.child("restrictionEnabled").getValue(Boolean::class.java) ?: false
                val restrictedApps = mutableListOf<String>()
                snap.child("restrictedApps").children.forEach { child ->
                    child.getValue(String::class.java)?.let { restrictedApps.add(it) }
                }
                val relatedAppPackage = snap.child("relatedAppPackage").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                val relatedAppName = snap.child("relatedAppName").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                val autoOpen = snap.child("autoOpenRelatedApp").getValue(Boolean::class.java) ?: false
                val updatedAt = snap.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                FocusSettings(
                    durationMinutes = duration,
                    linkedHabitId = habitId,
                    restrictionEnabled = restrictionEnabled,
                    restrictedApps = restrictedApps,
                    relatedAppPackage = relatedAppPackage,
                    relatedAppName = relatedAppName,
                    autoOpenRelatedApp = autoOpen,
                    updatedAt = updatedAt
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch focus settings error: ${e.message}")
            null
        }
    }

    suspend fun syncJournalEntryToCloud(userId: String, entry: JournalEntry) {
        val db = database ?: return
        try {
            val map = hashMapOf<String, Any?>(
                "id" to entry.id,
                "userId" to entry.userId,
                "title" to entry.title,
                "content" to entry.content,
                "time" to entry.time,
                "date" to entry.date,
                "tags" to entry.tags,
                "category" to entry.category,
                "createdAt" to entry.createdAt
            )
            db.reference.child("users").child(userId).child("journal").child(entry.id).setValue(map).await()
        } catch (e: Exception) {
            Log.w(TAG, "Journal sync error: ${e.message}")
        }
    }

    suspend fun fetchUserJournalEntries(userId: String): List<JournalEntry> {
        val db = database ?: return emptyList()
        return try {
            val snapshot = db.reference.child("users").child(userId).child("journal").safeGet(4000L) ?: return emptyList()
            val list = mutableListOf<JournalEntry>()
            for (child in snapshot.children) {
                val id = child.child("id").getValue(String::class.java) ?: child.key ?: continue
                val title = child.child("title").getValue(String::class.java) ?: ""
                val content = child.child("content").getValue(String::class.java) ?: ""
                val time = child.child("time").getValue(String::class.java) ?: ""
                val date = child.child("date").getValue(String::class.java) ?: ""
                val category = child.child("category").getValue(String::class.java) ?: "General"
                val createdAt = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                val tagsList = mutableListOf<String>()
                child.child("tags").children.forEach { tagSnapshot ->
                    val tagVal = tagSnapshot.getValue(String::class.java)
                    if (tagVal != null) {
                        tagsList.add(tagVal)
                    }
                }
                list.add(JournalEntry(id, userId, title, content, time, date, tagsList, category, createdAt))
            }
            list.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch journal error: ${e.message}")
            emptyList()
        }
    }

    suspend fun deleteJournalEntryFromCloud(userId: String, entryId: String) {
        val db = database ?: return
        try {
            db.reference.child("users").child(userId).child("journal").child(entryId).removeValue().await()
        } catch (e: Exception) {
            Log.w(TAG, "Delete journal error: ${e.message}")
        }
    }

    // =========================================================================
    // DAYFORGE CONTENT HUB (/content/{contentId})
    // =========================================================================

    suspend fun fetchPublishedContent(): List<com.example.data.models.DisciplineContent> {
        val db = database ?: return emptyList()
        return try {
            val snapshot = db.reference.child("content").safeGet(5000L) ?: return emptyList()
            if (!snapshot.exists()) return emptyList()

            val list = mutableListOf<com.example.data.models.DisciplineContent>()
            for (child in snapshot.children) {
                val contentId = child.child("contentId").getValue(String::class.java) ?: child.key ?: continue
                val isPublished = child.child("isPublished").getValue(Boolean::class.java) ?: true
                if (!isPublished) continue

                val title = child.child("title").getValue(String::class.java) ?: ""
                val description = child.child("description").getValue(String::class.java) ?: ""
                val mediaUrl = child.child("mediaUrl").getValue(String::class.java) ?: ""
                val thumbnailUrl = child.child("thumbnailUrl").getValue(String::class.java) ?: ""
                val category = child.child("category").getValue(String::class.java) ?: "DISCIPLINE"
                val creatorName = child.child("creatorName").getValue(String::class.java) ?: "DayForge Academy"
                val createdAt = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                val duration = child.child("duration").getValue(String::class.java) ?: "2 min"
                val isFeatured = child.child("isFeatured").getValue(Boolean::class.java) ?: false
                val ctaLabel = child.child("ctaLabel").getValue(String::class.java)
                val ctaAction = child.child("ctaAction").getValue(String::class.java)

                list.add(
                    com.example.data.models.DisciplineContent(
                        contentId = contentId,
                        title = title,
                        description = description,
                        mediaUrl = mediaUrl,
                        thumbnailUrl = thumbnailUrl,
                        category = category,
                        creatorName = creatorName,
                        createdAt = createdAt,
                        duration = duration,
                        isPublished = isPublished,
                        isFeatured = isFeatured,
                        ctaLabel = ctaLabel,
                        ctaAction = ctaAction
                    )
                )
            }
            list.sortedWith(compareByDescending<com.example.data.models.DisciplineContent> { it.isFeatured }.thenByDescending { it.createdAt })
        } catch (e: Exception) {
            Log.w(TAG, "Fetch content error: ${e.message}")
            emptyList()
        }
    }

    suspend fun seedInitialContentIfEmpty(items: List<com.example.data.models.DisciplineContent>) {
        val db = database ?: return
        try {
            val snapshot = db.reference.child("content").safeGet(4000L)
            if (snapshot == null || !snapshot.exists() || !snapshot.hasChildren()) {
                val updates = hashMapOf<String, Any>()
                items.forEach { item ->
                    updates[item.contentId] = hashMapOf(
                        "contentId" to item.contentId,
                        "title" to item.title,
                        "description" to item.description,
                        "mediaUrl" to item.mediaUrl,
                        "thumbnailUrl" to item.thumbnailUrl,
                        "category" to item.category,
                        "creatorName" to item.creatorName,
                        "createdAt" to item.createdAt,
                        "duration" to item.duration,
                        "isPublished" to item.isPublished,
                        "isFeatured" to item.isFeatured,
                        "ctaLabel" to (item.ctaLabel ?: ""),
                        "ctaAction" to (item.ctaAction ?: "")
                    )
                }
                db.reference.child("content").updateChildren(updates).await()
                Log.d(TAG, "Seeded ${items.size} initial DayForge content items to cloud RTDB")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Seed content error: ${e.message}")
        }
    }

    suspend fun saveBookmarkContent(userId: String, contentId: String, isSaved: Boolean) {
        val db = database ?: return
        try {
            val ref = db.reference.child("users").child(userId).child("savedContent").child(contentId)
            if (isSaved) {
                ref.setValue(System.currentTimeMillis()).await()
            } else {
                ref.removeValue().await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Bookmark content error: ${e.message}")
        }
    }

    suspend fun fetchSavedContentIds(userId: String): Set<String> {
        val db = database ?: return emptySet()
        return try {
            val snapshot = db.reference.child("users").child(userId).child("savedContent").safeGet(3000L) ?: return emptySet()
            val set = mutableSetOf<String>()
            for (child in snapshot.children) {
                child.key?.let { set.add(it) }
            }
            set
        } catch (e: Exception) {
            Log.w(TAG, "Fetch saved content error: ${e.message}")
            emptySet()
        }
    }

    suspend fun recordContentView(userId: String, contentId: String) {
        val db = database ?: return
        try {
            db.reference.child("users").child(userId).child("contentHistory").child(contentId).setValue(
                hashMapOf(
                    "contentId" to contentId,
                    "viewedAt" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "Record content view error: ${e.message}")
        }
    }

    suspend fun fetchContentViewHistory(userId: String): Set<String> {
        val db = database ?: return emptySet()
        return try {
            val snapshot = db.reference.child("users").child(userId).child("contentHistory").safeGet(3000L) ?: return emptySet()
            val set = mutableSetOf<String>()
            for (child in snapshot.children) {
                child.key?.let { set.add(it) }
            }
            set
        } catch (e: Exception) {
            Log.w(TAG, "Fetch content history error: ${e.message}")
            emptySet()
        }
    }
}

