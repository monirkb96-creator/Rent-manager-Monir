package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface SessionResolution {
    data object Loading : SessionResolution
    data object Unauthenticated : SessionResolution
    data class NeedsWorkspaceSetup(val user: FirebaseUser) : SessionResolution
    data class Authenticated(
        val user: FirebaseUser,
        val profile: UserProfile,
        val isAdmin: Boolean
    ) : SessionResolution
    data class InactiveAccount(val user: FirebaseUser, val profile: UserProfile) : SessionResolution
    data class Error(val message: String) : SessionResolution
}

class AuthRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        if (auth.currentUser != null) {
            onAuthSuccess()
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onUnauthenticated()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val activity = context as? Activity
                    ?: throw IllegalStateException("Activity context required for sign-in")
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("Auth", "Failed to clear credential state: ${e.message}")
                } finally {
                    auth.signOut()
                    onSignOutComplete()
                }
            }
        }
    }

    private fun isBootstrappedAdminEmail(email: String?): Boolean {
        return email.equals("MonirKb96@gmail.com", ignoreCase = true)
    }

    suspend fun resolveUserSession(
        user: FirebaseUser,
        preferredRole: String? = null
    ): SessionResolution {
        val uid = user.uid
        val email = user.email.orEmpty()
        val displayName = user.displayName?.ifBlank { null } ?: email.substringBefore("@").ifBlank { "User" }

        return try {
            // 1. Check if user already has a profile
            val userDoc = try {
                db.collection("users").document(uid).get().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, "users/$uid")
                null
            }

            // 2. Check if user is in admins/{uid}
            val adminExists = try {
                db.collection("admins").document(uid).get().await().exists()
            } catch (_: Exception) {
                false
            }

            val isBootstrapped = isBootstrappedAdminEmail(email)

            if (isBootstrapped || adminExists) {
                ensureAdminProvisioned(uid, email, displayName)
                val freshUser = db.collection("users").document(uid).get().await()
                    .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(id = uid, userId = uid, email = email, displayName = displayName, role = "admin")
                return SessionResolution.Authenticated(user, freshUser, isAdmin = true)
            }

            if (userDoc != null && userDoc.exists()) {
                val profile = userDoc.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(id = uid, userId = uid, email = email, displayName = displayName, role = "tenant")
                if (profile.status == "inactive") {
                    return SessionResolution.InactiveAccount(user, profile)
                }
                if (profile.role == "admin") {
                    ensureAdminProvisioned(uid, email, displayName)
                    return SessionResolution.Authenticated(user, profile, isAdmin = true)
                }
                if (preferredRole == "admin" && profile.flatId.isBlank() && profile.tenantId.isBlank()) {
                    return initializeAsOwner(user)
                }
                val linkedProfile = linkTenantRecordsIfNeeded(uid, email, profile)
                return SessionResolution.Authenticated(user, linkedProfile, isAdmin = false)
            }

            if (preferredRole == "admin") {
                return initializeAsOwner(user)
            }
            if (preferredRole == "tenant") {
                return initializeAsTenant(user)
            }

            // Fallback when no preferredRole was supplied (e.g. cold startup check):
            val configSnapshot = try {
                db.collection("settings").document("app_config").get().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, "settings/app_config")
                null
            }

            if (configSnapshot != null && !configSnapshot.exists()) {
                return SessionResolution.NeedsWorkspaceSetup(user)
            }

            initializeAsTenant(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$uid")
            SessionResolution.Error(e.localizedMessage ?: "Failed to verify user session")
        }
    }

    suspend fun initializeAsOwner(user: FirebaseUser): SessionResolution {
        val uid = user.uid
        val email = user.email.orEmpty()
        val displayName = user.displayName?.ifBlank { null } ?: email.substringBefore("@").ifBlank { "Owner" }
        return try {
            ensureAdminProvisioned(uid, email, displayName)
            val profile = db.collection("users").document(uid).get().await()
                .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(
                    id = uid,
                    userId = uid,
                    email = email,
                    displayName = displayName,
                    role = "admin",
                    status = "active"
                )
            SessionResolution.Authenticated(user, profile, isAdmin = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "settings/owner_$uid")
            SessionResolution.Error(e.localizedMessage ?: "Failed to initialize owner account")
        }
    }

    suspend fun upgradeUnassignedTenantToOwner(user: FirebaseUser): SessionResolution =
        initializeAsOwner(user)

    suspend fun initializeAsTenant(user: FirebaseUser): SessionResolution {
        val uid = user.uid
        val email = user.email.orEmpty()
        val displayName = user.displayName?.ifBlank { null } ?: email.substringBefore("@").ifBlank { "Tenant" }
        return try {
            val payload = mapOf(
                "userId" to uid,
                "email" to email,
                "displayName" to displayName,
                "phone" to "",
                "role" to "tenant",
                "status" to "active",
                "tenantId" to "",
                "buildingId" to "",
                "flatId" to "",
                "preferredLanguage" to "bn",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(payload).await()
            val profile = UserProfile(
                id = uid,
                userId = uid,
                email = email,
                displayName = displayName,
                role = "tenant",
                status = "active"
            )
            val linked = linkTenantRecordsIfNeeded(uid, email, profile)
            SessionResolution.Authenticated(user, linked, isAdmin = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid")
            SessionResolution.Error(e.localizedMessage ?: "Failed to create tenant profile")
        }
    }

    private suspend fun ensureAdminProvisioned(
        uid: String,
        email: String,
        displayName: String
    ) {
        val batch = db.batch()
        var hasWrites = false

        val defaultSettingsPayload = mapOf(
            "ownerUid" to uid,
            "appName" to "Smart Rent Manager",
            "defaultLanguage" to "bn",
            "sslCommerzStoreId" to "",
            "sslCommerzSandboxMode" to true,
            "bkashNumber" to "",
            "nagadNumber" to "",
            "rocketNumber" to "",
            "bankTransferInstructions" to "",
            "bankAccountsJson" to "",
            "autoBillDay" to 1,
            "carryForwardEnabled" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val ownerSettingsRef = db.collection("settings").document("owner_$uid")
        val ownerSettingsSnap = ownerSettingsRef.get().await()
        if (!ownerSettingsSnap.exists()) {
            batch.set(ownerSettingsRef, defaultSettingsPayload)
            hasWrites = true
        }

        val appConfigRef = db.collection("settings").document("app_config")
        val appConfigSnap = try {
            appConfigRef.get().await()
        } catch (_: Exception) {
            null
        }
        if (appConfigSnap != null && !appConfigSnap.exists()) {
            batch.set(appConfigRef, defaultSettingsPayload)
            hasWrites = true
        }

        val adminRef = db.collection("admins").document(uid)
        val adminSnap = adminRef.get().await()
        if (!adminSnap.exists()) {
            batch.set(
                adminRef,
                mapOf(
                    "userId" to uid,
                    "email" to email.ifBlank { "$uid@owner.local" },
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            hasWrites = true
        }

        val userRef = db.collection("users").document(uid)
        val userSnap = userRef.get().await()
        if (!userSnap.exists()) {
            batch.set(
                userRef,
                mapOf(
                    "userId" to uid,
                    "email" to email,
                    "displayName" to displayName,
                    "phone" to "",
                    "role" to "admin",
                    "status" to "active",
                    "tenantId" to "",
                    "buildingId" to "",
                    "flatId" to "",
                    "preferredLanguage" to "bn",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            hasWrites = true
        } else if (userSnap.getString("role") != "admin") {
            batch.update(
                userRef,
                mapOf(
                    "role" to "admin",
                    "status" to "active",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            hasWrites = true
        }

        if (hasWrites) {
            batch.commit().await()
        }
    }

    private suspend fun linkTenantRecordsIfNeeded(
        uid: String,
        email: String,
        profile: UserProfile
    ): UserProfile {
        if (email.isBlank()) return profile
        return try {
            val matchingTenants = db.collection("tenants")
                .whereEqualTo("email", email)
                .get()
                .await()
            val tenantDoc = matchingTenants.documents.firstOrNull() ?: return profile
            val tenantId = tenantDoc.id
            val buildingId = tenantDoc.getString("buildingId").orEmpty()
            val flatId = tenantDoc.getString("flatId").orEmpty()
            val phone = tenantDoc.getString("phone").orEmpty()
            val currentTenantUid = tenantDoc.getString("tenantUserId").orEmpty()

            if (currentTenantUid.isBlank()) {
                tenantDoc.reference.update(
                    mapOf(
                        "tenantUserId" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (flatId.isNotBlank()) {
                val flatDocs = db.collection("flats")
                    .whereEqualTo("tenantEmail", email)
                    .get()
                    .await()
                for (fDoc in flatDocs.documents) {
                    if (fDoc.getString("tenantUserId").orEmpty().isBlank()) {
                        fDoc.reference.update(
                            mapOf(
                                "tenantUserId" to uid,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }

            val billDocs = db.collection("monthly_bills")
                .whereEqualTo("tenantEmail", email)
                .get()
                .await()
            for (bDoc in billDocs.documents) {
                if (bDoc.getString("tenantUserId").orEmpty().isBlank()) {
                    bDoc.reference.update(
                        mapOf(
                            "tenantUserId" to uid,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            if (profile.tenantId != tenantId || profile.buildingId != buildingId || profile.flatId != flatId) {
                db.collection("users").document(uid).update(
                    mapOf(
                        "tenantId" to tenantId,
                        "buildingId" to buildingId,
                        "flatId" to flatId,
                        "phone" to phone.ifBlank { profile.phone },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                profile.copy(
                    tenantId = tenantId,
                    buildingId = buildingId,
                    flatId = flatId,
                    phone = phone.ifBlank { profile.phone }
                )
            } else {
                profile
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("AuthRepository", "Tenant auto-link skipped: ${e.message}")
            profile
        }
    }

    suspend fun getAppSettings(): AppSettings? {
        return try {
            db.collection("settings").document("app_config").get().await()
                .toObject(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "settings/app_config")
            null
        }
    }
}
