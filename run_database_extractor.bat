@echo off
echo RuneLiteAI Database Extractor
echo ============================
echo.

REM Check if Python is available
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Python not found in PATH
    echo Please install Python 3.7+ and add to PATH
    pause
    exit /b 1
)

echo Installing required Python packages...
pip install -r requirements.txt

echo.
echo Starting database extraction...
echo.

python database_extractor.py

echo.
echo Extraction completed. Check the generated database_export_* folder.
pause