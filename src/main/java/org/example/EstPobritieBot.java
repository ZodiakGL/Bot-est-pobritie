package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Instant;

public class EstPobritieBot extends TelegramLongPollingBot {
    private final PermissionChecker permissionChecker;
    private final String botToken;

    public EstPobritieBot() {
        this.botToken = System.getenv("BOT_TOKEN");
        System.out.println("EstPobritieBot constructor - token: " +
                (this.botToken != null ? "SET (length: " + this.botToken.length() + ")" : "NULL"));

        if (this.botToken == null || this.botToken.isEmpty()) {
            throw new IllegalStateException("BOT_TOKEN environment variable is not set!");
        }
        this.permissionChecker = new PermissionChecker(this);
    }

    @Override
    public String getBotToken() {
        System.out.println("getBotToken() called, returning: " +
                (this.botToken != null ? "***" + this.botToken.substring(this.botToken.length() - 4) : "NULL"));
        return this.botToken;
    }

    @Override
    public String getBotUsername() {
        String username = "EstPobritieBot";
        System.out.println("getBotUsername() called, returning: " + username);
        return username;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Message message = update.getMessage();
        String text = message.getText();

        if (text.startsWith("/mute")) {
            handleMuteCommand(message);
        } else if (text.startsWith("/myrights")) {
            handleMyRightsCommand(message);
        } else if (text.startsWith("/checkrights")) {
            handleCheckRightsCommand(message);
        }
    }

    public void muteUser(Long chatId, Long userId, int minutes) {
        ChatPermissions permissions = new ChatPermissions();
        permissions.setCanSendMessages(false);
        permissions.setCanSendMediaMessages(false);
        permissions.setCanSendOtherMessages(false);
        permissions.setCanAddWebPagePreviews(false);

        RestrictChatMember restrict = new RestrictChatMember();
        restrict.setChatId(chatId.toString());
        restrict.setUserId(userId);
        restrict.setPermissions(permissions);

        long muteUntil = Instant.now().plusSeconds(minutes * 60L).getEpochSecond();
        restrict.setUntilDate((int) muteUntil);

        try {
            execute(restrict);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleMuteCommand(Message message) {
        Long chatId = message.getChatId();
        Long executorUserId = message.getFrom().getId();

        Integer messageThreadId = message.getMessageThreadId();

        System.out.println("Chat ID: " + chatId);
        System.out.println("Message Thread ID: " + messageThreadId);
        System.out.println("Is topic message: " + (messageThreadId != null && messageThreadId > 0));
        System.out.println("Message text: " + message.getText());

        // Проверяем, что команда в группе
        if (!permissionChecker.isGroupChat(message.getChat())) {
            sendText(chatId, "❌ Эта команда работает только в группах и супергруппах!");
            return;
        }

        Long targetUserId = getTargetUserId(message);
        if (targetUserId == null) {
            return;
        }

        PermissionCheckResult permissionCheck = permissionChecker.checkMutePermissions(
                chatId, executorUserId, targetUserId
        );

        if (permissionCheck.hasError()) {
            sendText(chatId, permissionCheck.getErrorMessage());
            return;
        }

        try {
            int minutes = parseMuteTime(message.getText());

            if (!TimeParser.isValidMuteTime(minutes)) {
                sendText(chatId, String.format(
                        "❌ Время мута должно быть от 1 минуты до %d дней!",
                        TimeParser.getMaxMuteTime() / (60 * 24)
                ));
                return;
            }

            muteUser(chatId, targetUserId, minutes);
            String targetUsername = getUsername(message.getReplyToMessage().getFrom());
            sendText(chatId, "✅ Пользователю " + targetUsername + " выдан мут на " + minutes + " минут");

        } catch (NumberFormatException e) {
            sendText(chatId, "❌ Неверный формат времени. Используйте: /mute время_в_минутах");
        }
    }

    private void sendText(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Всегда используем ответ на сообщение для отправки в нужную тему
    private void sendReply(Message originalMessage, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(originalMessage.getChatId().toString());
        message.setText(text);
        message.setReplyToMessageId(originalMessage.getMessageId());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private Long getTargetUserId(Message message) {
        if (message.getReplyToMessage() != null) {
            return message.getReplyToMessage().getFrom().getId();
        }
        return null;
    }

    private int parseMuteTime(String text) {
        String[] parts = text.split(" ");
        if (parts.length >= 2) {
            return TimeParser.parseTime(parts[1]);
        }
        return 5; // значение по умолчанию
    }

    private String getUsername(User user) {
        if (user.getUserName() != null) {
            return "@" + user.getUserName();
        } else {
            return user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "");
        }
    }

    private void handleMyRightsCommand(Message message) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        String roleInfo = permissionChecker.getUserRoleInfo(chatId, userId);
        boolean canBan = permissionChecker.canUserBanMembers(chatId, userId);

        String response = "👤 Ваши права:\n" +
                roleInfo + "\n\n" +
                "Может использовать /mute: " + (canBan ? "✅ Да" : "❌ Нет");

        sendReply(message, response);
    }

    private void handleCheckRightsCommand(Message message) {
        // Проверяем права того, на кого ответили
        if (message.getReplyToMessage() == null) {
            sendReply(message, "❌ Ответьте на сообщение пользователя, чтобы проверить его права");
            return;
        }

        Long chatId = message.getChatId();
        Long targetUserId = message.getReplyToMessage().getFrom().getId();

        String roleInfo = permissionChecker.getUserRoleInfo(chatId, targetUserId);
        boolean canBeMuted = permissionChecker.canUserBeMuted(chatId, targetUserId);

        String response = "👤 Права пользователя " + getUsername(message.getReplyToMessage().getFrom()) + ":\n" +
                roleInfo + "\n\n" +
                "Может быть замьючен: " + (canBeMuted ? "✅ Да" : "❌ Нет");

        sendReply(message, response);
    }
}
