package com.messenger.models;

/**
 * Модель повідомлення — центральна сутність системи.
 *
 * Кожне повідомлення має унікальний ідентифікатор, прив'язане до розмови
 * та відправника, містить текст і метадані (час створення, поточний статус).
 *
 * Життєвий цикл статусу (Варіант 2):
 *
 *   [*] ──► sent ──► delivered ──► read ──► [*]
 *
 *   sent      — повідомлення збережено в БД, доставка ініційована
 *   delivered — клієнт-одержувач підтвердив отримання (ACK delivered)
 *   read      — одержувач відкрив повідомлення (ACK read)
 */
public class Message {

    /** Унікальний ідентифікатор повідомлення (UUID). */
    private String id;

    /** Ідентифікатор розмови, до якої належить повідомлення. */
    private String conversationId;

    /** Ідентифікатор користувача, який надіслав повідомлення. */
    private String senderId;

    /** Текстовий вміст повідомлення. */
    private String text;

    /**
     * Час створення повідомлення у форматі ISO-8601.
     * Генерується через {@code Instant.now().toString()}.
     * Приклад: "2025-01-01T12:00:00Z"
     */
    private String createdAt;

    /**
     * Поточний статус повідомлення.
     * Допустимі значення: "sent", "delivered", "read".
     * За замовчуванням встановлюється "sent" при створенні.
     */
    private String status;

    /** Конструктор без аргументів — для десеріалізації з файлу. */
    public Message() {}

    /**
     * Основний конструктор. Статус встановлюється як "sent" автоматично.
     *
     * @param id             UUID повідомлення
     * @param conversationId UUID розмови
     * @param senderId       UUID відправника
     * @param text           текст повідомлення
     * @param createdAt      час створення (ISO-8601)
     */
    public Message(String id, String conversationId, String senderId,
                   String text, String createdAt) {
        this.id             = id;
        this.conversationId = conversationId;
        this.senderId       = senderId;
        this.text           = text;
        this.createdAt      = createdAt;
        this.status         = "sent"; // початковий статус завжди "sent"
    }

    // ── Гетери та сетери ─────────────────────────────────────────────────────

    public String getId()                      { return id; }
    public void   setId(String id)             { this.id = id; }
    public String getConversationId()          { return conversationId; }
    public void   setConversationId(String c)  { this.conversationId = c; }
    public String getSenderId()                { return senderId; }
    public void   setSenderId(String s)        { this.senderId = s; }
    public String getText()                    { return text; }
    public void   setText(String t)            { this.text = t; }
    public String getCreatedAt()               { return createdAt; }
    public void   setCreatedAt(String c)       { this.createdAt = c; }
    public String getStatus()                  { return status; }
    public void   setStatus(String s)          { this.status = s; }

    /**
     * Серіалізує повідомлення у JSON-рядок.
     * Спеціальні символи в тексті екрануються методом {@link #escape(String)}.
     *
     * @return JSON-представлення повідомлення
     */
    @Override
    public String toString() {
        return "{"
            + "\"id\":\""             + id             + "\","
            + "\"conversationId\":\"" + conversationId + "\","
            + "\"senderId\":\""       + senderId       + "\","
            + "\"text\":\""           + escape(text)   + "\","
            + "\"createdAt\":\""      + createdAt      + "\","
            + "\"status\":\""         + status         + "\""
            + "}";
    }

    /**
     * Екранує спеціальні символи для коректного JSON-рядка.
     * Замінює зворотні слеші та лапки відповідними escape-послідовностями.
     *
     * @param s вхідний рядок
     * @return рядок із заекранованими символами
     */
    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
