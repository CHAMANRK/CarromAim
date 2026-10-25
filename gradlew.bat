@echo off
set VERSION=8.13
if not defined GRADLE_USER_HOME set GRADLE_USER_HOME=%USERPROFILE%\.gradle
set BASE=%GRADLE_USER_HOME%\bootstrap
set DIST=%BASE%\gradle-%VERSION%
if not exist "%DIST%\bin\gradle.bat" (
  if not exist "%BASE%" mkdir "%BASE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%VERSION%-bin.zip' -OutFile '%BASE%\gradle-%VERSION%-bin.zip'; Expand-Archive -Force '%BASE%\gradle-%VERSION%-bin.zip' '%BASE%'"
)
call "%DIST%\bin\gradle.bat" %*
