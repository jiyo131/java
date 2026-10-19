# 狐妖小红娘项目 · AI 启动索引 (PROJECT_AI_NOTE.md)

> 本文件供 AI 助手快速理解项目，避免本机每次重置后重新探索。
> 重置约定：node_modules、target 会被清空；MySQL 服务保留，但库可能需重建。
> 下次启动本项目，请先读本文件，然后直接跑 `start.bat` / `pause.bat`。

## 0. 一句话结论
一套「涂山红线登录/注册」全栈 Demo（狐妖小红娘主题）：Spring Boot 后端 + Vue3 前端，MySQL 存储。
启动顺序：MySQL → 建库 → 构建后端 → 起后端(8081) → 装前端依赖 → 起前端(5173)。
已提供 `start.bat` / `pause.bat` 一键启停（GBK 编码，路径基于脚本目录可移动）。

## 1. 项目结构
- `springBootTest/` —— 后端 (Spring Boot 3.4.5 / MyBatis-Plus 3.5.9 / MySQL8 / BCrypt)
  - 启动类：`src/main/java/com/fenglin/springboottest/SpringBootTestApplication.java`
  - 配置：`src/main/resources/application-dev.properties`（端口 8081，数据源 root/123456，库 swallowed_star，历史命名保留）
  - 启动建表：`src/main/resources/schema.sql`（**只建表，不建库**）
  - 本地建库建表脚本：`star_user.sql`（根目录，幂等，必须先执行以保证库存在）
  - 构建产物：`target/springBootTest-0.0.1-SNAPSHOT.jar`
- `swallowed-star-vue/` —— 前端 (Vue3 + Vite5)
  - 代理：`vite.config.js` 把 `/api` 代理到 `http://localhost:8081`
  - 脚本：`npm run dev`(5173) / build / preview / lint / format
  - 依赖锁：`package-lock.json` 存在，可用 `npm ci`

## 2. 本机环境（已验证 2026-10-15）
- Java：OpenJDK 21 (Zulu) ✅ 满足 Spring Boot 3.4.5 需要 17+
- Node：v22.22.2 / npm 10.9.7 ✅
- Maven：不在 PATH；IDEA 装在 **D 盘**，用 IDEA 自带 →
  `D:\IntelliJ IDEA 2024.3.2.2\plugins\maven\lib\maven3\bin\mvn.cmd`
  - `start.bat` 已做多候选路径探测：先试 D 盘已知路径，再扫 `C:\Program Files\JetBrains\IntelliJ IDEA *`、`D:\IntelliJ IDEA *` 等，最后退回项目 `springBootTest/mvnw.cmd`（wrapper 会下载 Maven 3.9.9，README 提示个别环境会损坏，优先用 IDEA Maven）
  - `~/.m2/repository` 已缓存 763M，可离线构建
- MySQL：服务名 `MySQL80`（当前运行中）；客户端
  `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`；账号 root / 123456；
  库 `swallowed_star` 初始不存在（需初始化）

## 3. 启动步骤（下次直接跑 start.bat，无需逐步手动）
1. `net start MySQL80`（若已运行忽略）
2. `"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 < springBootTest\star_user.sql`（建库+表，幂等）
3. 后端构建：`mvn -DskipTests package`（在 springBootTest 目录；target 被清空需重建）
4. 后端运行：`java -jar target\springBootTest-0.0.1-SNAPSHOT.jar --server.port=8081 --spring.profiles.active=dev`
5. 前端：`cd swallowed-star-vue && npm install && npm run dev`（node_modules 被清空需重装）
- 演示账号：`admin / 123456`（后端首次启动由 DataInitializer 自动创建）

## 4. 常见坑（已写进脚本，这里留给 AI 复盘）
- `schema.sql` 只建表不建库 → 必须先跑 `star_user.sql`，否则后端起不来。
- Maven 不在 PATH → 用 IDEA 自带 Maven 绝对路径（脚本已处理）。
- MySQL 客户端不在 PATH → 脚本用绝对路径。
- node_modules / target 每次重置被删 → 脚本每次重新 install / package。
- 若重置把 `~/.m2` 也清了，首次 `mvn package` 需联网下载依赖。
- 后端根路径 `http://127.0.0.1:8081/` 由 `static/index.html`（welcome page）提供。
  **2026-10-16 改造**：原来是教学遗留的 Hello World 页（链接直接进 Thymeleaf 老登录页，容易走错），
  现已换成「服务入口」页：自动探测前端 5173 是否可达 → 可达则自动跳转主应用登录页，
  不可达则提示先启动前端并保留教学链路入口。
