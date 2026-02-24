package foz.cueaside.aa.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import foz.cueaside.aa.model.AppSettings
import foz.cueaside.aa.model.DesignMode
import foz.cueaside.aa.model.THEMES

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Appearance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            ThemePicker(selectedThemeId = settings.themeId) {
                onUpdateSettings(settings.copy(themeId = it))
            }
        }

        item {
            DesignPicker(selectedDesign = settings.design) {
                onUpdateSettings(settings.copy(design = it))
            }
        }

        item {
            Text("Default Settings", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            ToggleRow("Global High Priority", settings.highPriority) {
                onUpdateSettings(settings.copy(highPriority = it))
            }
            ToggleRow("Default Bubble", settings.defaultBubble) {
                onUpdateSettings(settings.copy(defaultBubble = it))
            }
        }
    }
}

@Composable
fun ThemePicker(selectedThemeId: String, onThemeSelected: (String) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(THEMES) { theme ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(theme.accent)))
                    .clickable { onThemeSelected(theme.id) }
                    .padding(4.dp)
            ) {
                if (theme.id == selectedThemeId) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

@Composable
fun DesignPicker(selectedDesign: DesignMode, onDesignSelected: (DesignMode) -> Unit) {
    Column {
        DesignRow("Bold Design", selectedDesign == DesignMode.BOLD) { onDesignSelected(DesignMode.BOLD) }
        DesignRow("Minimal Design", selectedDesign == DesignMode.MINIMAL) { onDesignSelected(DesignMode.MINIMAL) }
    }
}

@Composable
fun DesignRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        if (selected) {
            Text("Active", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
