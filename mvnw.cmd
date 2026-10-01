@REM ----------------------------------------------------------------------------
@REM Maven Wrapper script for Windows (using Java 21)
@REM ----------------------------------------------------------------------------
@echo off
setlocal
if exist "C:\Users\parij\.jdks\ms-21.0.11" (
    set "JAVA_HOME=C:\Users\parij\.jdks\ms-21.0.11"
    set "PATH=C:\Users\parij\.jdks\ms-21.0.11\bin;%PATH%"
)
if exist "C:\Users\parij\apache-maven-3.9.6\bin\mvn.cmd" (
    "C:\Users\parij\apache-maven-3.9.6\bin\mvn.cmd" %*
) else (
    mvn %*
)
