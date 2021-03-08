package community.fabricmc.bot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kordex.ext.mappings.extMappings
import me.shedaniel.linkie.namespaces.YarnNamespace

@Suppress("UnderscoresInNumericLiterals")  // It's a Snowflake, really
private const val MOD_DEV_ALT = 789958205211803698L

/** Launch function. **/
suspend fun main() {
    val bot = ExtensibleBot(System.getenv("BOT_TOKEN")) {
        commands {
            defaultPrefix = "?"
        }

        extensions {
            sentry = false

            extMappings {
                namespaceCheck { namespace ->
                    { event ->
                        var result = true

                        if (namespace != YarnNamespace && event.message.channelId.value != MOD_DEV_ALT) {
                            result = false
                        }

                        result
                    }
                }
            }
        }
    }

    bot.start()
}
