package foz.cueaside.aa.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import foz.cueaside.aa.model.AppInfo
import foz.cueaside.aa.model.Routine
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    installedApps: List<AppInfo>,
    onSave: (Routine) -> Unit,
    onBack: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var selectedApps by remember { mutableStateOf(setOf<AppInfo>()) }
    var searchQuery by remember { mutableStateOf("") }

    // Config state
    var cond by remember { mutableStateOf("launched") }
    var msg by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var cueName by remember { mutableStateOf("") }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isEmpty()) installedApps
        else installedApps.filter { it.name.contains(searchQuery, ignoreCase = true) || it.pkg.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (step == 0) "Select Apps" else "Configure Routine") },
                navigationIcon = {
                    if (step > 0) {
                        IconButton(onClick = { step-- }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedApps.isNotEmpty()) {
                FloatingActionButton(onClick = {
                    if (step == 0) step = 1
                    else {
                        val routine = Routine(
                            id = System.currentTimeMillis().toString(),
                            seqId = 0, // Placeholder
                            cueName = if (cueName.isNotEmpty()) cueName else UUID.randomUUID().toString().take(5),
                            apps = selectedApps.toList(),
                            cond = cond,
                            dur = 20,
                            unit = "m",
                            timeMode = "session",
                            msg = msg,
                            title = title,
                            enabled = true
                        )
                        onSave(routine)
                    }
                }) {
                    Icon(Icons.Default.Check, contentDescription = "Next")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (step == 0) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredApps) { app ->
                        val isSelected = selectedApps.contains(app)
                        AppItemRow(
                            app = app,
                            isSelected = isSelected,
                            onToggle = {
                                selectedApps = if (isSelected) selectedApps - app else selectedApps + app
                            }
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    Text("Condition", style = MaterialTheme.typography.labelLarge)
                    listOf("launched" to "When Launched", "used" to "Time-based", "exiting" to "When Exiting").forEach { (id, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { cond = id }) {
                            RadioButton(selected = cond == id, onClick = { cond = id })
                            Text(label)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = cueName, onValueChange = { cueName = it }, label = { Text("Cue Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Notification Title (Optional)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = msg,
                        onValueChange = { msg = it },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
                }
            }
        }
    }
}

@Composable
fun AppItemRow(app: AppInfo, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(app.name, style = MaterialTheme.typography.bodyLarge)
            Text(app.pkg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Checkbox(checked = isSelected, onCheckedChange = null)
    }
}
