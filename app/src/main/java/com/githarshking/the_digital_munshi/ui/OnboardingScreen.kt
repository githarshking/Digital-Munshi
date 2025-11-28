package com.githarshking.the_digital_munshi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.githarshking.the_digital_munshi.UserPreferences

@Composable
fun OnboardingScreen(
    context: android.content.Context,
    onFinished: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to Digital Munshi", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Tell us about your work so the AI can help you better.", style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = occupation,
            onValueChange = { occupation = it },
            label = { Text("Occupation (e.g. Driver, Shop Owner)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Describe your work briefly") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                UserPreferences.saveUser(context, name, occupation, description)
                onFinished()
            },
            enabled = name.isNotEmpty() && occupation.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start My Financial Journey")
        }
    }
}