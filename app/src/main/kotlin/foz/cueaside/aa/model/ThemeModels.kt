package foz.cueaside.aa.model

enum class DesignMode(val id: String, val label: String) {
    BOLD("1", "Bold"),
    MINIMAL("2", "Minimal")
}

data class AppTheme(
    val id: String,
    val label: String,
    val background: String,
    val accent: String,
    val accentSecondary: String
)

val THEMES = listOf(
    AppTheme("default", "Dark", "#111318", "#4fc3f7", "#7c4dff"),
    AppTheme("light", "Light", "#ffffff", "#1976d2", "#6200ea"),
    AppTheme("aurora", "Aurora", "#0f1117", "#a78bfa", "#34d399"),
    AppTheme("sunset", "Sunset", "#150c10", "#fb923c", "#e11d48"),
    AppTheme("ocean", "Ocean", "#0a1620", "#22d3ee", "#0ea5e9"),
    AppTheme("forest", "Forest", "#0c150c", "#4ade80", "#16a34a"),
    AppTheme("candy", "Candy", "#160c18", "#f9a8d4", "#c084fc"),
    AppTheme("lava", "Lava", "#1a0800", "#ff6b35", "#ff3d00")
)
