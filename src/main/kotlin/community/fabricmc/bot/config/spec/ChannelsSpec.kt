package community.fabricmc.bot.config.spec

import com.uchuhimo.konf.ConfigSpec

object ChannelsSpec : ConfigSpec() {
    val releases by required<Long>()
    val updates by required<Long>()

    val logs by required<Long>()
    val modDevAlt by required<Long>()
}
