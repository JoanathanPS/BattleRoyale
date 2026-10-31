@echo off
title Battle Royale Arena
echo Launching Battle Royale Arena...
echo.
java -jar "BattleRoyaleArena-1.0.jar"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Java not found or the game crashed.
    echo        Make sure Java 8+ is installed and on your PATH.
    pause
)
