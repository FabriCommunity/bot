package community.fabricmc.bot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.respond
import com.kotlindiscord.kordex.ext.mappings.MappingsExtension

private const val modDevAlt = 789958205211803698L

val bot = ExtensibleBot(
    System.getenv("BOT_TOKEN"),
    prefix = "?",
    addSentryExtension = false
)

suspend fun main() {
    MappingsExtension.addCheck { command ->
        { event ->
            var result = true

            if (!command.startsWith("y")) {
                if (event.message.channelId.value != modDevAlt) {
                    event.message.respond("Non-Yarn commands may not be used outside of <#$modDevAlt>.")

                    result = false
                }
            }

            result
        }
    }
}
