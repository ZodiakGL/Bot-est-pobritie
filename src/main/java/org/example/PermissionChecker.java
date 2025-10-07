package org.example;

import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner;
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
     * Проверяет, может ли пользователь банить других пользователей
     */
    public boolean canUserBanMembers(Long chatId, Long userId) {
        try {
            ChatMember member = getChatMember(chatId, userId);

            // Владелец может все
            if (member instanceof ChatMemberOwner) {
                return true;
            }

            // Администратор - проверяем право ban users
            if (member instanceof ChatMemberAdministrator) {
                ChatMemberAdministrator admin = (ChatMemberAdministrator) member;
                return admin.getCanRestrictMembers(); // Это и есть право "Ban users"
            }

            // Обычный пользователь не может банить
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
     * Проверяет, может ли пользователь быть замьючен (не админ и не владелец)
     */
    public boolean canUserBeMuted(Long chatId, Long userId) {
        try {
            ChatMember member = getChatMember(chatId, userId);

            // Владелец и администраторы не могут быть замьючены
            return !(member instanceof ChatMemberOwner || member instanceof ChatMemberAdministrator);

        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
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
        // Проверяем права исполнителя - может ли он банить
        if (!canUserBanMembers(chatId, executorUserId)) {
            return PermissionCheckResult.noPermission("❌ Ты чо индеец, у тебя нет прав мутить пользователей!");
        }

        // Проверяем права бота
        if (!canBotRestrictMembers(chatId)) {
            return PermissionCheckResult.noPermission(
                    "❌ У бота нет прав ограничивать пользователей!\n" +
                            "Пожалуйста, выдайте боту права администратора с разрешением 'Ban users'"
            );
        }

        // Проверяем, что цель не админ и не владелец
        if (!canUserBeMuted(chatId, targetUserId)) {
            return PermissionCheckResult.noPermission("❌ Ты чо блинов объелся? Админов нельзя мутить!");
        }

        return PermissionCheckResult.success();
    }

    /**
     * Дополнительный метод для отладки прав пользователя
     */
    public String getUserRoleInfo(Long chatId, Long userId) {
        try {
            ChatMember member = getChatMember(chatId, userId);

            if (member instanceof ChatMemberOwner) {
                return "Владелец";
            } else if (member instanceof ChatMemberAdministrator) {
                ChatMemberAdministrator admin = (ChatMemberAdministrator) member;
                return String.format("Администратор (может банить: %s)", admin.getCanRestrictMembers());
            } else {
                return "Обычный пользователь";
            }

        } catch (TelegramApiException e) {
            return "Ошибка при получении информации: " + e.getMessage();
        }
    }
}
