package community.fabricmc.bot.config.spec

import com.uchuhimo.konf.ConfigSpec

object RolesSpec : ConfigSpec() {
    val admin by required<Long>()
}
