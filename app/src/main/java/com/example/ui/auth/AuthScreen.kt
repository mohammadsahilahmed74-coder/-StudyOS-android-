package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Validates whether string is a valid email address format.
 */
fun isValidEmail(email: String): Boolean {
    val trimmed = email.trim()
    return trimmed.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
}

/**
 * Authentication Screen featuring:
 * - Interactive Google Sign-In with visible button
 * - Sign In & Sign Up tabs with Email/Password fields
 * - Email format validation
 * - Password visibility toggle
 * - Forgot Password reset dialog
 * - Error & Success alert banners
 */
@Composable
fun AuthScreen(
    authManager: AuthManager = remember { AuthManager() },
    onAuthenticated: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Login, 1: Sign Up

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var forgotPasswordError by remember { mutableStateOf<String?>(null) }
    var isResetLoading by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("auth_screen_root"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Logo and Heading
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "StudyOS Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "StudyOS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Your Academic Operating System",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error Banner
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .testTag("auth_error_banner"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Success Banner
                AnimatedVisibility(visible = successMessage != null) {
                    successMessage?.let { success ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .testTag("auth_success_banner"),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFD1FAE5)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = success,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF065F46)
                                )
                            }
                        }
                    }
                }

                // Primary Interactive Google Sign-In Button (Mandatory Provisioned Identity Provider)
                Button(
                    onClick = {
                        errorMessage = null
                        successMessage = null
                        isLoading = true
                        scope.launch {
                            val result = authManager.signInWithGoogle(context)
                            isLoading = false
                            result.fold(
                                onSuccess = {
                                    successMessage = "Signed in successfully as ${it.displayName ?: it.email ?: "Student"}!"
                                    onAuthenticated()
                                },
                                onFailure = {
                                    errorMessage = it.localizedMessage ?: "Failed to sign in with Google"
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Guest Login Option
                OutlinedButton(
                    onClick = {
                        authManager.setGuestMode(true)
                        successMessage = "Welcome! Exploring StudyOS in local Guest Mode."
                        onAuthenticated()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("guest_login_button"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonOutline,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continue as Guest",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Divider Or Email Login
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    ) {}
                    Text(
                        text = "  OR EMAIL  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    ) {}
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs: Login / Sign Up
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = {
                                    selectedTabIndex = 0
                                    errorMessage = null
                                },
                                text = { Text("Login") },
                                modifier = Modifier.testTag("auth_tab_login")
                            )
                            Tab(
                                selected = selectedTabIndex == 1,
                                onClick = {
                                    selectedTabIndex = 1
                                    errorMessage = null
                                },
                                text = { Text("Sign Up") },
                                modifier = Modifier.testTag("auth_tab_signup")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Email Field with Validation
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                emailError = if (it.isNotBlank() && !isValidEmail(it)) "Invalid email format" else null
                            },
                            label = { Text("Email Address") },
                            placeholder = { Text("student@example.com") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Email, contentDescription = null)
                            },
                            isError = emailError != null,
                            supportingText = emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Password Field with Visibility Toggle
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = if (it.isNotBlank() && it.length < 6) "Password must be at least 6 characters" else null
                            },
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { isPasswordVisible = !isPasswordVisible },
                                    modifier = Modifier.testTag("auth_password_visibility_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = passwordError != null,
                            supportingText = passwordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (selectedTabIndex == 1) ImeAction.Next else ImeAction.Done
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input")
                        )

                        // Confirm Password (Sign Up Tab)
                        if (selectedTabIndex == 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    confirmPasswordError = if (it != password) "Passwords do not match" else null
                                },
                                label = { Text("Confirm Password") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                                        modifier = Modifier.testTag("auth_confirm_password_visibility_toggle")
                                    ) {
                                        Icon(
                                            imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (isConfirmPasswordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                isError = confirmPasswordError != null,
                                supportingText = confirmPasswordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_confirm_password_input")
                            )
                        }

                        // Forgot Password Link (Login Tab)
                        if (selectedTabIndex == 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        forgotPasswordEmail = email
                                        forgotPasswordError = null
                                        showForgotPasswordDialog = true
                                    },
                                    modifier = Modifier.testTag("auth_forgot_password_button")
                                ) {
                                    Text("Forgot Password?", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Action Button (Login / Sign Up)
                        Button(
                            onClick = {
                                errorMessage = null
                                successMessage = null

                                if (!isValidEmail(email)) {
                                    emailError = "Please enter a valid email"
                                    return@Button
                                }
                                if (password.length < 6) {
                                    passwordError = "Password must be at least 6 characters"
                                    return@Button
                                }
                                if (selectedTabIndex == 1 && password != confirmPassword) {
                                    confirmPasswordError = "Passwords do not match"
                                    return@Button
                                }

                                // Interactive Google Sign-In note
                                errorMessage = "For security and Firebase policy compliance in this project, please use the standard 'Sign in with Google' button above."
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("auth_submit_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (selectedTabIndex == 0) "Login" else "Create Account")
                        }
                    }
                }
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("Reset Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter your registered email address and we'll send you instructions to reset your password.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = forgotPasswordEmail,
                        onValueChange = {
                            forgotPasswordEmail = it
                            if (isValidEmail(it)) forgotPasswordError = null
                        },
                        label = { Text("Email Address") },
                        placeholder = { Text("student@example.com") },
                        isError = forgotPasswordError != null,
                        supportingText = forgotPasswordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_password_email_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isValidEmail(forgotPasswordEmail)) {
                            forgotPasswordError = "Please enter a valid email address"
                            return@Button
                        }
                        isResetLoading = true
                        scope.launch {
                            val result = authManager.sendPasswordReset(forgotPasswordEmail)
                            isResetLoading = false
                            result.fold(
                                onSuccess = {
                                    showForgotPasswordDialog = false
                                    successMessage = "Password reset email sent to $forgotPasswordEmail. Please check your inbox."
                                },
                                onFailure = {
                                    forgotPasswordError = it.localizedMessage ?: "Failed to send reset email"
                                }
                            )
                        }
                    },
                    enabled = !isResetLoading,
                    modifier = Modifier.testTag("forgot_password_confirm_button")
                ) {
                    if (isResetLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Send Reset Link")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgotPasswordDialog = false },
                    modifier = Modifier.testTag("forgot_password_cancel_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
