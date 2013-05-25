@rem .
@rem Copyright (c) 2009-2010 Sonatype, Inc. All rights reserved.
@rem .

@if "%WRAPPER_DEBUG%" == "" @echo off

if "%OS%"=="Windows_NT" goto begin
echo Unsupported Windows version: %OS%
pause
goto :eof

:begin
setlocal enableextensions

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.\

set DIST_BITS=32
if "%PROCESSOR_ARCHITECTURE%" == "AMD64" goto amd64
if not "%ProgramW6432%" == "" set DIST_BITS=64
goto pickwrapper

:amd64
set DIST_BITS=64

:pickwrapper
set WRAPPER_EXE=%DIRNAME%jsw\exec\wrapper-windows-x86-%DIST_BITS%.exe
if exist "%WRAPPER_EXE%" goto pickconfig
echo Missing wrapper executable: %WRAPPER_EXE%
pause
goto end

:pickconfig
set WRAPPER_CONF=%DIRNAME%..\${confdir}\wrapper.conf
if exist "%WRAPPER_CONF%" goto execute
echo Missing wrapper config: %WRAPPER_CONF%
pause
goto end

:execute
for /F %%v in ('echo %1^|findstr "^console$ ^start$ ^pause$ ^resume$ ^stop$ ^restart$ ^install$ ^remove"') do call :exec set COMMAND=%%v

if "%COMMAND%" == "" (
    echo Usage: %0 { console : start : pause : resume : stop : restart : install : remove }
    pause
    goto end
) else (
    shift
)

call :%COMMAND%
if errorlevel 1 pause
goto end

:console
"%WRAPPER_EXE%" -c "%WRAPPER_CONF%"
goto :eof

:start
"%WRAPPER_EXE%" -t "%WRAPPER_CONF%"
goto :eof

:pause
"%WRAPPER_EXE%" -a "%WRAPPER_CONF%"
goto :eof

:resume
"%WRAPPER_EXE%" -e "%WRAPPER_CONF%"
goto :eof

:stop
"%WRAPPER_EXE%" -p "%WRAPPER_CONF%"
goto :eof

:install
"%WRAPPER_EXE%" -i "%WRAPPER_CONF%"
goto :eof

:remove
"%WRAPPER_EXE%" -r "%WRAPPER_CONF%"
goto :eof

:restart
call :stop
call :start
goto :eof

:exec
%*
goto :eof

:end
endlocal

:finish
cmd /C exit /B %ERRORLEVEL%
