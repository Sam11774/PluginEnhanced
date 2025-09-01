@echo off
REM ================================================================
REM                   BATCH FILE LOGGING FRAMEWORK
REM                         Version 1.0.0
REM ================================================================
REM Provides structured logging capabilities for all RuneLiteAI batch operations
REM Supports multiple log levels and automatic file rotation

REM Initialize logging environment
set "LOG_BASE_DIR=D:\RuneliteAI\Logs\batch"
set "CURRENT_DATE=%DATE:~-4,4%-%DATE:~-10,2%-%DATE:~-7,2%"
set "CURRENT_TIME=%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%"
set "CURRENT_TIME=%CURRENT_TIME: =0%"
set "SESSION_ID=%CURRENT_DATE%_%CURRENT_TIME%"
set "MAIN_LOG=%LOG_BASE_DIR%\batch-operations-%CURRENT_DATE%.log"
set "ERROR_LOG=%LOG_BASE_DIR%\batch-errors-%CURRENT_DATE%.log"

REM Ensure log directories exist
if not exist "%LOG_BASE_DIR%" mkdir "%LOG_BASE_DIR%"

REM ================================================================
REM                      LOGGING FUNCTIONS
REM ================================================================

:LOG_INFO
    set "MESSAGE=%~1"
    set "TIMESTAMP=%DATE% %TIME%"
    echo [%TIMESTAMP%] [INFO] %MESSAGE% >> "%MAIN_LOG%"
    echo [INFO] %MESSAGE%
goto :eof

:LOG_WARN  
    set "MESSAGE=%~1"
    set "TIMESTAMP=%DATE% %TIME%"
    echo [%TIMESTAMP%] [WARN] %MESSAGE% >> "%MAIN_LOG%"
    echo [%TIMESTAMP%] [WARN] %MESSAGE% >> "%ERROR_LOG%"
    echo [WARN] %MESSAGE%
goto :eof

:LOG_ERROR
    set "MESSAGE=%~1"
    set "TIMESTAMP=%DATE% %TIME%"
    echo [%TIMESTAMP%] [ERROR] %MESSAGE% >> "%MAIN_LOG%"
    echo [%TIMESTAMP%] [ERROR] %MESSAGE% >> "%ERROR_LOG%"
    echo [ERROR] %MESSAGE%
goto :eof

:LOG_DEBUG
    set "MESSAGE=%~1"
    set "TIMESTAMP=%DATE% %TIME%"
    if "%DEBUG_LOGGING%"=="true" (
        echo [%TIMESTAMP%] [DEBUG] %MESSAGE% >> "%MAIN_LOG%"
        echo [DEBUG] %MESSAGE%
    )
goto :eof

:LOG_START_OPERATION
    set "OPERATION=%~1"
    set "TIMESTAMP=%DATE% %TIME%"
    echo. >> "%MAIN_LOG%"
    echo ================================================================ >> "%MAIN_LOG%"
    echo [%TIMESTAMP%] [START] %OPERATION% >> "%MAIN_LOG%"
    echo ================================================================ >> "%MAIN_LOG%"
    echo.
    echo ================================================================
    echo [START] %OPERATION%
    echo ================================================================
goto :eof

:LOG_END_OPERATION
    set "OPERATION=%~1"
    set "STATUS=%~2"
    set "TIMESTAMP=%DATE% %TIME%"
    echo [%TIMESTAMP%] [END] %OPERATION% - Status: %STATUS% >> "%MAIN_LOG%"
    echo ================================================================ >> "%MAIN_LOG%"
    echo. >> "%MAIN_LOG%"
    echo [END] %OPERATION% - Status: %STATUS%
    echo ================================================================
    echo.
goto :eof

:LOG_DATABASE_COMMAND
    set "COMMAND=%~1"
    set "TIMESTAMP=%DATE% %TIME%"
    call :LOG_DEBUG "Executing database command: %COMMAND%"
goto :eof

:LOG_MAVEN_COMMAND
    set "COMMAND=%~1"
    set "TIMESTAMP=%DATE% %TIME%"
    call :LOG_DEBUG "Executing Maven command: %COMMAND%"
goto :eof

:LOG_PERFORMANCE_METRIC
    set "METRIC_NAME=%~1"
    set "METRIC_VALUE=%~2"
    set "TIMESTAMP=%DATE% %TIME%"
    echo [%TIMESTAMP%] [METRIC] %METRIC_NAME%: %METRIC_VALUE% >> "%MAIN_LOG%"
    call :LOG_DEBUG "Performance metric - %METRIC_NAME%: %METRIC_VALUE%"
