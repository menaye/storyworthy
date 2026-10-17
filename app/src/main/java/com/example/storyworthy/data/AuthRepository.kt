package com.example.storyworthy.data

import com.example.storyworthy.Entry
import com.example.storyworthy.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth? get() = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
    private val firestore: FirebaseFirestore? get() = try { FirebaseFirestore.getInstance() } catch (_: Exception) { null }

    suspend fun signup(user: User, password: String): Result<User> {
        return try {
            val auth = auth ?: throw Exception("Firebase not initialized")
            val firestore = firestore ?: throw Exception("Firebase not initialized")
            val authResult = auth.createUserWithEmailAndPassword(user.email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("Signup failed")
            
            // Save additional user info to Firestore
            firestore.collection("users").document(uid).set(user).await()
            
            Result.success(user)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun login(email: String, psswd: String): Result<User> {
        return try {
            val auth = auth ?: throw Exception("Firebase not initialized")
            val authResult = auth.signInWithEmailAndPassword(email, psswd).await()
            val uid = authResult.user?.uid ?: throw Exception("Login failed")
            fetchUserData(uid)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun loginWithGoogle(idToken: String): Result<User> {
        return try {
            val auth = auth ?: throw Exception("Firebase not initialized")
            val firestore = firestore ?: throw Exception("Firebase not initialized")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val uid = authResult.user?.uid ?: throw Exception("Google sign in failed")
            
            // Check if user exists in Firestore, if not create a skeleton
            val userResult = fetchUserData(uid)
            if (userResult.isSuccess) {
                userResult
            } else {
                val firebaseUser = authResult.user
                val newUser = User(
                    firebaseUser?.displayName ?: "Google User",
                    "", // No password for Google users
                    firebaseUser?.displayName?.split(" ")?.firstOrNull() ?: "Google",
                    firebaseUser?.displayName?.split(" ")?.lastOrNull() ?: "User"
                ).apply {
                    email = firebaseUser?.email
                }
                firestore.collection("users").document(uid).set(newUser).await()
                Result.success(newUser)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun fetchUserData(uid: String): Result<User> {
        return try {
            val firestore = firestore ?: throw Exception("Firebase not initialized")
            val user = firestore.collection("users").document(uid).get().await().toObject(User::class.java)
            if (user != null) Result.success(user) else throw Exception("User not found")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    fun getCurrentUser() = auth?.currentUser

    suspend fun usernameExists(username: String): Boolean {
        return try {
            val firestore = firestore ?: return false
            val snapshot = firestore.collection("users")
                .whereEqualTo("uName", username)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        }
    }

    suspend fun emailExists(email: String): Boolean {
        return try {
            val firestore = firestore ?: return false
            val snapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        }
    }

    suspend fun fetchUserEntries(uid: String): Result<List<Entry>> {
        return try {
            val firestore = firestore ?: throw Exception("Firebase not initialized")
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("user-entries")
                .get()
                .await()
            val entries = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Entry::class.java)
            }
            Result.success(entries)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun updateEntryStarStatus(uid: String, entryId: String, isStarred: Boolean): Result<Unit> {
        return try {
            val firestore = firestore ?: throw Exception("Firebase not initialized")
            firestore.collection("users")
                .document(uid)
                .collection("user-entries")
                .document(entryId)
                .update("isStarred", isStarred)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun deleteEntry(uid: String, entryId: String): Result<Unit> {
        return try {
            val firestore = firestore ?: throw Exception("Firebase not initialized")
            firestore.collection("users")
                .document(uid)
                .collection("user-entries")
                .document(entryId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
