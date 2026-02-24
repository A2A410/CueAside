package foz.cueaside.aa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import foz.cueaside.aa.data.RoutineManager
import foz.cueaside.aa.model.*
import foz.cueaside.aa.ui.screen.CreateScreen
import foz.cueaside.aa.ui.screen.ListScreen
import foz.cueaside.aa.ui.screen.SettingsScreen
import foz.cueaside.aa.ui.theme.CueAsideTheme

class MainViewModel(context: Context) : ViewModel() {
    private val routineManager = RoutineManager(context)

    var settings by mutableStateOf(routineManager.getSettings())
        private set

    var routines by mutableStateOf(routineManager.getRoutines())
        private set

    fun updateSettings(newSettings: AppSettings) {
        settings = newSettings
        routineManager.saveSettings(newSettings)
    }

    fun refreshRoutines() {
        routines = routineManager.getRoutines()
    }

    fun toggleRoutine(id: String, enabled: Boolean) {
        routineManager.toggleRoutine(id, enabled)
        refreshRoutines()
    }

    fun deleteRoutine(id: String) {
        routineManager.deleteRoutine(id)
        refreshRoutines()
    }

    fun addRoutine(routine: Routine) {
        routineManager.addRoutine(routine)
        refreshRoutines()
    }

    var installedApps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    fun fetchApps(context: Context) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
        val appList = mutableListOf<AppInfo>()
        for (app in apps) {
            if ((app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (app.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                continue
            }
            appList.add(AppInfo(
                name = pm.getApplicationLabel(app).toString(),
                pkg = app.packageName,
                icon = "" // For now, we'll use package name to load icon in UI
            ))
        }
        installedApps = appList.sortedBy { it.name }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MainViewModel = viewModel(factory = object : androidx.lifecycle.viewmodel.ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(applicationContext) as T
                }
            })

            CueAsideTheme(
                themeId = vm.settings.themeId,
                designMode = vm.settings.design
            ) {
                MainShell(vm)
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Create : Screen("create", "Create", Icons.Default.Add)
    object List : Screen("list", "List", Icons.Default.List)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(vm: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            NavigationBar {
                val screens = listOf(Screen.Create, Screen.List, Screen.Settings)
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.List.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Create.route) {
                LaunchedEffect(Unit) { vm.fetchApps(context) }
                CreateScreen(
                    installedApps = vm.installedApps,
                    onSave = {
                        vm.addRoutine(it)
                        navController.navigate(Screen.List.route) {
                            popUpTo(Screen.List.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.List.route) {
                ListScreen(
                    routines = vm.routines,
                    onToggle = { id, enabled -> vm.toggleRoutine(id, enabled) },
                    onDelete = { id -> vm.deleteRoutine(id) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    settings = vm.settings,
                    onUpdateSettings = { vm.updateSettings(it) }
                )
            }
        }
    }
}
