@echo off
REM ============================================================
REM 狐妖小红娘 项目一键启动脚本 (GBK 编码)
REM 适用场景: 本机每次重置后 node_modules / target 被清空, 需重新构建
REM 路径策略: 所有项目内路径基于脚本所在目录 %~dp0, 可随文件夹移动
REM 用法: 双击本文件, 或在 cmd 中运行 start.bat
REM
REM 2026-10-21 修复「脚本路径错误」:
REM   原判据 IF NOT EXIST node_modules 只检查目录是否存在。
REM   重置后 node_modules 常残留 108 个"空壳"包目录(有目录无文件),
REM   脚本误判为已安装 -> 跳过 npm install -> npm run dev 找不到 vite,
REM   报 "Cannot find module ...node_modules\vite\bin\vite.js" / 路径不正确。
REM   现改为按关键文件 node_modules\vite\package.json 判断, 并自动清理空壳重装。
REM 2026-11-06 修复「IP 漂移」:
REM   本机 IP 由 DHCP 分配, 旧脚本硬编码 192.168.68.72, 换网络后失效(变为 .58),
REM   导致前端 /api 代理打不到后端、提示信息也指错地址。
REM   现改为启动时自动探测本机局域网 IPv4(取有默认网关的网卡, 天然排除
REM   VMware/WSL 等无网关的虚拟网卡), 探测失败退回 127.0.0.1, 换网络无需改脚本。
REM 2026-11-06 修复「jar 占用导致构建失败」:
REM   旧脚本直接 mvn package, 若 8081 还有旧后端进程在跑, jar 被占用构建必失败。
REM   现在打包前先按端口自动停掉残留的前后端进程(8081/5173)。
REM ============================================================

SETLOCAL
REM 固定代码页为 GBK, 避免控制台中文乱码
chcp 936 >nul 2>&1

SET "ROOT=%~dp0"
SET "BACKEND=%ROOT%springBootTest"
SET "FRONTEND=%ROOT%swallowed-star-vue"

REM ---- 先校验项目目录 (路径不对时给出明确提示, 而不是后面莫名报错) ----
IF NOT EXIST "%BACKEND%\pom.xml" (
  echo [错误] 找不到后端目录: "%BACKEND%"
  echo        请确认 start.bat 与 springBootTest 文件夹在同一层级。
  pause
  goto :eof
)
IF NOT EXIST "%FRONTEND%\package.json" (
  echo [错误] 找不到前端目录: "%FRONTEND%"
  echo        请确认 start.bat 与 swallowed-star-vue 文件夹在同一层级。
  pause
  goto :eof
)

REM ---- 外部工具路径 (本机绝对路径, 这些不在项目内) ----
SET "JAVA=java"
SET "MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

REM MySQL 客户端: 首选上面路径, 找不到则扫描常见安装目录
IF NOT EXIST "%MYSQL%" (
  FOR /D %%d IN ("C:\Program Files\MySQL\MySQL Server *","D:\MySQL\MySQL Server *","%ProgramFiles%\MySQL\MySQL Server *") DO (
    IF EXIST "%%d\bin\mysql.exe" SET "MYSQL=%%d\bin\mysql.exe"
  )
)
IF NOT EXIST "%MYSQL%" (
  echo [警告] 未找到 mysql 客户端, 跳过建库步骤: "%MYSQL%"
  echo        若数据库已存在可忽略本警告。
)

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

REM ---- 自动探测本机局域网 IPv4 (取有默认网关的网卡; VMware/WSL 虚拟网卡没有默认网关, 会被自动排除) ----
SET "LAN_IP="
FOR /F %%i IN ('powershell -NoProfile -Command "(Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway } | Select-Object -First 1).IPv4Address.IPAddress"') DO SET "LAN_IP=%%i"
IF NOT DEFINED LAN_IP SET "LAN_IP=127.0.0.1"
echo 本机局域网 IP: %LAN_IP%  (自动探测, 换网络无需改脚本)

echo [1/6] 启动 MySQL 服务 (MySQL80) ...
net start MySQL80 >nul 2>&1
echo        如已运行会提示, 可忽略

echo [2/6] 初始化数据库 (建库/建表, 可重复执行) ...
IF EXIST "%MYSQL%" (
  "%MYSQL%" -uroot -p123456 < "%BACKEND%\star_user.sql"
  IF ERRORLEVEL 1 (
    echo [警告] 数据库初始化失败, 请确认 MySQL 已启动且密码为 123456
  ) ELSE (
    echo        数据库就绪
  )
) ELSE (
  echo        已跳过 ^(未找到 mysql 客户端^)
)

echo [3/6] 停止残留的前后端进程 (避免 jar 被占用导致构建失败) ...
FOR /F "tokens=5" %%a IN ('netstat -ano ^| findstr ":8081" ^| findstr "LISTENING"') DO taskkill /PID %%a /F >nul 2>&1
FOR /F "tokens=5" %%a IN ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING"') DO taskkill /PID %%a /F >nul 2>&1
timeout /t 2 /nobreak >nul

echo [4/6] 用 Maven 重新打包后端 (target 已清空, 必须重建) ...
cd /d "%BACKEND%"
call "%MAVEN%" -DskipTests package
IF ERRORLEVEL 1 (
  echo [错误] 后端构建失败。
  echo        常见原因: Maven/网络异常, 请检查: %MAVEN%
  pause
  goto :eof
)
IF NOT EXIST "%BACKEND%\target\springBootTest-0.0.1-SNAPSHOT.jar" (
  echo [错误] 构建结束但未生成 target\springBootTest-0.0.1-SNAPSHOT.jar
  pause
  goto :eof
)

echo [5/6] 启动后端  http://%LAN_IP%:8081 ...
start "swallowed-star-backend" cmd /k "cd /d "%BACKEND%" && java -jar target\springBootTest-0.0.1-SNAPSHOT.jar --server.port=8081 --spring.profiles.active=dev"

echo        等待后端启动 10 秒 ...
timeout /t 10 /nobreak >nul

echo [6/6] 安装前端依赖并启动  http://%LAN_IP%:5173 ...
cd /d "%FRONTEND%"
REM 关键修复: 用 node_modules\vite\package.json 判断依赖是否真的装好了
IF NOT EXIST "node_modules\vite\package.json" (
  IF EXIST "node_modules" (
    echo        检测到 node_modules 只剩空壳目录^(重置残留^), 先清理再重装 ...
    rmdir /s /q "node_modules"
  )
  echo        正在安装前端依赖, 首次或重置后较慢, 请耐心等待 ...
  call npm install
  IF ERRORLEVEL 1 (
    echo [错误] 前端依赖安装失败, 请检查网络或 npm 配置后再试。
    pause
    goto :eof
  )
)
IF NOT EXIST "node_modules\vite\package.json" (
  echo [错误] 依赖安装后仍找不到 vite, 前端无法启动。
  echo        请手动在 "%FRONTEND%" 执行: npm install
  pause
  goto :eof
)
start "swallowed-star-frontend" cmd /k "cd /d "%FRONTEND%" && npm run dev"

echo.
echo ===================================================
echo  启动完成!
echo  本机访问:  http://localhost:5173
echo  局域网访问: http://%LAN_IP%:5173  (/api 自动代理到 8081)
echo  演示账号: admin / 123456  (首次启动自动创建)
echo  停止项目请运行 pause.bat
echo ===================================================
ENDLOCAL
