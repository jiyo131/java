-- ============================================================
-- 吞噬星空 · 用户表 SQL
-- 数据库：swallowed_star
-- 说明：请在本机 MySQL（root / 123456）中执行本文件。
-- ============================================================

-- 1) 若数据库尚未创建，先执行下面两行（已加 IF NOT EXISTS，可重复执行）：
CREATE DATABASE IF NOT EXISTS swallowed_star
  CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 2) 切换到目标库
USE swallowed_star;

-- 3) 创建星域用户表（幂等）
CREATE TABLE IF NOT EXISTS star_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username    VARCHAR(32)  NOT NULL                COMMENT '账号',
  password    VARCHAR(128) NOT NULL                COMMENT '密码（BCrypt 加密存储）',
  nickname    VARCHAR(64)  DEFAULT NULL            COMMENT '昵称',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='星域用户表';

-- 4) 演示账号说明：
--    演示账号 admin / 123456 由后端在「首次启动」时自动创建（密码经 BCrypt 加密），
--    无需手动插入。如需手动插入，请使用 BCrypt 加密后的密文，例如：
--    INSERT INTO star_user (username, password, nickname)
--    VALUES ('admin', '$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', '星河守护者');
