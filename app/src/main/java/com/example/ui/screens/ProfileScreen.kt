package com.example.ui.screens

import android.net.Uri
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.auth.isValidEmail
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * User Profile Screen allowing users to view and update:
 * - Display Name
 * - Email address
 * - Profile Avatar / Picture (via Photo Picker and custom URLs)
 * - Account Password
 * All changes persist directly to Firebase Auth & local preferences.
 */
@Composable
fun ProfileScreen(
    user: FirebaseUser?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth: FirebaseAuth = remember { Firebase.auth }

    val sharedPrefs = remember {
        context.getSharedPreferences("studyos_user_profile_prefs", android.content.Context.MODE_PRIVATE)
    }

    val isGuest = user == null

    // Persisted Avatar URI in local prefs as backup
    val userKey = user?.uid ?: "guest"
    val persistedAvatarUri = sharedPrefs.getString("custom_avatar_uri_$userKey", user?.photoUrl?.toString())

    val initialName = if (isGuest) {
        sharedPrefs.getString("user_name_guest", "Guest User") ?: "Guest User"
    } else {
        user?.displayName?.takeIf { it.isNotBlank() }
            ?: sharedPrefs.getString("user_name_${user.uid}", null)
            ?: user.email?.substringBefore("@")
            ?: "Student"
    }

    val initialEmail = if (isGuest) {
        sharedPrefs.getString("user_email_guest", "Guest Account (Local Mode)") ?: "Guest Account (Local Mode)"
    } else {
        user?.email ?: ""
    }

    var displayName by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }
    var avatarUriString by remember { mutableStateOf(persistedAvatarUri ?: "") }

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isNewPasswordVisible by remember { mutableStateOf(false) }

    var isSavingProfile by remember { mutableStateOf(false) }
    var isUpdatingPassword by remember { mutableStateOf(false) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var shortsStatus by remember {
        mutableStateOf(com.example.util.YouTubeShortsBlockerManager.getStatus(context))
    }

    // Photo picker launcher (complying with zero-permission Android Photo Picker standard)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarUriString = uri.toString()
            sharedPrefs.edit().putString("custom_avatar_uri_$userKey", uri.toString()).apply()
            statusMessage = "Profile picture selected! Click 'Save Profile Changes' to persist."
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("profile_screen_root"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("profile_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Dashboard"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "User Profile & Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Feedback Banners
        AnimatedVisibility(visible = errorMessage != null) {
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("profile_error_banner"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        AnimatedVisibility(visible = statusMessage != null) {
            statusMessage?.let { status ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("profile_status_banner"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(status, style = MaterialTheme.typography.bodySmall, color = Color(0xFF065F46))
                    }
                }
            }
        }

        // Profile Avatar Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("profile_avatar_picker_box"),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUriString.isNotBlank()) {
                        AsyncImage(
                            model = avatarUriString,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Picture",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change photo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = displayName.ifBlank { "Student" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = email.ifBlank { "No email associated" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag("change_photo_button")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change Picture")
                }
            }
        }

        // Personal Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Personal Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("profile_email_input")
                )

                Button(
                    onClick = {
                        errorMessage = null
                        statusMessage = null
                        if (displayName.isBlank()) {
                            errorMessage = "Display name cannot be empty."
                            return@Button
                        }
                        if (email.isNotBlank() && !isValidEmail(email)) {
                            errorMessage = "Please enter a valid email address."
                            return@Button
                        }

                        isSavingProfile = true
                        scope.launch {
                            try {
                                val currentUser = auth.currentUser
                                if (currentUser != null) {
                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                        .setDisplayName(displayName.trim())
                                        .apply {
                                            if (avatarUriString.isNotBlank()) {
                                                setPhotoUri(Uri.parse(avatarUriString))
                                            }
                                        }
                                        .build()

                                    currentUser.updateProfile(profileUpdates).await()

                                    // Save in SharedPreferences for permanent local persistence
                                    sharedPrefs.edit()
                                        .putString("user_name_${currentUser.uid}", displayName.trim())
                                        .putString("custom_avatar_uri_${currentUser.uid}", avatarUriString)
                                        .apply()

                                    statusMessage = "Profile information updated successfully!"
                                } else {
                                    // GUEST MODE: Save custom name and avatar locally on-device
                                    sharedPrefs.edit()
                                        .putString("user_name_guest", displayName.trim())
                                        .putString("user_email_guest", email.trim().ifBlank { "Guest Account" })
                                        .putString("custom_avatar_uri_guest", avatarUriString)
                                        .apply()

                                    statusMessage = "Guest profile updated and saved locally!"
                                }
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: "Failed to update profile."
                            } finally {
                                isSavingProfile = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_button"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSavingProfile
                ) {
                    if (isSavingProfile) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile Changes")
                    }
                }
            }
        }

        // Change Password Card (Only for authenticated Firebase accounts)
        if (!isGuest) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Change Account Password",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(
                                onClick = { isNewPasswordVisible = !isNewPasswordVisible },
                                modifier = Modifier.testTag("toggle_new_password_visibility")
                            ) {
                                Icon(
                                    imageVector = if (isNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isNewPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("profile_new_password_input")
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm New Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("profile_confirm_password_input")
                    )

                    Button(
                        onClick = {
                            errorMessage = null
                            statusMessage = null
                            if (newPassword.length < 6) {
                                errorMessage = "New password must be at least 6 characters long."
                                return@Button
                            }
                            if (newPassword != confirmPassword) {
                                errorMessage = "Passwords do not match."
                                return@Button
                            }

                            isUpdatingPassword = true
                            scope.launch {
                                try {
                                    val currentUser = auth.currentUser
                                    if (currentUser != null) {
                                        currentUser.updatePassword(newPassword.trim()).await()
                                        newPassword = ""
                                        confirmPassword = ""
                                        statusMessage = "Password successfully changed!"
                                    } else {
                                        errorMessage = "Please log in again to update your password."
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.localizedMessage ?: "Failed to update password. You may need to re-login first."
                                } finally {
                                    isUpdatingPassword = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("update_password_button"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isUpdatingPassword
                    ) {
                        if (isUpdatingPassword) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        } else {
                            Text("Update Password")
                        }
                    }
                }
            }
        }

        // Digital Well-Being: Independent YouTube Shorts Blocker in Settings
        YouTubeShortsBlockerCard(
            status = shortsStatus,
            onToggleEnabled = { enabled ->
                com.example.util.YouTubeShortsBlockerManager.setBlockerEnabled(context, enabled)
                shortsStatus = com.example.util.YouTubeShortsBlockerManager.getStatus(context)
            },
            onTemporarilyDisable = { mins ->
                com.example.util.YouTubeShortsBlockerManager.temporarilyDisable(context, mins)
                shortsStatus = com.example.util.YouTubeShortsBlockerManager.getStatus(context)
            },
            onCancelTemporaryDisable = {
                com.example.util.YouTubeShortsBlockerManager.cancelTemporaryDisable(context)
                shortsStatus = com.example.util.YouTubeShortsBlockerManager.getStatus(context)
            },
            onRequestAccessibilityPerm = {
                com.example.util.YouTubeShortsBlockerManager.openAccessibilitySettings(context)
            }
        )

        // About StudyOS Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_about_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "About StudyOS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "A complete modern academic operating system for ambitious students, featuring smart task tracking, syllabus revision, timetable management, and Regain-style deep focus sessions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Version",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "v2.4 (Production)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Developer & Creator",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "Created by MD. Sahil Ahmad",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
