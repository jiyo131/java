-- ============================================================================
-- 涂山人物志 · 独立交付版建库脚本（自动生成，勿手改）
-- 生成方式: springBootTest/build_page_init.py（拼接 schema.sql + data.sql）
-- 内容: CREATE DATABASE swallowed_star + 全部 7 张表 DDL + 全部种子数据
--       （4 分类 / 18 档案 / 10 委托）+ 演示账号 admin（明文密码，口令 123456，管理员权限）
-- 用法: mysql -uroot -p123456 --default-character-set=utf8mb4 < star_page_init.sql
-- 全幂等: CREATE TABLE IF NOT EXISTS + INSERT IGNORE，重复执行不产生重复数据。
-- 注意: 后端 schema.sql / data.sql 修改后，需重新运行生成脚本同步本文件。
-- ============================================================================

CREATE DATABASE IF NOT EXISTS swallowed_star DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE swallowed_star;

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
  password    VARCHAR(128) NOT NULL                COMMENT '密码（明文存储）',
  nickname    VARCHAR(64)  DEFAULT NULL            COMMENT '昵称',
  status      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 正常 / 0 停用（停用后无法登录）',
  permission  TINYINT      NOT NULL DEFAULT 1      COMMENT '权限：1 默认权限 / 0 管理员权限（0 才能看到并登录后台管理系统）',
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
  icon        VARCHAR(255) DEFAULT NULL            COMMENT '任务图标图片路径，icon 为空时回退 glyph',
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
  camp            TINYINT  NOT NULL DEFAULT 1      COMMENT '修炼阵营：1 修士 / 2 妖族',
  updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='红线仙情绪值';

