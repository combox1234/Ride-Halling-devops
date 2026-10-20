-- =============================================================================
-- Database Initialization Script for Ride-Hailing DevOps Platform
-- Executed on fresh MySQL container initialization (/docker-entrypoint-initdb.d)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `auth_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `passenger_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `driver_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `ride_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `pricing_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `billing_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `rating_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `reporting_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant all privileges to the application user
GRANT ALL PRIVILEGES ON `auth_db`.* TO 'appuser'@'%';
GRANT ALL PRIVILEGES ON `passenger_db`.* TO 'appuser'@'%';
GRANT ALL PRIVILEGES ON `driver_db`.* TO 'appuser'@'%';
GRANT ALL PRIVILEGES ON `ride_db`.* TO 'appuser'@'%';
GRANT ALL PRIVILEGES ON `pricing_db`.* TO 'appuser'@'%';
GRANT ALL PRIVILEGES ON `billing_db`.* TO 'appuser'@'%';
GRANT ALL PRIVILEGES ON `rating_db`.* TO 'appuser'@'%';
GRANT ALL PRIVILEGES ON `reporting_db`.* TO 'appuser'@'%';

FLUSH PRIVILEGES;
