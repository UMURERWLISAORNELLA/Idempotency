@REM Maven Wrapper script for Windows
@REM Downloads Maven if not present, then runs it.
@echo off
setlocal

set MAVEN_VERSION=3.9.6
set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%
set MAVEN_ZIP_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip
set MAVEN_ZIP=%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip

if exist "%MAVEN_HOME%\bin\mvn.cmd" goto run

echo Downloading Apache Maven %MAVEN_VERSION%...
powershell -Command "Invoke-WebRequest -Uri '%MAVEN_ZIP_URL%' -OutFile '%MAVEN_ZIP%'"
powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists' -Force"

:run
"%MAVEN_HOME%\bin\mvn.cmd" %*
endlocal
