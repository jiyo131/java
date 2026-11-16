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
- **2026-10-21 修复「脚本路径错误」**：重置后 `node_modules` 往往只留下 108 个**空壳目录**
  （有文件夹、0 个文件），原脚本用 `IF NOT EXIST node_modules` 判断，会误判「已安装」而跳过
  `npm install`，于是 `npm run dev` 报 `Cannot find module ...\node_modules\vite\bin\vite.js`
  （表现为路径错误 / 'vite' 不是内部或外部命令）。现将判据改为 `node_modules\vite\package.json`，
  并在检测到空壳时自动 `rmdir /s /q node_modules` 后重装；同时增加项目目录、mysql 客户端、
  构建产物的存在性校验与明确报错，脚本开头加 `chcp 936` 固定中文不乱码。
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

## 9. 后台管理系统（2026-10-28 开发）
首页导航栏右侧新增「后台管理」按钮（金色锁图标，`Home.vue` `.nav-user` 内 / `style.css` `.btn-admin`），
点击进入 `/admin/login` 后台登录页（**仅登录、无注册**，账号限定 `admin`，即演示账号 admin/123456）。
登录后进入 `/admin`（默认跳 `/admin/entries`），对首页「涂山人物志」展示数据进行管理。

- 前端（swallowed-star-vue）：
  - 路由：`/admin/login`、`/admin`（children: `entries` 档案管理 / `categories` 分类管理）；
    守卫用**独立 token 键 `ss_admin_token`**（`router/index.js`），与前台 `ss_token` 互不影响。
  - `src/api/admin.js`：独立请求层（复制 http.js 逻辑换 token 键）+ 全部后台接口封装；
    会话存 `ss_admin_session`。
  - 页面：`views/AdminLogin.vue`（登录）、`views/admin/AdminLayout.vue`（顶栏+6 张统计卡，
    provide `adminReload` 供子页刷新统计）、`AdminEntries.vue`（档案表格：关键词/分类/状态筛选、
    分页、新增/编辑弹窗、上下架、删除）、`AdminCategories.vue`（分类树：新增一级/子分类、编辑、
    删除，最多三级）。
- 后端（springBootTest，均在 `controller/admin/`，全部接口先过 `AdminGuard` 校验）：
  - `service/AdminGuard.java`：约定 star_user 表 username='admin' 即管理员（无单独角色字段）。
  - `AdminAuthController` `/api/admin/auth/`：login（非 admin 账号明确拒绝）/ me / logout。
  - `AdminEntryController` `/api/admin/entries`：GET 列表（keyword/categoryId/status 筛选，
    含下架数据，一级分类自动带子分类）、POST 新增、PUT /{id} 编辑、PUT /{id}/status 上下架、
    DELETE /{id}。校验：名称必填且唯一、分类必存在、各字段长度。
  - `AdminCategoryController` `/api/admin/categories`：GET tree（**用 treeAll 含停用分类**）、
    POST/PUT/DELETE（code 唯一、层级 ≤3、父分类不能选自己；删除时有子分类或档案则拒绝）。
  - `AdminOverviewController` `/api/admin/overview`：档案总数/上架/下架/分类数/浏览量合计/
    近30天检索次数+热词。
  - `CategoryService` 新增 `treeAll()`（含停用分类），`CategoryNode` 新增 `status` 字段；
    前台 `tree()` 行为不变。
- 已验证（curl 全链路）：登录/非 admin 拒绝/无 token 401/概览/列表/新增/同名校验/编辑/下架
  （下架后前台 `/api/entries` 检索 total=0）/删除/分类建删。前端 `npm run build`、`npm run lint` 通过。
- 注意：后台与前台共用 TokenStore（内存实现），后端重启后管理员也需重新登录。


