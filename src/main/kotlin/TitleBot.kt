import org.telegram.abilitybots.api.bot.AbilityBot
import org.telegram.abilitybots.api.objects.Ability
import org.telegram.abilitybots.api.objects.Locality.GROUP
import org.telegram.abilitybots.api.objects.MessageContext
import org.telegram.abilitybots.api.objects.Privacy.GROUP_ADMIN
import org.telegram.abilitybots.api.toggle.BareboneToggle
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember
import org.telegram.telegrambots.meta.api.methods.groupadministration.PromoteChatMember
import org.telegram.telegrambots.meta.api.methods.groupadministration.SetChatAdministratorCustomTitle
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator
import java.util.function.Predicate

class TitleBot(token: String = TOKEN, username: String = USERNAME, options: DefaultBotOptions = DefaultBotOptions()) :
    AbilityBot(token, username, BareboneToggle(), options) {

    private val sBotStartupTime = System.currentTimeMillis() / 1000

    private val canPromoteMembers =
        Predicate<Update> { upd ->
            val msg = upd.message
            val member = getChatMember(msg.chatId.toString(), msg.from.id) ?: return@Predicate false
            if (member.status == "creator") return@Predicate true
            if (member.status == "administrator") {
                return@Predicate (member as ChatMemberAdministrator).canPromoteMembers
            }
            false
        }

    // 不处理bot启动前的消息
    private val outdatedMsg = Predicate<Update> { upd -> upd.message.date > sBotStartupTime }

    // 不处理由bot发出的消息
    private val botMsg =  Predicate<Update> { upd -> !upd.message.from.isBot }

    fun title(): Ability = Ability.builder().run {
        name("title")
        info("set title for member")
        locality(GROUP)
        privacy(GROUP_ADMIN)
        flag(botMsg, outdatedMsg)
        action { ctx ->
            val msg = ctx.update().message
            val chatId = ctx.chatId().toString()
            val userId = if (msg.isReply) {
                // 拥有 PromoteMembers 权限才能修改他人头衔
                if (!canPromoteMembers.test(ctx.update())) return@action
                msg.replyToMessage.from.id
            } else ctx.user().id

            val admins = getAdmins(chatId) ?: return@action
            val checkBotPermission =
                admins.any { it.user.id == me.id && (it as ChatMemberAdministrator).canPromoteMembers }
            if (!checkBotPermission) {
                ctx.send("设置头衔失败，机器人缺少添加新管理员权限！")
                return@action
            }

            val needAddAdmin = !admins.any { it.user.id == userId }
            if (needAddAdmin) {
                println("PromoteChatMember: chatId = $chatId, userId = $userId")
                val ret = addAdmin(chatId, userId)
                if (!ret) return@action
            }

            val args = ctx.arguments()
            val title = if (args.isEmpty()) "" else args.joinToString(" ")
            println("setTitle: chatId = $chatId, userId = $userId, customTitle = $title")
            val ret = setTitle(chatId, userId, title)
            if (!ret) return@action
        }
        build()
    }

    private fun getChatMember(chatId: String, userId: Long): ChatMember? =
        runCatching {
            execute(GetChatMember().apply {
                this.chatId = chatId
                this.userId = userId
            })
        }.onFailure {
            silent.send("读取管理列表失败: ${it.message}", chatId.toLong())
        }.getOrNull()

    private fun getAdmins(chatId: String): ArrayList<ChatMember>? =
        runCatching {
            execute(GetChatAdministrators().apply {
                this.chatId = chatId
            })
        }.onFailure {
            silent.send("读取管理列表失败: ${it.message}", chatId.toLong())
        }.getOrNull()

    private fun addAdmin(chatId: String, userId: Long): Boolean =
        runCatching {
            execute(PromoteChatMember().apply {
                this.chatId = chatId
                this.userId = userId
                canManageChat = true
            })
        }.onFailure {
            silent.send("添加管理失败: ${it.message}", chatId.toLong())
        }.isSuccess

    private fun setTitle(chatId: String, userId: Long, title: String): Boolean =
        runCatching {
            execute(SetChatAdministratorCustomTitle().apply {
                this.chatId = chatId
                this.userId = userId
                this.customTitle = title
            })
        }.onFailure {
            silent.send("设置头衔失败: ${it.message}", chatId.toLong())
        }.isSuccess

    private fun MessageContext.send(msg: String) {
        silent.send(msg, chatId())
    }

    override fun creatorId() = ADMIN
}