goto :eof

REM ================================================================
REM                    LOGGING INITIALIZATION
REM ================================================================

:INIT_BATCH_LOGGING
    call :LOG_START_OPERATION "BATCH LOGGING INITIALIZATION"
    call :LOG_INFO "Batch logging framework initialized"
    call :LOG_INFO "Session ID: %SESSION_ID%"
    call :LOG_INFO "Main log: %MAIN_LOG%"
    call :LOG_INFO "Error log: %ERROR_LOG%"
    call :LOG_INFO "Debug logging: %DEBUG_LOGGING%"
    
    REM Log system information
    call :LOG_INFO "System: %COMPUTERNAME%"
    call :LOG_INFO "User: %USERNAME%"
    call :LOG_INFO "Working directory: %CD%"
    
    call :LOG_END_OPERATION "BATCH LOGGING INITIALIZATION" "SUCCESS"
goto :eof

REM ================================================================
REM                      UTILITY FUNCTIONS
REM ================================================================

:CHECK_FILE_EXISTS
    set "FILE_PATH=%~1"
    set "DESCRIPTION=%~2"
    if exist "%FILE_PATH%" (
        call :LOG_INFO "%DESCRIPTION% found: %FILE_PATH%"
        set "FILE_CHECK_RESULT=SUCCESS"
    ) else (
        call :LOG_ERROR "%DESCRIPTION% NOT FOUND: %FILE_PATH%"
        set "FILE_CHECK_RESULT=FAILURE"
    )
goto :eof

:CHECK_DIRECTORY_EXISTS
    set "DIR_PATH=%~1"
    set "DESCRIPTION=%~2"
    if exist "%DIR_PATH%" (
        call :LOG_INFO "%DESCRIPTION% found: %DIR_PATH%"
        set "DIR_CHECK_RESULT=SUCCESS"
    ) else (
        call :LOG_ERROR "%DESCRIPTION% NOT FOUND: %DIR_PATH%"
        set "DIR_CHECK_RESULT=FAILURE"
    )
goto :eof

:EXECUTE_WITH_LOGGING
    set "COMMAND=%~1"
    set "DESCRIPTION=%~2"
    set "START_TIME=%TIME%"
    
    call :LOG_INFO "Executing: %DESCRIPTION%"
    call :LOG_DEBUG "Command: %COMMAND%"
    
    %COMMAND% >> "%MAIN_LOG%" 2>&1
    set "EXIT_CODE=%ERRORLEVEL%"
    
    set "END_TIME=%TIME%"
    
    if %EXIT_CODE%==0 (
        call :LOG_INFO "%DESCRIPTION% completed successfully"
    ) else (
        call :LOG_ERROR "%DESCRIPTION% failed with exit code %EXIT_CODE%"
    )
    
    call :LOG_PERFORMANCE_METRIC "Command execution time" "%START_TIME% to %END_TIME%"
goto :eof

:CLEANUP_OLD_LOGS
    call :LOG_INFO "Cleaning up log files older than 30 days..."
    
    REM Clean up old batch logs (keep 30 days)
    forfiles /p "%LOG_BASE_DIR%" /s /m *.log /d -30 /c "cmd /c del @path" 2>nul
    if %ERRORLEVEL%==0 (
        call :LOG_INFO "Old log files cleanup completed"
    ) else (
        call :LOG_DEBUG "No old log files found to clean up"
    )
goto :eof

REM ================================================================
REM                        USAGE EXAMPLE
REM ================================================================

REM To use this logging framework in other batch files:
REM 1. call batch_logging.bat
REM 2. call :INIT_BATCH_LOGGING
REM 3. Use logging functions like:
REM    call :LOG_INFO "Your message"
REM    call :LOG_ERROR "Error message"
REM    call :EXECUTE_WITH_LOGGING "your_command" "Description"

if "%1"=="EXAMPLE" (
    call :INIT_BATCH_LOGGING
    call :LOG_INFO "This is an example info message"
    call :LOG_WARN "This is an example warning message"  
    call :LOG_ERROR "This is an example error message"
    call :LOG_DEBUG "This is an example debug message"
    call :EXECUTE_WITH_LOGGING "echo Hello World" "Test command execution"
)

REM Initialize logging when this file is called directly
if "%1"=="" (
    call :INIT_BATCH_LOGGING
)