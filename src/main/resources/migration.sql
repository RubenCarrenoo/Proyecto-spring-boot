-- =====================================================================
-- CoWorkHub - migration.sql
-- Script completo para preparar MySQL 8 SIN depender de Hibernate:
--   1) crea la base y el usuario coworkhub/coworkhub (solo desarrollo)
--   2) crea las 10 tablas con sus claves, indices y restricciones
--   3) carga los mismos datos de prueba que import.sql
-- Uso (como root):  mysql -u root -p < src/main/resources/migration.sql
-- Nota: la aplicacion usa ddl-auto=create + import.sql al arrancar; este
-- script sirve para revisar el modelo, cargar la base a mano o usarla con
-- ddl-auto=none/validate.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS coworkhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'coworkhub'@'%' IDENTIFIED BY 'coworkhub';
GRANT ALL PRIVILEGES ON coworkhub.* TO 'coworkhub'@'%';
FLUSH PRIVILEGES;

USE coworkhub;
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `booking_items`;
DROP TABLE IF EXISTS `bookings`;
DROP TABLE IF EXISTS `user_accounts`;
DROP TABLE IF EXISTS `members`;
DROP TABLE IF EXISTS `room_equipment`;
DROP TABLE IF EXISTS `rooms`;
DROP TABLE IF EXISTS `membership_plans`;
DROP TABLE IF EXISTS `additional_services`;
DROP TABLE IF EXISTS `equipment`;
DROP TABLE IF EXISTS `locations`;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- Esquema
-- ---------------------------------------------------------------------

