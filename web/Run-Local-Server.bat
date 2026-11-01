@echo off
title Battle Royale Arena — Local Web Server
cd /d "%~dp0"
start "" "http://localhost:8000"

where python >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    python -m http.server 8000
    exit /b
)

where node >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    node -e "const http=require('http'),fs=require('fs'),path=require('path');http.createServer((q,s)=>{let p=path.join('.',q.url.split('?')[0]==='/'?'index.html':q.url.split('?')[0]);if(fs.existsSync(p)&&fs.statSync(p).isFile())fs.createReadStream(p).pipe(s);else{s.statusCode=404;s.end();}}).listen(8000);"
    exit /b
)
pause

