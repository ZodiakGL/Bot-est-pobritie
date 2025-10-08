package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;

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

            Timer keepAliveTimer = new Timer(true);
            keepAliveTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        System.out.println("🔄 Keep-alive ping: " + new Date() +
                                " | Instance: " + System.getenv("RENDER_INSTANCE_ID"));
                    } catch (Exception e) {
                        System.out.println("❌ Keep-alive error: " + e.getMessage());
                    }
                }
            }, 0, 2 * 60 * 1000); // Каждые 2 минуты

            System.out.println("🔄 Keep-alive timer started (every 2 minutes)");

            while (true) {
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Ошибка при запуске бота: " + e.getMessage());
        }
    }
}