CREATE TABLE `locations` (
  `closing_time` time NOT NULL,
  `opening_time` time NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `city` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `address` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `equipment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm6whtgwe6ya84f59coio1j1wh` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `additional_services` (
  `unit_price` decimal(12,2) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKf7cdh07s4bs1qavhcdtiftbv4` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `membership_plans` (
  `included_hours_per_month` int NOT NULL,
  `max_active_bookings` int NOT NULL,
  `overage_discount` decimal(5,4) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKnconflq2x1uvxvs7sc0mnmh4` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `rooms` (
  `active` bit(1) NOT NULL,
  `capacity` int NOT NULL,
  `hourly_rate` decimal(12,2) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `location_id` bigint NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` enum('DESK','MEETING_ROOM','PRIVATE_OFFICE') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_location_name` (`location_id`,`name`),
  CONSTRAINT `FKn44nnd0bt5f0gwkdtw5ub1esg` FOREIGN KEY (`location_id`) REFERENCES `locations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `room_equipment` (
  `equipment_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  PRIMARY KEY (`equipment_id`,`room_id`),
  KEY `FKiyhaivc7it7c8go6ursatnvte` (`room_id`),
  CONSTRAINT `FKiyhaivc7it7c8go6ursatnvte` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `FKrmdm2ihfq0rjieivqjmnty6hq` FOREIGN KEY (`equipment_id`) REFERENCES `equipment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL,
  `document_number` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `full_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('ACTIVE','SUSPENDED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKd5c97lgxv17hn0p5c4ttbxxqk` (`document_number`),
  UNIQUE KEY `UK9d30a9u1qpg8eou0otgkwrp5d` (`email`),
  KEY `FKjuwy40aicmdogitnkjj38s78k` (`plan_id`),
  CONSTRAINT `FKjuwy40aicmdogitnkjj38s78k` FOREIGN KEY (`plan_id`) REFERENCES `membership_plans` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint DEFAULT NULL,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('ADMIN','MEMBER','RECEPTIONIST') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKlxwlgwuy2yrbye2vgs9w9x7mr` (`username`),
  UNIQUE KEY `UKr3au8txwg06sajwqs7ccpmk6j` (`member_id`),
  CONSTRAINT `FKi5xgh0k33trcuvdqc0dhokec` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `bookings` (
  `attendees` int NOT NULL,
  `cancellation_fee` decimal(12,2) NOT NULL,
  `included_hours_applied` decimal(6,2) NOT NULL,
  `room_cost` decimal(12,2) NOT NULL,
  `services_cost` decimal(12,2) NOT NULL,
  `total_cost` decimal(12,2) NOT NULL,
  `end_time` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `start_time` datetime(6) NOT NULL,
  `status` enum('CANCELLED','COMPLETED','CONFIRMED','PENDING') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_booking_room_time` (`room_id`,`start_time`,`end_time`),
  KEY `idx_booking_member_time` (`member_id`,`start_time`),
  CONSTRAINT `FK2hu43b2w26um1gql30wbf0173` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`),
  CONSTRAINT `FKrgoycol97o21kpjodw1qox4nc` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `booking_items` (
  `applied_unit_price` decimal(12,2) NOT NULL,
  `quantity` int NOT NULL,
  `booking_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `service_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_booking_item_service` (`booking_id`,`service_id`),
  KEY `FKq2m1rsv5kb7464rmpliamu490` (`service_id`),
  CONSTRAINT `FKq2m1rsv5kb7464rmpliamu490` FOREIGN KEY (`service_id`) REFERENCES `additional_services` (`id`),
  CONSTRAINT `FKrw74irmyat5c39cnjkn02u99m` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Datos de prueba (copiados de import.sql)
-- ---------------------------------------------------------------------

-- =====================================================================================
-- CoWorkHub - Datos iniciales de prueba (Hibernate lo ejecuta con ddl-auto=create)
-- Regla: una sentencia por linea terminada en ';'. Se insertan primero las tablas
-- referenciadas y luego las que tienen claves foraneas.
-- Las fechas de las reservas son RELATIVAS (NOW()/CURDATE()) para que nunca caduquen.
-- =====================================================================================

-- ---------- Sedes (2) ----------
INSERT INTO locations (id, name, city, address, opening_time, closing_time) VALUES (1, 'CoWork Centro', 'Bucaramanga', 'Calle 35 # 19-41', '07:00:00', '20:00:00');
INSERT INTO locations (id, name, city, address, opening_time, closing_time) VALUES (2, 'CoWork Cabecera', 'Bucaramanga', 'Carrera 33 # 48-20', '08:00:00', '18:00:00');

-- ---------- Equipamiento (4) ----------
INSERT INTO equipment (id, name) VALUES (1, 'Proyector');
INSERT INTO equipment (id, name) VALUES (2, 'Pizarra');
INSERT INTO equipment (id, name) VALUES (3, 'Videoconferencia');
INSERT INTO equipment (id, name) VALUES (4, 'Televisor 55 pulgadas');

-- ---------- Salas (6, una inactiva) ----------
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (1, 'Sala Andes', 'MEETING_ROOM', 8, 50000.00, true, 1);
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (2, 'Sala Caribe', 'MEETING_ROOM', 12, 70000.00, true, 1);
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (3, 'Oficina Privada 101', 'PRIVATE_OFFICE', 4, 40000.00, true, 1);
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (4, 'Escritorio Flex 1', 'DESK', 1, 15000.00, true, 1);
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (5, 'Sala Pacifico', 'MEETING_ROOM', 6, 45000.00, true, 2);
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (6, 'Sala Amazonas', 'MEETING_ROOM', 10, 60000.00, false, 2);

-- ---------- Equipamiento por sala (tabla intermedia N:M) ----------
INSERT INTO room_equipment (room_id, equipment_id) VALUES (1, 1);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (1, 2);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (1, 3);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (2, 1);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (2, 2);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (2, 3);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (2, 4);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (3, 2);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (3, 4);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (5, 1);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (5, 2);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (6, 1);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (6, 4);

-- ---------- Servicios adicionales (3) ----------
INSERT INTO additional_services (id, name, unit_price) VALUES (1, 'Cafe ilimitado', 8000.00);
INSERT INTO additional_services (id, name, unit_price) VALUES (2, 'Impresiones (paquete 20 hojas)', 5000.00);
INSERT INTO additional_services (id, name, unit_price) VALUES (3, 'Refrigerio', 12000.00);

-- ---------- Planes de membresia (3) ----------
INSERT INTO membership_plans (id, name, included_hours_per_month, overage_discount, max_active_bookings) VALUES (1, 'Basico', 5, 0.0000, 2);
INSERT INTO membership_plans (id, name, included_hours_per_month, overage_discount, max_active_bookings) VALUES (2, 'Profesional', 20, 0.1500, 5);
INSERT INTO membership_plans (id, name, included_hours_per_month, overage_discount, max_active_bookings) VALUES (3, 'Empresarial', 60, 0.3000, 15);

-- ---------- Miembros (5, uno suspendido) ----------
INSERT INTO members (id, document_number, full_name, email, status, plan_id) VALUES (1, '1098000001', 'Ana Maria Gomez', 'ana.gomez@mail.com', 'ACTIVE', 2);
INSERT INTO members (id, document_number, full_name, email, status, plan_id) VALUES (2, '1098000002', 'Carlos Ruiz', 'carlos.ruiz@mail.com', 'ACTIVE', 1);
INSERT INTO members (id, document_number, full_name, email, status, plan_id) VALUES (3, '1098000003', 'Laura Perez', 'laura.perez@mail.com', 'ACTIVE', 3);
INSERT INTO members (id, document_number, full_name, email, status, plan_id) VALUES (4, '1098000004', 'Jorge Diaz', 'jorge.diaz@mail.com', 'SUSPENDED', 1);
INSERT INTO members (id, document_number, full_name, email, status, plan_id) VALUES (5, '1098000005', 'Sofia Torres', 'sofia.torres@mail.com', 'ACTIVE', 3);

-- ---------- Cuentas de usuario (4) - contrasenas BCrypt ----------
-- admin / Admin123*   recepcion / Recep123*   ana / Ana123*   carlos / Carlos123*
INSERT INTO user_accounts (id, username, password, role, member_id) VALUES (1, 'admin', '$2a$10$u3ZxP3mI3HOFJAPXfIV0KuTNVJIFYtkhjTv1he.x9fwZ8cmu7GQha', 'ADMIN', NULL);
INSERT INTO user_accounts (id, username, password, role, member_id) VALUES (2, 'recepcion', '$2a$10$8Jbh/U.HjKGUplSW9mwnqe4GApf5/n/KutKxSWEJ5EAd43q68u4ge', 'RECEPTIONIST', NULL);
INSERT INTO user_accounts (id, username, password, role, member_id) VALUES (3, 'ana', '$2a$10$2kv1t0ZlKVo1LkuKxDiN2.K7Wvtd5X0F5L9n4dRWgQnBUK0QXKab2', 'MEMBER', 1);
INSERT INTO user_accounts (id, username, password, role, member_id) VALUES (4, 'carlos', '$2a$10$ct22q0N1zqrjx89l1akIPuSNnF5psk8ntptyL.kZhGv094TsNALwi', 'MEMBER', 2);

-- ---------- Reservas (15) en distintos estados ----------
-- 1: ana, dentro de 30 h  -> escenario 5 (cancelar gratis)
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, included_hours_applied, room_id, member_id) VALUES (1, NOW() + INTERVAL 30 HOUR, NOW() + INTERVAL 31 HOUR, 2, 'PENDING', 0.00, 0.00, 0.00, 0.00, 1.00, 3, 1);
-- 2: ana, dentro de 5 h, 2 h excedentes -> escenario 6 (penalizacion 50 % = 34000)
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, included_hours_applied, room_id, member_id) VALUES (2, NOW() + INTERVAL 5 HOUR, NOW() + INTERVAL 7 HOUR, 3, 'CONFIRMED', 68000.00, 0.00, 68000.00, 0.00, 0.00, 3, 1);
-- 3: ana, dentro de 1 h -> escenario 7 (no se puede cancelar)
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, included_hours_applied, room_id, member_id) VALUES (3, NOW() + INTERVAL 1 HOUR, NOW() + INTERVAL 2 HOUR, 1, 'PENDING', 0.00, 0.00, 0.00, 0.00, 1.00, 4, 1);
-- 4: carlos, cancelada -> escenario 8 (confirmar una cancelada)
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, included_hours_applied, room_id, member_id) VALUES (4, TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '10:00:00'), TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '12:00:00'), 4, 'CANCELLED', 0.00, 0.00, 0.00, 0.00, 0.00, 1, 2);
-- 5: carlos, futura con servicios -> escenario 9 (ana no puede verla)
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, included_hours_applied, room_id, member_id) VALUES (5, TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '09:00:00'), TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '11:00:00'), 5, 'PENDING', 0.00, 16000.00, 16000.00, 0.00, 2.00, 1, 2);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, included_hours_applied, room_id, member_id) VALUES (6, TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '14:00:00'), TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '16:00:00'), 10, 'CONFIRMED', 0.00, 120000.00, 120000.00, 0.00, 2.00, 2, 3);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, included_hours_applied, room_id, member_id) VALUES (7, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '09:00:00'), TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '12:00:00'), 8, 'COMPLETED', 0.00, 0.00, 0.00, 0.00, 3.00, 2, 3);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, included_hours_applied, room_id, member_id) VALUES (8, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '10:00:00'), TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '12:00:00'), 4, 'COMPLETED', 0.00, 0.00, 0.00, 0.00, 2.00, 5, 5);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, included_hours_applied, room_id, member_id) VALUES (9, TIMESTAMP(CURDATE() + INTERVAL 4 DAY, '08:00:00'), TIMESTAMP(CURDATE() + INTERVAL 4 DAY, '10:00:00'), 5, 'PENDING', 0.00, 10000.00, 10000.00, 0.00, 2.00, 5, 5);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, included_hours_applied, room_id, member_id) VALUES (10, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '08:00:00'), TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '12:00:00'), 1, 'COMPLETED', 0.00, 0.00, 0.00, 0.00, 4.00, 4, 4);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, included_hours_applied, room_id, member_id) VALUES (11, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '15:00:00'), TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '16:00:00'), 2, 'CANCELLED', 0.00, 0.00, 0.00, 0.00, 1.00, 3, 2);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, included_hours_applied, room_id, member_id) VALUES (12, TIMESTAMP(CURDATE() + INTERVAL 7 DAY, '08:00:00'), TIMESTAMP(CURDATE() + INTERVAL 7 DAY, '10:00:00'), 6, 'CONFIRMED', 0.00, 0.00, 0.00, 0.00, 2.00, 1, 3);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, included_hours_applied, room_id, member_id) VALUES (13, TIMESTAMP(CURDATE() + INTERVAL 10 DAY, '13:00:00'), TIMESTAMP(CURDATE() + INTERVAL 10 DAY, '17:00:00'), 9, 'PENDING', 0.00, 0.00, 0.00, 0.00, 4.00, 2, 5);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, included_hours_applied, room_id, member_id) VALUES (14, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '16:00:00'), TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '18:00:00'), 4, 'COMPLETED', 0.00, 24000.00, 24000.00, 0.00, 2.00, 1, 1);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, included_hours_applied, room_id, member_id) VALUES (15, TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '11:00:00'), TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '12:30:00'), 3, 'PENDING', 0.00, 0.00, 0.00, 0.00, 1.50, 5, 3);

-- ---------- Servicios de las reservas (precio aplicado historico) ----------
INSERT INTO booking_items (id, quantity, applied_unit_price, booking_id, service_id) VALUES (1, 2, 8000.00, 5, 1);
INSERT INTO booking_items (id, quantity, applied_unit_price, booking_id, service_id) VALUES (2, 10, 12000.00, 6, 3);
INSERT INTO booking_items (id, quantity, applied_unit_price, booking_id, service_id) VALUES (3, 2, 5000.00, 9, 2);
INSERT INTO booking_items (id, quantity, applied_unit_price, booking_id, service_id) VALUES (4, 3, 8000.00, 14, 1);
