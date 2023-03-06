DROP DATABASE IF EXISTS hospital;
CREATE DATABASE hospital;

-- 患者
CREATE TABLE hospital.patient (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '患者ID',
  `name` varchar(255) NOT NULL COMMENT '患者氏名',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登録日時',
  `created_user` int(11) NOT NULL DEFAULT 0 COMMENT '登録者',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  `updated_user` int(11) NOT NULL DEFAULT 0 COMMENT '更新者',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 病室
CREATE TABLE hospital.room (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '病室ID',
  `number` varchar(255) NOT NULL COMMENT '病室番号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登録日時',
  `created_user` int(11) NOT NULL DEFAULT 0 COMMENT '登録者',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  `updated_user` int(11) NOT NULL DEFAULT 0 COMMENT '更新者',
  PRIMARY KEY (`id`),
  UNIQUE KEY `room_unique_key` (`number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 入院
CREATE TABLE hospital.admission (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `patient_id` int(11) NOT NULL COMMENT '患者ID',
  `room_id` int(11) DEFAULT NULL COMMENT '病室ID (移動済みの可能性あり)',
  `start_date` date NOT NULL COMMENT '入院日',
  `end_date` date DEFAULT NULL COMMENT '退院日',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登録日時',
  `created_user` int(11) NOT NULL DEFAULT 0 COMMENT '登録者',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  `updated_user` int(11) NOT NULL DEFAULT 0 COMMENT '更新者',
  PRIMARY KEY (`id`),
  UNIQUE KEY `admission_unique_key` (`patient_id`, `start_date`),
  KEY `admission_patient_idx` (`patient_id`),
  KEY `admission_room_idx` (`room_id`),
  CONSTRAINT `admission_patient_idx` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `admission_room_idx` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 外出
CREATE TABLE hospital.room_outside (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `admission_id` int(11) NOT NULL COMMENT '入院ID',
  `start_date` date NOT NULL COMMENT '外出開始日',
  `end_date` date NOT NULL COMMENT '外出終了日',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登録日時',
  `created_user` int(11) NOT NULL DEFAULT 0 COMMENT '登録者',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  `updated_user` int(11) NOT NULL DEFAULT 0 COMMENT '更新者',
  PRIMARY KEY (`id`),
  UNIQUE KEY `room_outside_unique_key` (`admission_id`, `start_date`),
  KEY `room_outside_admission_idx` (`admission_id`),
  CONSTRAINT `room_outside_admission_idx` FOREIGN KEY (`admission_id`) REFERENCES `admission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 移動
CREATE TABLE hospital.room_move (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `admission_id` int(11) NOT NULL COMMENT '入院ID',
  `from_room_id` int(11) NOT NULL COMMENT '移動元の病室ID',
  `to_room_id` int(11) NOT NULL COMMENT '移動先の病室ID',
  `move_date` date NOT NULL COMMENT '移動日',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登録日時',
  `created_user` int(11) NOT NULL DEFAULT 0 COMMENT '登録者',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  `updated_user` int(11) NOT NULL DEFAULT 0 COMMENT '更新者',
  PRIMARY KEY (`id`),
  UNIQUE KEY `room_move_unique_key` (`admission_id`, `from_room_id`, `move_date`),
  KEY `room_move_admission_idx` (`admission_id`),
  KEY `room_move_from_room_idx` (`from_room_id`),
  KEY `room_move_to_room_idx` (`to_room_id`),
  CONSTRAINT `room_move_admission_idx` FOREIGN KEY (`admission_id`) REFERENCES `admission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `room_move_from_room_idx` FOREIGN KEY (`from_room_id`) REFERENCES `room` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `room_move_to_room_idx` FOREIGN KEY (`to_room_id`) REFERENCES `room` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
