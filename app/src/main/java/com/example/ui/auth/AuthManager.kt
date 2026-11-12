package com.example.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthManager"

/**
 * Manages Firebase Authentication operations:
 * - Reactive authStateFlow for user session state
 * - Interactive Google Sign-In with CredentialManager
 * - Password Reset / Forgot Password
 * - Sign Out / Logout
 */
class AuthManager(
    private val auth: FirebaseAuth = Firebase.auth,
    context: Context? = null
) {
    private val prefs = context?.getSharedPreferences("studyos_auth_prefs", Context.MODE_PRIVATE)

    private val _isGuestMode = kotlinx.coroutines.flow.MutableStateFlow(
        prefs?.getBoolean("is_guest_mode", false) ?: false
    )
    val isGuestMode: Flow<Boolean> = _isGuestMode

    fun setGuestMode(isGuest: Boolean) {
        _isGuestMode.value = isGuest
        prefs?.edit()?.putBoolean("is_guest_mode", isGuest)?.apply()
    }

    val isGuestActive: Boolean
        get() = _isGuestMode.value

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Converts FirebaseAuth listener into a reactive Kotlin Flow
     */
    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Interactive Google Sign-In using CredentialManager and GetSignInWithGoogleOption.
     */
    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            val credentialManager = CredentialManager.create(context)
            val serverClientId = context.getString(R.string.default_web_client_id)

            val googleSignInOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleSignInOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("Failed to retrieve user after sign in"))
                }
            } else {
                Result.failure(Exception("Unexpected credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "User cancelled Google Sign-In", e)
            Result.failure(Exception("Sign-in cancelled. Please select an account to continue."))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error during sign in", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to get credentials"))
        } catch (e: Exception) {
            Log.e(TAG, "Authentication failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sends password reset email for registered accounts.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed for $email", e)
            Result.failure(e)
        }
    }

    /**
     * Signs out the user and clears device credential state.
     */
    suspend fun signOut(context: Context) {
        try {
            setGuestMode(false)
            auth.signOut()
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "Error during sign out", e)
        }
    }
}
