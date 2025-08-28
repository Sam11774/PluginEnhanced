@echo off
echo ================================================================
echo                  DATABASE CONTENT VERIFICATION
echo ================================================================
echo.

REM Load database configuration  
call "%~dp0Database\db_config.bat"

echo Recent Sessions:
psql -U postgres -h localhost -p 5432 -d runelite_ai -c "SELECT session_id, player_name, activity_type, start_time, total_ticks FROM sessions ORDER BY session_id DESC LIMIT 3;"

echo.
echo Game Ticks Count by Session:
psql -U postgres -h localhost -p 5432 -d runelite_ai -c "SELECT session_id, COUNT(*) as tick_records FROM game_ticks GROUP BY session_id ORDER BY session_id DESC LIMIT 3;"

echo.
echo Player Location Data Count:
psql -U postgres -h localhost -p 5432 -d runelite_ai -c "SELECT session_id, COUNT(*) as location_records FROM player_location GROUP BY session_id ORDER BY session_id DESC LIMIT 3;"

echo.
echo All Tables with Data:
psql -U postgres -h localhost -p 5432 -d runelite_ai -c "SELECT schemaname, tablename, n_tup_ins as records FROM pg_stat_user_tables WHERE n_tup_ins > 0 ORDER BY n_tup_ins DESC;"

echo.
echo ================================================================
pause