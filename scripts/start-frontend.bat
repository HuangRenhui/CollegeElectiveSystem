@echo off
REM =====================================================================
REM  前端启动脚本（Windows）
REM  用法：在项目根目录双击运行，或在 PowerShell / CMD 中执行
REM =====================================================================

chcp 65001 >nul
setlocal

cd /d "%~dp0..\frontend"

echo.
echo ==========================================================
echo  高校选修课管理系统 - 前端启动
echo ==========================================================
echo.

where node >nul 2>nul
if errorlevel 1 (
    echo [错误] 未检测到 Node.js，请先安装 Node.js 18+ 并配置到 PATH
    pause
    exit /b 1
)

if not exist "node_modules" (
    echo [1/2] 首次运行，正在安装依赖（使用国内镜像加速）...
    call npm config set registry https://registry.npmmirror.com
    call npm install
    if errorlevel 1 (
        echo [错误] 依赖安装失败
        pause
        exit /b 1
    )
) else (
    echo [1/2] 依赖已存在，跳过安装
)

echo.
echo [2/2] 启动前端开发服务器（默认 http://localhost:5173）...
echo       按 Ctrl + C 可停止服务
echo.

call npm run dev

endlocal
pause
