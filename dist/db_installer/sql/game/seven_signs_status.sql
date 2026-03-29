CREATE TABLE IF NOT EXISTS `seven_signs_status` (
  `id` INT NOT NULL DEFAULT 0,
  `current_cycle` INT NOT NULL DEFAULT 1,
  `active_period` INT NOT NULL DEFAULT 0,
  `date` INT NOT NULL DEFAULT 0,
  `previous_winner` INT NOT NULL DEFAULT 0,
  `dawn_stone_score` BIGINT NOT NULL DEFAULT 0,
  `dusk_stone_score` BIGINT NOT NULL DEFAULT 0,
  `avarice_score` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO `seven_signs_status` VALUES (0, 1, 0, 0, 0, 0, 0, 0);
