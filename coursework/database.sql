-- database.sql
-- Інформаційна система обліку дефектів будівельних об’єктів (Варіант 16)
-- MySQL 8.x / compatible

CREATE DATABASE IF NOT EXISTS building_defects
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE building_defects;

-- 1. Будівельний об’єкт
DROP TABLE IF EXISTS repair_task;
DROP TABLE IF EXISTS inspection;
DROP TABLE IF EXISTS defect;
DROP TABLE IF EXISTS building_object;

CREATE TABLE building_object (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200)  NOT NULL,
    address         VARCHAR(300)  NOT NULL,
    object_type     VARCHAR(100)  NOT NULL,   -- житловий, промисловий, громадський тощо
    year_built      INT,
    floors          INT,
    status          VARCHAR(50)   DEFAULT 'ACTIVE',
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Дефект (об’єкт життєвого циклу)
CREATE TABLE defect (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    building_object_id  INT           NOT NULL,
    title               VARCHAR(200)  NOT NULL,
    description         TEXT,
    severity            DECIMAL(5,2)  NOT NULL DEFAULT 0,   -- F1 0-100
    area_scale          DECIMAL(5,2)  NOT NULL DEFAULT 0,   -- F2 0-100 (нормалізована площа/масштаб)
    safety_impact       DECIMAL(5,2)  NOT NULL DEFAULT 0,   -- F3 0-100
    integral_score      DECIMAL(5,2)  GENERATED ALWAYS AS (0.40 * severity + 0.35 * area_scale + 0.25 * safety_impact) STORED,
    state               VARCHAR(30)   NOT NULL DEFAULT 'ЗАФІКСОВАНО',
    discovered_at       DATE,
    created_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_defect_building FOREIGN KEY (building_object_id) REFERENCES building_object(id) ON DELETE CASCADE,
    CONSTRAINT chk_state CHECK (state IN ('ЗАФІКСОВАНО','ПЕРЕВІРЕНО','РЕМОНТ_ПРИЗНАЧЕНО','ЗАКРИТО')),
    CONSTRAINT chk_severity CHECK (severity BETWEEN 0 AND 100),
    CONSTRAINT chk_area CHECK (area_scale BETWEEN 0 AND 100),
    CONSTRAINT chk_safety CHECK (safety_impact BETWEEN 0 AND 100)
) ENGINE=InnoDB;

-- 3. Обстеження (імпортується з CSV)
CREATE TABLE inspection (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    building_object_id  INT           NOT NULL,
    defect_id           INT           NULL,          -- може бути прив’язано до дефекту пізніше
    inspection_date     DATE          NOT NULL,
    inspector_name      VARCHAR(150)  NOT NULL,
    findings            TEXT,
    measured_value      DECIMAL(10,2),
    unit                VARCHAR(30),
    notes               VARCHAR(500),
    created_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_insp_building FOREIGN KEY (building_object_id) REFERENCES building_object(id) ON DELETE CASCADE,
    CONSTRAINT fk_insp_defect   FOREIGN KEY (defect_id) REFERENCES defect(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 4. Ремонтне завдання
CREATE TABLE repair_task (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    defect_id       INT           NOT NULL,
    title           VARCHAR(200)  NOT NULL,
    description     TEXT,
    assigned_to     VARCHAR(150),
    planned_date    DATE,
    completed_date  DATE,
    status          VARCHAR(50)   DEFAULT 'PLANNED',  -- PLANNED, IN_PROGRESS, DONE, CANCELLED
    cost_estimate   DECIMAL(12,2),
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_repair_defect FOREIGN KEY (defect_id) REFERENCES defect(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Демонстраційні дані
INSERT INTO building_object (name, address, object_type, year_built, floors, status) VALUES
('ЖК "Сонячний"', 'вул. Центральна, 15', 'Житловий', 2015, 9, 'ACTIVE'),
('Торговий центр "Мега"', 'пр. Миру, 42', 'Громадський', 2010, 4, 'ACTIVE'),
('Завод "Промтех"', 'вул. Індустріальна, 8', 'Промисловий', 1998, 3, 'ACTIVE'),
('Школа №12', 'вул. Шкільна, 3', 'Громадський', 1985, 3, 'ACTIVE'),
('Офісний центр "Бізнес"', 'вул. Лісова, 27', 'Офісний', 2018, 12, 'ACTIVE');

INSERT INTO defect (building_object_id, title, description, severity, area_scale, safety_impact, state, discovered_at) VALUES
(1, 'Тріщини в несучій стіні', 'Вертикальні тріщини на 3-му поверсі, ширина до 3 мм', 75.0, 40.0, 85.0, 'ЗАФІКСОВАНО', '2026-09-10'),
(1, 'Протікання покрівлі', 'Протікання в районі водостоку', 55.0, 25.0, 40.0, 'ПЕРЕВІРЕНО', '2026-09-15'),
(2, 'Руйнування фасадної плитки', 'Відшарування плитки на південному фасаді', 45.0, 60.0, 50.0, 'РЕМОНТ_ПРИЗНАЧЕНО', '2026-08-20'),
(3, 'Корозія металевих конструкцій', 'Значна корозія балок у цеху №2', 80.0, 70.0, 90.0, 'ЗАФІКСОВАНО', '2026-09-01'),
(4, 'Пошкодження підлоги в коридорі', 'Деформація покриття, небезпека для дітей', 35.0, 20.0, 65.0, 'ЗАКРИТО', '2026-07-12');

INSERT INTO inspection (building_object_id, defect_id, inspection_date, inspector_name, findings, measured_value, unit, notes) VALUES
(1, 1, '2026-09-12', 'Іваненко О.П.', 'Підтверджено наявність тріщин, потрібен моніторинг', 2.8, 'мм', 'Рекомендовано додаткове обстеження'),
(1, 2, '2026-09-18', 'Петренко М.С.', 'Виявлено місце протікання', 1.5, 'м²', NULL),
(2, 3, '2026-08-25', 'Коваленко А.В.', 'Площа пошкодження збільшилась', 45.0, 'м²', 'Потрібен терміновий ремонт'),
(3, 4, '2026-09-05', 'Сидоренко В.І.', 'Корозія критична', 3.2, 'мм', 'Товщина стінки зменшена'),
(4, NULL, '2026-09-20', 'Мельник Т.О.', 'Планове обстеження без критичних дефектів', NULL, NULL, 'Стан задовільний');

INSERT INTO repair_task (defect_id, title, description, assigned_to, planned_date, status, cost_estimate) VALUES
(3, 'Заміна фасадної плитки', 'Демонтаж і монтаж нової плитки на південному фасаді', 'Бригада "Фасад-Сервіс"', '2026-10-15', 'IN_PROGRESS', 125000.00),
(2, 'Ремонт покрівлі', 'Герметизація водостоку та заміна гідроізоляції', 'ТОВ "Покрівля+"', '2026-10-20', 'PLANNED', 45000.00),
(5, 'Відновлення підлогового покриття', 'Заміна деформованих ділянок', 'Шкільна господарська служба', '2026-08-01', 'DONE', 18000.00);

-- Індекси для швидкого пошуку
CREATE INDEX idx_defect_state ON defect(state);
CREATE INDEX idx_defect_building ON defect(building_object_id);
CREATE INDEX idx_inspection_date ON inspection(inspection_date);
CREATE INDEX idx_repair_defect ON repair_task(defect_id);
