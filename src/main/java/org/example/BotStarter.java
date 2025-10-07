package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


public class BotStarter {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new EstPobritieBot());
            System.out.println("✅ Бот EstPobritieBot запущен и работает!");

            // Бесконечный цикл чтобы бот не завершал работу
            while (true) {
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Ошибка при запуске бота: " + e.getMessage());
        }
    }
}