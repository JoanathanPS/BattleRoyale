-- Battle Royale Arena - Database Schema
-- Run this file to create the required table: mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS battle_royale_db;
USE battle_royale_db;

CREATE TABLE IF NOT EXISTS match_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player_name VARCHAR(50) NOT NULL,
    difficulty VARCHAR(10) NOT NULL,
    score INT NOT NULL,
    survival_time INT NOT NULL,
    result VARCHAR(10) NOT NULL,
    date_played TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
