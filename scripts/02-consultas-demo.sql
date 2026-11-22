-- =====================================================================
-- CoWorkHub - Consultas para la demostración en MySQL Workbench
-- Ejecute cada bloque con Ctrl+Enter (sentencia actual) o Ctrl+Shift+Enter (todo).
-- =====================================================================
USE coworkhub;

-- 1. Tablas creadas por Hibernate (ddl-auto=create)
SHOW TABLES;

-- 2. Cantidad de registros cargados por import.sql
SELECT 'locations' AS tabla, COUNT(*) AS registros FROM locations
UNION ALL SELECT 'rooms', COUNT(*) FROM rooms
UNION ALL SELECT 'equipment', COUNT(*) FROM equipment
UNION ALL SELECT 'room_equipment', COUNT(*) FROM room_equipment
UNION ALL SELECT 'additional_services', COUNT(*) FROM additional_services
UNION ALL SELECT 'membership_plans', COUNT(*) FROM membership_plans
UNION ALL SELECT 'members', COUNT(*) FROM members
UNION ALL SELECT 'user_accounts', COUNT(*) FROM user_accounts
UNION ALL SELECT 'bookings', COUNT(*) FROM bookings
UNION ALL SELECT 'booking_items', COUNT(*) FROM booking_items;

-- 3. Sedes y sus salas (Location 1:N Room), con equipamiento (Room N:M Equipment)
SELECT l.name AS sede, r.name AS sala, r.type AS tipo, r.capacity AS capacidad,
       r.hourly_rate AS tarifa_hora, r.active AS activa,
       GROUP_CONCAT(e.name ORDER BY e.id SEPARATOR ', ') AS equipamiento
FROM rooms r
JOIN locations l ON l.id = r.location_id
LEFT JOIN room_equipment re ON re.room_id = r.id
LEFT JOIN equipment e ON e.id = re.equipment_id
GROUP BY r.id
ORDER BY r.id;

-- 4. Miembros, plan y cuenta de usuario (Member N:1 Plan, Member 1:1 UserAccount opcional)
SELECT m.id, m.full_name AS miembro, m.status AS estado, p.name AS plan,
       p.included_hours_per_month AS horas_mes, p.max_active_bookings AS max_reservas,
       u.username AS usuario, u.role AS rol
FROM members m
JOIN membership_plans p ON p.id = m.plan_id
LEFT JOIN user_accounts u ON u.member_id = m.id
ORDER BY m.id;

-- 5. Cuentas: contraseñas guardadas con BCrypt (nunca en texto plano)
SELECT id, username, role, member_id, LEFT(password, 20) AS hash_bcrypt FROM user_accounts;

-- 6. Reservas con sala, miembro, estado y costos
SELECT b.id, m.full_name AS miembro, r.name AS sala, b.start_time AS inicio, b.end_time AS fin,
       b.status AS estado, b.included_hours_applied AS horas_plan, b.room_cost AS costo_sala,
       b.services_cost AS costo_servicios, b.total_cost AS total, b.cancellation_fee AS penalizacion
FROM bookings b
JOIN members m ON m.id = b.member_id
JOIN rooms r ON r.id = b.room_id
ORDER BY b.start_time;

-- 7. Reservas por estado
SELECT status AS estado, COUNT(*) AS cantidad FROM bookings GROUP BY status;

-- 8. Servicios de cada reserva con precio histórico aplicado (Booking 1:N BookingItem N:1 AdditionalService)
SELECT bi.booking_id AS reserva, s.name AS servicio, bi.quantity AS cantidad,
       bi.applied_unit_price AS precio_aplicado, s.unit_price AS precio_actual,
       bi.quantity * bi.applied_unit_price AS subtotal
FROM booking_items bi
JOIN additional_services s ON s.id = bi.service_id
ORDER BY bi.booking_id;

-- 9. Restricciones de unicidad (RN-11) definidas en la base de datos
SELECT TABLE_NAME AS tabla, INDEX_NAME AS indice, GROUP_CONCAT(COLUMN_NAME) AS columnas
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'coworkhub' AND NON_UNIQUE = 0 AND INDEX_NAME <> 'PRIMARY'
GROUP BY TABLE_NAME, INDEX_NAME;

-- 10. Claves foráneas (relaciones JPA)
SELECT TABLE_NAME AS tabla, COLUMN_NAME AS columna, REFERENCED_TABLE_NAME AS referencia
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'coworkhub' AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME;
