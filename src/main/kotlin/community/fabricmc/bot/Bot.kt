package community.fabricmc.bot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kordex.ext.mappings.extMappings
import com.kotlindiscord.kordex.ext.mappings.extMappingsNamespaceCheck
import me.shedaniel.linkie.namespaces.YarnNamespace

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
    bot.extMappingsNamespaceCheck { namespace ->
        { event ->
            var result = true

            if (namespace != YarnNamespace && event.message.channelId.value != MOD_DEV_ALT) {
                result = false
            }

            result
        }
    }

    bot.extMappings()
    bot.start()
}