- 教学链路 `/user/to_login` → `/user/login` → 视图 `user/main`。
  **2026-10-16 修复**：`templates/user/main.html` 原本缺失，导致 POST `/user/login` 直接 500
  （TemplateInputException: Error resolving template [user/main]）→ 已补建该首页模板。
  口令已改为 **双层校验**：优先查数据库真实账号（与 SPA 共用 `star_user` + BCrypt，即
  `admin/123456` 可用），同时兼容教学演示口令 `123`；失败时回登录页并显示 `${msg}` 提示。
- **TokenStore 是内存实现**：后端一重启（`start.bat` 重建 / devtools 热重启），所有旧 token 立即失效。
  前端 `Home.vue` 挂载时调 `/api/auth/me` 校验，失败会自动清 token 并弹回 `/login`——属于预期行为。
- Windows 下 `mvn package` 覆盖 jar 前必须先停掉正在占用 jar 的 java 进程，否则构建失败。

## 5. 启停脚本说明
- `start.bat`（GBK）：一键执行上面 1–5，后端/前端各开一个命令行窗口常驻。
- `pause.bat`（GBK）：按端口 8081/5173 杀进程并关闭窗口（保留 MySQL）。
- 路径全部基于 `%~dp0`（脚本所在目录）的相对引用，项目文件夹可整体移动。

## 6. 接口速查
- 基址：`http://127.0.0.1:8081/api`
- GET `/api/auth/me`（带 `Authorization: Bearer <token>`）
- POST `/api/auth/login` `{username,password}` → token
- POST `/api/auth/register` `{username,password}`
- POST `/api/auth/logout`
- （教学遗留 Thymeleaf 链路）GET `/user/to_login`、POST `/user/login`
  （口令：`admin/123456` 走数据库 BCrypt，或教学口令 `123`）→ 视图 `user/main`

## 7. 前端路由守卫（2026-10-16 修复）
`src/router/index.js`：
- `/` 按登录态分流：有 token → `/home`，无 token → `/login`
- 未登录访问 `/home` → 重定向 `/login`（原有）
- **已登录访问 `/login` 或 `/` → 重定向 `/home`**（新增，解决「登录页进不去首页」）
- 判定依据是 `localStorage.ss_token` 是否存在；真正的有效性由 `Home.vue` 调 `/api/auth/me` 兜底
- 路由 `meta.title` + `router.afterEach` 统一设置 `document.title`

## 8. 首页「涂山人物志」（2026-10-16 开发，纯前端）
`src/views/Home.vue` 已由「姻缘档案卡片」改造为**人物介绍首页**。**未改动任何后端接口**
（仍只用 `/api/auth/me` 校验登录态、`/api/auth/logout` 退出）。
- 结构：固定顶栏 → 首屏（标题 + 搜索栏 + 热词）→ 人物卡片网格 → 人物志详情浮层。
- 导航栏 = 档案分类筛选：全体档案 14 / 人物 4 / 妖族 5 / 圣地 1 / 法宝 2 / 组织 2，每项带数量角标；
  点击分类会滚动到卡片区；**窄屏（≤760px）不隐藏导航**，改为横向滑动条。
- 搜索栏：回车或点「检 索」生效；命中范围 = 姓名 + 称号 + 简介 + 分类 + 势力 + 标签；
  输入框有内容时出现 ✕ 清空按钮；可与分类筛选叠加。
- 数据：`Home.vue` 内的 `DATABASE` 常量（14 条演示数据）。
  **后端检索接口未实现、已预留** —— 接入时只需把 `results` 的数据来源从本地常量换成接口返回。
- 详情：点卡片弹出「人物志」浮层（人物简介 / 关键词 / 经典台词 / 红线羁绊）。
  关闭方式：Esc、右上角 ✕、点击遮罩；打开时给 `body` 加 `.modal-locked` 锁住主页滚动。
  因为「其他页面先不做」，详情用浮层而非新路由。
- 样式：沿用登录页设计令牌（`--pink/--rose/--gold/--panel/--radius`），新增 `.char-*`、`.char-modal*`、
  `.nav-count`、`.btn-clear` 样式块；原 `.star-card` 姻缘档案样式已被 `.char-card` 取代（无其他引用）。
- 验证方式：CDP 真实时间驱动 Chrome（登录填表 → /home → 搜索/分类/弹窗/Esc/退出），全项通过；
  注意 `--virtual-time-budget` 下 CSS transition 的 leave 阶段不会完成，会造成「弹窗关不掉」的假象。
