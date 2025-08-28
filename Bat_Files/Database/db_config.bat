@echo off
REM ================================================================
REM Central Database Configuration
REM ================================================================
REM This file contains all database connection settings
REM All other batch files should call this file for configuration

REM Database Connection Settings
set PGHOST=localhost
set PGPORT=5432
set PGDATABASE=runelite_ai

REM Use environment variable if set, otherwise use default
if "%DB_USER%"=="" (
    set PGUSER=postgres
) else (
    set PGUSER=%DB_USER%
)

if "%DB_PASSWORD%"=="" (
    set PGPASSWORD=sam11773
) else (
    set PGPASSWORD=%DB_PASSWORD%
)

REM Path to SQL files - Using existing SQL directory
set SQL_PATH=%~dp0SQL

REM Verify SQL directory exists (should already exist)
if not exist "%SQL_PATH%" (
    echo WARNING: SQL directory not found at %SQL_PATH%
    REM Do not create - this should already exist
)

REM Try to find PostgreSQL installation
set PG_BIN_PATH=
set PSQL_FOUND=0

REM Check common PostgreSQL installation paths
for %%v in (17 16 15 14 13 12 11 10) do (
    if exist "C:\Program Files\PostgreSQL\%%v\bin\psql.exe" (
        set PG_BIN_PATH=C:\Program Files\PostgreSQL\%%v\bin
        set PSQL_FOUND=1
        goto :found_psql
    )
)

REM Check if psql is in PATH
where psql >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set PG_BIN_PATH=
    set PSQL_FOUND=1
    goto :found_psql
)

REM Check other common locations
if exist "C:\PostgreSQL\bin\psql.exe" (
    set PG_BIN_PATH=C:\PostgreSQL\bin
    set PSQL_FOUND=1
    goto :found_psql
)

:found_psql
if %PSQL_FOUND% EQU 0 (
    echo ERROR: PostgreSQL not found!
    echo Please ensure PostgreSQL is installed and add its bin directory to PATH.
    echo.
    echo Common locations:
    echo   C:\Program Files\PostgreSQL\[version]\bin
    echo.
    pause
    exit /b 1
)

REM psql common parameters
set PSQL_PARAMS=-h %PGHOST% -p %PGPORT% -d %PGDATABASE%

REM Set psql commands based on whether path is found
if "%PG_BIN_PATH%"=="" (
    set PSQL_ADMIN=psql -U %PGUSER% %PSQL_PARAMS%
    set PSQL_USER=psql -U runelite_user %PSQL_PARAMS%
    set PSQL_POSTGRES=psql -U %PGUSER% -h %PGHOST% -p %PGPORT% -d postgres
    set PG_DUMP=pg_dump
) else (
    set PSQL_ADMIN="%PG_BIN_PATH%\psql" -U %PGUSER% %PSQL_PARAMS%
    set PSQL_USER="%PG_BIN_PATH%\psql" -U runelite_user %PSQL_PARAMS%
    set PSQL_POSTGRES="%PG_BIN_PATH%\psql" -U %PGUSER% -h %PGHOST% -p %PGPORT% -d postgres
    set PG_DUMP="%PG_BIN_PATH%\pg_dump"
)

REM Display configuration (without password)
REM echo Database: %PGDATABASE% on %PGHOST%:%PGPORT%
REM echo User: %PGUSER%