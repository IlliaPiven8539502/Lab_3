package com.messenger.models;

/**
 * Модель користувача системи.
 *
 * Зберігає мінімально необхідні дані для ідентифікації учасника розмови.
 * Об'єкти цього класу створюються у {@link com.messenger.services.MessageService}
 * та зберігаються через {@link com.messenger.storage.Database}.
 *
 * Поля:
 *   id   — унікальний ідентифікатор (UUID), генерується автоматично
 *   name — ім'я користувача, задане при реєстрації
 */
public class User {

    /** Унікальний ідентифікатор користувача у форматі UUID. */
    private String id;

    /** Відображуване ім'я користувача. */
    private String name;

    /** Конструктор без аргументів — потрібен для десеріалізації. */
    public User() {}

    /**
     * Основний конструктор.
     *
     * @param id   UUID-ідентифікатор
     * @param name ім'я користувача
     */
    public User(String id, String name) {
        this.id   = id;
        this.name = name;
    }

    // ── Гетери та сетери ─────────────────────────────────────────────────────

    public String getId()            { return id; }
    public void   setId(String id)   { this.id = id; }
    public String getName()          { return name; }
    public void   setName(String n)  { this.name = n; }

    /**
     * Серіалізує об'єкт у рядок JSON.
     * Використовується при збереженні у файл та формуванні HTTP-відповідей.
     *
     * @return JSON-представлення користувача
     */
    @Override
    public String toString() {
        return "{\"id\":\"" + id + "\",\"name\":\"" + name + "\"}";
    }
}
