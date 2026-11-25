# Архітектура програмної системи (варіант 16)

## Маршрут даних

```
JavaFX (MainApp) → HTTP/JSON → Servlet → Service → DAO → JDBC → MySQL
```

Кожен шар має одну відповідальність (SOLID):

| Шар | Пакет / клас | Що робить | Чого НЕ робить |
|-----|--------------|-----------|----------------|
| **UI** | `client/.../ui/MainApp` | форми, таблиці, ProgressBar, HTTP-виклики | SQL, формула I, бізнес-правила |
| **Servlet** | `server/.../servlet/*Servlet` | прийом HTTP, коди 4xx/5xx, JSON | SQL, формула, ЖЦ |
| **Service** | `server/.../service/*Service` | валідація, I, переходи станів | SQL, GUI |
| **DAO** | `server/.../dao/*Dao` | PreparedStatement, CRUD | валідація, JavaFX |
| **DB** | MySQL `building_defects` | зберігання | — |

## Життєвий цикл дефекту

Реалізовано в `DefectState` + `DefectService.changeState()`:

```
ЗАФІКСОВАНО → ПЕРЕВІРЕНО → РЕМОНТ_ПРИЗНАЧЕНО → ЗАКРИТО
```

- Перехід перевіряється в Service; Servlet лише передає `{"state":"..."}`.
- Звичайний `PUT /api/defects/{id}` **не змінює** стан (захист від обходу).
- У UI комбобокс «Новий стан» заповнюється лише `DefectState.allowedNext(...)`.

## Інтегральний показник

```
I = 0,40·F1 + 0,35·F2 + 0,25·F3
```

| Фактор | Зміст (варіант 16) | Діапазон |
|--------|--------------------|----------|
| F1 | тяжкість дефекту | 0–100 |
| F2 | площа / масштаб | 0–100 |
| F3 | вплив на безпеку експлуатації | 0–100 |

Розрахунок — **один раз** у `DefectService.calculateIntegral()`.  
Значення поза [0;100] обрізаються (`clamp`). У БД є generated column `integral_score` для зручності вибірки.

## CSV-імпорт

- Файл обирається через `FileChooser` (немає жорстких шляхів).
- Розбір і HTTP-надсилання — у `javafx.concurrent.Task` (`InspectionView`).
- `ProgressBar` прив’язаний до `progressProperty()`; UI не блокується.
- Підтримується `cancel()` / `isCancelled()`.
- Формат: `buildingObjectId;inspectionDate;inspectorName;findings;measuredValue;unit;notes`

## Тести (JUnit 5)

≥ 59 тестів у `server/src/test`:

- формула I (ваги, clamp, округлення);
- матриця переходів ЖЦ;
- валідація всіх Service.

Запуск: `cd server && mvn test`

## Переносимість

- немає `C:\Users\...` / абсолютних шляхів;
- DB і API URL — через `DB_*` / `API_BASE_URL`;
- `target/`, `.idea/`, секрети — у `.gitignore`.
