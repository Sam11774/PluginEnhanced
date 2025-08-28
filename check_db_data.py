#!/usr/bin/env python3
"""Quick database check script"""

import psycopg2

def check_database():
    conn = psycopg2.connect(
        host='localhost',
        port='5432',
        database='runelite_ai',
        user='postgres',
        password='sam11773'
    )
    
    cursor = conn.cursor()
    
    # Check sessions
    cursor.execute("SELECT COUNT(*) FROM sessions")
    session_count = cursor.fetchone()[0]
    print(f"Sessions: {session_count}")
    
    # Check game_ticks
    cursor.execute("SELECT COUNT(*) FROM game_ticks")
    tick_count = cursor.fetchone()[0]
    print(f"Game ticks: {tick_count}")
    
    # Get latest session details
    cursor.execute("""
        SELECT session_id, player_name, start_time, status 
        FROM sessions 
        ORDER BY session_id DESC 
        LIMIT 5
    """)
    sessions = cursor.fetchall()
    
    if sessions:
        print("\nLatest sessions:")
        for session in sessions:
            print(f"  ID: {session[0]}, Player: {session[1]}, Start: {session[2]}, Status: {session[3]}")
    
    # Check if session 1 has any ticks
    if session_count > 0:
        cursor.execute("SELECT COUNT(*) FROM game_ticks WHERE session_id = 1")
        session1_ticks = cursor.fetchone()[0]
        print(f"\nSession 1 tick count: {session1_ticks}")
    
    cursor.close()
    conn.close()

if __name__ == "__main__":
    check_database()