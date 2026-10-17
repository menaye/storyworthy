package com.example.storyworthy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyworthy.Entry
import com.example.storyworthy.User
import com.example.storyworthy.data.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val currentUser get() = repository.getCurrentUser()

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries

    init {
        checkUserSession()
        loadUserEntries()
    }

    fun loadUserEntries() {
        val firebaseUser = repository.getCurrentUser()
        if (firebaseUser != null) {
            viewModelScope.launch {
                val result = repository.fetchUserEntries(firebaseUser.uid)
                if (result.isSuccess) {
                    _entries.value = result.getOrDefault(emptyList())
                }
            }
        }
    }

    private fun checkUserSession() {
        viewModelScope.launch {
            val firebaseUser = repository.getCurrentUser()
            if (firebaseUser != null) {
                _authState.value = AuthState.Loading
                val result = repository.fetchUserData(firebaseUser.uid)
                if (result.isSuccess) {
                    _authState.value = AuthState.Success(result.getOrThrow())
                } else {
                    _authState.value = AuthState.Idle
                }
            }
        }
    }

    suspend fun verifyAccountCredentials(uName: String, psswd: String, psswd2: String, fName: String, lName: String, email: String): String? {
        // empty fields
        if (uName.isEmpty() || psswd.isEmpty() || fName.isEmpty() || lName.isEmpty() || email.isEmpty()) {
            return "All fields must be filled"
        }
        // username is taken
        if (repository.usernameExists(uName)) {
            return "Username is already taken"
        }
        // email is taken
        if (repository.emailExists(email)) {
            return "Email is already in use"
        }
        // passwords don't match
        if (psswd.isNotEmpty() && psswd2.isNotEmpty() && psswd != psswd2) {
            return "Passwords do not match"
        }
        // password length check
        if (psswd.length < 8) {
            return "Password must be at least 8 characters long"
        }
        // password contains at least one number
        if (!psswd.any { it.isDigit() }) {
            return "Password must contain at least one number"
        }
        // password contains at least one special character
        if (!psswd.any { !it.isLetterOrDigit() }) {
            return "Password must contain at least one special character"
        }
        return null
    }

    fun signup(uName: String, psswd: String, fName: String, lName: String, email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val validationError = verifyAccountCredentials(uName, psswd, psswd, fName, lName, email)
            if (validationError != null) {
                _authState.value = AuthState.Error(validationError)
                return@launch
            }
            
            val user = User(uName, psswd, fName, lName)
            user.email = email
            
            val result = repository.signup(user, psswd)
            if (result.isSuccess) {
                _authState.value = AuthState.Success(result.getOrThrow())
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Signup failed")
            }
        }
    }

    fun login(email: String, psswd: String) {
        if (email.isEmpty() || psswd.isEmpty()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.login(email, psswd)
            if (result.isSuccess) {
                _authState.value = AuthState.Success(result.getOrThrow())
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Login failed. Please try again.")
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.loginWithGoogle(idToken)
            if (result.isSuccess) {
                _authState.value = AuthState.Success(result.getOrThrow())
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Google Login failed")
            }
        }
    }

    fun resetPassword() {
        // Implement password reset logic here
    }

    fun toggleStar(entry: Entry) {
        val firebaseUser = repository.getCurrentUser()
        val entryId = entry.id
        if (firebaseUser != null && !entryId.isNullOrBlank()) {
            val newStarred = !entry.isStarred
            entry.isStarred = newStarred
            viewModelScope.launch {
                val result = repository.updateEntryStarStatus(firebaseUser.uid, entryId, newStarred)
                if (result.isFailure) {
                    entry.isStarred = !newStarred
                } else {
                    loadUserEntries()
                }
            }
        }
    }

    fun deleteEntry(entry: Entry) {
        val firebaseUser = repository.getCurrentUser()
        val entryId = entry.id
        _entries.value = _entries.value.filter { it != entry && (entryId.isNullOrBlank() || it.id != entryId) }
        if (firebaseUser != null && !entryId.isNullOrBlank()) {
            viewModelScope.launch {
                repository.deleteEntry(firebaseUser.uid, entryId)
                loadUserEntries()
            }
        }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        _authState.value = AuthState.Idle
    }
}
