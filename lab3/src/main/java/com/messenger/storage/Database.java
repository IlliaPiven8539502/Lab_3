package com.messenger.storage;

import com.messenger.models.Conversation;
import com.messenger.models.Message;
import com.messenger.models.User;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Клас для роботи з файловим сховищем даних.
 *
 * Використовує формат JSON Lines (JSONL) — кожен рядок файлу є окремим
 * JSON-об'єктом. Такий підхід дозволяє:
 *   - дозаписувати нові записи без перечитування всього файлу (APPEND)
 *   - зберігати дані між перезапусками сервера (персистентність)
 *   - читати файли звичайним текстовим редактором
 *
 * Файли сховища:
 *   users.jsonl         — записи користувачів
 *   conversations.jsonl — записи розмов
 *   messages.jsonl      — записи повідомлень
 *
 * Парсинг JSON виконується власним методом {@link #extractField(String, String)}
 * без зовнішніх бібліотек.
 */
public class Database {

    /** Шлях до директорії з файлами даних. */
    private final Path dataDir;

    /** Файл із записами користувачів. */
    private final Path usersFile;

    /** Файл із записами розмов. */
    private final Path conversationsFile;

    /** Файл із записами повідомлень. */
    private final Path messagesFile;

    /**
     * Ініціалізує сховище: створює директорію та порожні файли, якщо їх немає.
     *
     * @param dataDirectory шлях до директорії для зберігання файлів
     * @throws IOException якщо не вдається створити директорію або файли
     */
    public Database(String dataDirectory) throws IOException {
        this.dataDir           = Paths.get(dataDirectory);
        this.usersFile         = dataDir.resolve("users.jsonl");
        this.conversationsFile = dataDir.resolve("conversations.jsonl");
        this.messagesFile      = dataDir.resolve("messages.jsonl");

        // Створюємо директорію та файли, якщо вони відсутні
        Files.createDirectories(dataDir);
        for (Path p : List.of(usersFile, conversationsFile, messagesFile)) {
            if (!Files.exists(p)) Files.createFile(p);
        }
    }

    // ── Користувачі ───────────────────────────────────────────────────────────

    /**
     * Зберігає нового користувача у файл (дозаписує рядок).
     *
     * @param user об'єкт користувача для збереження
     * @throws IOException при помилці запису
     */
    public void saveUser(User user) throws IOException {
        appendLine(usersFile, user.toString());
    }

    /**
     * Завантажує всіх користувачів з файлу.
     *
     * @return список усіх збережених користувачів
     * @throws IOException при помилці читання
     */
    public List<User> loadUsers() throws IOException {
        List<User> users = new ArrayList<>();
        for (String line : Files.readAllLines(usersFile)) {
            if (!line.isBlank()) users.add(parseUser(line));
        }
        return users;
    }

    /**
     * Шукає користувача за ідентифікатором.
     *
     * @param id UUID користувача
     * @return Optional з користувачем, або порожній Optional якщо не знайдено
     * @throws IOException при помилці читання
     */
    public Optional<User> findUserById(String id) throws IOException {
        return loadUsers().stream()
            .filter(u -> u.getId().equals(id))
            .findFirst();
    }

    // ── Розмови ───────────────────────────────────────────────────────────────

    /**
     * Зберігає нову розмову у файл.
     *
     * @param conv об'єкт розмови для збереження
     * @throws IOException при помилці запису
     */
    public void saveConversation(Conversation conv) throws IOException {
        appendLine(conversationsFile, conv.toString());
    }

    /**
     * Завантажує всі розмови з файлу.
     *
     * @return список усіх розмов
     * @throws IOException при помилці читання
     */
    public List<Conversation> loadConversations() throws IOException {
        List<Conversation> list = new ArrayList<>();
        for (String line : Files.readAllLines(conversationsFile)) {
            if (!line.isBlank()) list.add(parseConversation(line));
        }
        return list;
    }

    /**
     * Шукає розмову за ідентифікатором.
     *
     * @param id UUID розмови
     * @return Optional з розмовою, або порожній Optional якщо не знайдено
     * @throws IOException при помилці читання
     */
    public Optional<Conversation> findConversationById(String id) throws IOException {
        return loadConversations().stream()
            .filter(c -> c.getId().equals(id))
            .findFirst();
    }

    // ── Повідомлення ──────────────────────────────────────────────────────────

    /**
     * Зберігає нове повідомлення у файл (дозаписує рядок).
     *
     * @param msg об'єкт повідомлення для збереження
     * @throws IOException при помилці запису
     */
    public void saveMessage(Message msg) throws IOException {
        appendLine(messagesFile, msg.toString());
    }

    /**
     * Завантажує всі повідомлення конкретної розмови.
     *
     * @param conversationId UUID розмови
     * @return список повідомлень, відфільтрованих за розмовою
     * @throws IOException при помилці читання
     */
    public List<Message> loadMessagesByConversation(String conversationId) throws IOException {
        List<Message> list = new ArrayList<>();
        for (String line : Files.readAllLines(messagesFile)) {
            if (line.isBlank()) continue;
            Message m = parseMessage(line);
            if (conversationId.equals(m.getConversationId())) list.add(m);
        }
        return list;
    }

    /**
     * Оновлює статус повідомлення за його ідентифікатором.
     *
     * Оскільки формат JSONL не підтримує редагування рядків на місці,
     * метод перечитує весь файл, знаходить потрібний рядок, замінює статус
     * і перезаписує файл повністю.
     *
     * @param messageId UUID повідомлення
     * @param newStatus новий статус ("sent", "delivered" або "read")
     * @return true — якщо повідомлення знайдено і оновлено, false — якщо ні
     * @throws IOException при помилці читання або запису
     */
    public boolean updateMessageStatus(String messageId, String newStatus) throws IOException {
        List<String> lines   = Files.readAllLines(messagesFile);
        List<String> updated = new ArrayList<>();
        boolean found = false;

        for (String line : lines) {
            if (line.isBlank()) continue;
            Message m = parseMessage(line);
            if (m.getId().equals(messageId)) {
                // Знайдено — оновлюємо статус і серіалізуємо знову
                m.setStatus(newStatus);
                updated.add(m.toString());
                found = true;
            } else {
                updated.add(line);
            }
        }

        if (found) Files.write(messagesFile, updated);
        return found;
    }

    // ── Приватні утиліти ──────────────────────────────────────────────────────

    /**
     * Дозаписує рядок у кінець файлу з переносом рядка.
     *
     * @param file   шлях до файлу
     * @param content рядок для запису
     * @throws IOException при помилці запису
     */
    private void appendLine(Path file, String content) throws IOException {
        Files.writeString(file, content + "\n", StandardOpenOption.APPEND);
    }

    /**
     * Витягує значення поля з JSON-рядка за ключем.
     *
     * Реалізує мінімальний парсинг без зовнішніх бібліотек:
     * шукає шаблон {@code "key":"value"} і повертає value.
     * Підтримує екранування символів (\\ та \").
     *
     * @param json JSON-рядок
     * @param key  назва поля
     * @return значення поля або порожній рядок якщо не знайдено
     */
    private String extractField(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return "";
        start += search.length();

        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                // Екранований символ — беремо наступний
                sb.append(json.charAt(i + 1));
                i++;
            } else if (c == '"') {
                // Кінець значення
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Десеріалізує рядок JSON у об'єкт {@link User}. */
    private User parseUser(String json) {
        return new User(extractField(json, "id"), extractField(json, "name"));
    }

    /** Десеріалізує рядок JSON у об'єкт {@link Conversation}. */
    private Conversation parseConversation(String json) {
        return new Conversation(extractField(json, "id"), extractField(json, "type"));
    }

    /** Десеріалізує рядок JSON у об'єкт {@link Message}. */
    private Message parseMessage(String json) {
        Message m = new Message(
            extractField(json, "id"),
            extractField(json, "conversationId"),
            extractField(json, "senderId"),
            extractField(json, "text"),
            extractField(json, "createdAt")
        );
        m.setStatus(extractField(json, "status"));
        return m;
    }
}
