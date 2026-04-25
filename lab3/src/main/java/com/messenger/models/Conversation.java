package com.messenger.models;

/**
 * Модель розмови (чату).
 *
 * Розмова є контейнером для повідомлень між двома або більше учасниками.
 * Кожне повідомлення ({@link Message}) прив'язане до конкретної розмови
 * через поле {@code conversationId}.
 *
 * Типи розмов:
 *   direct — приватна розмова між двома користувачами
 *   group  — групова розмова (розширення на майбутнє)
 */
public class Conversation {

    /** Унікальний ідентифікатор розмови (UUID). */
    private String id;

    /**
     * Тип розмови.
     * Допустимі значення: "direct", "group".
     * Валідація відбувається у {@link com.messenger.services.MessageService}.
     */
    private String type;

    /** Конструктор без аргументів — для десеріалізації з файлу. */
    public Conversation() {}

    /**
     * Основний конструктор.
     *
     * @param id   UUID розмови
     * @param type тип: "direct" або "group"
     */
    public Conversation(String id, String type) {
        this.id   = id;
        this.type = type;
    }

    // ── Гетери та сетери ─────────────────────────────────────────────────────

    public String getId()              { return id; }
    public void   setId(String id)    { this.id = id; }
    public String getType()            { return type; }
    public void   setType(String type) { this.type = type; }

    /**
     * Серіалізує розмову у JSON-рядок.
     *
     * @return JSON-представлення розмови
     */
    @Override
    public String toString() {
        return "{\"id\":\"" + id + "\",\"type\":\"" + type + "\"}";
    }
}