## 11. 删除「圣地秘境 / 法宝器物 / 两界组织」（2026-11-02）
经用户确认连数据一起删除：DB 中移除 3 个一级分类 + 6 个二级分类（涂山圣地/傲来秘境/红娘法器/神兵符箓/涂山势力/道盟势力）
+ 12 位档案（id 10,11,12,13,14,21,22,26,27,28,29,30），无外键约束直接删；剩余 4 分类（人物档案+涂山一脉/道门修士/妖族众生）、18 档案、10 桩委托。
关键点：dev 配置 `spring.sql.init.mode=always`，data.sql 每次启动都会 INSERT IGNORE 回填，因此**同步删除了 data.sql 中对应行**
（一级 5/8/11、二级 6/7/9/10/12/13 及 12 个档案块），并重新打包重启后端验证数据不会复活。
导航现为：全体档案 18 · 人物档案 18 · 🧵 红线仙任务；首页统计 18 卷档案 / 4 类分档。
回滚：`backup/swallowed_star_before_delete_20261102.sql`（全库 dump，恢复执行即可）+ `backup/data.sql.bak_20261102`（旧种子文件）。

## 10. 首页「红线仙任务」（2026-11-02 开发并全链路验证）
独立页面 `src/views/RedLineTaskPage.vue`，路由 `/tasks`（登录守卫内，标题「红线仙任务 · 涂山委托告示板」）。顶部导航抽出为共用组件 `src/components/TopNav.vue`（时钟/用户区/导航项事件上抛），「🧵 红线仙任务」入口固定插在首个分类「人物档案」之后（分类未返回时也保留入口）；首页不再内嵌任务区。任务页含面包屑（涂山人物志 › 红线仙任务 + 返回档案库按钮），任务页点分类导航跳 `/home?cat=<id>`，Home 挂载时按 query 落到对应分类。告示板本体仍是 `src/components/RedLineTasks.vue`，可**接取 / 取消（二次确认）/ 完成得情绪值**，样式沿用登录页设计令牌。任务页左上角文字已主题化：面包屑当前项为粉金渐变+红线小图标（.crumb-now/.crumb-thread），标题为白→粉→金三段渐变+柔光（.rl-intro-text h3），上方金色英文眉线 TUSHAN · RED LINE COMMISSION（.rl-en），副标题分隔点改金色（.rl-intro-text .dot）。

- 数据库（schema.sql §5-7，data.sql 已种 10 桩委托）：
  `star_redline_task`（告示板）、`star_task_record`（唯一键 user_id+task_id，1 进行中 / 2 已圆满，
  **取消 = 删除记录**，故可反复接取直到完成；已完成不可取消/再接）、`star_user_emotion`（主键 user_id）。
- 后端：`TaskController` `/api/tasks`（全部要登录态，401 由 TokenStore.resolveUserId 判定）：
  GET 看板（任务卡 + 用户状态 + 情绪值/阶位/进度）、POST `/{id}/accept|cancel|complete`。
  `TaskServiceImpl`：synchronized + 唯一键兜底防并发重复接取；complete 加事务并结算情绪值，
  `TaskAction` 带 `gained / levelUp / levelTitle`（结算前后比阶位，前端据此播升阶动效）。
  阶位阈值 0/60/180/360/600 ↔ 初见红线/心有灵犀/红线牵引/三世情缘/苦情之证（前后端文案一致）。
- 前端：`src/api/tasks.js` 封装四个接口；RedLineTasks.vue 含情绪值面板（飘字 +N、升阶流光 .emo-card.levelup）、
  四页签筛选（全部/待接取/我的委托/已圆满）、骨架屏/错误/空态；取消按钮先变「再想想/确认取消」（4s 自动收回），
  接取后跳「我的委托」，完成后跳「已圆满」，取消后回「全部委托」。样式在 style.css `.rl-*`/`.emo-*`/`.task-*`/`.btn-task`。
- 已验证（CDP 真实 Chrome 驱动 + curl）：登录→看板渲染→接取→取消确认→取消→再接→完成结算
  （+50/+45/+30，跨 180 触发升阶 glow=True）→已圆满页签→退出；含 401/重复接取/重复完成/
  取消已完成/不存在任务等异常分支；多用户状态隔离（新注册用户与 admin 互不影响）。
  `npm run build`、`npm run lint` 通过；`mvn -DskipTests package` 通过（注意先停 8081 的 java 再构建）。
