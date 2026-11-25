# Інформаційна система обліку дефектів будівельних об’єктів

**Варіант 16** | Дисципліна «Кросплатформене програмування» | 2026–2027

**Студент:** Омельченко Ярослав Олександрович  
**Група:** 201-ТН

## Опис

Клієнт-серверна кросплатформена інформаційна система мовою Java:

- **JavaFX-клієнт** ↔ HTTP/JSON ↔ **Servlet** → **Service** → **DAO** → **JDBC** → **MySQL**
- 4 основні сутності: Будівельний об’єкт, Дефект, Обстеження, Ремонтне завдання
- Життєвий цикл **Дефекту**: `ЗАФІКСОВАНО` → `ПЕРЕВІРЕНО` → `РЕМОНТ_ПРИЗНАЧЕНО` → `ЗАКРИТО`
- Інтегральний показник: \( I = 0{,}40\cdot F1 + 0{,}35\cdot F2 + 0{,}25\cdot F3 \)  
  (F1 — тяжкість, F2 — площа/масштаб, F3 — вплив на безпеку; 0–100)
- Фоновий CSV-імпорт обстежень через JavaFX `Task` + ProgressBar
- ≥ 10 модульних тестів JUnit 5
- Переносимість: без абсолютних шляхів, конфігурація через змінні середовища

## Вимоги до середовища

| Компонент        | Версія / примітка              |
|------------------|--------------------------------|
| JDK              | 21                             |
| Maven            | 3.8+                           |
| MySQL            | 8.x                            |
| Apache Tomcat    | **9.x** (javax.servlet)        |
| JavaFX           | 21 (підтягується Maven)        |

## Структура репозиторію

```
coursework/
├── database.sql              # схема + демо-дані
├── sample-data/
│   └── inspections.csv       # приклад CSV для імпорту
├── server/                   # Maven WAR-проєкт (Servlet + Service + DAO)
│   └── pom.xml
├── client/                   # Maven JAR-проєкт (JavaFX)
│   └── src/main/java/com/coursework/
│       ├── ui/MainApp.java   # ЄДИНИЙ клас інтерфейсу (усі вкладки + HTTP)
│       ├── model/            # моделі даних
│       └── util/JsonUtil.java
│   └── pom.xml
├── README.md
└── .gitignore
```

**Важливо (вимоги 2026):** JavaFX-клієнт реалізовано в **одному класі** `MainApp.java`.
Окремі View-класи та клас `ApiClient` відсутні — HTTP-виклики та весь UI знаходяться безпосередньо в `MainApp`.

## 1. Підготовка MySQL

```bash
mysql -u root -p < database.sql
```

Або виконати `database.sql` у MySQL Workbench / CLI.

База: `building_defects`.

### Змінні середовища (опційно)

```bash
export DB_URL="jdbc:mysql://localhost:3306/building_defects?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DB_USER="root"
export DB_PASSWORD="your_password"
```

За замовчуванням: `root` / `root`.

## 2. Збірка та тести сервера

```bash
cd server
mvn clean test
mvn package
```

WAR: `server/target/building-defects-server.war`

## 3. Запуск сервера (Tomcat 9)

1. Скопіювати `building-defects-server.war` у `$CATALINA_HOME/webapps/`
2. Запустити Tomcat 9
3. Контекст: `http://localhost:8080/building-defects-server`

Перевірка:

```bash
curl http://localhost:8080/building-defects-server/api/buildings
```

## 4. Запуск JavaFX-клієнта

```bash
cd client
mvn clean javafx:run
```

Або:

```bash
mvn clean package
# далі javafx:run або java з module-path JavaFX
```

Базовий URL API можна перевизначити:

```bash
export API_BASE_URL="http://localhost:8080/building-defects-server"
```

## 5. CSV-імпорт

Файл `sample-data/inspections.csv`:

```
buildingObjectId;inspectionDate;inspectorName;findings;measuredValue;unit;notes
```

У клієнті: вкладка **«Обстеження + CSV»** → кнопка **«Імпорт CSV…»**.  
Імпорт виконується у фоновому `Task`, прогрес відображається, інтерфейс не блокується. Можливе скасування.

## 6. Основні API-ендпоінти

| Метод | Шлях | Опис |
|-------|------|------|
| GET/POST | `/api/buildings` | CRUD будівельних об’єктів |
| GET/PUT/DELETE | `/api/buildings/{id}` | |
| GET/POST | `/api/defects` | CRUD дефектів |
| POST | `/api/defects/{id}/state` | зміна стану `{"state":"ПЕРЕВІРЕНО"}` |
| GET/POST | `/api/inspections` | CRUD обстежень |
| POST | `/api/inspections/import` | пакетний імпорт JSON-масиву |
| GET/POST | `/api/repairs` | CRUD ремонтних завдань |

## 7. Тести

```bash
cd server && mvn test
```

Тести покривають:
- формулу інтегрального показника (різні випадки, clamp 0–100)
- допустимі та недопустимі переходи життєвого циклу
- парсинг enum стану
- узгодженість розрахунку в моделі та сервісі

## 8. Сценарій демонстрації

1. Запустити MySQL + виконати `database.sql`
2. Задеплоїти WAR у Tomcat 9
3. Запустити JavaFX-клієнт
4. Вкладка «Будівельні об’єкти» — перегляд / додавання
5. Вкладка «Дефекти» — створення, зміна стану по ланцюжку, перегляд I
6. Вкладка «Обстеження + CSV» — імпорт `sample-data/inspections.csv`
7. Вкладка «Ремонтні завдання» — створення завдання для дефекту
8. Показати логи та `mvn test`

## Автор

Омельченко Ярослав Олександрович, група 201-ТН  
Національний університет «Полтавська політехніка імені Юрія Кондратюка»

## Git-історія (змістовні commit’и)

| Commit | Повідомлення |
|--------|--------------|
| 1 | Initialize Maven multi-module coursework project (variant 16) |
| 2 | Add MySQL schema and sample CSV for inspections |
| 3 | Add domain models and DB connection utility |
| 4 | Implement DAO layer with JDBC PreparedStatement |
| 5 | Implement Service layer: validation, integral score, lifecycle |
| 6 | Add Servlet API endpoints for HTTP/JSON interaction |
| 7 | Implement JavaFX UI with CRUD and background CSV import |
| 8 | Add 59 JUnit 5 tests for service logic and lifecycle |

Фінальний tag: **`coursework-final`**

```bash
git remote add origin https://github.com/USERNAME/kpp-coursework-omelchenko-variant16.git
git push -u origin main
git push origin coursework-final
```
