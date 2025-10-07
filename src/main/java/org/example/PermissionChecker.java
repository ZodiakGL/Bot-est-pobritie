package org.example;

import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

public class PermissionChecker {
    private final AbsSender bot;

    public PermissionChecker(AbsSender bot) {
        this.bot = bot;
    }

    /**
     * Проверяет, является ли чат группой или супергруппой
     */
    public boolean isGroupChat(Chat chat) {
        return chat.isGroupChat() || chat.isSuperGroupChat();
    }

    /**
     * Проверяет, является ли пользователь администратором чата
     */
    public boolean isUserAdmin(Long chatId, Long userId) {
        try {
            List<ChatMember> admins = getChatAdmins(chatId);

            for (ChatMember admin : admins) {
                if (admin.getUser().getId().equals(userId)) {
                    return true;
                }
            }
            return false;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Проверяет, может ли бот ограничивать пользователей
     */
    public boolean canBotRestrictMembers(Long chatId) {
        try {
            String botId = bot.getMe().getId().toString();
            ChatMember botMember = getChatMember(chatId, Long.valueOf(botId));

            if (botMember instanceof ChatMemberAdministrator) {
                ChatMemberAdministrator admin = (ChatMemberAdministrator) botMember;
                return admin.getCanRestrictMembers();
            }

            return false;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Проверяет, может ли пользователь быть замьючен (не админ)
     */
    public boolean canUserBeMuted(Long chatId, Long userId) {
        return !isUserAdmin(chatId, userId);
    }

    /**
     * Получает список администраторов чата
     */
    public List<ChatMember> getChatAdmins(Long chatId) throws TelegramApiException {
        GetChatAdministrators getAdmins = new GetChatAdministrators();
        getAdmins.setChatId(chatId.toString());
        return bot.execute(getAdmins);
    }

    /**
     * Получает информацию о участнике чата
     */
    public ChatMember getChatMember(Long chatId, Long userId) throws TelegramApiException {
        GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId(chatId.toString());
        getChatMember.setUserId(userId);
        return bot.execute(getChatMember);
    }

    /**
     * Комплексная проверка перед мутом
     */
    public PermissionCheckResult checkMutePermissions(Long chatId, Long executorUserId, Long targetUserId) {
        // Проверяем права исполнителя
        if (!isUserAdmin(chatId, executorUserId)) {
            return PermissionCheckResult.noPermission("❌ Ты чо индеец, у тебя нет прав!");
        }

        // Проверяем права бота
        if (!canBotRestrictMembers(chatId)) {
            return PermissionCheckResult.noPermission(
                    "❌ У бота нет прав ограничивать пользователей!\n" +
                            "Пожалуйста, выдайте боту права администратора с разрешением 'Ban users'"
            );
        }

        // Проверяем, что цель не админ
        if (!canUserBeMuted(chatId, targetUserId)) {
            return PermissionCheckResult.noPermission("❌ Ты чо блинов объелся? Админов нельзя мутить!");
        }

        return PermissionCheckResult.success();
    }
}
