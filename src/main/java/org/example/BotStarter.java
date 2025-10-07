package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class BotStarter {
    public static void main(String[] args) {
        try {
            String botToken = System.getenv("BOT_TOKEN");
            System.out.println("=== HARD DEBUG ===");
            System.out.println("BOT_TOKEN: '" + botToken + "'");
            System.out.println("Is null: " + (botToken == null));
            System.out.println("Is empty: " + (botToken != null && botToken.isEmpty()));
            System.out.println("Length: " + (botToken != null ? botToken.length() : 0));

            System.out.println("All environment variables:");
            System.getenv().forEach((key, value) -> {
                System.out.println("  " + key + " = " + value);
            });

            if (botToken == null || botToken.isEmpty()) {
                System.out.println("❌ BOT_TOKEN IS NOT AVAILABLE!");
                return;
            }

            System.out.println("✅ BOT_TOKEN is available, starting bot...");

            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new EstPobritieBot());
            System.out.println("✅ Бот EstPobritieBot запущен и работает!");

            while (true) {
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Ошибка при запуске бота: " + e.getMessage());
        }
    }
}