-- 8) 兑换商店商品 -----------------------------------------------------------
-- 苦情树旁的小铺：法宝 / 丹药 / 功法三柜，以情绪值为通货。
-- price 为兑换所需情绪值；stock=-1 表示不限量，>=0 表示剩余库存（兑一次减一）。
CREATE TABLE IF NOT EXISTS star_shop_item (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  code        VARCHAR(48)  NOT NULL                COMMENT '商品编码（唯一，便于脚本引用）',
  category    VARCHAR(16)  NOT NULL                COMMENT '分类：fabao 法宝 / danyao 丹药 / gongfa 功法',
  name        VARCHAR(64)  NOT NULL                COMMENT '商品名称',
  subtitle    VARCHAR(128) DEFAULT NULL            COMMENT '副标题 / 一句话说明',
  description VARCHAR(500) DEFAULT NULL            COMMENT '商品详情',
  glyph       VARCHAR(16)  DEFAULT NULL            COMMENT '商品图标（emoji）',
  icon        VARCHAR(255) DEFAULT NULL            COMMENT '商品图标图片路径，icon 为空时回退 glyph',
  color       VARCHAR(16)  DEFAULT NULL            COMMENT '商品主色（#RRGGBB），驱动前端卡片光效',
  price       INT          NOT NULL DEFAULT 10     COMMENT '兑换所需情绪值',
  exp_gain    INT          NOT NULL DEFAULT 0      COMMENT '服用/参悟可得修为（0 表示不可服用，如法宝）',
  stock       INT          NOT NULL DEFAULT -1     COMMENT '剩余库存，-1 不限量',
  tags        VARCHAR(255) DEFAULT NULL            COMMENT '关键词标签，英文逗号分隔',
  sort_no     INT          NOT NULL DEFAULT 0      COMMENT '排序（小值优先）',
  status      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 上架 / 0 下架',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_item_code (code),
  KEY idx_item_cat (category, status, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='兑换商店商品表';

-- 9) 兑换记录 ---------------------------------------------------------------
-- 只增不改：兑换即扣情绪值并落一条记录；库存不为 -1 时同步减一。
-- 丹药 / 功法可在「我的主页」服用转为修为：used 1 未使用 / 2 已服用。
CREATE TABLE IF NOT EXISTS star_shop_record (
  id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id    BIGINT   NOT NULL                COMMENT '兑换人 id',
  item_id    BIGINT   NOT NULL                COMMENT '商品 id',
  price      INT      NOT NULL DEFAULT 0      COMMENT '成交时消耗的情绪值',
  used       TINYINT  NOT NULL DEFAULT 1      COMMENT '1 未使用 / 2 已服用（法宝无服用概念，恒为 1）',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '兑换时间',
  PRIMARY KEY (id),
  KEY idx_shop_record_user (user_id, item_id),
  KEY idx_shop_record_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='兑换商店兑换记录';

-- 10) 用户修为（2026-11-16 新增） --------------------------------------------
-- 境界（修士/妖族两线）改由「累计修为」驱动，与情绪值（商店货币）彻底分离：
--   total_cultivation 只增不减，境界永不回退；
--   修为来源：每日打坐（meditate_date 记录最近打坐日，一天一次）+ 服用丹药/参悟功法。
CREATE TABLE IF NOT EXISTS star_user_cultivation (
  user_id           BIGINT   NOT NULL                COMMENT '用户 id（主键）',
  total_cultivation INT      NOT NULL DEFAULT 0      COMMENT '累计修为（境界依据，只增不减）',
  meditate_date     DATE     DEFAULT NULL            COMMENT '最近一次打坐日期（判每日一次）',
  meditate_count    INT      NOT NULL DEFAULT 0      COMMENT '累计打坐次数',
  updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='红线仙修为（境界驱动）';

-- 11) 图标图片化迁移（2026-11-20 新增，幂等） --------------------------------
-- 为任务 / 商品表补 icon 图片列。已有该列时会报 Duplicate column 错误，
-- 由 spring.sql.init.continue-on-error=true 忽略，因此可重复启动。
ALTER TABLE star_redline_task
  ADD COLUMN icon VARCHAR(255) DEFAULT NULL COMMENT '任务图标图片路径，icon 为空时回退 glyph' AFTER glyph;
ALTER TABLE star_shop_item
  ADD COLUMN icon VARCHAR(255) DEFAULT NULL COMMENT '商品图标图片路径，icon 为空时回退 glyph' AFTER glyph;

-- 12) 涂山小游戏战绩（2026-11-20 新增） --------------------------------------
-- 目前仅打地鼠（game_key='whack'）：每局结束结算一次情绪值，
-- 服务端按本表统计当日局数做限次（每日 3 局），防刷上限在 GameService 校验。
CREATE TABLE IF NOT EXISTS star_game_record (
  id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT   NOT NULL                COMMENT '用户 id',
  game_key    VARCHAR(32) NOT NULL DEFAULT 'whack' COMMENT '游戏标识：whack 打地鼠',
  score       INT      NOT NULL DEFAULT 0      COMMENT '本局得分',
  hits        INT      NOT NULL DEFAULT 0      COMMENT '命中次数',
  golden_hits INT      NOT NULL DEFAULT 0      COMMENT '金狐命中次数',
  duration_ms INT      NOT NULL DEFAULT 0      COMMENT '本局时长（毫秒）',
  reward      INT      NOT NULL DEFAULT 0      COMMENT '实得情绪值',
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='涂山小游戏战绩';

-- 13) 好友关系（2026-11-23 新增，我的主页·红线挚友） -------------------------
-- 双向存储：A 与 B 成为好友时写 (A,B) 与 (B,A) 两行，查「我的好友」只扫 user_id。
CREATE TABLE IF NOT EXISTS star_friend (
  id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id    BIGINT   NOT NULL                COMMENT '归属用户 id',
  friend_id  BIGINT   NOT NULL                COMMENT '好友用户 id',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '结缘时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_friend (user_id, friend_id),
  KEY idx_friend_of (friend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='红线挚友（好友关系，双向两行）';

-- 14) 好友申请 ---------------------------------------------------------------
-- 同一方向的申请只有一条记录（uk_req）：同意后落 star_friend 两行并把 status 置 2；
-- 拒绝把 status 置 3、撤回删除记录，被拒后可再次发起（复用原记录改回待处理）。
CREATE TABLE IF NOT EXISTS star_friend_request (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  from_user_id BIGINT       NOT NULL                COMMENT '发起人 id',
  to_user_id   BIGINT       NOT NULL                COMMENT '接收人 id',
  message      VARCHAR(128) DEFAULT NULL            COMMENT '申请留言',
  status       TINYINT      NOT NULL DEFAULT 1      COMMENT '1 待处理 / 2 已同意 / 3 已拒绝',
  created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  handled_at   DATETIME     DEFAULT NULL            COMMENT '处理时间（同意/拒绝）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_req (from_user_id, to_user_id),
  KEY idx_req_to (to_user_id, status),
  KEY idx_req_from (from_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='好友申请';

-- 15) 涂山拍卖行 · 拍品（2026-11-23 新增，独立页面 /auction，导航在「打地鼠」之后） ----
-- 拍品即模板：base_price 底价 / step 加价幅度 / buyout_price 一口价（0 = 无一口价）/
-- duration_min 每场时长（分钟）。runtime 三字段是当前场次状态：current_price 当前价
-- （首价 = 底价，此后每次 ≥ 当前价 + step）、leader_user_id 当前领先者、end_time 结拍时间。
-- 结拍（end_time <= now）懒结算：有领先者 → 竞价成交落 star_auction_deal；无人出价 → 流拍；
-- 结算后立刻开下一场（current_price=base_price，leader 清空，end_time=now+duration_min），永不断档。
CREATE TABLE IF NOT EXISTS star_auction_lot (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  code          VARCHAR(48)  NOT NULL                COMMENT '拍品编码（唯一，便于脚本引用）',
  name          VARCHAR(64)  NOT NULL                COMMENT '拍品名称',
  subtitle      VARCHAR(128) DEFAULT NULL            COMMENT '副标题 / 一句话说明',
  description   VARCHAR(500) DEFAULT NULL            COMMENT '拍品详情',
  glyph         VARCHAR(16)  DEFAULT NULL            COMMENT '拍品图标（emoji）',
  icon          VARCHAR(255) DEFAULT NULL            COMMENT '拍品图标图片路径，icon 为空时回退 glyph',
  color         VARCHAR(16)  DEFAULT NULL            COMMENT '拍品主色（#RRGGBB），驱动前端卡片光效',
  base_price    INT          NOT NULL DEFAULT 10     COMMENT '底价（首拍最低出价）',
  step          INT          NOT NULL DEFAULT 10     COMMENT '加价幅度（每次出价至少 +step）',
  buyout_price  INT          NOT NULL DEFAULT 0      COMMENT '一口价（0 = 不设一口价）',
  duration_min  INT          NOT NULL DEFAULT 60     COMMENT '每场拍卖时长（分钟）',
  tags          VARCHAR(255) DEFAULT NULL            COMMENT '关键词标签，英文逗号分隔',
  sort_no       INT          NOT NULL DEFAULT 0      COMMENT '排序（小值优先）',
  status        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 上架 / 0 下架',
  current_price INT          NOT NULL DEFAULT 0      COMMENT '当前场次最高价（开拍时重置为底价）',
  leader_user_id BIGINT      DEFAULT NULL            COMMENT '当前领先者用户 id（无人出价为 NULL）',
  end_time      DATETIME     DEFAULT NULL            COMMENT '当前场次结拍时间',
  created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_lot_code (code),
  KEY idx_lot_status (status, sort_no),
  KEY idx_lot_end (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='涂山拍卖行拍品（模板+当前场次状态）';

-- 16) 拍卖出价流水 -----------------------------------------------------------
-- 只增不改：每次有效出价落一条（出价即扣情绪值，被超越全额退款，退款不落此表）。
CREATE TABLE IF NOT EXISTS star_auction_bid (
  id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  lot_id     BIGINT   NOT NULL                COMMENT '拍品 id',
  user_id    BIGINT   NOT NULL                COMMENT '出价人 id',
  amount     INT      NOT NULL DEFAULT 0      COMMENT '出价（情绪值）',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '出价时间',
  PRIMARY KEY (id),
  KEY idx_bid_lot (lot_id, created_at),
  KEY idx_bid_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='拍卖行出价流水';

-- 17) 拍卖成交 / 流拍留档 ------------------------------------------------------
-- 只增不改：结拍懒结算或一口价成交时落一条。deal_type：1 竞价成交 / 2 一口价 / 3 流拍。
-- 「我的夺宝」= winner_user_id = 我的 1/2 记录；流拍仅作行史，winner 为 NULL。
CREATE TABLE IF NOT EXISTS star_auction_deal (
  id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  lot_id        BIGINT      NOT NULL                COMMENT '拍品 id',
  name          VARCHAR(64) NOT NULL                COMMENT '拍品名称快照（轮转重开后 lot 行会复用）',
  glyph         VARCHAR(16) DEFAULT NULL            COMMENT '拍品图标快照',
  color         VARCHAR(16) DEFAULT NULL            COMMENT '拍品主色快照',
  price         INT         NOT NULL DEFAULT 0      COMMENT '成交价（流拍为 0）',
  deal_type     TINYINT     NOT NULL DEFAULT 1      COMMENT '1 竞价成交 / 2 一口价 / 3 流拍',
  winner_user_id BIGINT     DEFAULT NULL            COMMENT '得主用户 id（流拍为 NULL）',
  created_at    DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '成交 / 流拍时间',
  PRIMARY KEY (id),
  KEY idx_deal_winner (winner_user_id),
  KEY idx_deal_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='拍卖行成交/流拍留档';

-- ============================================================================
-- 涂山人物志 · 首页种子数据
-- 由 spring.sql.init.data-locations 在启动时执行；
-- 全部使用 INSERT IGNORE + 唯一键（分类 code / 条目 name），可重复执行不产生重复数据。
-- 分类固定 id（1-4），条目固定 id（1-30，其中已移除 12 位）并通过 category_id 挂载。（2026-11-02 按需求删除「圣地秘境/法宝器物/两界组织」及其 6 个子分类、12 位档案，见 backup/）
-- avatar 指向前端 public/avatars/ 下的原创立绘（AI 生成）。
-- ============================================================================

-- 一级分类 -------------------------------------------------------------------
INSERT IGNORE INTO star_category (id, parent_id, code, name, level, icon, color, description, sort_no, status) VALUES
(1, 0, 'RENWU',   '人物档案', 1, '🧑', '#ff9eb5', '涂山与人妖两界有名有姓的众生相', 10, 1);

-- 二级分类 -------------------------------------------------------------------
INSERT IGNORE INTO star_category (id, parent_id, code, name, level, icon, color, description, sort_no, status) VALUES
(2,  1, 'RW_TUSHAN',   '涂山一脉',   2, '🦊', '#ff9eb5', '涂山狐妖一族与红线仙',           10, 1),
(3,  1, 'RW_DAOMEN',   '道门修士',   2, '🗡', '#ffc2cf', '一气道盟与各大家族的修行者',     20, 1),
(4,  1, 'RW_YAOZU',    '妖族众生',   2, '🌸', '#ff5c7a', '散落两界、各有执念的妖',         30, 1);

-- 档案条目 -------------------------------------------------------------------
INSERT IGNORE INTO star_entry (id, category_id, name, alias, glyph, avatar, intro, quote, bond, faction, tags, hot_score, view_count, sort_no, status) VALUES
(1, 2, '涂山苏苏', '涂山三小姐 · 红线仙', '🦊', '/avatars/tushan-susu.jpg',
 '涂山三姐妹中的小妹，一只通体赤红的九尾小狐妖。天性呆萌、爱吃爱闹，却立志成为世上最出色的红娘，与白月初结伴游走人妖两界，为人续那一世未了之缘。',
 '「苏苏一定会成为最棒的红娘哒！」',
 '白月初 —— 契约红线仙搭档，一妖一道，互为软肋',
 '涂山', '呆萌,红线仙,续缘术', 100, 1286, 10, 1),

(2, 3, '白月初', '一气道盟 · 天才道士', '🗡', '/avatars/baiyuechu.jpg',
 '一气道盟的后起之秀，毒舌傲娇、心口不一，嘴上说着不管，却总在关键时刻挡在苏苏身前。符箓剑意纵横，承接两界的续缘委托。',
 '「我才不是为了你才留下来的！」',
 '涂山苏苏 —— 契约搭档，斗嘴一生，护她一生',
 '一气道盟', '毒舌,符箓,契约', 96, 1104, 20, 1),

(3, 2, '涂山红红', '涂山之主 · 妖盟盟主', '👑', '/avatars/tushan-honghong.jpg',
 '涂山至高守护者，九尾天狐，亦是妖盟盟主。以一人之力镇守人妖两界的平衡，是所有妖族心中不可撼动的传说，也是红线一脉真正的源头。',
 '「只要我还在，涂山就不会倒。」',
 '东方月初 —— 跨越前世今生的未了之约',
 '涂山', '九尾天狐,妖盟盟主,涂山之主', 99, 1532, 30, 1),

(4, 3, '东方月初', '傲来国三少', '🌙', '/avatars/dongfang-yuechu.jpg',
 '白月初的前世，傲来国三少，天赋绝伦却命途多舛。与涂山红红的相遇，写下了一段被红线反复重提的宿命。',
 '「这一世，我还是要站在你身边。」',
 '涂山红红 —— 前世之约，红线另一端的执念',
 '傲来国', '前世,傲来国,宿命', 92, 986, 40, 1),

(5, 3, '王权富贵', '王权家 · 道门兵人', '⚔', '/avatars/wangquan-fugui.jpg',
 '王权家倾全族之力培养的天才兵人，剑法凌厉、心性却极静。因清瞳一句「我愿意」，第一次违逆家族意志，令两界为之动容。',
 '「我愿意。」',
 '清瞳 —— 一句承诺，牵动两界',
 '王权家', '王家剑法,兵人,觉醒', 95, 1203, 50, 1),

(6, 4, '清瞳', '蛛妖 · 织梦者', '🕸', '/avatars/qingtong.jpg',
 '温柔的蜘蛛精少女，被王权富贵所救。以丝为笔，为他织出「千里眼」，也织出了一段被红线铭记的深情。',
 '「我织给你的，是整个世界。」',
 '王权富贵 —— 以丝为绳，系住彼此的命',
 '独立', '蛛丝,千里眼,温柔', 90, 942, 60, 1),

(7, 2, '涂山雅雅', '涂山二姐 · 冰系大师', '❄', '/avatars/tushan-yaya.jpg',
 '涂山二小姐，冰系法术登峰造极，外冷内热。嘴上嫌弃苏苏，行动上却永远是第一个冲出来护短的姐姐。',
 '「谁敢动我妹妹，先问过我的冰。」',
 '涂山苏苏 —— 表面嫌弃、实则宠溺的姐姐线',
 '涂山', '冰系,傲娇,护短', 88, 876, 70, 1),

(8, 2, '涂山容容', '涂山大姐 · 首席智囊', '📖', '/avatars/tushan-rongrong.jpg',
 '涂山大小姐，掌管涂山一切大小事务，精于算计与经营。看似温婉柔和，实则握着整个涂山的情报网与钱袋。',
 '「涂山从不做亏本的姻缘。」',
 '涂山姐妹 —— 一个家族的定海神针',
 '涂山', '智谋,掌事,情报', 86, 803, 80, 1),

(9, 3, '王权霸业', '王权家家主', '🛡', '/avatars/wangquan-baye.jpg',
 '王权家家主，强者为尊的践行者。一手托起王权家的荣光，也亲手将儿子推入了冰冷的宿命，是父亲，更是家主。',
 '「王权家的儿子，没有选择。」',
 '王权富贵 —— 父子之间最沉重的那把剑',
 '王权家', '家主,威严,家族', 78, 612, 90, 1),

(15, 3, '白裘恩', '白月初之父 · 一气道盟长老', '🍶', '/avatars/baiqien.jpg',
 '白月初的父亲，看似嗜酒贪财、吊儿郎当，实则深藏不露。嘴上总把儿子挂在嘴边，关键时刻却比谁都靠得住。',
 '「儿子，爹这是为你好。」',
 '白月初 —— 一对互相嫌弃又彼此牵挂的父子',
 '一气道盟', '长老,深藏不露,父子', 74, 512, 150, 1),

(16, 4, '石宽', '北山妖帝', '🗿', '/avatars/shikuan.jpg',
 '北山之主，妖族的一方帝王。外表粗豪、心思却极细腻，为了心中所念，甘愿把整个北山都押上去。',
 '「北山，从不欠人。」',
 '欢都落兰 —— 隔着毒与火的执念',
 '北山', '妖帝,北山,痴情', 76, 540, 160, 1),

(17, 4, '欢都落兰', '南国公主 · 毒术大家', '🦋', '/avatars/huandu-luolan.jpg',
 '南国公主，毒术出神入化，性子骄纵却不失真心。一身毒，一身傲，遇上了便再不肯退。',
 '「本公主的东西，谁也别想抢。」',
 '石宽 —— 一个愿打，一个愿挨',
 '南国', '毒术,公主,傲娇', 73, 498, 170, 1),

(18, 3, '东方淮竹', '东方世家 · 王权富贵之母', '🎋', '/avatars/dongfang-huaizhu.jpg',
 '东方世家之女，气质清冷、灵力精纯。与王权霸业的结合始于家族，却在冷硬的规矩里留下了一点温度，也是王权富贵性情的来处。',
 '「这孩子，不该只是一把剑。」',
 '王权富贵 —— 母亲留下的那一点柔软',
 '东方世家', '东方世家,母亲,灵力', 68, 402, 180, 1),

(19, 3, '东方秦兰', '东方世家 · 东方月初之母', '🌺', '/avatars/dongfang-qinlan.jpg',
 '东方世家的女儿，东方淮竹之妹，东方月初的母亲。性烈如火、护子心切，把东方一脉最后的血脉护到了最后。',
 '「活下去，比什么都重要。」',
 '东方月初 —— 母亲用命换来的那一线生机',
 '东方世家', '东方世家,母亲,护子', 66, 388, 190, 1),

(20, 3, '律笺文', '一气道盟 · 执法者', '📜', '/avatars/lvjianwen.jpg',
 '一气道盟的执法者，铁面无私、只认规矩不认人情。手持律令行走两界，是与非在她这里从没有模糊地带。',
 '「律法面前，人妖同罪。」',
 '一气道盟 —— 规矩的化身',
 '一气道盟', '执法,律令,铁面', 64, 356, 200, 1),

(23, 3, '王权醉', '王权家小妹', '🎐', '/avatars/wangquan-zui.jpg',
 '王权家的小女儿，王权富贵的妹妹。性子活泼娇蛮，一身剑术却货真价实，是王权家冷硬规矩里少有的一点热闹。',
 '「我哥不行，还有我呢！」',
 '王权富贵 —— 兄妹之间，一个冷一个闹',
 '王权家', '王权家,小妹,剑术', 62, 342, 230, 1),

(24, 4, '颜如玉', '千颜 · 画皮之妖', '🎭', '/avatars/yan-ruyu.jpg',
 '能变化万千面容的画皮之妖，游走两界只为寻一个「真实的自己」。与律笺文的相遇，让他第一次想以真面目示人。',
 '「这一次，我想用真的脸见你。」',
 '律笺文 —— 一张脸换一颗心',
 '妖族', '画皮,千颜,变脸', 71, 520, 240, 1),

(25, 4, '欢都擎天', '南国之主', '🐉', '/avatars/huandu-qingtian.jpg',
 '南国之主，欢都落兰的父亲。一身蛊毒之术冠绝两界，护短护到没边，是南国这一方毒泽里说一不二的存在。',
 '「南国的规矩，我说了算。」',
 '欢都落兰 —— 女儿是他的软肋',
 '南国', '南国,蛊毒,王', 60, 298, 250, 1);

-- ============================================================================
-- 红线仙任务告示板（首页「红线仙任务」）
-- 固定 id 1-10，code 唯一；INSERT IGNORE 保证重复启动不产生脏数据。
-- reward = 完成可得情绪值，按品阶定档：difficulty 1 轻缘 500 / 2 深缘 1200 / 3 宿缘 2000。
-- ============================================================================
INSERT IGNORE INTO star_redline_task
(id, code, title, subtitle, description, client, glyph, icon, color, difficulty, reward, duration, scene, tags, sort_no, status) VALUES
(1, 'RL-001', '苦情树下的第一卦', '替一只迷路的小狐问一句姻缘', '有只刚化形的小狐妖在苦情树下转了三天，说什么也不肯开口问卦。她不缺姻缘，只缺一个敢替她问的人。带上红线仙令，替她摇一卦吧。',
 '涂山苏苏', '🦊', '/icons/task-rl-001.png', '#ff9eb5', 1, 500, '当日可成', '苦情树下', '姻缘,初见,问卦', 10, 1),

(2, 'RL-002', '忘忧酒馆的旧账', '找回被撕走的那页红线记录', '忘忧酒馆的账本少了一页，上面记着一对夫妻的续缘凭证。容容说，账可以糊涂，缘不能。去酒馆后厨的酒坛堆里翻一翻。',
 '涂山容容', '🍶', '/icons/task-rl-002.png', '#e09bff', 2, 1200, '限一日', '忘忧酒馆', '记录,酒馆,旧账', 20, 1),

(3, 'RL-003', '千年前的半截红线', '把断在轮回里的红线重新续上', '越山的那根红线只牵了一半，另一端在千年前就断了。要续上它，得先找到当年那个人留在苦情树根下的半枚铜钱。',
 '涂山红红', '🧵', '/icons/task-rl-003.png', '#ff5c7a', 3, 2000, '三日为期', '苦情树根', '续缘,前世,红线', 30, 1),

(4, 'RL-004', '白月初的符箓残页', '追回被山风吹散的三张符纸', '某人画符时打了个喷嚏，三张新符被山风卷走，散在一气道盟的山道上。他嘴上说不必管，符却少一张都画不成阵。',
 '白月初', '🗡', '/icons/task-rl-004.png', '#ffc2cf', 1, 500, '半日', '一气道盟山门', '符箓,跑腿,叽叽喳喳', 40, 1),

(5, 'RL-005', '红线仙的入门考核', '为一对别扭的小情侣牵一次线', '雅雅设的考核：城南那对小情侣明明互相惦记，却谁都不肯先低头。不许用法术，只许用嘴和腿，把他们牵到苦情树下。',
 '涂山雅雅', '🎴', '/icons/task-rl-005.png', '#ffd479', 2, 1200, '两日', '涂山街头', '考核,牵线,实战', 50, 1),

(6, 'RL-006', '傲来国潮汐之约', '赶在潮水退去前把信送到', '东海傲来国，只有落潮那一刻礁石路才会浮出来。信要在潮水退尽之前交到岸边等了三年的那个人手里，误了时辰，路就没了。',
 '东方月初', '🌊', '/icons/task-rl-006.png', '#7fe7c9', 3, 2000, '潮退为限', '傲来国海岸', '送信,潮汐,约定', 60, 1),

(7, 'RL-007', '情蛊的解药', '替一位不敢开口的姑娘解开心结', '一位姑娘中了情蛊，蛊不伤人，只让她每夜梦见同一个人。解药不在药庐，在她自己那句始终没说出口的话里。',
 '无名小妖', '🪷', '/icons/task-rl-007.png', '#ff9eb5', 2, 1200, '三日', '涂山药庐', '心结,情蛊,解药', 70, 1),

(8, 'RL-008', '涂山夜巡', '巡一遍山门，把闲散的执念送回原处', '入夜后的涂山最热闹，也最容易出事。提着灯走一圈山门，遇着不肯走的执念，就替它们指一条回去的路。',
 '涂山守卫', '🏮', '/icons/task-rl-008.png', '#f7b26b', 1, 500, '入夜起更', '涂山山门', '夜巡,守山,日常', 80, 1),

(9, 'RL-009', '一气道盟的和谈书', '把涂山的和谈书送到道盟手上', '两界又起了些摩擦，涂山备下和谈书。路上不太平，送信的人得让道盟明白：这封信不是求和，是给两界都留一条退路。',
 '涂山容容', '📜', '/icons/task-rl-009.png', '#9fe8d6', 2, 1200, '限两日', '一气道盟', '送信,和谈,两界', 90, 1),

(10, 'RL-010', '写给前世的一封信', '替红线仙写一封不必寄出的信', '苦情树下压着一封没写完的信，写信的人已经转世，忘了自己写过。红线仙的任务很简单：替他把最后一句补上。写完，情绪值最高。',
 '苦情树', '✉️', '/icons/task-rl-010.png', '#ffd9a0', 3, 2000, '不限时', '苦情树下', '心事,前世,执笔', 100, 1);

-- ============================================================================
-- 兑换商店（法宝 / 丹药 / 功法 三柜，以情绪值为通货）
-- 固定 id 1-12，code 唯一；INSERT IGNORE 保证重复启动不产生脏数据。
-- price = 兑换所需情绪值；stock = -1 不限量 / >=0 限量。
-- exp_gain = 服用（丹药）/参悟（功法）可得修为，0 表示不可服用（法宝为收藏品）。
-- ============================================================================
INSERT IGNORE INTO star_shop_item
(id, code, category, name, subtitle, description, glyph, icon, color, price, exp_gain, stock, tags, sort_no, status) VALUES
(1, 'SHOP-FB-01', 'fabao', '天书残页', '苦情树亲赐 · 可窥一线姻缘', '涂山禁地的天书被风掀落了一页，纸上朱砂小字时隐时现。贴身收着，偶尔能在梦里看见与自己红线相连的另一端。',
 '📖', '/icons/shop-fb-01.png', '#ffd479', 120, 0, -1, '天书,姻缘,涂山', 10, 1),

(2, 'SHOP-FB-02', 'fabao', '王权剑（仿）', '道盟名剑的工坊仿制版', '王权家的剑开不了裂天，斩不断执念，但握在手里腰杆会直三分。适合新晋红线仙壮胆，也适合摆在案头镇纸。',
 '⚔️', '/icons/shop-fb-02.png', '#9fc7ff', 200, 0, 30, '王权剑,道盟,仿制', 20, 1),

(3, 'SHOP-FB-03', 'fabao', '虚空之泪挂坠', '留不住的人，至少留住一滴泪', '虚空之泪凝成的挂坠，据说戴上它，梦里能回到最想回去的那一天。涂山容容替客人挑礼物时，十次有八次选它。',
 '💧', '/icons/shop-fb-03.png', '#e09bff', 260, 0, -1, '虚空之泪,挂坠,念想', 30, 1),

(4, 'SHOP-FB-04', 'fabao', '灭妖神火符', '纯爱天成 · 一符镇妖邪', '东方灵族的灭妖神火封入符纸，遇险时燃符可保一夜平安。红线仙夜巡山门的必备之物，雅雅亲自批的头批。',
 '🔥', '/icons/shop-fb-04.png', '#ff7a59', 320, 0, 20, '神火,符箓,防身', 40, 1),

(5, 'SHOP-DY-01', 'danyao', '忘忧散', '一盏忘忧，暂别尘嚣', '涂山药庐的日常方子，不删记忆，只让心口那点酸涩钝上三晚。睡一觉醒来，明天还是能笑着牵线的。',
 '🍵', '/icons/shop-dy-01.png', '#7fe7c9', 60, 15, -1, '忘忧,安神,药庐', 10, 1),

(6, 'SHOP-DY-02', 'danyao', '定情丹', '一颗定心，红线更牢', '不是催情的丹，是定心的丹。服下后三日内心念澄明，替人牵线时能听清对方没说出口的那半句话。',
 '💊', '/icons/shop-dy-02.png', '#ff9eb5', 110, 25, -1, '定情,心念,牵线', 20, 1),

(7, 'SHOP-DY-03', 'danyao', '月华露', '月圆之夜凝的露，妖修至爱', '只在满月夜收集的月华露，妖族吐纳前饮下一滴，化形能稳三分。苏苏小时候被雅雅按头喝过整整一坛。',
 '🌙', '/icons/shop-dy-03.png', '#ffe9a0', 150, 30, 40, '月华,妖修,化形', 30, 1),

(8, 'SHOP-DY-04', 'danyao', '九转还魂丹', '涂山压箱底的救命丹', '药庐最深处那格才敢放的丹，一粒可吊住一缕将散的魂。库存极少，非紧急不许动，动用需容容点头。',
 '🟡', '/icons/shop-dy-04.png', '#ffd9a0', 400, 80, 5, '还魂,救命,珍稀', 40, 1),

(9, 'SHOP-GF-01', 'gongfa', '涂山御妖诀 · 入门卷', '涂山弟子第一课', '雅雅亲自校订的入门功法，从吐纳到结印一应俱全。苏苏当年练了三个月才让毛耳朵收回去，你可以更快一点。',
 '🎋', '/icons/shop-gf-01.png', '#a0e8b0', 80, 20, -1, '涂山,御妖,入门', 10, 1),

(10, 'SHOP-GF-02', 'gongfa', '虚空造物术 · 上卷', '无中生有，是套路也是本事', '月初的看家本领，凭空造物骗过半个妖界。上卷只教造杯茶、造把伞，造别的，得等下卷。',
 '✨', '/icons/shop-gf-02.png', '#c7b7ff', 180, 35, -1, '虚空,造物,月初', 20, 1),

(11, 'SHOP-GF-03', 'gongfa', '续缘术 · 释义本', '红线仙的看家法术，先学理论', '把断掉的红线重新续上，是涂山最大的秘密。释义本只讲原理不做演示——真正施展续缘术，需要一颗完整的苦情之心。',
 '🧵', '/icons/shop-gf-03.png', '#ff5c7a', 300, 50, 10, '续缘,红线,秘术', 30, 1),

(12, 'SHOP-GF-04', 'gongfa', '纯爱天成（残卷）', '东方灵血激发的至高神通', '记载着「灭妖神火·纯爱天成」起手式的残卷，缺了后一半。缺的部分要靠自己的故事补全——这很狐妖。',
 '📕', '/icons/shop-gf-04.png', '#ff9eb5', 480, 80, 3, '纯爱天成,神火,残卷', 40, 1);

-- ============================================================================
-- 涂山拍卖行拍品（2026-11-23 新增，独立页面 /auction）
-- 拍品即模板，INSERT IGNORE 幂等；runtime 状态（current_price / leader / end_time）
-- 由 AuctionService 懒结算 + 轮转开拍维护，种子 end_time 过期也无妨（结算后自动开新场）。
-- ============================================================================
INSERT IGNORE INTO star_auction_lot
(id, code, name, subtitle, description, glyph, icon, color, base_price, step, buyout_price, duration_min, tags, sort_no, status, current_price, leader_user_id, end_time) VALUES
(1, 'AU-LOT-01', '苦情巨树的一缕情丝', '苦情树亲赐 · 万中挑一', '每整点由雅雅亲手从苦情巨树上摘下一缕最亮的情丝，装在琉璃匣里。传说握着它入睡，能梦见红线另一端的人此刻在做的事。',
 '🪢', '/icons/au-lot-01.png', '#ff9eb5', 60, 10, 300, 30, '情丝,苦情树,雅雅', 10, 1, 60, NULL, NOW() + INTERVAL 30 MINUTE),

(2, 'AU-LOT-02', '涂山苏苏的兔耳发饰（复刻版）', '三小姐同款 · 呆萌加倍', '苏苏那对毛耳朵的官方复刻发饰，戴上后连语气都会不自觉软三分。容容监修，每场拍卖只此一件，售出即绝版。',
 '🐰', '/icons/au-lot-02.png', '#ffc2cf', 120, 20, 600, 45, '兔耳,苏苏,复刻', 20, 1, 120, NULL, NOW() + INTERVAL 45 MINUTE),

(3, 'AU-LOT-03', '白月初的符笔', '毒舌道士的随身之物', '白月初画符用旧的笔，笔尖还残留着一点神火之气。据本人说「就是支普通破笔」，但一气道盟的人抢破了头。',
 '🖌', '/icons/au-lot-03.png', '#7fc7ff', 150, 20, 700, 60, '符笔,月初,神火', 30, 1, 150, NULL, NOW() + INTERVAL 60 MINUTE),

(4, 'AU-LOT-04', '王权剑断刃碎片', '王权富贵一剑的余威', '王权剑当年斩妖崩落的断刃，巴掌大小，握住时能听见隐约的剑鸣。红中姐验过货，说是真品，还替它讨了个好彩头。',
 '⚔', '/icons/au-lot-04.png', '#ffb35c', 200, 20, 900, 60, '王权剑,断刃,剑鸣', 40, 1, 200, NULL, NOW() + INTERVAL 60 MINUTE),

(5, 'AU-LOT-05', '虚空之泪 · 未镶坠版', '留不住的人，留得住一滴泪', '尚未镶坠的虚空之泪原石，蓝得像深夜的湖。镶坠要另付工钱，但懂行的人都知道，原石的念想比成品更重。',
 '💧', '/icons/au-lot-05.png', '#e09bff', 300, 30, 0, 90, '虚空之泪,原石,念想', 50, 1, 300, NULL, NOW() + INTERVAL 90 MINUTE),

(6, 'AU-LOT-06', '续缘术演示名额', '容容亲授 · 全场压轴', '涂山不外传的看家法术演示名额一个，由容容亲自讲解原理（不含实操）。每季只开一场，价高者得，雅雅还附赠一杯苦情树茶。',
 '🧵', '/icons/au-lot-06.png', '#ff5c7a', 500, 50, 2000, 120, '续缘术,容容,名额', 60, 1, 500, NULL, NOW() + INTERVAL 120 MINUTE);

-- ============================================================================
-- 演示账号 admin / 123456（明文密码，permission=0 管理员权限）
-- ============================================================================
INSERT IGNORE INTO star_user (id, username, password, nickname, status, permission) VALUES
(1, 'admin', '123456', '涂山红娘', 1, 0);
