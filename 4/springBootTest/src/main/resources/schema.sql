-- ============================================================================
-- 涂山人物志 · 首页数据结构
-- 启动时自动执行（幂等：表已存在则跳过，数据由 data.sql 用 INSERT IGNORE 补齐）
--
--   star_category    多级分类（parent_id 自关联，支持任意层级）
--   star_entry       首页档案条目（挂在叶子分类上，供展示 / 检索 / 详情）
--   star_search_log  检索日志（热门搜索词、搜索分析的数据来源）
--   star_user        登录用户（原有）
-- ============================================================================

-- 1) 多级分类 ---------------------------------------------------------------
-- level=1 为一级分类，level=2 为二级分类；parent_id=0 表示顶层。
-- 后端按 parent_id 递归组树，因此新增三级、四级分类无需改表结构。
CREATE TABLE IF NOT EXISTS star_category (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id   BIGINT       NOT NULL DEFAULT 0      COMMENT '父分类 id，0 表示一级分类',
  code        VARCHAR(48)  NOT NULL                COMMENT '分类编码（唯一，便于前端/脚本引用）',
  name        VARCHAR(48)  NOT NULL                COMMENT '分类名称',
  level       TINYINT      NOT NULL DEFAULT 1      COMMENT '层级：1 一级 / 2 二级 / …',
  icon        VARCHAR(16)  DEFAULT NULL            COMMENT '分类图标（emoji）',
  color       VARCHAR(16)  DEFAULT NULL            COMMENT '分类主色（#RRGGBB），驱动前端角标与卡片光效',
  description VARCHAR(255) DEFAULT NULL            COMMENT '分类说明',
  sort_no     INT          NOT NULL DEFAULT 0      COMMENT '同级排序（小值优先）',
  status      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 启用 / 0 停用',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code),
  KEY idx_parent (parent_id),
  KEY idx_level_sort (level, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='涂山档案分类表（多级）';

-- 2) 档案条目 ---------------------------------------------------------------
-- tags 用英文逗号分隔（如「呆萌,红线仙,续缘术」），检索时与 name/alias/intro 等字段一起做模糊匹配。
-- hot_score 为人工热度权重，view_count 为真实浏览计数，二者共同决定「热门推荐」排序。
CREATE TABLE IF NOT EXISTS star_entry (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  category_id BIGINT       NOT NULL                COMMENT '所属分类 id（一般挂在二级分类上）',
  name        VARCHAR(64)  NOT NULL                COMMENT '名称',
  alias       VARCHAR(128) DEFAULT NULL            COMMENT '称号 / 别名',
  glyph       VARCHAR(16)  DEFAULT NULL            COMMENT '卡片图标（emoji 或单字），avatar 为空时回退使用',
  avatar      VARCHAR(255) DEFAULT NULL            COMMENT '人物形象图（前端 /avatars/ 下的图片路径）',
  intro       TEXT                                 COMMENT '简介',
  quote       VARCHAR(255) DEFAULT NULL            COMMENT '经典台词',
  bond        VARCHAR(255) DEFAULT NULL            COMMENT '红线羁绊',
  faction     VARCHAR(64)  DEFAULT NULL            COMMENT '所属势力',
  tags        VARCHAR(255) DEFAULT NULL            COMMENT '关键词标签，英文逗号分隔',
  hot_score   INT          NOT NULL DEFAULT 0      COMMENT '热度权重（编辑推荐）',
  view_count  INT          NOT NULL DEFAULT 0      COMMENT '浏览量（详情页累加）',
  sort_no     INT          NOT NULL DEFAULT 0      COMMENT '默认排序（小值优先）',
  status      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 上架 / 0 下架',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_name (name),
  KEY idx_category (category_id, status, sort_no),
  KEY idx_hot (status, hot_score, view_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='涂山档案条目表';

-- 3) 检索日志 ---------------------------------------------------------------
-- 每次有关键词的检索写一条，用于「热门搜索」与后续搜索分析（失败不影响检索主流程）。
CREATE TABLE IF NOT EXISTS star_search_log (
  id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  keyword      VARCHAR(64) NOT NULL                COMMENT '检索词',
  user_id      BIGINT      DEFAULT NULL            COMMENT '检索用户 id（未登录为 NULL）',
  result_count INT         NOT NULL DEFAULT 0      COMMENT '命中条数（用于分析零结果词）',
  created_at   DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '检索时间',
  PRIMARY KEY (id),
  KEY idx_keyword (keyword),
  KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='涂山档案检索日志表';

-- 4) 用户表（原有，保持兼容） -------------------------------------------------
CREATE TABLE IF NOT EXISTS star_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username    VARCHAR(32)  NOT NULL                COMMENT '账号',
  password    VARCHAR(128) NOT NULL                COMMENT '密码（BCrypt 加密存储）',
  nickname    VARCHAR(64)  DEFAULT NULL            COMMENT '昵称',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='涂山红线用户表';

-- 5) 红线仙任务 -------------------------------------------------------------
-- 苦情树下的委托告示板：reward 为完成可得情绪值，difficulty 1-3（轻缘 / 深缘 / 宿缘）。
CREATE TABLE IF NOT EXISTS star_redline_task (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  code        VARCHAR(48)  NOT NULL                COMMENT '任务编码（唯一，便于脚本引用）',
  title       VARCHAR(64)  NOT NULL                COMMENT '任务名称',
  subtitle    VARCHAR(128) DEFAULT NULL            COMMENT '副标题 / 一句话说明',
  description VARCHAR(500) DEFAULT NULL            COMMENT '委托详情',
  client      VARCHAR(48)  DEFAULT NULL            COMMENT '委托方',
  glyph       VARCHAR(16)  DEFAULT NULL            COMMENT '任务图标（emoji）',
  color       VARCHAR(16)  DEFAULT NULL            COMMENT '任务主色（#RRGGBB）',
  difficulty  TINYINT      NOT NULL DEFAULT 1      COMMENT '难度：1 轻缘 / 2 深缘 / 3 宿缘',
  reward      INT          NOT NULL DEFAULT 10     COMMENT '完成可得情绪值',
  duration    VARCHAR(32)  DEFAULT NULL            COMMENT '时限描述',
  scene       VARCHAR(48)  DEFAULT NULL            COMMENT '委托地点',
  tags        VARCHAR(255) DEFAULT NULL            COMMENT '关键词标签，英文逗号分隔',
  sort_no     INT          NOT NULL DEFAULT 0      COMMENT '排序（小值优先）',
  status      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 可接取 / 0 下架',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_code (code),
  KEY idx_task_status (status, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='红线仙任务告示板';

-- 6) 任务接取记录 -----------------------------------------------------------
-- 一个用户对同一任务只有一条记录：status=1 进行中 / 2 已圆满。
-- 「取消接取」直接删除记录，因此可再次接取；已完成记录保留，不可回退。
CREATE TABLE IF NOT EXISTS star_task_record (
  id           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id      BIGINT   NOT NULL                COMMENT '接取人 id',
  task_id      BIGINT   NOT NULL                COMMENT '任务 id',
  status       TINYINT  NOT NULL DEFAULT 1      COMMENT '1 进行中 / 2 已圆满',
  accepted_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '接取时间',
  completed_at DATETIME DEFAULT NULL            COMMENT '完成时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_task (user_id, task_id),
  KEY idx_record_user (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='红线仙任务接取记录';

-- 7) 用户情绪值 -------------------------------------------------------------
-- 红线仙的情绪值：完成委托累积而来，用于体现「牵线越多，心动越深」的成长。
CREATE TABLE IF NOT EXISTS star_user_emotion (
  user_id         BIGINT   NOT NULL                COMMENT '用户 id（主键）',
  emotion         INT      NOT NULL DEFAULT 0      COMMENT '当前情绪值',
  total_gain      INT      NOT NULL DEFAULT 0      COMMENT '累计获得情绪值',
  completed_count INT      NOT NULL DEFAULT 0      COMMENT '累计完成委托数',
  updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='红线仙情绪值';
