@echo off
title Battle Royale Arena — Local Web Server
echo ========================================================
echo   Starting Local HTTP Server on http://localhost:8000
echo ========================================================
echo.
cd /d "%~dp0"
if exist web cd web

start "" "http://localhost:8000"

where python >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Using Python HTTP server...
    python -m http.server 8000
    goto end
)

where npx >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Using NPX serve...
    npx serve -l 8000
    goto end
)

where node >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Using Node.js HTTP server...
    node -e "const http=require('http'),fs=require('fs'),path=require('path');http.createServer((q,s)=>{let p=path.join('.',q.url.split('?')[0]==='/'?'index.html':q.url.split('?')[0]);if(fs.existsSync(p)&&fs.statSync(p).isFile())fs.createReadStream(p).pipe(s);else{s.statusCode=404;s.end();}}).listen(8000);console.log('Serving on http://localhost:8000');"
    goto end
)

echo No Python or Node.js detected on PATH.
echo Open https://battleroyale.joanathan.in to play online!
pause

:end