- 演示数据已复位：admin 无接取记录、情绪值 0，10 桩委托全部待接取。

## 12. 修复 data.sql 悬空逗号导致种子数据全灭（2026-11-04）
现象：机器重置后启动，`/api/home/overview` 返回 entryCount=0 / categoryCount=0，首页空白。
根因：2026-11-02 删除「圣地秘境/法宝器物/两界组织」时，`data.sql` 里三条语句的结尾行
（一级分类 id=1、二级分类 id=4、档案 id=25）都留下了**悬空逗号 `,` 后直接跟下一条 INSERT**
（没有分号终止），整个脚本在第一条语句处即语法错误；而 dev 配置
`spring.sql.init.continue-on-error=true` 把错误静默吞掉，后端照常启动，库里却一条种子都没有。
同次编辑还**误删了第 10 桩委托 RL-010「写给前世的一封信」**（已从 backup/data.sql.bak_20261102 找回）。
修复：三处 `,` 改 `;`，补回 RL-010；`mysql < data.sql` 手动灌库验证（18 档案 / 4 分类 / 10 委托）；
停 8081 → `mvn package` 重建 → 重启后端，`/api/home/overview` 恢复 18/4/热词。
教训：**改完 data.sql 必须用 mysql 客户端实际执行一遍验证语法**，不能只看启动日志
（continue-on-error 会吞错）；INSERT IGNORE 幂等，重复执行安全。
验证要点：`curl http://127.0.0.1:8081/api/home/overview` 应返回 entryCount=18、categoryCount=4；
`/api/entries?size=50` total=18；登录 admin/123456 后 `/api/tasks` 返回 10 桩。

## 13. 独立建表脚本 star_page_init.sql（2026-11-04 新增）
应需求生成的**独立交付版**建表脚本，位于项目根目录：`star_page_init.sql`（UTF-8，323 行）。
- 内容：CREATE DATABASE swallowed_star + schema.sql 全部 7 张表 DDL + data.sql 全部种子
  （4 分类 / 18 档案 / 10 委托）+ 演示账号 admin（BCrypt，口令 123456）。
- 用法：`mysql -uroot -p123456 --default-character-set=utf8mb4 < star_page_init.sql`
- 全幂等（CREATE TABLE IF NOT EXISTS + INSERT IGNORE），已在临时库完整跑通并在真实库重复执行验证不产生重复。
- 与 start.bat 的 star_user.sql 区别：star_user.sql 只建表无种子；本脚本自带页面数据，
  单独执行即可让首页有人物介绍（无需依赖后端 sql.init），适合交付/演示/换机重建。
- **注意**：后端 schema.sql / data.sql 若再修改，需重新拼接同步本脚本（拼装方式见 git 或本节）。

## 14. 前后端 IP 统一为本机以太网 IPv4（2026-11-04）
应需求把前后端服务的 IP 从 127.0.0.1/localhost 换成本机局域网 IP（ipconfig 以太网适配器）：
当前为 **192.168.68.72**（DHCP 分配，**换网络后会变**，需同步改下面几处）。
- `swallowed-star-vue/vite.config.js`：`/api` 代理 target → `http://192.168.68.72:8081`
  （`server.host: true` 原本就有，5173 绑定 0.0.0.0，局域网可直接访问前端）。
- 后端 `static/index.html` 欢迎页 spa-link、`templates/user/main.html` 两处入口 → `http://192.168.68.72:5173`。
- `start.bat` / `start.bat.bak` 提示文字（GBK 文件，仅用 sed 替换 ASCII URL，中文不受影响）。
- 注释同步：`src/api/http.js`、`src/api/auth.js`。
- **未改动**：`application-dev.properties` 的 MySQL 连接仍为 127.0.0.1（root 只授权了本地，
  且数据库不属于"前后端服务"）；后端 Tomcat 本身绑定 0.0.0.0，天然可通过本机 IP 访问；CORS 为 `**` 通配。
