package org.classapp.network

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import org.classapp.network.ui.theme.NetworkTheme

@Composable
fun SettingsScreen(
    user: UserProfile,
    isGoogleSignIn: Boolean
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var selectedVisibility by remember { mutableStateOf(user.visibility ?: "Private") }
    var expanded by remember { mutableStateOf(false) }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }

    NetworkTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!user.profileImage.isNullOrEmpty()) {
                    ProfileImageWithGlide(user.profileImage, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Column {
                    Text(user.fullName, style = MaterialTheme.typography.titleMedium)
                    Text(user.email, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Visibility", style = MaterialTheme.typography.labelMedium)

            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val visibilityIcon = if (selectedVisibility == "Public") {
                        FontAwesome.Icon.faw_eye
                    } else {
                        FontAwesome.Icon.faw_eye_slash
                    }

                    Image(
                        visibilityIcon,
                        contentDescription = selectedVisibility,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedVisibility)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                }

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("Public", "Private").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedVisibility = option
                                expanded = false

                                db.collection("UserProfiles")
                                    .document(user.userId)
                                    .update("visibility", option)
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            context,
                                            "Visibility updated",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            context,
                                            "Failed to update",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            SettingsItem(icon = Icons.Default.Person, label = "Edit Profile") {
                context.startActivity(Intent(context, ProfileSetupActivity::class.java))
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            if (!isGoogleSignIn) {
                SettingsItem(icon = Icons.Default.Lock, label = "Change Password") {
                    showPasswordDialog = true
                }
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    auth.signOut()
                    context.startActivity(Intent(context, AuthenticationActivity::class.java))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Text("Log Out", color = Color.White)
            }
        }

        if (showPasswordDialog) {
            var confirmPassword by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = {
                    showPasswordDialog = false
                    newPassword = ""
                    confirmPassword = ""
                },
                confirmButton = {
                    TextButton(onClick = {
                        val currentUser = auth.currentUser

                        when {
                            newPassword.isBlank() || confirmPassword.isBlank() -> {
                                Toast.makeText(
                                    context,
                                    "Please fill in all fields",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            newPassword.length < 6 -> {
                                Toast.makeText(
                                    context,
                                    "Password must be at least 6 characters",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            newPassword != confirmPassword -> {
                                Toast.makeText(
                                    context,
                                    "Passwords do not match",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            else -> {
                                currentUser?.updatePassword(newPassword)
                                    ?.addOnSuccessListener {
                                        Toast.makeText(
                                            context,
                                            "Password updated successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        showPasswordDialog = false
                                        newPassword = ""
                                        confirmPassword = ""
                                    }
                                    ?.addOnFailureListener {
                                        Toast.makeText(
                                            context,
                                            "Failed to update password: ${it.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }
                    }) {
                        Text("Change")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPasswordDialog = false
                        newPassword = ""
                        confirmPassword = ""
                    }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Change Password") },
                text = {
                    Column {
                        Text("Enter a new password:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            placeholder = { Text("New Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            placeholder = { Text("Confirm Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
