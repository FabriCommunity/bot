package community.fabricmc.bot.config

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kordex.ext.common.configuration.base.TomlConfig
import community.fabricmc.bot.config.spec.BotSpec
import community.fabricmc.bot.config.spec.ChannelsSpec
import community.fabricmc.bot.config.spec.RolesSpec
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Role
import dev.kord.core.entity.channel.GuildMessageChannel

class BotConfig : TomlConfig(
    baseName = "main",
    specs = arrayOf(BotSpec, ChannelsSpec, RolesSpec),
    resourcePrefix = "bot",
    configFolder = "bot"
) {
    init {
        config = config.from.env()
    }

    val botToken: String get() = config[BotSpec.token]
    val botGuild: Snowflake get() = Snowflake(config[BotSpec.guild])

    val channelLogs: Snowflake get() = Snowflake(config[ChannelsSpec.logs])
    val channelModDevAlt: Snowflake get() = Snowflake(config[ChannelsSpec.modDevAlt])
    val channelReleases: Snowflake get() = Snowflake(config[ChannelsSpec.releases])
    val channelUpdates: Snowflake get() = Snowflake(config[ChannelsSpec.updates])

    val rolesAdmin: Snowflake get() = Snowflake(config[RolesSpec.admin])

    suspend fun getAdminRole(bot: ExtensibleBot): Role? = getGuild(bot)?.getRole(rolesAdmin)
    suspend fun getGuild(bot: ExtensibleBot): Guild? = bot.kord.getGuild(botGuild)

    suspend fun getLogsChannel(bot: ExtensibleBot): GuildMessageChannel? = getGuild(bot)?.getChannelOf(channelLogs)

    suspend fun getModDevAltChannel(bot: ExtensibleBot): GuildMessageChannel? =
        getGuild(bot)?.getChannelOf(channelModDevAlt)

    suspend fun getReleasesChannel(bot: ExtensibleBot): GuildMessageChannel? =
        getGuild(bot)?.getChannelOf(channelReleases)

    suspend fun getUpdatesChannel(bot: ExtensibleBot): GuildMessageChannel? =
        getGuild(bot)?.getChannelOf(channelUpdates)
}
