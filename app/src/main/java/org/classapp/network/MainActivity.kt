package org.classapp.network

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.classapp.network.ui.theme.NetworkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetworkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    UserGreeting(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun UserGreeting(modifier: Modifier = Modifier) {
    var userName by remember { mutableStateOf("Guest") }

    val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(user) {
        userName = user?.displayName ?: "Guest"
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Hello, $userName!",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NetworkTheme {
        UserGreeting()
    }
}
