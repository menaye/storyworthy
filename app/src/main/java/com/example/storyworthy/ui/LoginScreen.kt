package com.example.storyworthy.ui

import android.content.Context
import android.content.MutableContextWrapper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.storyworthy.R
import com.example.storyworthy.ui.theme.StoryworthyTheme
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.Base64
import kotlin.time.Duration.Companion.milliseconds

const val TAG = "LoginScreen"

sealed class SignInResult {
    data class Success(val idToken: String) : SignInResult()
    data class Failure(val exception: Exception) : SignInResult()
}

suspend fun signIn(request: GetCredentialRequest, context: Context): SignInResult {
    val credentialManager = CredentialManager.create(context)
    val failureMessage = "Sign in failed!"
    delay(250.milliseconds)
    return try {
        val result = credentialManager.getCredential(
            request = request,
            context = MutableContextWrapper(context),
        )
        Log.i(TAG, result.toString())

        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            Log.i(TAG, "Signed in as: ${googleIdTokenCredential.id}")
            Log.i(TAG, "(☞ﾟヮﾟ)☞  Sign in Successful!  ☜(ﾟヮﾟ☜)")
            SignInResult.Success(googleIdTokenCredential.idToken)
        } else {
            SignInResult.Failure(Exception("Received unexpected credential type"))
        }
    } catch (e: Exception) {
        val message = when(e) {
            is GoogleIdTokenParsingException -> "Issue with parsing received GoogleIdToken"
            is NoCredentialException -> "No credentials found"
            is GetCredentialCancellationException -> "Sign-in was cancelled"
            is GetCredentialCustomException -> "Issue with custom credential request"
            is GetCredentialException -> "Failure getting credentials"
            else -> e.message ?: "Unknown error"
        }
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
        Log.e(TAG, "$failureMessage: $message", e)
        SignInResult.Failure(e)
    }
}

fun generateSecureRandomNonce(byteLength: Int = 32): String {
    val randomBytes = ByteArray(byteLength)
    SecureRandom.getInstanceStrong().nextBytes(randomBytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
}

@Composable
fun ButtonUI(webClientId: String, onTokenReceived: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val onClick: () -> Unit = {
        val signInWithGoogleOption: GetSignInWithGoogleOption = GetSignInWithGoogleOption
            .Builder(serverClientId = webClientId)
            .setNonce(generateSecureRandomNonce())
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        coroutineScope.launch {
            val result = signIn(request, context)
            if (result is SignInResult.Success) {
                onTokenReceived(result.idToken)
            }
        }
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp), 
        // Removing the border and setting a common Google Light Gray background
        // to see if it blends better with the siwg2.png asset.
        color = Color(0xFFF1F3F4) 
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.siwg_button),
                contentDescription = null,
                modifier = Modifier.fillMaxHeight(),
                contentScale = ContentScale.Fit
            )
        }
    }
}


@Composable
fun LoginScreen(
    onNavigateToSignup: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()

    LoginContent(
        authState = authState,
        onLogin = { email, password -> viewModel.login(email, password) },
        onNavigateToSignup = onNavigateToSignup,
        onGoogleLogin = { idToken -> viewModel.loginWithGoogle(idToken) },
        resetPassword = {}
    )

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onLoginSuccess()
        }
    }
}

@Composable
fun LoginContent(
    authState: AuthState,
    onLogin: (String, String) -> Unit,
    onNavigateToSignup: () -> Unit,
    onGoogleLogin: (String) -> Unit,
    resetPassword: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,

    ) {
        Image(
        painterResource(id = R.drawable.sw_icon_bright),
        contentDescription = "Application icon detailing a blue sparkling feather pen encased in a circle.",
        modifier = Modifier
            .padding(top = 64.dp)
            .size(80.dp),
        alignment = Alignment.Center)
        Text("Storyworthy", modifier=Modifier.padding(top=154.dp))
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Welcome Back!", style = MaterialTheme.typography.headlineLarge)
        Text("Log in to write your daily entry.")

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            shape = MaterialTheme.shapes.medium,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            shape = MaterialTheme.shapes.medium,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { resetPassword() },
            modifier = Modifier.align(Alignment.End),
            content = { Text("Forgot Password?") })

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onLogin(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val webClientId = "413799267087-ojtug93vlnjpkm495ijt0ic6tu37doju.apps.googleusercontent.com"
        ButtonUI(webClientId, onTokenReceived = onGoogleLogin)

        TextButton(onClick = onNavigateToSignup) {
            Text("Don't have an account? Sign up")
        }

        if (authState is AuthState.Error) {
            Text(authState.message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    StoryworthyTheme {
        LoginContent(
            authState = AuthState.Idle,
            onLogin = { _, _ -> },
            onNavigateToSignup = {},
            onGoogleLogin = {},
            resetPassword = {}
        )
    }
}
