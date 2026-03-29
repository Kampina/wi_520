CREATE TABLE IF NOT EXISTS `fort_functions` (
  `fort_id` int(11) NOT NULL DEFAULT 0,
  `type` int(11) NOT NULL DEFAULT 0,
  `lvl` int(11) NOT NULL DEFAULT 0,
  `lease` int(11) NOT NULL DEFAULT 0,
  `rate` bigint(20) unsigned NOT NULL DEFAULT 0,
  `endTime` bigint(20) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`fort_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;