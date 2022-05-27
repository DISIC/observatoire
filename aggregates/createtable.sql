CREATE TABLE `avis_byday` (
  `day` date NOT NULL,
  `demarche` varchar(255) COLLATE utf8_bin NOT NULL DEFAULT '',
  `count` int(11) DEFAULT NULL,
  `avis3` int(11) DEFAULT NULL,
  `avis2` int(11) DEFAULT NULL,
  `avis1` int(11) DEFAULT NULL,
  `facile3` int(11) DEFAULT NULL,
  `facile2` int(11) DEFAULT NULL,
  `facile1` int(11) DEFAULT NULL,
  `compr3` int(11) DEFAULT NULL,
  `compr2` int(11) DEFAULT NULL,
  `compr1` int(11) DEFAULT NULL,
  `diff_manquedinformations` int(11) DEFAULT NULL,
  `diff_dysfonctionnement` int(11) DEFAULT NULL,
  `diff_mobile` int(11) DEFAULT NULL,
  `diff_piecesjointes` int(11) DEFAULT NULL,
  `diff_suite` int(11) DEFAULT NULL,
  `diff_autre` int(11) DEFAULT NULL,
  `diff_aucune` int(11) DEFAULT NULL,
  `aide_proche` int(11) DEFAULT NULL,
  `aide_association` int(11) DEFAULT NULL,
  `aide_agent` int(11) DEFAULT NULL,
  `aide_internet` int(11) DEFAULT NULL,
  `aide_autre` int(11) DEFAULT NULL,
  `aide_aucune` int(11) DEFAULT NULL,
  `modalite_enligneentierement` int(11) DEFAULT NULL,
  `modalite_enlignepartielement` int(11) DEFAULT NULL,
  `modalite_autrement` int(11) DEFAULT NULL,
  `commentaire` int(11) DEFAULT NULL,
  PRIMARY KEY (`day`,`demarche`),
  KEY `avis_byday_day` (`day`),
  KEY `avis_byday_demarche` (`demarche`),
  KEY `avis_byday_day_demarche` (`day`,`demarche`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin
