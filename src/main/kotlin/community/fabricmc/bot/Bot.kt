package community.fabricmc.bot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.inGuild
import com.kotlindiscord.kord.extensions.utils.module
import com.kotlindiscord.kordex.ext.mappings.extMappings
import community.fabricmc.bot.config.BotConfig
import community.fabricmc.bot.extensions.FollowExtension
import me.shedaniel.linkie.namespaces.YarnNamespace
import org.koin.dsl.bind

/** Launch function. **/
suspend fun main() {
    val config = BotConfig()

    val bot = ExtensibleBot(config.botToken) {
        extensions {
            sentry = false

            add(::FollowExtension)

            extMappings {
                namespaceCheck { namespace ->
                    { event ->
                        var result = true

                        if (namespace != YarnNamespace && event.message.channelId != config.channelModDevAlt) {
                            result = false
                        }

                        result
                    }
                }
            }
        }

        hooks {
            afterKoinCreated {
                koin.module {
                    single { config } bind BotConfig::class
                }
            }
        }

        messageCommands {
            defaultPrefix = "?"

            check(inGuild(config.botGuild))
        }

        slashCommands {
            enabled = true
        }
    }

    bot.start()
}