- 已重新打包重启后端、重启 vite（vite.config.js 修改必须重启才生效）。
- 验证：`http://192.168.68.72:8081/api/home/overview` 200；`http://192.168.68.72:5173/` 200；
  经 5173 代理的 overview / 登录接口均正常；欢迎页链接已指向新 IP。
- 局域网其它设备访问不通常查 Windows 防火墙入站规则（放行 8081/5173）。
  **已放行**：新增两条入站规则 `Tushan-Backend-8081` / `Tushan-Frontend-5173`（TCP，域/专用/公用全配置文件启用）。
- 前后端 README 已同步新 IP（swallowed-star-vue/README.md、springBootTest/README.md），并注明 DHCP 换网络后需改的位置。

## 15. 后台「账号管理」（2026-11-04 开发并全链路验证）
后台新增第三个页签 `/admin/users`（账号管理），对 star_user 做增删改查。

- 数据库：`star_user` 新增 `status TINYINT NOT NULL DEFAULT 1`（1 正常 / 0 停用）。
  **老库迁移**：`CREATE TABLE IF NOT EXISTS` 不会给已存在的表加列，故在 `DataInitializer`
  里用 JdbcTemplate `ALTER TABLE ... ADD COLUMN` + try/catch 幂等补列（Duplicate column 忽略）；
  schema.sql 的 CREATE TABLE 已同步加列（新库直接建出来）。star_page_init.sql 已重新拼接同步。
- 后端：`AdminUserController /api/admin/users`（全部先过 AdminGuard）：
  GET 列表（keyword 匹配账号/昵称、status 筛选、分页、**永不返回密码字段**，行带 admin 标记）；
  POST 新增（用户名 3-16 位中文/字母/数字/下划线、唯一；密码 6-20 位 BCrypt 入库；昵称默认同账号）；
  PUT /{id} 改昵称（账号名不可改）；PUT /{id}/status 停用/启用；PUT /{id}/password 重置密码；
  DELETE /{id} 删除（物理删，同时清理 star_task_record / star_user_emotion；检索日志保留）。
  **保护规则**：内置 admin 不可停用、不可删除、不可改账号名。
- 登录链路：`AuthController.login`、`AdminAuthController.login`、`AuthController.me` 对
  status=0 返回"该账号已被停用"；`AdminGuard.adminOf` 对停用管理员返回 null（token 立即失效）。
  `UserServiceImpl.register` 显式写 status=1。
- 前端：`views/admin/AdminUsers.vue`（工具栏：关键词/状态筛选/新增；表格：头像字/账号+管理员徽标/
  昵称/状态/创建时间/操作；一个弹窗三用：新增/改昵称/重置密码；admin 行的停用删除按钮禁用）；
  `admin.js` 加 6 个接口封装；`router` 加 admin-users 路由；`AdminLayout` 加页签 + 
  「👥 账号数 / 🧑 正常账号」两张统计卡（overview 接口新增 userTotal / userOnline）。
- 已验证（curl 14 项）：列表/新增/重名拒绝/参数校验/改昵称/停用→前台登录被拒/启用→可登录/
  重置密码→旧密失败新密成功/admin 停用删除均拒绝/删除（含关联清理）/未登录 401/概览账号数。
  `npm run lint` 0 error、`npm run build` 通过；vite 热更新后 /admin/users 路由 200。
- 注意：curl 测试脚本里不要用 `UID` 当变量名（bash 只读变量），取 id 用别的名字。

