@file:OptIn(KordPreview::class)

package community.fabricmc.bot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.converters.*
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.EPHEMERAL
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommandContext
import com.kotlindiscord.kord.extensions.extensions.KoinExtension
import com.kotlindiscord.kord.extensions.utils.ensureWebhook
import com.kotlindiscord.kord.extensions.utils.getUrl
import com.kotlindiscord.kord.extensions.utils.hasRole
import community.fabricmc.bot.config.BotConfig
import community.fabricmc.bot.data.FollowData
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.execute
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.NewsChannel
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.guild.GuildDeleteEvent
import dev.kord.core.event.guild.GuildUpdateEvent
import dev.kord.rest.Image
import org.koin.core.component.inject

@Suppress("MagicNumber")
private val BLURPLE = Color(0x7289DA)

@Suppress("MagicNumber")
private val GREEN = Color(0x72DA7E)

@Suppress("MagicNumber")
private val RED = Color(0xDA7272)

private const val CHUNK_SIZE = 10

class FollowExtension(bot: ExtensibleBot) : KoinExtension(bot) {
    override val name = "follow"

    val config: BotConfig by inject()
    val data = FollowData()

    override suspend fun setup() {
        data.addServer(config.botGuild)

        event<GuildCreateEvent> {
            action {
                if (event.guild.id == config.botGuild || data.hasServer(event.guild)) {
                    data.setServerName(event.guild)
                } else {
                    event.guild.leave()
                }
            }
        }

        event<GuildDeleteEvent> {
            action {
                val existingServer = data.getServer(event.guildId)
                val eventGuild = event.guild

                val name = eventGuild?.name ?: existingServer?.name

                logAction(
                    "Left Server",

                    "Left guild: `${event.guildId.value}` ".let {
                        it + if (name != null) {
                            " ($name)"
                        } else {
                            ""
                        }
                    },

                    RED
                )

                data.removeServer(event.guildId)
            }
        }

        event<GuildUpdateEvent> {
            action {
                if (event.guild.id == config.botGuild || data.hasServer(event.guild)) {
                    data.setServerName(event.guild)
                } else {
                    event.guild.leave()
                }
            }
        }

        slashCommand {
            name = "publish"
            description = "Publish release/update messages"

            subCommand {
                name = "help"
                description = "Learn about how publishing works."

                autoAck = false

                action {
                    ack {
                        content = "**__FabriComm Showcase Publishing__**\n\n"

                        content += "The publishing commands allow you, as a staff member on an allow-listed server, " +
                                "to publish your Fabric-related project updates to the FabriComm showcase channels " +
                                "easily.\n\n"

                        content += "**1)** Create and post your message in a channel you have the `Manage Messages` " +
                                "permission in.\n"
                        content += "**2)** Type `/publish release`, `/publish showcase` or `/publish update`, " +
                                "depending on the channel that best suits your post.\n"
                        content += "**3)** Use the command parameters to specify an earlier message, or to prevent " +
                                "the bot from publishing the message to any following channels, if required.\n\n"

                        content += "That's all there is to it. If you need any help with the bot, please let us " +
                                "know!\n\n"

                        content += "**__Notes__**\n\n"

                        content += "If you'd like the bot to publish messages to any following channels " +
                                "automatically, be sure to give it both the `Send Messages` and `Manage Messages` " +
                                "permissions in the announcements channel you're using. This means you'll be able " +
                                "to click two fewer buttons when publishing!"

                        flags = EPHEMERAL
                    }
                }
            }

            subCommand(::PublishArgs) {
                name = "release"
                description = "Publish a message to the new-releases channel."

                action {
                    if (channel !is GuildMessageChannel) {
                        interactionResponse?.edit {
                            content = "This command may only be run on a server."
                            flags = EPHEMERAL
                        }

                        acked = true

                        return@action
                    }

                    if (!data.hasServer(guild!!)) {
                        interactionResponse?.edit {
                            content = "This command may only be run on an allow-listed server."
                            flags = EPHEMERAL
                        }

                        acked = true

                        return@action
                    }

                    val channelObj = if (arguments.message != null) {
                        arguments.message!!.channel.asChannel() as GuildMessageChannel
                    } else {
                        channel as GuildMessageChannel
                    }

                    if (!hasManageMessages(channelObj)) {
                        interactionResponse?.edit {
                            content =
                                "You don't have permission to run this command. In order to publish messages, you" +
                                        "must have the `Manage Messages` permission on this server, or in the " +
                                        "channel you're publishing the message from."
                            flags = EPHEMERAL
                        }

                        acked = true

                        return@action
                    }

                    val message = if (arguments.message != null) {
                        arguments.message!!
                    } else {
                        channelObj.getLastMessage()!!
                    }

                    val targetChannel = config.getReleasesChannel(bot)!!
                    val webhook = ensureWebhook(targetChannel, "Showcase Publishing", logo = null)

                    val sentMessage = webhook.execute(webhook.token!!) {
                        this.avatarUrl = guild!!.getIconUrl(Image.Format.PNG)
                        this.username = guild!!.name

                        content = message.content

                        message.embeds.filter { allNull(it.provider, it.video, it.url) }.forEach {
                            embed { it.apply(this) }
                        }
                    }

                    if (channelObj is NewsChannel && arguments.publish) {
                        @Suppress("TooGenericExceptionCaught")
                        try {
                            message.publish()

                            interactionResponse?.edit {
                                content = "Message published to ${targetChannel.mention}. Thanks!"
                                flags = EPHEMERAL
                            }

                            acked = true
                        } catch (e: Exception) {
                            interactionResponse?.edit {
                                content = "Message published to ${targetChannel.mention}, but the bot was unable to " +
                                        "publish it to the following channels. Please check the bot's permissions, " +
                                        "and try publishing it yourself!"
                                flags = EPHEMERAL
                            }

                            acked = true
                        }
                    } else {
                        interactionResponse?.edit {
                            content = "Message published to ${targetChannel.mention}. Thanks!"
                            flags = EPHEMERAL
                        }

                        acked = true
                    }

                    val author = if (message.webhookId != null) {
                        "Webhook (`${message.webhookId}`)"
                    } else {
                        "${message.author!!.mention} (`${message.author!!.id.value}` / `${message.author!!.tag}`)"
                    }

                    logAction(
                        "Message Published",

                        "Message [published](${sentMessage.getUrl()}) to ${targetChannel.mention}\n\n" +

                                "**Author:** $author\n" +

                                "**Published By:** ${member!!.mention} (`${member!!.id.value}` / " +
                                "`${member!!.asMember().tag}`)\n" +

                                "**Source Channel:** ${channelObj.mention} (`${channelObj.id.value}` / " +
                                "`#${channelObj.name})`\n" +

                                "**Source Server:** ${guild!!.name} (`${guild!!.id.value}`)\n" +
                                "**Message:** ${message.content.length} characters.",
                        GREEN
                    )
                }
            }

            subCommand(::PublishArgs) {
                name = "showcase"
                description = "Publish a message to the showcase channel."

                action {
                    if (channel !is GuildMessageChannel) {
                        interactionResponse?.edit {
                            content = "This command may only be run on a server."
                            flags = EPHEMERAL
                        }

                        acked = true

                        return@action
                    }

                    if (!data.hasServer(guild!!)) {
                        interactionResponse?.edit {
                            content = "This command may only be run on an allow-listed server."
                            flags = EPHEMERAL
                        }

                        acked = true

                        return@action
                    }

                    val channelObj = if (arguments.message != null) {
                        arguments.message!!.channel.asChannel() as GuildMessageChannel
                    } else {
                        channel as GuildMessageChannel
                    }

                    if (!hasManageMessages(channelObj)) {
                        interactionResponse?.edit {
                            content =
                                "You don't have permission to run this command. In order to publish messages, you" +
                                        "must have the `Manage Messages` permission on this server, or in the " +
                                        "channel you're publishing the message from."
                            flags = EPHEMERAL
                        }

                        acked = true

                        return@action
                    }

                    val message = if (arguments.message != null) {
                        arguments.message!!
                    } else {
                        channelObj.getLastMessage()!!
                    }

                    val targetChannel = config.getShowcaseChannel(bot)!!
                    val webhook = ensureWebhook(targetChannel, "Showcase Publishing", logo = null)

                    val sentMessage = webhook.execute(webhook.token!!) {
                        this.avatarUrl = guild!!.getIconUrl(Image.Format.PNG)
                        this.username = guild!!.name

                        content = message.content

                        message.embeds.filter { allNull(it.provider, it.video, it.url) }.forEach {
                            embed { it.apply(this) }
                        }
                    }

                    if (channelObj is NewsChannel && arguments.publish) {
                        @Suppress("TooGenericExceptionCaught")
                        try {
                            message.publish()

                            interactionResponse?.edit {
                                content = "Message published to ${targetChannel.mention}. Thanks!"
                                flags = EPHEMERAL
                            }

                            acked = true
                        } catch (e: Exception) {
                            interactionResponse?.edit {
                                content = "Message published to ${targetChannel.mention}, but the bot was unable to " +
                                        "publish it to the following channels. Please check the bot's permissions, " +
                                        "and try publishing it yourself!"
                                flags = EPHEMERAL
                            }

                            acked = true
                        }
                    } else {
                        interactionResponse?.edit {
                            content = "Message published to ${targetChannel.mention}. Thanks!"
                            flags = EPHEMERAL
                        }

                        acked = true
                    }

                    val author = if (message.webhookId != null) {
                        "Webhook (`${message.webhookId}`)"
                    } else {
                        "${message.author!!.mention} (`${message.author!!.id.value}` / `${message.author!!.tag}`)"
                    }

                    logAction(
                        "Message Published",

                        "Message [published](${sentMessage.getUrl()}) to ${targetChannel.mention}\n\n" +

                                "**Author:** $author\n" +

                                "**Published By:** ${member!!.mention} (`${member!!.id.value}` / " +
                                "`${member!!.asMember().tag}`)\n" +

                                "**Source Channel:** ${channelObj.mention} (`${channelObj.id.value}` / " +
                                "`#${channelObj.name})`\n" +

                                "**Source Server:** ${guild!!.name} (`${guild!!.id.value}`)\n" +
                                "**Message:** ${message.content.length} characters.",
                        GREEN
                    )
                }
            }

            subCommand(::PublishArgs) {
                name = "update"
                description = "Publish a message to the update-releases channel."

                action {
                    if (channel !is GuildMessageChannel) {
                        interactionResponse?.edit {
                            content = "This command may only be run on a server."
                            flags = EPHEMERAL
                        }

                        acked = true

                        return@action
                    }

                    if (!data.hasServer(guild!!)) {
                        interactionResponse?.edit {
                            content = "This command may only be run on an allow-listed server."
                            flags = EPHEMERAL
                        }

                        acked = true

                        return@action
                    }

                    val channelObj = if (arguments.message != null) {
                        arguments.message!!.channel.asChannel() as GuildMessageChannel
                    } else {
                        channel as GuildMessageChannel
                    }

                    if (!hasManageMessages(channelObj)) {
                        interactionResponse?.edit {
                            content =
                                "You don't have permission to run this command. In order to publish messages, you" +
                                        "must have the `Manage Messages` permission on this server, or in the " +
                                        "channel you're publishing the message from."
                            flags = EPHEMERAL
                        }

                        acked = true

                        return@action
                    }

                    val message = if (arguments.message != null) {
                        arguments.message!!
                    } else {
                        channelObj.getLastMessage()!!
                    }

                    val targetChannel = config.getUpdatesChannel(bot)!!
                    val webhook = ensureWebhook(targetChannel, "Showcase Publishing", logo = null)

                    val sentMessage = webhook.execute(webhook.token!!) {
                        this.avatarUrl = guild!!.getIconUrl(Image.Format.PNG)
                        this.username = guild!!.name

                        content = message.content

                        message.embeds.filter { allNull(it.provider, it.video, it.url) }.forEach {
                            embed { it.apply(this) }
                        }
                    }

                    if (channelObj is NewsChannel && arguments.publish) {
                        @Suppress("TooGenericExceptionCaught")
                        try {
                            message.publish()

                            interactionResponse?.edit {
                                content = "Message published to ${targetChannel.mention}. Thanks!"
                                flags = EPHEMERAL
                            }

                            acked = true
                        } catch (e: Exception) {
                            interactionResponse?.edit {
                                content = "Message published to ${targetChannel.mention}, but the bot was unable to " +
                                        "publish it to the following channels. Please check the bot's permissions, " +
                                        "and try publishing it yourself!"
                                flags = EPHEMERAL
                            }

                            acked = true
                        }
                    } else {
                        interactionResponse?.edit {
                            content = "Message published to ${targetChannel.mention}. Thanks!"
                            flags = EPHEMERAL
                        }

                        acked = true
                    }

                    val author = if (message.webhookId != null) {
                        "Webhook (`${message.webhookId}`)"
                    } else {
                        "${message.author!!.mention} (`${message.author!!.id.value}` / `${message.author!!.tag}`)"
                    }

                    logAction(
                        "Message Published",

                        "Message [published](${sentMessage.getUrl()}) to ${targetChannel.mention}\n\n" +

                                "**Author:** $author\n" +

                                "**Published By:** ${member!!.mention} (`${member!!.id.value}` / " +
                                "`${member!!.asMember().tag}`)\n" +

                                "**Source Channel:** ${channelObj.mention} (`${channelObj.id.value}` / " +
                                "`#${channelObj.name})`\n" +

                                "**Source Server:** ${guild!!.name} (`${guild!!.id.value}`)\n" +
                                "**Message:** ${message.content.length} characters.",
                        GREEN
                    )
                }
            }
        }

        slashCommand {
            name = "server"
            description = "Showcase channel server management"

            guild(config.botGuild)

            subCommand(::AddServerArgs) {
                name = "allow"
                description = "Allow a server to publish to the showcase channels"

                autoAck = false

                action {
                    if (!hasAdmin()) {
                        ack {
                            content = "You don't have permission to run this command."
                            flags = EPHEMERAL
                        }

                        return@action
                    }

                    if (arguments.serverId == config.botGuild) {
                        ack {
                            content = "You can't allow or deny publishing on the main server."
                            flags = EPHEMERAL
                        }

                        return@action
                    }

                    val existingServer = data.getServer(arguments.serverId)

                    if (existingServer != null) {
                        ack {
                            content = "Server with ID `${existingServer.id.value}` is already allowed: " +
                                    "`${existingServer.name ?: "No name"}`"
                            flags = EPHEMERAL
                        }

                        return@action
                    }

                    data.addServer(arguments.serverId, arguments.name)

                    ack {
                        content = "Server allowed: `${arguments.serverId.value}` / `${arguments.name}`"
                        flags = EPHEMERAL
                    }

                    logAction(
                        "Server Allowed",
                        "Server allowed: `${arguments.serverId.value}` / `${arguments.name}`",
                        BLURPLE
                    )
                }
            }

            subCommand(::RemoveServerArgs) {
                name = "deny"
                description = "Deny an allowed server from publishing to the showcase channels"

                autoAck = false

                action {
                    if (!hasAdmin()) {
                        ack {
                            content = "You don't have permission to run this command."
                            flags = EPHEMERAL
                        }

                        return@action
                    }

                    if (arguments.serverId == config.botGuild) {
                        ack {
                            content = "You can't allow or deny publishing on the main server."
                            flags = EPHEMERAL
                        }

                        return@action
                    }

                    val existingServer = data.getServer(arguments.serverId)

                    if (existingServer == null) {
                        ack {
                            content = "Server with ID `${arguments.serverId.value}` is not allowed to publish."
                            flags = EPHEMERAL
                        }

                        return@action
                    }

                    data.removeServer(arguments.serverId)
                    bot.kord.getGuild(arguments.serverId)?.leave()  // Leave the guild if we're on it

                    ack {
                        content = "Server no longer allowed: `${existingServer.id.value}` / " +
                                "`${existingServer.name ?: "No name"}`"
                        flags = EPHEMERAL
                    }

                    logAction(
                        "Server Disallowed",
                        "Server disallowed: `${existingServer.id.value}` / " +
                                "`${existingServer.name ?: "No name"}`",
                        BLURPLE
                    )
                }
            }

            subCommand(::ListServerArgs) {
                name = "list"
                description = "List all the servers that are allowed to publish to the showcase channels"

                autoAck = false

                action {
                    if (!hasAdmin()) {
                        ack {
                            content = "You don't have permission to run this command."
                            flags = EPHEMERAL
                        }

                        return@action
                    }

                    val servers = data.getServers()

                    if (servers.isEmpty()) {
                        ack {
                            content = "No servers are allowed to publish."
                            flags = EPHEMERAL
                        }

                        return@action
                    }

                    val chunks = servers.chunked(CHUNK_SIZE)

                    val page = (arguments.page ?: 1) - 1

                    if (page < 0 || page >= chunks.size) {
                        ack {
                            content = "Invalid page: Must be a number from 1 to ${chunks.size}"
                            flags = EPHEMERAL
                        }

                        return@action
                    }

                    ack {
                        content = "**__Allowed servers: ${servers.size}__**\n\n"

                        content += chunks[page].joinToString("\n") {
                            "`${it.id.value}` **=>** ${it.name}"
                        }

                        content += "\n\n**Page:** ${page + 1} / ${chunks.size}"

                        flags = EPHEMERAL
                    }
                }
            }
        }
    }

