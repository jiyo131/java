@echo off
REM ============================================================
REM 吞噬星空 项目停止脚本 (GBK 编码)
REM 停止前端(5173)与后端(8081); MySQL 服务默认保留
REM 用法: 双击本文件
REM ============================================================

echo 正在停止 吞噬星空 项目 ...

REM 停止后端 (端口 8081)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8081" ^| findstr "LISTENING"') do (
  taskkill /PID %%a /F >nul 2>&1
)

REM 停止前端 (端口 5173)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING"') do (
  taskkill /PID %%a /F >nul 2>&1
)

REM 关闭启动脚本打开的命令行窗口 (按标题)
taskkill /FI "WINDOWTITLE eq swallowed-star-backend" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq swallowed-star-frontend" /F >nul 2>&1

echo 前后端已停止。
echo (MySQL 服务未停止; 如需停止请手动执行: net stop MySQL80)