## 16. 脚本修复：IP 漂移 + jar 占用 + star_page_init.sql 旧版回魂（2026-11-06）
本机重置后 DHCP IP 从 192.168.68.72 变为 192.168.68.58，暴露三处问题，已全部修复为「一劳永逸」方案：
- **start.bat（GBK）**：
  - 新增启动时自动探测本机局域网 IPv4（PowerShell `Get-NetIPConfiguration` 取有默认网关的网卡，天然排除 VMware/WSL 虚拟网卡；失败退回 127.0.0.1），提示文字全部用 `%LAN_IP%`，**换网络不再需要改脚本**。
  - 打包步骤前新增 `[3/6]` 按端口自动停掉 8081/5173 残留进程，避免 jar 被占用导致 mvn package 失败。
- **pause.bat（GBK）**：修正项目名「吞噬星空」→「狐妖小红娘」，补 `chcp 936`。
- **vite.config.js**：`/api` 代理 target 由硬编码局域网 IP 改为 `http://127.0.0.1:8081`（vite 与后端同机，代理由 vite 服务端转发，回环地址永远可达；局域网设备照常经 `<本机IP>:5173` 访问，功能等价且免疫 DHCP 换 IP）。
- **后端欢迎页 static/index.html / 教学首页 templates/user/main.html**：入口链接改为按 `location.hostname` 动态生成，访问后端用什么 IP 就跳什么 IP 的 5173。
- **star_page_init.sql 重新生成**：机器重置把该文件还原成了 2026-11-02 删除前的旧版（含圣地秘境/法宝器物/两界组织全量 30 档案 13 分类），一旦执行就会把已删数据复活。已用 `springBootTest/build_page_init.py`（新增的生成脚本）重新拼接：schema.sql 7 张表 + data.sql 当前 18/4/10 种子 + admin（BCrypt 123456），临时库实测 4/18/10/1 通过。**以后 schema/data.sql 改动后重跑该 py 即可同步**。
- 同步更新了 http.js/auth.js 注释、两份 README 中的旧 IP。
- **遗留问题（已处理）**：本次排查时发现 live 库被灌回了 2026-11-02 删除前的全库备份（30 档案/13 分类/含 10-23 的用户 jwy），疑似 backup/swallowed_star_before_delete_20261102.sql 被执行或 MySQL 数据目录被回滚。经用户确认后已重新执行删除：删 9 分类（id 5-13）+ 12 档案（id 10-14,21,22,26-30），保留 jwy 账号；删前全库 dump 存 `backup/swallowed_star_before_redelete_20261106.sql`。已验证前后端 overview 恢复 18/4。

## 17. 「我的主页」（2026-11-09 开发并全链路验证）
导航在「红线仙任务」之后新增「🏠 我的主页」入口（Home.vue / RedLineTaskPage.vue / MyHome.vue 三处 navs 一致：全体档案 → 分类（首个分类后插任务）→ 我的主页），独立页面 `src/views/MyHome.vue`，路由 `/me`（登录守卫内，标题「我的主页 · 涂山红线仙」）。

- 数据库：`star_user_emotion` 新增 `camp TINYINT NOT NULL DEFAULT 1`（1 修士 / 2 妖族）。老库迁移走 `DataInitializer` 幂等 ALTER（同 star_user.status 模式）；schema.sql 已同步；`star_page_init.sql` 已用 build_page_init.py 重新生成。
- 境界体系：情绪值同时驱动两条 5 阶境界线（阈值与红线仙阶位一致 0/60/180/360/600）：
  修士境界 练气期→筑基期→金丹期→元婴期→化神期；妖族境界 幼妖期→妖卒期→妖将期→妖王期→妖皇期。
  每阶返回 reached/current/progress（本境进度百分比），当前境金色高亮。
- 后端：`ProfileController /api/profile`（全部登录态，无 token 返回 body code=401，与 /api/tasks 一致）：
  - GET `/me` → ProfileVo（username/nickname/camp/campText/emotion/totalGain/completedCount/
    acceptedCount/cultivatorRealms/demonRealms/acceptedTasks/completedTasks/serverTime）；
    任务接取情况复用 `TaskService.overview`，按 state 拆成进行中/已圆满两张卡列表。
  - POST `/camp` body {camp:1|2} → 更新 star_user_emotion.camp（无情绪行则插入），非法值 400。
  - 实现：`ProfileService` + `ProfileServiceImpl`（境界阶梯 buildLadder 静态方法），DTO `ProfileVo`（含内部类 RealmNode）。
