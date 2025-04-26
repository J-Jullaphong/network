package org.classapp.network

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.classapp.network.ui.theme.NetworkTheme

class MainActivity : ComponentActivity() {

    private lateinit var navigateToContactsState: MutableState<Boolean>

    @SuppressLint("UnrememberedMutableState")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            navigateToContactsState = mutableStateOf(false)

            NetworkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreenWithBottomNavBar(
                        modifier = Modifier.padding(innerPadding),
                        startDestinationOverride = if (navigateToContactsState.value)
                            DestinationScreens.Contacts.route else null
                    )
                }
            }

            LaunchedEffect(Unit) {
                handleDeepLink(intent) { contactAdded ->
                    if (contactAdded) navigateToContactsState.value = true
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (::navigateToContactsState.isInitialized) {
            handleDeepLink(intent) { contactAdded ->
                if (contactAdded) navigateToContactsState.value = true
            }
        }
    }

    private fun handleDeepLink(intent: Intent?, onContactSaved: (Boolean) -> Unit) {
        val data = intent?.data ?: return
        if (data.scheme == "network" && data.host == "profile") {
            val userIdFromLink = data.lastPathSegment ?: return

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId == null) {
                Toast.makeText(this, "Please log in to save contacts", Toast.LENGTH_SHORT).show()
                onContactSaved(false)
                return
            }

            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("UserProfiles").document(currentUserId)

            userRef.get()
                .addOnSuccessListener { document ->
                    val savedContacts =
                        document.get("SavedContacts") as? List<*> ?: emptyList<Any>()
                    if (userIdFromLink in savedContacts) {
                        Toast.makeText(this, "Contact already saved", Toast.LENGTH_SHORT).show()
                        onContactSaved(true)
                    } else {
                        userRef.update(
                            "SavedContacts",
                            com.google.firebase.firestore.FieldValue.arrayUnion(userIdFromLink)
                        ).addOnSuccessListener {
                            Toast.makeText(this, "Contact added from deeplink!", Toast.LENGTH_SHORT)
                                .show()
                            onContactSaved(true)
                        }.addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Failed to add contact: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            onContactSaved(false)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Failed to fetch contact list: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    onContactSaved(false)
                }
        }
    }
}

@Composable
fun MainScreenWithBottomNavBar(
    modifier: Modifier = Modifier,
    startDestinationOverride: String? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val allNavItems = remember { NetWorkNavItemInfo().getAllNavItems() }
    val navSelectedItem =
        allNavItems.indexOfFirst { it.route == currentRoute }.takeIf { it >= 0 } ?: 0

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("UserProfiles").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val data = document.data
                        userProfile = UserProfile.fromMap(data, userId)
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = colorResource(id = R.color.primaryColor)
                ) {
                    allNavItems.forEachIndexed { index, itemInfo ->
                        NavigationBarItem(
                            selected = index == navSelectedItem,
                            onClick = {
                                navController.navigate(itemInfo.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = itemInfo.icon,
                                    contentDescription = itemInfo.label
                                )
                            },
                            label = { Text(text = itemInfo.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF6299E4),
                                unselectedIconColor = Color.White,
                                selectedTextColor = Color(0xFF6299E4),
                                unselectedTextColor = Color.White,
                                indicatorColor = colorResource(id = R.color.primaryColor)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            val isGoogleSignIn = FirebaseAuth.getInstance().currentUser
                ?.providerData
                ?.any { it.providerId == "google.com" } == true

            NavHost(
                navController = navController,
                startDestination = startDestinationOverride ?: DestinationScreens.MyCard.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(route = DestinationScreens.MyCard.route) {
                    userProfile?.let { profile -> MyCardScreen(profile = profile) }
                }
                composable(route = DestinationScreens.Discovery.route) {
                    DiscoveryScreen()
                }
                composable(route = DestinationScreens.Contacts.route) {
                    ContactsScreen()
                }
                composable(route = DestinationScreens.Settings.route) {
                    userProfile?.let { profile ->
                        SettingsScreen(
                            user = profile,
                            isGoogleSignIn = isGoogleSignIn
                        )
                    } ?: Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Profile not loaded.")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    NetworkTheme {
        MainScreenWithBottomNavBar()
    }
}
