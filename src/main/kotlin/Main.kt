import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.util.Scanner
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main(args: Array<String>) {

    println("Starting bot...")
    // println("Token is \"$TOKEN\"")
    println("Token length ${TOKEN.length}")
    println("Token hash ${TOKEN.hashCode()}")

    val opt = DefaultBotOptions()
    opt.allowedUpdates = listOf("message", "inline_query", "chosen_inline_result", "callback_query")
    val proxy = args.find { it.startsWith("--proxy=") }
    if (proxy != null) {
        val expr = proxy.substring(8)
        val protoName = expr.split("://")[0].lowercase()
        val addr = expr.split("://")[1].split(":")[0]
        val port = expr.split("://")[1].split(":")[1].toInt()
        opt.proxyType = when (protoName) {
            "http" -> DefaultBotOptions.ProxyType.HTTP
            "socks4" -> DefaultBotOptions.ProxyType.SOCKS4
            "socks5" ->  DefaultBotOptions.ProxyType.SOCKS5
            else -> throw IllegalArgumentException("Unknown proxy protocol: $protoName")
        }
        opt.proxyHost = addr
        opt.proxyPort = port
        println("Using proxy $protoName://$addr:$port")
    } else {
        println("Proxy not enabled, use \"--proxy=proto://addr:port\" to enable it")
    }


    val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)
    try {
        val botSession = telegramBotsApi.registerBot(TitleBot(options = opt))

        val scanner = Scanner(System.`in`)
        var line: String
        while (scanner.nextLine().also { line = it } != null) {
            if (line.startsWith("exit") || line.startsWith("stop")) {
                break
            }
        }
        println("Shutting down...")
        thread {
            try {
                Thread.sleep(10000)
                System.err.println("Took too long to stop, killed.")
                exitProcess(0)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        botSession.stop()
    } catch (e: TelegramApiException) {
        e.printStackTrace()
    }
    println("Stopped.")
    exitProcess(0)
}
