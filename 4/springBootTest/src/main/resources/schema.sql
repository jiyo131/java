-- 自动建表（启动幂等，表已存在则跳过）
CREATE TABLE IF NOT EXISTS star_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username    VARCHAR(32)  NOT NULL                COMMENT '账号',
  password    VARCHAR(128) NOT NULL                COMMENT '密码（BCrypt 加密存储）',
  nickname    VARCHAR(64)  DEFAULT NULL            COMMENT '昵称',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='涂山红线用户表';
