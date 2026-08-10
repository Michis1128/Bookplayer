package com.michis.player.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.michis.player.core.ui.theme.LocalMichisSpacing

@Composable
fun EmptyState(title: String, message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    val spacing = LocalMichisSpacing.current
    Column(
        modifier = Modifier.fillMaxSize().padding(spacing.large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(message, modifier = Modifier.padding(top = spacing.small), style = MaterialTheme.typography.bodyLarge)
        if (actionLabel != null) Button(onClick = onAction, modifier = Modifier.padding(top = spacing.medium)) { Text(actionLabel) }
    }
}
