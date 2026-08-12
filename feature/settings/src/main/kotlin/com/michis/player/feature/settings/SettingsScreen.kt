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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.michis.player.core.ui.theme.LocalMichisSpacing
import com.michis.player.core.ui.theme.paletteFor
import com.michis.player.domain.repository.SettingsRepository
import com.michis.player.domain.repository.LibraryRootRepository
import com.michis.player.domain.model.LibraryRoot
import com.michis.player.domain.repository.GlobalSettings
import com.michis.player.domain.repository.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedFolders by viewModel.selectedFolders.collectAsStateWithLifecycle()
    var pendingFolderRemoval by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<LibraryRoot?>(null) }
    val spacing = LocalMichisSpacing.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        item {
            Text("Configuración", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Reproducción",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = spacing.large),
            )
            PlaybackSettingSlider(
                label = "Velocidad predeterminada",
                value = settings.playbackSpeed,
                valueRange = 0.5f..3f,
                steps = 24,
                valueLabel = { "%.1f×".format(it) },
                onValueConfirmed = viewModel::setPlaybackSpeed,
            )
            PlaybackSettingSlider(
                label = "Retroceder",
                value = settings.skipBackwardSeconds.toFloat(),
                valueRange = 5f..60f,
                steps = 10,
                valueLabel = { "${it.toInt()} segundos" },
                onValueConfirmed = { viewModel.setSkipBackward(it.toInt()) },
            )
            PlaybackSettingSlider(
                label = "Avanzar",
                value = settings.skipForwardSeconds.toFloat(),
                valueRange = 5f..60f,
                steps = 10,
                valueLabel = { "${it.toInt()} segundos" },
                onValueConfirmed = { viewModel.setSkipForward(it.toInt()) },
            )
            Row(
                Modifier.fillMaxWidth().clickable { viewModel.setPictureInPicture(!settings.pictureInPictureEnabled) }.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Ventana flotante (PiP)")
                    Text(
                        "Muestra el reproductor sobre otras aplicaciones al salir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.pictureInPictureEnabled,
                    onCheckedChange = viewModel::setPictureInPicture,
                )
            }
            Text(
                "Carpetas de la biblioteca",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = spacing.large, bottom = spacing.extraSmall),
            )
            if (selectedFolders.isEmpty()) {
                Text("No hay carpetas seleccionadas.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                selectedFolders.forEach { folder ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().padding(top = spacing.extraSmall),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(folder.displayName, modifier = Modifier.weight(1f), maxLines = 2)
                            TextButton(onClick = { pendingFolderRemoval = folder }) { Text("Deseleccionar") }
                        }
                    }
                }
            }
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
                selected = settings.theme == option.preference,
                onSelect = { viewModel.selectTheme(option.preference) },
            )
        }
    }
    pendingFolderRemoval?.let { folder ->
        AlertDialog(
            onDismissRequest = { pendingFolderRemoval = null },
            title = { Text("Deseleccionar carpeta") },
            text = { Text("¿Quieres quitar “${folder.displayName}” de la biblioteca? Sus archivos permanecerán en el dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeFolder(folder.id)
                    pendingFolderRemoval = null
                }) { Text("Deseleccionar") }
            },
            dismissButton = { TextButton(onClick = { pendingFolderRemoval = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun PlaybackSettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: (Float) -> String,
    onValueConfirmed: (Float) -> Unit,
) {
    var pendingValue by androidx.compose.runtime.remember(value) { androidx.compose.runtime.mutableFloatStateOf(value) }
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(valueLabel(pendingValue), color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = pendingValue,
            onValueChange = { pendingValue = it },
            onValueChangeFinished = { onValueConfirmed(pendingValue) },
            valueRange = valueRange,
            steps = steps,
        )
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
    private val libraryRootRepository: LibraryRootRepository,
) : ViewModel() {
    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GlobalSettings())
    val selectedFolders = libraryRootRepository.observeRoots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectTheme(theme: ThemePreference) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch { settingsRepository.setPlaybackSpeed(speed) }
    }

    fun setSkipBackward(seconds: Int) {
        viewModelScope.launch { settingsRepository.setSkipBackwardSeconds(seconds) }
    }

    fun setSkipForward(seconds: Int) {
        viewModelScope.launch { settingsRepository.setSkipForwardSeconds(seconds) }
    }

    fun setPictureInPicture(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setPictureInPictureEnabled(enabled) }
    }

    fun removeFolder(id: String) {
        viewModelScope.launch { libraryRootRepository.removeRoot(id) }
    }
}
