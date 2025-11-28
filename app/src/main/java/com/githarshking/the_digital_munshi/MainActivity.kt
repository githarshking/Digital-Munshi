package com.githarshking.the_digital_munshi

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import com.githarshking.the_digital_munshi.ui.theme.DigitalMunshiTheme // Make sure this import matches your project

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // This is the root of our app's theme
            DigitalMunshiTheme {
                MunshiApp()
            }
        }
    }
}

/**
 * This is the main "app" composable.
 * It sets up the basic screen structure.
 */
@OptIn(ExperimentalMaterial3Api::class) // Needed for Scaffold/TopAppBar
@Composable
fun MunshiApp() {
    Scaffold(
        topBar = {
            // This is the bar you see at the top of the app
            TopAppBar(
                title = { Text("Digital Munshi") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary, // Gets the main color from your theme
                    titleContentColor = MaterialTheme.colorScheme.onPrimary // Text color on that main color
                )
            )
        }
    ) { innerPadding -> // This 'padding' is from the Scaffold

        // This is the main content area
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Apply padding so content isn't under the top bar
        ) {
            // We'll put our Home Screen content here
            HomeScreenBody()
        }
    }
}

/**
 * This composable holds the content for the Home screen.
 * For now, it's just a placeholder.
 */
@Composable
fun HomeScreenBody() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), // Add 16dp of padding around the content
        verticalArrangement = Arrangement.Center, // Center vertically
        horizontalAlignment = Alignment.CenterHorizontally // Center horizontally
    ) {
        Text(
            text = "Welcome to your Digital Munshi!",
            style = MaterialTheme.typography.titleLarge // Use a nice big title style
        )
        Text(
            text = "Your transaction list will be here.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * This lets us see a preview of our 'MunshiApp'
 * in the Android Studio "Split" or "Design" view.
 */
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DigitalMunshiTheme {
        MunshiApp()
    }
}