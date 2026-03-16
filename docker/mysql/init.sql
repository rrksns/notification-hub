CREATE DATABASE IF NOT EXISTS user_service;
CREATE DATABASE IF NOT EXISTS notification_service;
CREATE DATABASE IF NOT EXISTS delivery_service;

GRANT ALL PRIVILEGES ON user_service.* TO 'nhub'@'%';
GRANT ALL PRIVILEGES ON notification_service.* TO 'nhub'@'%';
GRANT ALL PRIVILEGES ON delivery_service.* TO 'nhub'@'%';
FLUSH PRIVILEGES;
