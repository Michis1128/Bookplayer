package com.michis.player.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.michis.player.core.ui.theme.LocalMichisSpacing
import com.michis.player.core.ui.theme.paletteFor
import com.michis.player.domain.repository.SettingsRepository
import com.michis.player.domain.repository.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val selectedTheme by viewModel.theme.collectAsStateWithLifecycle()
    val spacing = LocalMichisSpacing.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        item {
            Text("Configuración", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Tema de color",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = spacing.large, bottom = spacing.extraSmall),
            )
            Text(
                "Usa las mismas paletas disponibles en Michis Reader.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = spacing.small),
            )
        }
        items(themeOptions, key = { it.preference.name }) { option ->
            ThemeOption(
                option = option,
                selected = selectedTheme == option.preference,
                onSelect = { viewModel.selectTheme(option.preference) },
            )
        }
    }
}

@Composable
private fun ThemeOption(option: ThemeOption, selected: Boolean, onSelect: () -> Unit) {
    val preview = paletteFor(option.previewTheme)
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(end = 12.dp)) {
                PaletteDot(preview.background)
                PaletteDot(preview.card)
                PaletteDot(preview.accent)
            }
            Column(Modifier.weight(1f)) {
                Text(option.label, style = MaterialTheme.typography.bodyLarge)
                if (option.preference == ThemePreference.SYSTEM) {
                    Text("Día o Noche según el dispositivo", style = MaterialTheme.typography.bodySmall)
                }
            }
            RadioButton(selected = selected, onClick = onSelect)
        }
    }
}

@Composable
private fun PaletteDot(color: Color) {
    Box(Modifier.size(18.dp).clip(CircleShape).background(color))
}

private data class ThemeOption(
    val preference: ThemePreference,
    val label: String,
    val previewTheme: ThemePreference = preference,
)

private val themeOptions = listOf(
    ThemeOption(ThemePreference.SYSTEM, "Sistema", ThemePreference.LIGHT),
    ThemeOption(ThemePreference.LIGHT, "Día"),
    ThemeOption(ThemePreference.DARK, "Noche"),
    ThemeOption(ThemePreference.SEPIA, "Sepia"),
    ThemeOption(ThemePreference.TWILIGHT, "Crepúsculo"),
    ThemeOption(ThemePreference.CONSOLE, "Consola"),
    ThemeOption(ThemePreference.PAPER, "Papel"),
    ThemeOption(ThemePreference.SAND, "Arena"),
    ThemeOption(ThemePreference.LAVENDER, "Lavanda"),
    ThemeOption(ThemePreference.FOREST, "Bosque"),
    ThemeOption(ThemePreference.OCEAN, "Océano"),
    ThemeOption(ThemePreference.GRAPHITE, "Grafito"),
    ThemeOption(ThemePreference.MIDNIGHT, "Medianoche"),
    ThemeOption(ThemePreference.SOFT_PINK, "Rosa suave"),
    ThemeOption(ThemePreference.MINT, "Menta"),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val theme = settingsRepository.settings
        .map { it.theme }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemePreference.SYSTEM)

    fun selectTheme(theme: ThemePreference) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }
}
