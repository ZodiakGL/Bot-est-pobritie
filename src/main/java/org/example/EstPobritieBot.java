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
        if (this.botToken == null || this.botToken.isEmpty()) {
            throw new IllegalStateException("BOT_TOKEN environment variable is not set!");
        }
        this.permissionChecker = new PermissionChecker(this);
    }

    @Override
    public String getBotUsername() {
        return "EstPobritieBot";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Message message = update.getMessage();
        String text = message.getText();

        if (text.startsWith("/mute")) {
            handleMuteCommand(message);
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

        // Проверяем, что команда в группе
        if (!permissionChecker.isGroupChat(message.getChat())) {
            sendText(chatId, "❌ Эта команда работает только в группах и супергруппах!");
            return;
        }

        // Получаем ID целевого пользователя
        Long targetUserId = getTargetUserId(message);
        if (targetUserId == null) {
            sendText(chatId, "❌ Укажите пользователя для мута (ответом на сообщение)");
            return;
        }

        // Проверяем права
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

            // Выдаем мут
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
}
