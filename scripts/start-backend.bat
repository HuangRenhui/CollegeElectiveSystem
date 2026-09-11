@echo off
REM =====================================================================
REM  后端启动脚本（Windows）
REM  用法：在项目根目录双击运行，或在 PowerShell / CMD 中执行
REM =====================================================================

chcp 65001 >nul
setlocal

cd /d "%~dp0..\backend"

echo.
echo ==========================================================
echo  高校选修课管理系统 - 后端启动
echo ==========================================================
echo.

where mvn >nul 2>nul
if errorlevel 1 (
    echo [错误] 未检测到 Maven，请先安装 Maven 3.8+ 并配置到 PATH
    pause
    exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
    echo [错误] 未检测到 Java，请先安装 JDK 21 并配置到 PATH
    pause
    exit /b 1
)

echo [1/2] 编译项目...
call mvn -B clean compile -DskipTests
if errorlevel 1 (
    echo [错误] 编译失败，请检查错误信息
    pause
    exit /b 1
)

echo.
echo [2/2] 启动后端服务（默认端口 8080，接口文档 /api/doc.html）...
echo       按 Ctrl + C 可停止服务
echo.

call mvn spring-boot:run

endlocal
pause
