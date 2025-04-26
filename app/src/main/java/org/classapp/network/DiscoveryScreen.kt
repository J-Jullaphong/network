package org.classapp.network

import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.classapp.network.ui.theme.NetworkTheme

@Composable
fun DiscoveryScreen() {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var profiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedProfile by remember { mutableStateOf<UserProfile?>(null) }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(true) {
        if (currentUserId == null) return@LaunchedEffect

        val currentUserRef = db.collection("UserProfiles").document(currentUserId)

        currentUserRef.get()
            .addOnSuccessListener { userDoc ->
                val savedContacts = userDoc.get("SavedContacts") as? List<*> ?: emptyList<Any>()

                db.collection("UserProfiles")
                    .whereEqualTo("visibility", "Public")
                    .get()
                    .addOnSuccessListener { result ->
                        val list = result.documents.mapNotNull { doc ->
                            val userId = doc.id
                            if (
                                userId != currentUserId &&
                                !savedContacts.contains(userId)
                            ) {
                                UserProfile.fromMap(doc.data, userId)
                            } else null
                        }
                        profiles = list
                        isLoading = false
                    }
                    .addOnFailureListener { e ->
                        Log.e("DiscoveryScreen", "Error fetching profiles", e)
                        isLoading = false
                    }
            }
            .addOnFailureListener { e ->
                Log.e("DiscoveryScreen", "Error fetching current user contacts", e)
                isLoading = false
            }
    }

    NetworkTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Networking & Discovery",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search names, companies, and ...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                shape = RoundedCornerShape(48.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val filteredProfiles = profiles.filter {
                    it.fullName.contains(searchQuery.text, ignoreCase = true) ||
                            it.organization.contains(searchQuery.text, ignoreCase = true) ||
                            it.career.contains(searchQuery.text, ignoreCase = true) ||
                            it.city.contains(searchQuery.text, ignoreCase = true) ||
                            it.country.contains(searchQuery.text, ignoreCase = true)
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredProfiles) { profile ->
                        DiscoveryProfileItem(
                            profile = profile,
                            onClick = { selectedProfile = it },
                            onSaved = { savedProfile ->
                                profiles = profiles.filter { it.userId != savedProfile.userId }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        if (selectedProfile != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            ProfileCard(profile = selectedProfile!!)
                        }

                        IconButton(
                            onClick = { selectedProfile = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }


    }
}

@Composable
fun DiscoveryProfileItem(
    profile: UserProfile,
    onClick: (UserProfile) -> Unit,
    onSaved: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick(profile) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!profile.profileImage.isNullOrEmpty()) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(120, 120)
                        scaleType = ImageView.ScaleType.CENTER_CROP

                        Glide.with(this)
                            .load(profile.profileImage)
                            .circleCrop()
                            .into(this)
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .padding(end = 12.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.fullName,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
            )

            if (!profile.career.isNullOrBlank()) {
                Text(text = profile.career, fontSize = 14.sp, color = Color.Gray)
            }

            if (!profile.organization.isNullOrBlank()) {
                Text(text = profile.organization, fontSize = 14.sp, color = Color.Gray)
            }

            val location = listOfNotNull(profile.city, profile.country).joinToString(", ")
            if (location.isNotBlank()) {
                Text(text = location, fontSize = 14.sp, color = Color.Gray)
            }
        }

        IconButton(onClick = {
            currentUserId?.let { uid ->
                db.collection("UserProfiles")
                    .document(uid)
                    .update("SavedContacts", FieldValue.arrayUnion(profile.userId))
                    .addOnSuccessListener {
                        Toast.makeText(context, "Contact saved!", Toast.LENGTH_SHORT).show()
                        onSaved(profile)
                    }
                    .addOnFailureListener { e ->
                        Log.e("DiscoveryScreen", "Failed to save contact: ${e.message}")
                        Toast.makeText(context, "Failed to save contact", Toast.LENGTH_SHORT).show()
                    }
            }
        }) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Connect",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
