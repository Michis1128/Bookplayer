package com.michis.player.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.michis.player.core.ui.theme.LocalMichisSpacing

@Composable fun SettingsScreen() {
    val spacing = LocalMichisSpacing.current
    Column(Modifier.fillMaxSize().padding(spacing.medium)) {
        Text("Configuración", style = MaterialTheme.typography.headlineMedium)
        Text("Apariencia y reproducción", modifier = Modifier.padding(top = spacing.medium))
    }
}
