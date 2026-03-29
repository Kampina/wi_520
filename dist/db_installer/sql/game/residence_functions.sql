CREATE TABLE IF NOT EXISTS `residence_functions` (
  `residenceId` int(11) NOT NULL DEFAULT '0',
  `id` int(11) NOT NULL DEFAULT '0',
  `level` int(11) NOT NULL DEFAULT '0',
  `expiration` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`residenceId`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
