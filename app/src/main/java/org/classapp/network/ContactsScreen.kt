package org.classapp.network

import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import org.classapp.network.ui.theme.NetworkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen() {
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var contacts by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedProfile by remember { mutableStateOf<UserProfile?>(null) }


    fun removeContact(contactId: String) {
        if (currentUserId == null) return

        val userDocRef = db.collection("UserProfiles").document(currentUserId)

        userDocRef.get().addOnSuccessListener { document ->
            val currentContacts =
                document.get("SavedContacts") as? MutableList<*> ?: mutableListOf<String>()
            val updatedContacts = currentContacts.filterNot { it == contactId }

            userDocRef.update("SavedContacts", updatedContacts)
                .addOnSuccessListener {
                    contacts = contacts.filterNot { it.userId == contactId }
                }
                .addOnFailureListener {
                    Log.e("ContactsScreen", "Failed to update contacts list after removal", it)
                }
        }
    }

    LaunchedEffect(true) {
        if (currentUserId == null) return@LaunchedEffect

        db.collection("UserProfiles").document(currentUserId).get()
            .addOnSuccessListener { userDoc ->
                val savedContacts = userDoc.get("SavedContacts") as? List<*> ?: emptyList<Any>()

                if (savedContacts.isEmpty()) {
                    contacts = emptyList()
                    isLoading = false
                    return@addOnSuccessListener
                }

                db.collection("UserProfiles")
                    .whereIn(FieldPath.documentId(), savedContacts)
                    .get()
                    .addOnSuccessListener { result ->
                        val list = result.documents.mapNotNull { doc ->
                            val userId = doc.id
                            if (userId != currentUserId) {
                                UserProfile.fromMap(doc.data, userId)
                            } else null
                        }
                        contacts = list
                        isLoading = false
                    }
                    .addOnFailureListener { e ->
                        Log.e("ContactsScreen", "Error fetching contacts", e)
                        isLoading = false
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ContactsScreen", "Failed to fetch saved contacts", e)
                isLoading = false
            }
    }

    NetworkTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "My Contacts",
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
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val filtered = contacts.filter {
                        it.fullName.contains(searchQuery.text, ignoreCase = true) ||
                                it.organization.contains(searchQuery.text, ignoreCase = true) ||
                                it.career.contains(searchQuery.text, ignoreCase = true)
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filtered, key = { it.userId }) { contact ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                                        removeContact(contact.userId)
                                        true
                                    } else false
                                }
                            )

                            val target = dismissState.targetValue
                            val isSwiping = target != SwipeToDismissBoxValue.Settled

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    if (isSwiping) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = when (target) {
                                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                                else -> Alignment.Center
                                            },
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.Red
                                            )
                                        }
                                    }
                                },
                                content = {
                                    SavedContactItem(
                                        profile = contact,
                                        onViewClick = { selectedProfile = it })
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
                        .padding(horizontal = 16.dp, vertical = 32.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize(),
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

}

@Composable
fun SavedContactItem(profile: UserProfile, onViewClick: (UserProfile) -> Unit) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onViewClick(profile) },
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

        IconButton(onClick = { onViewClick(profile) }) {
            Image(
                FontAwesome.Icon.faw_eye,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
