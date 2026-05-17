-- ============================================================
-- SEED DATA: 10 Subscribers + Invoices (subscription readings)
-- Price per unit: 5.00 Dhs (flat rate)
-- Month 1: 01/02/2026 = 1769900400000 ms | Due: +30 days = 1772492400000 ms
-- Month 2: 01/03/2026 = 1772319600000 ms | Due: +30 days = 1774911600000 ms
-- Month 3: 01/04/2026 = 1774998000000 ms | Due: +30 days = 1777590000000 ms
-- Current month (May 2026) has NO invoices — empty current reading
-- ============================================================

-- ── Schema ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Zone (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS Subscriber (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    fullName TEXT NOT NULL,
    phone TEXT,
    meterNumber TEXT NOT NULL,
    address TEXT,
    zoneId INTEGER NOT NULL,
    isActive INTEGER NOT NULL DEFAULT 1,
    createdAt INTEGER NOT NULL,
    FOREIGN KEY (zoneId) REFERENCES Zone(id)
);

CREATE TABLE IF NOT EXISTS PricingTier (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    minUsage INTEGER NOT NULL,
    maxUsage INTEGER NOT NULL,
    pricePerUnit REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS Invoice (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    subscriberId INTEGER NOT NULL,
    previousReading INTEGER NOT NULL,
    currentReading INTEGER NOT NULL,
    consumption INTEGER NOT NULL,
    totalAmount REAL NOT NULL,
    status TEXT NOT NULL,
    issueDate INTEGER NOT NULL,
    dueDate INTEGER NOT NULL,
    isPenaltyApplied INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (subscriberId) REFERENCES Subscriber(id)
);

CREATE TABLE IF NOT EXISTS TransactionTable (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL,
    category TEXT NOT NULL,
    amount REAL NOT NULL,
    description TEXT,
    date INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS MaintenanceTicket (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    subscriberId INTEGER,
    issueType TEXT NOT NULL,
    description TEXT,
    status TEXT NOT NULL,
    reportedDate INTEGER NOT NULL,
    FOREIGN KEY (subscriberId) REFERENCES Subscriber(id)
);

CREATE TABLE IF NOT EXISTS AppSettings (
    id INTEGER NOT NULL PRIMARY KEY DEFAULT 1,
    lateFeeAmount REAL NOT NULL DEFAULT 5.0,
    monthlyFixedFee REAL NOT NULL DEFAULT 0.0,
    gracePeriodDays INTEGER NOT NULL DEFAULT 15,
    dueDateDays INTEGER NOT NULL DEFAULT 30,
    associationName TEXT NOT NULL DEFAULT 'Water Association',
    associationAddress TEXT NOT NULL DEFAULT '',
    associationPhone TEXT NOT NULL DEFAULT '',
    printFormat TEXT NOT NULL DEFAULT 'A4',
    logoPath TEXT DEFAULT NULL
);

-- ── Data ────────────────────────────────────────────────────

-- Ensure Zones exist
INSERT OR IGNORE INTO Zone(id, name, description) VALUES (1, 'Centre', 'Zone Centre Village');
INSERT OR IGNORE INTO Zone(id, name, description) VALUES (2, 'Nord', 'Zone Nord Douar');
INSERT OR IGNORE INTO Zone(id, name, description) VALUES (3, 'Sud', 'Zone Sud Douar');

-- ============================================================
-- 10 SUBSCRIBERS
-- ============================================================
INSERT INTO Subscriber(fullName, phone, meterNumber, address, zoneId, isActive, createdAt) VALUES ('Ahmed Benali', '0600000001', 'MTR001', '12 Rue Hassan II, Centre', 1, 1, 1730419200000);
INSERT INTO Subscriber(fullName, phone, meterNumber, address, zoneId, isActive, createdAt) VALUES ('Fatima Zahra Amrani', '0600000002', 'MTR002', '5 Av Mohammed V, Centre', 1, 1, 1730419200000);
INSERT INTO Subscriber(fullName, phone, meterNumber, address, zoneId, isActive, createdAt) VALUES ('Hassan Tazi', '0600000003', 'MTR003', '8 Rue Liberté, Nord', 2, 1, 1730419200000);
INSERT INTO Subscriber(fullName, phone, meterNumber, address, zoneId, isActive, createdAt) VALUES ('Khadija El Fassi', '0600000004', 'MTR004', '3 Imparc Oasis, Nord', 2, 1, 1730419200000);
INSERT INTO Subscriber(fullName, phone, meterNumber, address, zoneId, isActive, createdAt) VALUES ('Youssef Berrada', '0600000005', 'MTR005', '15 Bd Atlas, Centre', 1, 1, 1730419200000);
INSERT INTO Subscriber(fullName, phone, meterNumber, address, zoneId, isActive, createdAt) VALUES ('Amina Chraibi', '0600000006', 'MTR006', '7 Rue Palmier, Sud', 3, 1, 1730419200000);
INSERT INTO Subscriber(fullName, phone, meterNumber, address, zoneId, isActive, createdAt) VALUES ('Omar Sekkal', '0600000007', 'MTR007', '22 Av Irrigation, Sud', 3, 1, 1730419200000);
INSERT INTO Subscriber(fullName, phone, meterNumber, address, zoneId, isActive, createdAt) VALUES ('Rachida Ouazzani', '0600000008', 'MTR008', '9 Rue Source, Nord', 2, 1, 1730419200000);
INSERT INTO Subscriber(fullName, phone, meterNumber, address, zoneId, isActive, createdAt) VALUES ('Mohamed Alaoui', '0600000009', 'MTR009', '18 Rue Puits, Centre', 1, 1, 1730419200000);
INSERT INTO Subscriber(fullName, phone, meterNumber, address, zoneId, isActive, createdAt) VALUES ('Zineb Kettani', '0600000010', 'MTR010', '4 Av Cascade, Sud', 3, 0, 1730419200000); -- inactive

-- ============================================================
-- INVOICES (subscription readings) — 3 months per subscriber
-- ============================================================

-- ── Month 1: February 2026 ──────────────────────────────────

-- Sub 1: Ahmed — 0→80 = 80 units = 400 Dhs (PAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR001'), 0, 80, 80, 400.0, 'PAID', 1769900400000, 1772492400000, 0);

-- Sub 2: Fatima — 0→55 = 55 units = 275 Dhs (PAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR002'), 0, 55, 55, 275.0, 'PAID', 1769900400000, 1772492400000, 0);

-- Sub 3: Hassan — 0→120 = 120 units = 600 Dhs (PAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR003'), 0, 120, 120, 600.0, 'PAID', 1769900400000, 1772492400000, 0);

-- Sub 4: Khadija — 0→40 = 40 units = 200 Dhs (PAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR004'), 0, 40, 40, 200.0, 'PAID', 1769900400000, 1772492400000, 0);

-- Sub 5: Youssef — 0→95 = 95 units = 475 Dhs (UNPAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR005'), 0, 95, 95, 475.0, 'UNPAID', 1769900400000, 1772492400000, 0);

-- Sub 6: Amina — 0→30 = 30 units = 150 Dhs (PAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR006'), 0, 30, 30, 150.0, 'PAID', 1769900400000, 1772492400000, 0);

-- Sub 7: Omar — 0→110 = 110 units = 550 Dhs (UNPAID + penalty)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR007'), 0, 110, 110, 555.0, 'UNPAID', 1769900400000, 1772492400000, 1);

-- Sub 8: Rachida — 0→65 = 65 units = 325 Dhs (PAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR008'), 0, 65, 65, 325.0, 'PAID', 1769900400000, 1772492400000, 0);

-- Sub 9: Mohamed — 0→70 = 70 units = 350 Dhs (PAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR009'), 0, 70, 70, 350.0, 'PAID', 1769900400000, 1772492400000, 0);

-- Sub 10: Zineb — 0→45 = 45 units = 225 Dhs (UNPAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR010'), 0, 45, 45, 225.0, 'UNPAID', 1769900400000, 1772492400000, 0);

-- ── Month 2: March 2026 ──────────────────────────────────

-- Sub 1: Ahmed — 80→160 = 80 units = 400 Dhs (PAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR001'), 80, 160, 80, 400.0, 'PAID', 1772319600000, 1774911600000, 0);

-- Sub 2: Fatima — 55→100 = 45 units = 225 Dhs (PAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR002'), 55, 100, 45, 225.0, 'PAID', 1772319600000, 1774911600000, 0);

-- Sub 3: Hassan — 120→210 = 90 units = 450 Dhs (PAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR003'), 120, 210, 90, 450.0, 'PAID', 1772319600000, 1774911600000, 0);

-- Sub 4: Khadija — 40→75 = 35 units = 175 Dhs (UNPAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR004'), 40, 75, 35, 175.0, 'UNPAID', 1772319600000, 1774911600000, 0);

-- Sub 5: Youssef — 95→190 = 95 units = 475 Dhs (UNPAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR005'), 95, 190, 95, 475.0, 'UNPAID', 1772319600000, 1774911600000, 0);

-- Sub 6: Amina — 30→60 = 30 units = 150 Dhs (PAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR006'), 30, 60, 30, 150.0, 'PAID', 1772319600000, 1774911600000, 0);

-- Sub 7: Omar — 110→200 = 90 units = 450 Dhs (UNPAID + penalty)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR007'), 110, 200, 90, 455.0, 'UNPAID', 1772319600000, 1774911600000, 1);

-- Sub 8: Rachida — 65→120 = 55 units = 275 Dhs (PAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR008'), 65, 120, 55, 275.0, 'PAID', 1772319600000, 1774911600000, 0);

-- Sub 9: Mohamed — 70→150 = 80 units = 400 Dhs (PAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR009'), 70, 150, 80, 400.0, 'PAID', 1772319600000, 1774911600000, 0);

-- Sub 10: Zineb — 45→90 = 45 units = 225 Dhs (UNPAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR010'), 45, 90, 45, 225.0, 'UNPAID', 1772319600000, 1774911600000, 0);

-- ── Month 3: April 2026 ──────────────────────────────────

-- Sub 1: Ahmed — 160→250 = 90 units = 450 Dhs (UNPAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR001'), 160, 250, 90, 450.0, 'UNPAID', 1774998000000, 1777590000000, 0);

-- Sub 2: Fatima — 100→140 = 40 units = 200 Dhs (PAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR002'), 100, 140, 40, 200.0, 'PAID', 1774998000000, 1777590000000, 0);

-- Sub 3: Hassan — 210→280 = 70 units = 350 Dhs (PAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR003'), 210, 280, 70, 350.0, 'PAID', 1774998000000, 1777590000000, 0);

-- Sub 4: Khadija — 75→100 = 25 units = 125 Dhs (UNPAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR004'), 75, 100, 25, 125.0, 'UNPAID', 1774998000000, 1777590000000, 0);

-- Sub 5: Youssef — 190→300 = 110 units = 550 Dhs (UNPAID + penalty)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR005'), 190, 300, 110, 555.0, 'UNPAID', 1774998000000, 1777590000000, 1);

-- Sub 6: Amina — 60→85 = 25 units = 125 Dhs (PAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR006'), 60, 85, 25, 125.0, 'PAID', 1774998000000, 1777590000000, 0);

-- Sub 7: Omar — 200→260 = 60 units = 300 Dhs (UNPAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR007'), 200, 260, 60, 300.0, 'UNPAID', 1774998000000, 1777590000000, 0);

-- Sub 8: Rachida — 120→175 = 55 units = 275 Dhs (PAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR008'), 120, 175, 55, 275.0, 'PAID', 1774998000000, 1777590000000, 0);

-- Sub 9: Mohamed — 150→220 = 70 units = 350 Dhs (UNPAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR009'), 150, 220, 70, 350.0, 'UNPAID', 1774998000000, 1777590000000, 0);

-- Sub 10: Zineb — 90→130 = 40 units = 200 Dhs (UNPAID)
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES ((SELECT id FROM Subscriber WHERE meterNumber='MTR010'), 90, 130, 40, 200.0, 'UNPAID', 1774998000000, 1777590000000, 0);

-- ============================================================
-- PRICING TIERS (optional — flat 5 Dhs if not used)
-- ============================================================
INSERT OR IGNORE INTO PricingTier(id, minUsage, maxUsage, pricePerUnit) VALUES (1, 0, 5, 5.00);
INSERT OR IGNORE INTO PricingTier(id, minUsage, maxUsage, pricePerUnit) VALUES (2, 6, 15, 5.50);
INSERT OR IGNORE INTO PricingTier(id, minUsage, maxUsage, pricePerUnit) VALUES (3, 16, 999, 6.00);

-- ============================================================
-- APP SETTINGS (default row)
-- ============================================================
INSERT OR IGNORE INTO AppSettings(id, lateFeeAmount, monthlyFixedFee, gracePeriodDays, dueDateDays, associationName, associationAddress, associationPhone, printFormat, logoPath)
VALUES (1, 5.0, 0.0, 15, 30, 'Water Association', '', '', 'A4', NULL);
