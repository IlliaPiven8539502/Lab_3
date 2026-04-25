package com.messenger.services;

import com.messenger.models.Conversation;
import com.messenger.models.Message;
import com.messenger.models.User;
import com.messenger.storage.Database;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервіс повідомлень — рівень бізнес-логіки.
 *
 * Цей клас є посередником між HTTP API ({@link com.messenger.api.Router})
 * та сховищем даних ({@link Database}). Він:
 *   - виконує валідацію вхідних даних перед збереженням
 *   - генерує унікальні ідентифікатори та мітки часу
 *   - реалізує бізнес-правила (наприклад, допустимі статуси повідомлень)
 *   - ізолює HTTP-шар від деталей зберігання даних
 *
 * Принцип єдиної відповідальності (SRP):
 *   Router відповідає за HTTP, Database — за файли, MessageService — за логіку.
 */
public class MessageService {

    /**
     * Посилання на сховище даних.
     * Передається через конструктор (Dependency Injection) — це дозволяє
     * підмінити реалізацію Database у тестах без зміни коду сервісу.
     */
    private final Database db;

    /**
     * Конструктор із впровадженням залежності.
     *
     * @param db екземпляр бази даних для збереження та читання записів
     */
    public MessageService(Database db) {
        this.db = db;
    }

    // ── Користувачі ───────────────────────────────────────────────────────────

    /**
     * Створює нового користувача та зберігає його у сховищі.
     *
     * Валідація: ім'я не може бути null або порожнім рядком.
     * UUID генерується автоматично через {@code UUID.randomUUID()}.
     *
     * @param name ім'я нового користувача
     * @return створений об'єкт {@link User} з присвоєним id
     * @throws IllegalArgumentException якщо ім'я порожнє або null
     * @throws IOException при помилці запису у файл
     */
    public User createUser(String name) throws IOException {
        // Валідація: відхиляємо порожнє ім'я
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Ім'я користувача не може бути порожнім");
        }
        // Генеруємо унікальний id та зберігаємо користувача
        User user = new User(UUID.randomUUID().toString(), name.trim());
        db.saveUser(user);
        return user;
    }

    /**
     * Повертає список усіх зареєстрованих користувачів.
     *
     * @return список об'єктів {@link User}
     * @throws IOException при помилці читання з файлу
     */
    public List<User> getAllUsers() throws IOException {
        return db.loadUsers();
    }

    /**
     * Шукає користувача за його ідентифікатором.
     *
     * @param id UUID користувача
     * @return Optional із знайденим користувачем або порожній Optional
     * @throws IOException при помилці читання з файлу
     */
    public Optional<User> getUserById(String id) throws IOException {
        return db.findUserById(id);
    }

    // ── Розмови ───────────────────────────────────────────────────────────────

    /**
     * Створює нову розмову заданого типу.
     *
     * Валідація: тип має бути "direct" або "group".
     *
     * @param type тип розмови ("direct" або "group")
     * @return створений об'єкт {@link Conversation}
     * @throws IllegalArgumentException якщо тип не є допустимим
     * @throws IOException при помилці запису у файл
     */
    public Conversation createConversation(String type) throws IOException {
        if (!type.equals("direct") && !type.equals("group")) {
            throw new IllegalArgumentException("Тип розмови має бути 'direct' або 'group'");
        }
        Conversation conv = new Conversation(UUID.randomUUID().toString(), type);
        db.saveConversation(conv);
        return conv;
    }

    /**
     * Повертає список усіх розмов у системі.
     *
     * @return список об'єктів {@link Conversation}
     * @throws IOException при помилці читання з файлу
     */
    public List<Conversation> getAllConversations() throws IOException {
        return db.loadConversations();
    }

    // ── Повідомлення ──────────────────────────────────────────────────────────

    /**
     * Надсилає нове повідомлення у розмову.
     *
     * Виконує три перевірки перед збереженням:
     *   1. Текст повідомлення не може бути порожнім
     *   2. Відправник повинен існувати в системі
     *   3. Розмова повинна існувати в системі
     *
     * Початковий статус повідомлення — "sent".
     * Час створення фіксується у момент виклику методу.
     *
     * @param conversationId UUID розмови
     * @param senderId       UUID відправника
     * @param text           текст повідомлення
     * @return збережений об'єкт {@link Message} з id, createdAt та status="sent"
     * @throws IllegalArgumentException якщо будь-яка з перевірок не пройдена
     * @throws IOException при помилці запису у файл
     */
    public Message sendMessage(String conversationId, String senderId, String text)
            throws IOException {

        // Перевірка 1: текст не може бути порожнім
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Текст повідомлення не може бути порожнім");
        }

        // Перевірка 2: відправник повинен існувати
        Optional<User> sender = db.findUserById(senderId);
        if (sender.isEmpty()) {
            throw new IllegalArgumentException(
                "Користувача з id='" + senderId + "' не знайдено"
            );
        }

        // Перевірка 3: розмова повинна існувати
        Optional<Conversation> conv = db.findConversationById(conversationId);
        if (conv.isEmpty()) {
            throw new IllegalArgumentException(
                "Розмову з id='" + conversationId + "' не знайдено"
            );
        }

        // Створюємо повідомлення: UUID + поточний час + статус "sent"
        Message msg = new Message(
            UUID.randomUUID().toString(),  // унікальний id
            conversationId,
            senderId,
            text.trim(),
            Instant.now().toString()       // мітка часу ISO-8601
        );
        db.saveMessage(msg);
        return msg;
    }

    /**
     * Повертає всі повідомлення конкретної розмови.
     *
     * Перевіряє існування розмови перед поверненням результату.
     *
     * @param conversationId UUID розмови
     * @return список повідомлень у хронологічному порядку
     * @throws IllegalArgumentException якщо розмова не існує
     * @throws IOException при помилці читання з файлу
     */
    public List<Message> getMessages(String conversationId) throws IOException {
        Optional<Conversation> conv = db.findConversationById(conversationId);
        if (conv.isEmpty()) {
            throw new IllegalArgumentException(
                "Розмову з id='" + conversationId + "' не знайдено"
            );
        }
        return db.loadMessagesByConversation(conversationId);
    }

    /**
     * Оновлює статус повідомлення (Варіант 2 — відстеження статусів).
     *
     * Реалізує явне підтвердження від клієнта (ACK-механізм):
     *   sent → delivered : клієнт підтвердив отримання
     *   delivered → read : клієнт відкрив повідомлення
     *
     * Валідація: статус має бути одним із допустимих значень.
     *
     * @param messageId UUID повідомлення
     * @param newStatus новий статус ("sent", "delivered" або "read")
     * @return true — якщо повідомлення знайдено і оновлено
     * @throws IllegalArgumentException якщо статус не є допустимим
     * @throws IOException при помилці читання або запису
     */
    public boolean updateStatus(String messageId, String newStatus) throws IOException {
        // Перевірка допустимих значень статусу
        if (!List.of("sent", "delivered", "read").contains(newStatus)) {
            throw new IllegalArgumentException(
                "Невідомий статус '" + newStatus + "'. Допустимі: sent, delivered, read"
            );
        }
        return db.updateMessageStatus(messageId, newStatus);
    }
}
