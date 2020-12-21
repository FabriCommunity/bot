package community.fabricmc.bot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.respond
import com.kotlindiscord.kordex.ext.mappings.MappingsExtension

@Suppress("UnderscoresInNumericLiterals")  // It's a Snowflake, really
private const val MOD_DEV_ALT = 789958205211803698L

/** Global bot object. **/
val bot = ExtensibleBot(
    System.getenv("BOT_TOKEN"),
    prefix = "?",
    addSentryExtension = false
)

/** Launch function. **/
suspend fun main() {
    MappingsExtension.addCheck { command ->
        { event ->
            var result = true

            if (!command.startsWith("y") && event.message.channelId.value != MOD_DEV_ALT) {
                result = false
            }

            result
        }
    }

    bot.addExtension(MappingsExtension::class)

    bot.start()
}
