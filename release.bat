@echo off
setlocal EnableDelayedExpansion
title Battle Royale Arena - Build Release

echo ============================================
echo   Battle Royale Arena  ^|  Release Builder
echo ============================================
echo.

:: -- Configuration -----------------------------
set APP_NAME=BattleRoyaleArena
set VERSION=1.0
set JAR_SOURCE=desktop\build\libs\desktop-%VERSION%.jar
set RELEASE_DIR=release
set RELEASE_JAR=%RELEASE_DIR%\%APP_NAME%-%VERSION%.jar
set LAUNCH_BAT=%RELEASE_DIR%\Launch-%APP_NAME%.bat
:: ----------------------------------------------

echo [1/3] Building fat JAR via Gradle (dist task)...
echo.
call gradlew.bat :desktop:dist
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Gradle build failed. Aborting.
    pause
    exit /b 1
)

echo.
echo [2/3] Preparing release folder...
if not exist "%RELEASE_DIR%" mkdir "%RELEASE_DIR%"

:: Copy and rename the JAR
copy /Y "%JAR_SOURCE%" "%RELEASE_JAR%" >nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Could not copy JAR to release folder.
    echo        Expected: %JAR_SOURCE%
    pause
    exit /b 1
)

:: -- Write a companion launcher bat inside release\ -
(
echo @echo off
echo title Battle Royale Arena
echo echo Launching Battle Royale Arena...
echo echo.
echo java -jar "%APP_NAME%-%VERSION%.jar"
echo if %%ERRORLEVEL%% NEQ 0 ^(
echo     echo.
echo     echo [ERROR] Java not found or the game crashed.
echo     echo        Make sure Java 8+ is installed and on your PATH.
echo     pause
echo ^)
) > "%LAUNCH_BAT%"

echo.
echo [3/3] Done!
echo.
echo  Release folder : %RELEASE_DIR%\
echo  Runnable JAR   : %RELEASE_JAR%
echo  Launcher bat   : %LAUNCH_BAT%
echo.
echo  To run the game:  java -jar %RELEASE_JAR%
echo  Or double-click:  %LAUNCH_BAT%
echo.
pause
