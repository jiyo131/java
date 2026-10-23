# 狐妖小红娘 · 后端服务（Spring Boot）

涂山红线后端：登录 / 注册 + **首页展示（多级分类档案库）+ 搜索**，为 `swallowed-star-vue` 前端提供接口。

## 技术栈

- Spring Boot 3.4.5
- MyBatis-Plus 3.5.9（数据库层）
- MySQL 8（本地，账号 `root` / 密码 `123456`）
- BCrypt 密码加密
- 内存 Token 鉴权（演示用，重启失效；可平滑升级为 JWT）

## 目录结构

```
src/main/java/com/fenglin/springboottest/
├─ SpringBootTestApplication.java   # 启动类（@MapperScan）
├─ entity/
│  ├─ User.java                     # 用户实体 → star_user
│  ├─ Category.java                 # 多级分类 → star_category
│  ├─ Entry.java                    # 首页档案 → star_entry
│  └─ SearchLog.java                # 检索日志 → star_search_log
├─ mapper/
│  ├─ UserMapper.java / CategoryMapper.java / SearchLogMapper.java
│  ├─ EntryMapper.java              # 分类计数 / 总浏览量聚合查询
│  └─ projection/CategoryCount.java # 分类维度聚合投影
├─ service/
│  ├─ UserService.java / impl/      # 业务：注册 / 登录 / 按账号查
│  ├─ CategoryService.java / impl/  # 分类树 / 子孙分类 / 面包屑
│  ├─ EntryService.java / impl/     # 检索 / 分页 / 详情 / 推荐
│  ├─ SearchLogService.java / impl/ # 检索日志 / 热门搜索词
│  └─ TokenStore.java               # 内存 token ↔ 用户 映射
├─ controller/
│  ├─ AuthController.java           # /api/auth/* 接口
│  ├─ HomeController.java           # /api/home/* 首页聚合
│  ├─ CategoryController.java       # /api/categories/tree 分类树
│  ├─ EntryController.java          # /api/entries 列表 / 搜索 / 详情
│  ├─ SearchController.java         # /api/search/hot 热门搜索词
│  └─ dto/                          # LoginRequest / LoginResponse
│                                   # + CategoryNode / EntryCard / EntryDetail
│                                   # + PageResult / HomeOverview
├─ common/Result.java               # 统一返回 {code,message,data}
└─ config/                          # CorsConfig / DataInitializer
src/main/resources/
├─ application.properties           # 激活 dev 档
├─ application-dev.properties       # 数据源 / MyBatis-Plus / 端口 8081
├─ schema.sql                       # 启动幂等建表（4 张自建表）
└─ data.sql                         # 启动幂等种子数据（13 分类 / 30 档案，含 AI 立绘路径）
star_user.sql                       # 本地建库建表脚本（与本文件同目录）
```

## 环境准备

1. JDK 17+（已验证 JDK 21 可运行）。
2. 本地 MySQL 8 已启动，账号 `root` / 密码 `123456`。
3. 初始化数据库：在 MySQL 中执行项目根目录 `star_user.sql`
   （建库 `swallowed_star` + 建表 `star_user`，含 `IF NOT EXISTS`，可重复执行）。

## 构建与运行

> 项目自带 `mvnw` 在个别环境会下载损坏，推荐用 IntelliJ IDEA 自带 Maven：
> `D:\IntelliJ IDEA 2024.3.2.2\plugins\maven\lib\maven3\bin\mvn.cmd`

```bash
# 构建（跳过测试）
mvn -DskipTests package

# 运行（显式指定端口与 dev 环境，避免多实例端口冲突）
java -jar target/springBootTest-0.0.1-SNAPSHOT.jar --server.port=8081 --spring.profiles.active=dev
```

启动后接口基址：`http://127.0.0.1:8081/api`

## 接口一览

| 方法 | 路径                | 说明                                          |
| ---- | ------------------- | --------------------------------------------- |
| GET  | `/api/auth/me`      | 携带 `Authorization: Bearer <token>` 获取会话 |
| POST | `/api/auth/login`   | `{username, password}` → 返回 `token`         |
| POST | `/api/auth/register`| `{username, password}` → 成功 / 账号已存在     |
| POST | `/api/auth/logout`  | 登出（清除当前 token）                        |

统一返回结构：`{"code":0,"message":"success","data":{...}}`，`code ≠ 0` 表示业务失败。

## 首页接口（2026-10-23 新增）

| 方法 | 路径                        | 说明 |
| ---- | --------------------------- | ---- |
| GET  | `/api/home/overview`        | 首页聚合：分类树 + 统计 + 热门搜索词 + 编辑推荐 |
| GET  | `/api/home/hot-words`       | 热门搜索词（可单独刷新） |
| GET  | `/api/home/featured`        | 编辑推荐档案 |
| GET  | `/api/categories/tree`      | 多级分类树（count 含全部子孙分类档案数） |
| GET  | `/api/entries`              | 列表 / 检索：`keyword` `categoryId`（一级分类自动带出子分类）`sort=default/hot/new/name` `page` `size` `record` |
| GET  | `/api/entries/featured`     | 编辑推荐 |
| GET  | `/api/entries/{id}`         | 档案详情（浏览量 +1，含分类路径 + 同档推荐） |
| GET  | `/api/search/hot`           | 近 30 天热门搜索词 |

### 自建表

| 表 | 说明 |
| -- | ---- |
| `star_category` | 多级分类：`parent_id` 自关联 + `level` 层级，新增三级/四级分类无需改表 |
| `star_entry`    | 首页档案：挂在叶子分类上，`tags` 逗号分隔，`avatar` 人物形象图，`hot_score` / `view_count` 驱动热门排序 |
| `star_search_log` | 检索日志：热门搜索词与搜索分析数据来源，写库失败不影响检索主流程 |
| `star_user`     | 登录用户（原有） |

启动时 `schema.sql` 幂等建表 + `data.sql`（`INSERT IGNORE`）补齐种子数据：4 个一级分类 / 9 个二级分类 / 30 条档案（2026-10-23 新增王权醉、颜如玉、欢都擎天、涂山城、千里眼、姻缘簿、妖馨斋、红线仙令），可重复启动不重复插入。

> 档案头像为 AI 生成的原创国漫风立绘（5 张 3×2 拼图切分），存放于前端 `swallowed-star-vue/public/avatars/`，`star_entry.avatar` 存放对应路径；`avatar` 为空时前端回退为 emoji 图标。

## 演示账号

首次启动由 `DataInitializer` 自动创建：`admin / 123456`（昵称「星河守护者」），
密码经 BCrypt 加密，无需手动插入。
