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

    public boolean isGroupChat(Chat chat) {
        return chat.isGroupChat() || chat.isSuperGroupChat();
    }

    /**
     * Проверяет, может ли пользователь банить других пользователей
     */
    public boolean canUserBanMembers(Long chatId, Long userId) {
        try {
            System.out.println("🔍 Checking ban permissions for user: " + userId + " in chat: " + chatId);

            ChatMember member = getChatMember(chatId, userId);

            // Владелец может все
            if (member instanceof ChatMemberOwner) {
                System.out.println("✅ User is OWNER - can ban");
                return true;
            }

            // Администратор - проверяем право ban users
            if (member instanceof ChatMemberAdministrator) {
                ChatMemberAdministrator admin = (ChatMemberAdministrator) member;
                boolean canRestrict = admin.getCanRestrictMembers();
                System.out.println("👤 User is ADMIN - canRestrictMembers: " + canRestrict);

                // Детальная информация о правах администратора для отладки
                System.out.println("📋 Admin rights details:");
                System.out.println("   - canRestrictMembers: " + admin.getCanRestrictMembers());
                System.out.println("   - canDeleteMessages: " + admin.getCanDeleteMessages());
                System.out.println("   - canInviteUsers: " + admin.getCanInviteUsers());
                System.out.println("   - canPinMessages: " + admin.getCanPinMessages());
                System.out.println("   - canPromoteMembers: " + admin.getCanPromoteMembers());

                return canRestrict;
            }

            // Обычный пользователь не может банить
            System.out.println("❌ User is REGULAR USER - cannot ban");
            return false;

        } catch (TelegramApiException e) {
            System.out.println("❌ Error checking user permissions: " + e.getMessage());
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
            System.out.println("🤖 Checking bot permissions, bot ID: " + botId);

            ChatMember botMember = getChatMember(chatId, Long.valueOf(botId));

            if (botMember instanceof ChatMemberAdministrator) {
                ChatMemberAdministrator admin = (ChatMemberAdministrator) botMember;
                boolean canRestrict = admin.getCanRestrictMembers();
                System.out.println("🤖 Bot is ADMIN - canRestrictMembers: " + canRestrict);
                return canRestrict;
            }

            System.out.println("❌ Bot is NOT ADMIN or cannot restrict members");
            return false;
        } catch (TelegramApiException e) {
            System.out.println("❌ Error checking bot permissions: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Проверяет, может ли пользователь быть замьючен
     */
    public boolean canUserBeMuted(Long chatId, Long userId) {
        try {
            System.out.println("🔍 Checking if user can be muted: " + userId);

            ChatMember member = getChatMember(chatId, userId);

            boolean canBeMuted = !(member instanceof ChatMemberOwner || member instanceof ChatMemberAdministrator);
            System.out.println("👤 User can be muted: " + canBeMuted);

            return canBeMuted;

        } catch (TelegramApiException e) {
            System.out.println("❌ Error checking if user can be muted: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
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
        System.out.println("🚀 Starting permission check for mute:");
        System.out.println("   Executor: " + executorUserId);
        System.out.println("   Target: " + targetUserId);
        System.out.println("   Chat: " + chatId);

        // Проверяем права исполнителя - может ли он банить
        boolean executorCanBan = canUserBanMembers(chatId, executorUserId);
        System.out.println("🎯 Executor can ban: " + executorCanBan);

        if (!executorCanBan) {
            return PermissionCheckResult.noPermission("❌ Ты чо индеец, у тебя нет прав банить пользователей!");
        }

        // Проверяем права бота
        boolean botCanRestrict = canBotRestrictMembers(chatId);
        System.out.println("🎯 Bot can restrict: " + botCanRestrict);

        if (!botCanRestrict) {
            return PermissionCheckResult.noPermission(
                    "❌ У бота нет прав ограничивать пользователей!\n" +
                            "Пожалуйста, выдайте боту права администратора с разрешением 'Ban users'"
            );
        }

        // Проверяем, что цель не админ и не владелец
        boolean targetCanBeMuted = canUserBeMuted(chatId, targetUserId);
        System.out.println("🎯 Target can be muted: " + targetCanBeMuted);

        if (!targetCanBeMuted) {
            return PermissionCheckResult.noPermission("❌ Ты чо блинов объелся? Админов и владельца нельзя мутить!");
        }

        System.out.println("✅ All permissions check passed!");
        return PermissionCheckResult.success();
    }

    /**
     * Детальная информация о правах пользователя
     */
    public String getUserRoleInfo(Long chatId, Long userId) {
        try {
            ChatMember member = getChatMember(chatId, userId);

            if (member instanceof ChatMemberOwner) {
                return "👑 Владелец (может все)";
            } else if (member instanceof ChatMemberAdministrator) {
                ChatMemberAdministrator admin = (ChatMemberAdministrator) member;
                return String.format(
                        "👤 Администратор\n" +
                                "   • Может банить: %s\n" +
                                "   • Может удалять сообщения: %s\n" +
                                "   • Может приглашать: %s\n" +
                                "   • Может закреплять: %s\n" +
                                "   • Может добавлять админов: %s",
                        admin.getCanRestrictMembers() ? "✅" : "❌",
                        admin.getCanDeleteMessages() ? "✅" : "❌",
                        admin.getCanInviteUsers() ? "✅" : "❌",
                        admin.getCanPinMessages() ? "✅" : "❌",
                        admin.getCanPromoteMembers() ? "✅" : "❌"
                );
            } else {
                return "🙂 Обычный пользователь";
            }

        } catch (TelegramApiException e) {
            return "❌ Ошибка при получении информации: " + e.getMessage();
        }
    }
}