    private fun allNull(vararg objects: Any?): Boolean = objects.filterNotNull().isEmpty()

    private suspend fun logAction(title: String, description: String, colour: Color) {
        val channel = config.getLogsChannel(bot)

        channel!!.createEmbed {
            this.title = title
            this.description = description
            this.color = colour
        }
    }

    private suspend fun SlashCommandContext<*>.hasAdmin(): Boolean {
        val memberObj = member?.asMemberOrNull() ?: return false
        val role = config.getAdminRole(bot) ?: return false

        return memberObj.hasRole(role)
    }

    private suspend fun SlashCommandContext<*>.hasManageMessages(channelObj: GuildMessageChannel): Boolean {
        val memberObj = member?.asMemberOrNull() ?: return false

        return channelObj.getEffectivePermissions(memberObj.id).contains(Permission.ManageMessages)
    }

    class AddServerArgs : Arguments() {
        val serverId by snowflake("id", "Server ID to allow")
        val name by string("name", "Temporary server name")
    }

    class ListServerArgs : Arguments() {
        val page by optionalInt("page", "Page to display")
    }

    class RemoveServerArgs : Arguments() {
        val serverId by snowflake("server-id", "Server ID to deny")
    }

    class PublishArgs : Arguments() {
        val message by optionalMessage(
            "target-message",
            "Message ID to publish, if not the latest in the current channel"
        )

        val publish by defaultingBoolean(
            "auto-publish",
            "Supply \"False\" if you don't want the target message to be published to following channels.",
            true
        )
    }
}
