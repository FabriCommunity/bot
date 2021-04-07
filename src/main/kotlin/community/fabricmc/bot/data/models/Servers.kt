@file:Suppress("DataClassShouldBeImmutable")

package community.fabricmc.bot.data.models

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

@Serializable
data class Servers(
    val servers: MutableSet<Server> = mutableSetOf()
)

@Serializable
data class Server(
    val id: Snowflake,
    var name: String? = null
)
