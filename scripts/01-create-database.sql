-- =====================================================================
-- CoWorkHub - Creación de la base de datos y del usuario de la aplicación
-- Ejecutar UNA sola vez como root:
--   mysql -u root -p < scripts/01-create-database.sql
-- Las tablas NO se crean aquí: Hibernate las crea al iniciar
-- (spring.jpa.hibernate.ddl-auto=create) y luego ejecuta import.sql.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS coworkhub
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'coworkhub'@'localhost' IDENTIFIED BY 'coworkhub';
CREATE USER IF NOT EXISTS 'coworkhub'@'%' IDENTIFIED BY 'coworkhub';

GRANT ALL PRIVILEGES ON coworkhub.* TO 'coworkhub'@'localhost';
GRANT ALL PRIVILEGES ON coworkhub.* TO 'coworkhub'@'%';

FLUSH PRIVILEGES;

SELECT 'Base de datos coworkhub y usuario coworkhub listos' AS resultado;
