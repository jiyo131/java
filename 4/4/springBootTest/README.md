# 吞噬星空 · 后端服务（Spring Boot）

星域登录 / 注册后端，为桌面 `swallowed-star-vue` 前端提供认证接口。

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
├─ entity/User.java                 # 用户实体 → star_user
├─ mapper/UserMapper.java           # MyBatis-Plus Mapper
├─ service/
│  ├─ UserService.java / impl/      # 业务：注册 / 登录 / 按账号查
│  └─ TokenStore.java               # 内存 token ↔ 用户 映射
├─ controller/
│  ├─ AuthController.java           # /api/auth/* 接口
│  └─ dto/                          # LoginRequest / LoginResponse
├─ common/Result.java               # 统一返回 {code,message,data}
└─ config/                          # CorsConfig / DataInitializer
src/main/resources/
├─ application.properties           # 激活 dev 档
├─ application-dev.properties       # 数据源 / MyBatis-Plus / 端口 8081
└─ schema.sql                       # 启动幂等建表
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

## 演示账号

首次启动由 `DataInitializer` 自动创建：`admin / 123456`（昵称「星河守护者」），
密码经 BCrypt 加密，无需手动插入。
