CREATE TABLE IF NOT EXISTS `seven_signs` (
  `char_obj_id` INT NOT NULL DEFAULT 0,
  `cabal` ENUM('dawn','dusk','') NOT NULL DEFAULT '',
  `seal` TINYINT NOT NULL DEFAULT 0,
  `dawn_red_stones` INT UNSIGNED NOT NULL DEFAULT 0,
  `dawn_green_stones` INT UNSIGNED NOT NULL DEFAULT 0,
  `dawn_blue_stones` INT UNSIGNED NOT NULL DEFAULT 0,
  `dawn_ancient_adena_amount` INT UNSIGNED NOT NULL DEFAULT 0,
  `dawn_contribution_score` INT UNSIGNED NOT NULL DEFAULT 0,
  `dusk_red_stones` INT UNSIGNED NOT NULL DEFAULT 0,
  `dusk_green_stones` INT UNSIGNED NOT NULL DEFAULT 0,
  `dusk_blue_stones` INT UNSIGNED NOT NULL DEFAULT 0,
  `dusk_ancient_adena_amount` INT UNSIGNED NOT NULL DEFAULT 0,
  `dusk_contribution_score` INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_obj_id`)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
