@echo off
title Battle Royale Lite
echo Launching Battle Royale Lite...
echo.
java -jar "BattleRoyaleLite.jar"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Java not found or the game crashed.
    echo        Make sure Java 8+ is installed and on your PATH.
    pause
)
