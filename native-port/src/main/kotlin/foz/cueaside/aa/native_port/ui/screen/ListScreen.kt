package foz.cueaside.aa.native_port.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import foz.cueaside.aa.native_port.model.Routine

@Composable
fun ListScreen(
    routines: List<Routine>,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    if (routines.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No routines yet.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(routines) { routine ->
                RoutineCard(routine, onToggle, onDelete)
            }
        }
    }
}

@Composable
fun RoutineCard(
    routine: Routine,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    var showDetail by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetail = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Placeholder
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(routine.icon?.e ?: "◎")
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "[${routine.cueName}] ${routine.title ?: routine.apps.firstOrNull()?.name ?: "Routine"}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when (routine.cond) {
                        "launched" -> "On launch"
                        "exiting" -> "On exit"
                        "used" -> "After ${routine.dur}${routine.unit}"
                        else -> routine.cond
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = routine.enabled,
                onCheckedChange = { onToggle(routine.id, it) }
            )
        }
    }

    if (showDetail) {
        AlertDialog(
            onDismissRequest = { showDetail = false },
            title = { Text(routine.cueName) },
            text = {
                Column {
                    Text(routine.msg)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Condition: ${routine.cond}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetail = false }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = { onDelete(routine.id); showDetail = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}
