package foz.cueaside.aa.native_port.model

data class Routine(
    val id: String,
    val seqId: Int,
    val cueName: String,
    val apps: List<AppInfo>,
    val cond: String, // "launched", "used", "exiting"
    val dur: Int,
    val unit: String, // "m", "h", "s"
    val timeMode: String, // "session", "total"
    val icon: IconInfo? = null,
    val title: String? = null,
    val msg: String,
    val bubble: Boolean = false,
    val enabled: Boolean = true,
    val highPriority: Boolean = true,
    val timeout: Int = 0 // seconds
)

data class AppInfo(
    val name: String,
    val pkg: String,
    val icon: String // base64 or emoji
)

data class IconInfo(
    val type: String, // "app", "preset", "lib"
    val pkg: String? = null,
    val e: String? = null,
    val src: String? = null
)
