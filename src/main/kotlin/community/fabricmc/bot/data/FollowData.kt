package community.fabricmc.bot.data

import com.kotlindiscord.kordex.ext.common.data.SerializedData
import community.fabricmc.bot.data.models.Server
import community.fabricmc.bot.data.models.Servers
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import kotlinx.serialization.serializer

class FollowData : SerializedData<Servers>(
    "follow",
    "bot",
    serializer(),
    { Servers() }
) {
    init {
        load()
    }

    fun addServer(id: Snowflake, name: String? = null): Boolean {
        if (hasServer(id)) return false

        val result = data.servers.add(Server(id, name))

        save()

        return result
    }

    fun addServer(server: Guild): Boolean = addServer(server.id, server.name)

    fun getServer(server: Guild): Server? = getServer(server.id)
    fun getServer(id: Snowflake): Server? = data.servers.firstOrNull { it.id == id }

    fun getServers(): List<Server> = data.servers.toList()

    fun hasServer(id: Snowflake): Boolean = data.servers.firstOrNull { it.id == id } != null
    fun hasServer(server: Guild): Boolean = hasServer(server.id)

    fun removeServer(id: Snowflake): Boolean {
        val server = data.servers.firstOrNull { it.id == id } ?: return false
        val result = data.servers.remove(server)

        save()

        return result
    }

    fun removeServer(server: Guild): Boolean = removeServer(server.id)

    fun setServerName(id: Snowflake, name: String): Boolean {
        val server = data.servers.firstOrNull { it.id == id } ?: return false

        server.name = name
        save()

        return true
    }

    fun setServerName(server: Guild): Boolean = setServerName(server.id, server.name)
}