- 前端：`src/api/profile.js`（fetchProfile/updateCamp）；MyHome.vue 结构 = 面包屑（涂山人物志 › 我的主页）→
  名帖卡（头像字/昵称/主修徽标/情绪值·累计·已圆满·进行中四统计）→ 主修阵营双卡（点击切阵营，徽标随动）→
  修士境界阶梯 → 妖族境界阶梯 → 委托接取情况（进行中带「去完成」按钮跳 /tasks，已圆满带金色「已圆满」印章）。
  样式在 style.css `.my-*` 块（含 ≤760px 响应式：阶梯降两列、阵营/接取单列）。
- 已验证（curl + 真实 Chrome）：/me 各字段、无 token 401、camp 1↔2 切换、非法 camp 400、
  接取后进行中 +1、完成后已圆满 +1 且情绪值进度联动（12/60→练气期 20%）、取消已完成拒绝；
  浏览器实测登录 → 导航点击进入 /me → 点妖族一脉徽标变「妖族 · 主修」。`npm run lint`(0 error)/`build`、
  `mvn package` 通过。演示数据已复位：admin 无任务记录、情绪值 0、camp=1。

## 15. 兑换商店（2026-11-16 开发并全链路验证）
独立页面 `src/views/ShopPage.vue`，路由 `/shop`（登录守卫内，标题「兑换商店 · 涂山小铺」）。
导航入口固定排在「我的主页」之后，四个页面（Home / RedLineTaskPage / MyHome / ShopPage）的
navs computed 末尾统一 push `{ key:'shop', label:'兑换商店' }`，TopNav 图标 🎁。

- **玩法闭环**：做任务赚情绪值 → 商店兑换。商品分三柜：法宝 fabao / 丹药 danyao / 功法 gongfa，
  以情绪值为通货（price），stock=-1 不限量 / >=0 限量（兑一次减一）。
- 数据库（schema.sql §8-9，data.sql 已种 12 件商品，各柜 4 件）：
  `star_shop_item`（code 唯一）、`star_shop_record`（只增不改，price 记成交价）。
- 后端：`ShopController /api/shop`（全登录态，401 走 body code 约定同 TaskController）：
  GET 看板（商品 + 情绪值/阶位 + 我的兑换记录倒序）、POST `/{id}/redeem`。
  `ShopServiceImpl.redeem`：synchronized + @Transactional，校验上架/库存/余额 → 扣情绪值
  （只动 emotion，不动 total_gain/completed_count）→ 减库存 → 落记录。
- 前端：`src/api/shop.js`；商店页含店招（情绪钱袋卡，兑换成功 bump 动效）、四页签
  （全部/法宝/丹药/功法，带数量角标）、货架卡片（emoji 主色卡、标签、余量、两段确认兑换：
  先点变「确认兑」4s 内再点才扣值，仿任务页取消确认）、「我的兑换」记录区 + 去接委托入口。
- 已验证（curl 全链路）：看板 12 件 4/4/4、兑换扣值（125→65）、余额不足报差值、
  限量库存 30→28、售罄 400、不存在商品 400、无 token body code=401、重复兑换正常累计。
  `npm run build`、`npm run lint` 通过；`mvn -DskipTests package` 通过（先停 8081 java）。
  测试数据已复原：admin 情绪值 125、库存复位、star_shop_record 清空。
- `star_page_init.sql` 已同步（现含 10 张表 + 12 件商品），临时库完整跑通验证。

## 16. 修为体系：境界与情绪值分离（2026-11-16 开发并全链路验证）
应需求「修炼不要用情绪值来增加境界」重构：**情绪值 = 商店货币（可花掉），修为 = 境界驱动（只增不减）**。
此前境界直接按当前情绪值算，兑换消费会导致境界倒退——已彻底修复。

