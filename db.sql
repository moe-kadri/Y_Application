-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
-- -----------------------------------------------------
-- Schema y
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema y
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `y` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci ;
USE `y` ;

-- -----------------------------------------------------
-- Table `y`.`users`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `y`.`users` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(100) NOT NULL,
  `name` VARCHAR(100) NULL DEFAULT NULL,
  `email` VARCHAR(100) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `unique_username` (`username` ASC) VISIBLE)
ENGINE = InnoDB
AUTO_INCREMENT = 2
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `y`.`followers`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `y`.`followers` (
  `follower_id` INT NOT NULL,
  `followed_id` INT NOT NULL,
  PRIMARY KEY (`follower_id`, `followed_id`),
  INDEX `followed_id` (`followed_id` ASC) VISIBLE,
  CONSTRAINT `followers_ibfk_1`
    FOREIGN KEY (`follower_id`)
    REFERENCES `y`.`users` (`id`),
  CONSTRAINT `followers_ibfk_2`
    FOREIGN KEY (`followed_id`)
    REFERENCES `y`.`users` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `y`.`messages`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `y`.`messages` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NULL DEFAULT NULL,
  `content` TEXT NULL DEFAULT NULL,
  `posted_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `user_id` (`user_id` ASC) VISIBLE,
  CONSTRAINT `messages_ibfk_1`
    FOREIGN KEY (`user_id`)
    REFERENCES `y`.`users` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 4
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

SELECT m.content, u.username 
FROM messages m 
JOIN users u ON m.user_id = u.id;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

// commands:
javac -cp "Connector J 8.0\mysql-connector-j-8.0.33.jar" *.java 
java -cp ".;Connector J 8.0\mysql-connector-j-8.0.33.jar" YServer 56300
java -cp ".;Connector J 8.0\mysql-connector-j-8.0.33.jar" YClient