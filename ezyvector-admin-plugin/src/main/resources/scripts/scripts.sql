/*
 * Copyright 2026 youngmonkeys.org
 *
 * Licensed under the ezyplatform, Version 1.0.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://youngmonkeys.org/licenses/ezyplatform-1.0.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

CREATE TABLE IF NOT EXISTS `ezyvector_collections` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `name` varchar(120) NOT NULL,
    `vector_size` bigint unsigned NOT NULL,
    `distance` varchar(50) NOT NULL,
    `index_type` varchar(50) NOT NULL DEFAULT 'HNSW',
    `status` varchar(50) NOT NULL DEFAULT 'ACTIVE',
    `points_count` bigint unsigned NOT NULL DEFAULT 0,
    `config` text COLLATE utf8mb4_unicode_520_ci,
    `created_at` datetime NOT NULL,
    `updated_at` datetime NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `key_name` (`name`),
    INDEX `index_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;

CREATE TABLE IF NOT EXISTS `ezyvector_collection_points` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `collection_id` bigint unsigned NOT NULL,
    `point_id` bigint unsigned NOT NULL,
    `vector` mediumblob NOT NULL,
    `payload` mediumtext COLLATE utf8mb4_unicode_520_ci,
    `status` varchar(50) NOT NULL,
    `version` bigint unsigned NOT NULL DEFAULT 1,
    `created_at` datetime NOT NULL,
    `updated_at` datetime NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `key_collection_point` (`collection_id`, `point_id`),
    INDEX `index_collection_status` (`collection_id`, `status`),
    INDEX `index_updated_at` (`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;

CREATE TABLE IF NOT EXISTS `ezyvector_collection_segments` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `collection_id` bigint unsigned NOT NULL,
    `segment_no` bigint unsigned NOT NULL,
    `segment_type` varchar(50) NOT NULL,
    `status` varchar(50) NOT NULL,
    `points_count` bigint unsigned NOT NULL DEFAULT 0,
    `min_point_id` bigint unsigned,
    `max_point_id` bigint unsigned,
    `index_version` bigint unsigned NOT NULL DEFAULT 1,
    `created_at` datetime NOT NULL,
    `updated_at` datetime NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `key_collection_segment` (`collection_id`, `segment_no`),
    INDEX `index_collection_status` (`collection_id`, `status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
