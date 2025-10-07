package org.example;

public class TimeParser {

    /**
     * Парсит время из строки в минуты
     */
    public static int parseTime(String timeStr) throws NumberFormatException {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            throw new NumberFormatException("Пустая строка времени");
        }

        timeStr = timeStr.toLowerCase().trim();

        try {
            if (timeStr.endsWith("h")) {
                return Integer.parseInt(timeStr.replace("h", "")) * 60;
            } else if (timeStr.endsWith("d")) {
                return Integer.parseInt(timeStr.replace("d", "")) * 60 * 24;
            } else if (timeStr.endsWith("m")) {
                return Integer.parseInt(timeStr.replace("m", ""));
            } else {
                return Integer.parseInt(timeStr);
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Неверный формат времени: " + timeStr);
        }
    }

    /**
     * Проверяет валидность времени мута
     */
    public static boolean isValidMuteTime(int minutes) {
        return minutes > 0 && minutes <= 60 * 24 * 30; // от 1 минуты до 30 дней
    }

    /**
     * Получает максимальное допустимое время мута
     */
    public static int getMaxMuteTime() {
        return 60 * 24 * 30; // 30 дней в минутах
    }
}
