package com.skillarena.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MenuScreen(rating: Int, onPlay: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Skill Arena", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text("Geen levels. Alleen skill.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(24.dp))
            Text("Rating: $rating", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onPlay) { Text("Speel tegen bot") }
        }
    }
}
