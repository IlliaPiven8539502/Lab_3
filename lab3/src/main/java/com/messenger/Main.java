package com.messenger;

import com.messenger.api.Router;
import com.messenger.services.MessageService;
import com.messenger.storage.Database;

/**
 * Точка входу в застосунок — клас Main.
 *
 * Відповідає за ініціалізацію та з'єднання всіх компонентів системи:
 *
 *   1. Database      — файлове сховище даних
 *   2. MessageService — сервіс бізнес-логіки (отримує Database)
 *   3. Router         — HTTP-сервер (отримує MessageService)
 *
 * Такий порядок ініціалізації реалізує патерн Dependency Injection вручну:
 * кожен компонент отримує свої залежності через конструктор,
 * а не створює їх самостійно.
 *
 * Параметри запуску (необов'язкові):
 *   args[0] — порт сервера (за замовчуванням 8080)
 *   args[1] — шлях до директорії з даними (за замовчуванням ./data)
 *
 * Приклади запуску:
 *   java -cp out com.messenger.Main
 *   java -cp out com.messenger.Main 9090
 *   java -cp out com.messenger.Main 9090 /tmp/messenger-data
 */
public class Main {

    public static void main(String[] args) throws Exception {
        // Зчитуємо параметри або використовуємо значення за замовчуванням
        int    port    = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        String dataDir = args.length > 1 ? args[1] : "./data";

        // Ініціалізація компонентів у порядку залежностей
        Database       db      = new Database(dataDir);       // 1. сховище
        MessageService service = new MessageService(db);      // 2. бізнес-логіка
        Router         router  = new Router(service, port);   // 3. HTTP API

        // Запуск сервера
        router.start();
        System.out.println("Дані зберігаються у: " + dataDir);
        System.out.println("Натисніть Ctrl+C для зупинки.");
    }
}
