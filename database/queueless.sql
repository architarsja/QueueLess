CREATE DATABASE IF NOT EXISTS queueless CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE queueless;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(160) NOT NULL UNIQUE,
  phone VARCHAR(20),
  password VARCHAR(255) NOT NULL,
  role ENUM('ADMIN','STAFF') NOT NULL DEFAULT 'STAFF',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS services (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  service_name VARCHAR(120) NOT NULL UNIQUE,
  description VARCHAR(500),
  prefix VARCHAR(5) NOT NULL,
  average_service_time INT NOT NULL DEFAULT 5,
  status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_service_prefix (prefix)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS queue_counters (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  service_id BIGINT NOT NULL UNIQUE,
  last_token_number INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_counter_service FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS queue_entries (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token_number VARCHAR(20) NOT NULL,
  token_sequence INT NOT NULL,
  customer_name VARCHAR(120) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  service_id BIGINT NOT NULL,
  registration_type ENUM('ONLINE','PHYSICAL') NOT NULL,
  status ENUM('WAITING','SERVING','COMPLETED','SKIPPED','CANCELLED') NOT NULL DEFAULT 'WAITING',
  registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  called_at TIMESTAMP NULL,
  completed_at TIMESTAMP NULL,
  UNIQUE KEY uq_service_sequence (service_id, token_sequence),
  INDEX idx_queue_service_status_time (service_id, status, registered_at, id),
  INDEX idx_queue_token (token_number),
  CONSTRAINT fk_queue_service FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- The database intentionally contains no customers, services, queue entries, or counters.
-- Create the first administrator/staff account using the setup instructions in README.md.
