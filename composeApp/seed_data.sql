-- Insert Subscribers
INSERT INTO Subscriber(fullName, phone, meterNumber, address, zoneId, isActive, createdAt) 
VALUES ('Ahmed Test 1', '0600000001', 'TEST001', 'Address 1', 1, 1, 1730419200000);

INSERT INTO Subscriber(fullName, phone, meterNumber, address, zoneId, isActive, createdAt) 
VALUES ('Fatima Test 2', '0600000002', 'TEST002', 'Address 2', 1, 1, 1730419200000);

INSERT INTO Subscriber(fullName, phone, meterNumber, address, zoneId, isActive, createdAt) 
VALUES ('Hassan Test 3', '0600000003', 'TEST003', 'Address 3', 1, 1, 1730419200000);

-- Insert Invoices for Nov 2025 (Date: 01/11/2025 = 1730419200000 ms)
-- Due Date: 30 days later = 1733011200000

-- Sub 1: Prev 0, Curr 100
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES (
    (SELECT id FROM Subscriber WHERE meterNumber = 'TEST001'),
    0, 100, 100, 
    (100 * 5) + 0, -- Assuming price is 5 approx (recalc will happen if edited, but this is seed)
    'PAID', 
    1730419200000, 
    1733011200000, 
    0
);

-- Sub 2: Prev 50, Curr 120
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES (
    (SELECT id FROM Subscriber WHERE meterNumber = 'TEST002'),
    50, 120, 70, 
    (70 * 5) + 0,
    'PAID', 
    1730419200000, 
    1733011200000, 
    0
);

-- Sub 3: Prev 100, Curr 110
INSERT INTO Invoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate, isPenaltyApplied)
VALUES (
    (SELECT id FROM Subscriber WHERE meterNumber = 'TEST003'),
    100, 110, 10, 
    (10 * 5) + 0, 
    'UNPAID', 
    1730419200000, 
    1733011200000, 
    0
);
