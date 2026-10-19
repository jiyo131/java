@echo off
REM ============================================================
REM 吞噬星空 项目一键启动脚本 (GBK 编码)
REM 适用场景: 本机每次重置后 node_modules / target 被清空, 需重新构建
REM 路径策略: 所有项目内路径基于脚本所在目录 %~dp0, 可随文件夹移动
REM 用法: 双击本文件, 或在 cmd 中运行 start.bat
REM ============================================================

SETLOCAL
SET "ROOT=%~dp0"
SET "BACKEND=%ROOT%springBootTest"
SET "FRONTEND=%ROOT%swallowed-star-vue"

REM ---- 外部工具路径 (本机绝对路径, 这些不在项目内) ----
SET "JAVA=java"
SET "MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

REM 定位 IDEA 自带 Maven (本机装在 D 盘, 不在 C:\Program Files)
SET "MAVEN="
IF EXIST "D:\IntelliJ IDEA 2024.3.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" (
  SET "MAVEN=D:\IntelliJ IDEA 2024.3.2.2\plugins\maven\lib\maven3\bin\mvn.cmd"
)
IF NOT DEFINED MAVEN (
  FOR /D %%d IN ("C:\Program Files\JetBrains\IntelliJ IDEA *","D:\IntelliJ IDEA *","%ProgramFiles%\JetBrains\IntelliJ IDEA *") DO (
    IF EXIST "%%d\plugins\maven\lib\maven3\bin\mvn.cmd" SET "MAVEN=%%d\plugins\maven\lib\maven3\bin\mvn.cmd"
  )
)
IF NOT DEFINED MAVEN SET "MAVEN=%BACKEND%\mvnw.cmd"
echo 使用 Maven: %MAVEN%

echo [1/5] 启动 MySQL 服务 (MySQL80) ...
net start MySQL80 >nul 2>&1
echo        如已运行会提示, 可忽略

echo [2/5] 初始化数据库 (建库/建表, 可重复执行) ...
"%MYSQL%" -uroot -p123456 < "%BACKEND%\star_user.sql"
IF ERRORLEVEL 1 (
  echo [警告] 数据库初始化失败, 请确认 MySQL 已启动且密码为 123456
) ELSE (
  echo        数据库就绪
)

echo [3/5] 用 Maven 重新打包后端 (target 已清空, 必须重建) ...
cd /d "%BACKEND%"
call "%MAVEN%" -DskipTests package
IF ERRORLEVEL 1 (
  echo [错误] 后端构建失败, 请检查 Maven 与网络
  pause
  goto :eof
)

echo [4/5] 启动后端  http://127.0.0.1:8081 ...
start "swallowed-star-backend" cmd /k "cd /d "%BACKEND%" && java -jar target\springBootTest-0.0.1-SNAPSHOT.jar --server.port=8081 --spring.profiles.active=dev"

echo        等待后端启动 10 秒 ...
timeout /t 10 /nobreak >nul

echo [5/5] 安装前端依赖并启动  http://localhost:5173 ...
cd /d "%FRONTEND%"
IF NOT EXIST node_modules (
  call npm install
)
start "swallowed-star-frontend" cmd /k "cd /d "%FRONTEND%" && npm run dev"

echo.
echo ===================================================
echo  启动完成!
echo  前端: http://localhost:5173  (/api 自动代理到 8081)
echo  后端: http://127.0.0.1:8081/api
echo  演示账号: admin / 123456  (首次启动自动创建)
echo  停止项目请运行 pause.bat
echo ===================================================
ENDLOCAL