- **修为来源（用户选定两途径，未选「完成委托双结算」）**：
  ① 每日打坐：`POST /api/cultivation/meditate`，每自然日一次（meditate_date 判定），随机 5-12 修为；
  ② 服用丹药 / 参悟功法：商店兑入后主页「服用」，消耗一条兑换记录转修为（exp_gain：丹药 15/25/30/80，功法 20/35/50/80；法宝 exp_gain=0 不可服用）。
- **数据库（存量库已 ALTER，schema.sql 同步）**：
  新表 `star_user_cultivation`（user_id PK / total_cultivation / meditate_date / meditate_count）；
  `star_shop_item` 加 `exp_gain`；`star_shop_record` 加 `used`（1 未使用 / 2 已服用）。
  境界阈值沿用 0/60/180/360/600（练气→筑基→金丹→元婴→化神 / 幼妖→妖卒→妖将→妖王→妖皇）。
- **后端**：`common/RealmLadder`（公共阶梯工具，Profile 与 Cultivation 共用）、`common/Categories`；
  `CultivationController /api/cultivation`：GET 面板（修为+今日状态+可服用背包）、POST `/meditate`、
  POST `/use/{recordId}`（synchronized+事务；校验归属/未服用/品类/仅丹药功法）；升境时返回 levelUp+realmTitle。
  `ProfileServiceImpl` 境界改由 cultivationMapper 读累计修为；ProfileVo 新增 `cultivation` 字段；
  `ShopCard` 新增 `expGain`（商店卡可标注服用收益）。
- **前端**：`api/cultivation.js`；MyHome 新增「修炼」卡（累计修为大数字 + 打坐按钮每日一次态 +
  可服用列表服用按钮，服用/升境 toast，升境按钮 flash）；两条境界线文案「情绪值」→「修为」，
  realmSummary 改用 profile.cultivation；ShopPage 商品卡标「服用 +N 修为」、兑入可服用道具后 toast 指引去主页。
- **已验证（curl 全链路）**：面板初始 0 → 打坐 +8（随机段 5-12 内）→ 重复打坐 400 → profile 修为线正确 →
  兑忘忧散（expGain=15 透出）→ 服用 +15（8→23）→ 重复服用 400 → 兑王权剑后修为不变（消费不掉境界核心验证）→
  法宝不出现在可服用列表、强服 400；`npm run build/lint`、`mvn -DskipTests package` 通过。
  测试数据已复原（admin 情绪值 125 / 库存复位 / shop_record 与 cultivation 清空，今日打坐次数归零可重试）。
- `star_page_init.sql` 已再同步（11 张表，含 exp_gain/used/修为表），脚本语法经实际执行验证。

## 17. 任务奖励按品阶定档（2026-11-16 更新）
应用户要求，完成委托的情绪值奖励改为按品阶（difficulty）定档，上下限 500~2000：
**轻缘(difficulty=1) 500 / 深缘(2) 1200 / 宿缘(3) 2000**（用户确认档位，且明确**不同步**放大商店价格与红线仙阶位阈值）。

- data.sql：10 个任务的 reward 按难度更新，注释同步「按品阶定档」；存量库用
  `UPDATE star_redline_task SET reward=CASE difficulty WHEN 1 THEN 500 WHEN 2 THEN 1200 ELSE 2000 END` 打补丁
  （INSERT IGNORE 不更新已有行，改种子必须同时改存量）。
- star_page_init.sql 任务段已同步（3×500 / 4×1200 / 3×2000）。
- 后端零改动（reward 从库读）；无需重新打包。curl 实测：任务列表展示新奖励、
  完成 RL-001 到账 +500（125→625），测试痕迹已复原（admin 回到 125 / 记录删除）。
- 教训：data.sql 批量改值时正则分组计数错位曾把 `12, '当日可成'` 拼坏成 `50012当日可成'`，
  已按「新奖励+旧奖励+时长」形态逐行修复并复核 10 行三元组完整。
