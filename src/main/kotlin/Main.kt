import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.logging.LogLevel
import java.util.concurrent.ConcurrentHashMap
import com.github.kotlintelegrambot.dispatcher.text

// Константы
const val BOT_TOKEN = ""
const val CREATOR_ID = 0 // Замените на ваш Telegram ID

// Хранение данных

val userForms = ConcurrentHashMap<Long, UserForm>()
val userStates = ConcurrentHashMap<Long, String>()

fun main() {
    val bot = bot {
        token = BOT_TOKEN
        logLevel = LogLevel.Network.Body

        dispatch {
            text {
                handleUpdate(bot, update)
            }
        }
    }

    bot.startPolling()
}

fun handleUpdate(bot: com.github.kotlintelegrambot.Bot, update: Update) {
    val chatId = update.message?.chat?.id ?: return
    val text = update.message?.text ?: ""

    val state = userStates[chatId] ?: "START"

    when (state) {
        "START" -> {
            bot.sendMessage(ChatId.fromId(chatId), "Привет! Как вас зовут?")
            userStates[chatId] = "NAME"
            userForms[chatId] = UserForm()
        }
        "NAME" -> {
            if (text.isNotBlank()) {
                userForms[chatId]?.name = text
                bot.sendMessage(ChatId.fromId(chatId), "Укажите ваш email:")
                userStates[chatId] = "EMAIL"
            } else {
                bot.sendMessage(ChatId.fromId(chatId), "Имя не может быть пустым. Попробуйте снова.")
            }
        }
        "EMAIL" -> {
            if (isValidEmail(text)) {
                userForms[chatId]?.email = text
                bot.sendMessage(ChatId.fromId(chatId), "Какие у вас пожелания к подарку?")
                userStates[chatId] = "WISHES"
            } else {
                bot.sendMessage(ChatId.fromId(chatId), "Некорректный email. Попробуйте снова.")
            }
        }
        "WISHES" -> {
            if (text.isNotBlank()) {
                userForms[chatId]?.wishes = text
                bot.sendMessage(ChatId.fromId(chatId), "Спасибо! Анкета заполнена. Мы передали ваши данные создателю бота.")

                // Отправка данных создателю
                val form = userForms[chatId]
                bot.sendMessage(
                    chatId = ChatId.fromId(CREATOR_ID.toLong()),
                    text = """
                        Новая анкета:
                        Имя: ${form?.name}
                        Email: ${form?.email}
                        Пожелания: ${form?.wishes}
                    """.trimIndent()
                )

                // Сброс состояния
                userStates[chatId] = "START"
            } else {
                bot.sendMessage(ChatId.fromId(chatId), "Пожелания не могут быть пустыми. Попробуйте снова.")
            }
        }
    }
}

fun isValidEmail(email: String?): Boolean {
    return email != null && Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matches(email)
}
