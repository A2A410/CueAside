package foz.cueaside.aa.native_port.model

data class AppSettings(
    val defaultBubble: Boolean = false,
    val highPriority: Boolean = true,
    val design: DesignMode = DesignMode.MINIMAL,
    val themeId: String = "default",
    val lastSeqId: Int = 0
)
