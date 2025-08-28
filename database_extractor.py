#!/usr/bin/env python3
"""
RuneLiteAI Database Extractor
Extracts all data from PostgreSQL database into structured folders and files
"""

import os
import psycopg2
import json
import csv
from datetime import datetime
import pandas as pd

class DatabaseExtractor:
    def __init__(self):
        self.conn_params = {
            'host': 'localhost',
            'port': '5432',
            'database': 'runelite_ai',
            'user': 'postgres',
            'password': 'sam11773'
        }
        self.output_dir = 'database_export'
        self.conn = None
        
    def connect(self):
        """Connect to PostgreSQL database"""
        try:
            self.conn = psycopg2.connect(**self.conn_params)
            print(f"[OK] Connected to database: {self.conn_params['database']}")
        except Exception as e:
            print(f"[ERROR] Database connection failed: {e}")
            raise
            
    def create_output_structure(self):
        """Create output directory structure"""
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        self.output_dir = f'database_export_{timestamp}'
        
        directories = [
            self.output_dir,
            f'{self.output_dir}/csv',
            f'{self.output_dir}/json',
            f'{self.output_dir}/summary',
            f'{self.output_dir}/raw_sql'
        ]
        
        for directory in directories:
            os.makedirs(directory, exist_ok=True)
            
        print(f"[INFO] Created export directory: {self.output_dir}")
        
    def get_all_tables(self):
        """Get list of all tables in the database"""
        cursor = self.conn.cursor()
        cursor.execute("""
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_schema = 'public' 
            AND table_name NOT LIKE 'pg_%'
            ORDER BY table_name;
        """)
        tables = [row[0] for row in cursor.fetchall()]
        cursor.close()
        print(f"[INFO] Found {len(tables)} tables: {', '.join(tables)}")
        return tables
        
    def get_table_info(self, table_name):
        """Get table structure and row count"""
        cursor = self.conn.cursor()
        
        # Get column info
        cursor.execute("""
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns 
            WHERE table_name = %s 
            ORDER BY ordinal_position;
        """, (table_name,))
        columns = cursor.fetchall()
        
        # Get row count
        cursor.execute(f"SELECT COUNT(*) FROM {table_name}")
        row_count = cursor.fetchone()[0]
        
        cursor.close()
        return {
            'columns': columns,
            'row_count': row_count
        }
        
    def export_table_to_csv(self, table_name):
        """Export table data to CSV"""
        try:
            cursor = self.conn.cursor()
            cursor.execute(f"SELECT * FROM {table_name}")
            
            # Get column names
            column_names = [desc[0] for desc in cursor.description]
            
            # Write CSV file
            csv_file = f'{self.output_dir}/csv/{table_name}.csv'
            with open(csv_file, 'w', newline='', encoding='utf-8') as f:
                writer = csv.writer(f)
                writer.writerow(column_names)
                writer.writerows(cursor.fetchall())
                
            cursor.close()
            print(f"[CSV] Exported {table_name} to CSV")
            return True
            
        except Exception as e:
            print(f"[WARN] Failed to export {table_name} to CSV: {e}")
            return False
            
    def export_table_to_json(self, table_name, limit=1000):
        """Export table data to JSON (limited records for readability)"""
        try:
            cursor = self.conn.cursor()
            cursor.execute(f"SELECT * FROM {table_name} LIMIT {limit}")
            
            # Get column names
            column_names = [desc[0] for desc in cursor.description]
            
            # Convert to list of dictionaries
            rows = cursor.fetchall()
            data = []
            for row in rows:
                row_dict = {}
                for i, value in enumerate(row):
                    # Handle datetime and other non-serializable types
                    if hasattr(value, 'isoformat'):
                        row_dict[column_names[i]] = value.isoformat()
                    else:
                        row_dict[column_names[i]] = value
                data.append(row_dict)
            
            # Write JSON file
            json_file = f'{self.output_dir}/json/{table_name}.json'
            with open(json_file, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=2, default=str)
                
            cursor.close()
            print(f"[JSON] Exported {table_name} to JSON ({len(data)} records)")
            return True
            
        except Exception as e:
            print(f"[WARN] Failed to export {table_name} to JSON: {e}")
            return False
            
    def generate_summary_report(self, tables_info):
        """Generate summary report of database contents"""
        summary = {
            'export_timestamp': datetime.now().isoformat(),
            'database_info': self.conn_params['database'],
            'total_tables': len(tables_info),
            'tables': {}
        }
        
        total_rows = 0
        for table_name, info in tables_info.items():
            summary['tables'][table_name] = {
                'row_count': info['row_count'],
                'column_count': len(info['columns']),
                'columns': [{'name': col[0], 'type': col[1], 'nullable': col[2]} 
                           for col in info['columns']]
            }
            total_rows += info['row_count']
            
        summary['total_rows'] = total_rows
        
        # Write summary JSON
        summary_file = f'{self.output_dir}/summary/database_summary.json'
        with open(summary_file, 'w', encoding='utf-8') as f:
            json.dump(summary, f, indent=2)
            
        # Write readable summary
        readable_file = f'{self.output_dir}/summary/database_summary.txt'
        with open(readable_file, 'w', encoding='utf-8') as f:
            f.write(f"RuneLiteAI Database Export Summary\n")
            f.write(f"{'='*50}\n\n")
            f.write(f"Export Time: {summary['export_timestamp']}\n")
            f.write(f"Database: {summary['database_info']}\n")
            f.write(f"Total Tables: {summary['total_tables']}\n")
            f.write(f"Total Rows: {summary['total_rows']:,}\n\n")
            
            for table_name, info in summary['tables'].items():
                f.write(f"Table: {table_name}\n")
                f.write(f"  Rows: {info['row_count']:,}\n")
                f.write(f"  Columns: {info['column_count']}\n")
                for col in info['columns']:
                    f.write(f"    - {col['name']} ({col['type']})\n")
                f.write("\n")
                
        print(f"[SUMMARY] Generated summary reports")
        
    def export_sample_queries(self):
        """Export sample SQL queries for analysis"""
        queries = {
            'sessions_overview': """
                SELECT session_id, player_name, start_time, end_time,
                       EXTRACT(EPOCH FROM (end_time - start_time))/60 as duration_minutes
                FROM sessions 
                ORDER BY start_time DESC 
                LIMIT 10;
            """,
            'tick_performance': """
                SELECT session_id, 
                       AVG(processing_time_ms) as avg_processing_ms,
                       COUNT(*) as tick_count,
                       MIN(processing_time_ms) as min_processing_ms,
                       MAX(processing_time_ms) as max_processing_ms
                FROM game_ticks 
                GROUP BY session_id 
                ORDER BY avg_processing_ms DESC;
            """,
            'player_locations': """
                SELECT world_x, world_y, plane, COUNT(*) as frequency
                FROM player_location 
                GROUP BY world_x, world_y, plane 
                ORDER BY frequency DESC 
                LIMIT 20;
            """,
            'recent_activity': """
                SELECT gt.session_id, gt.tick_number, gt.timestamp, gt.processing_time_ms,
                       pl.world_x, pl.world_y, pl.plane
                FROM game_ticks gt
                LEFT JOIN player_location pl ON gt.session_id = pl.session_id 
                    AND gt.tick_number = pl.tick_number
                ORDER BY gt.timestamp DESC 
                LIMIT 100;
            """
        }
        
        results_file = f'{self.output_dir}/summary/sample_queries_results.txt'
        with open(results_file, 'w', encoding='utf-8') as f:
            f.write("RuneLiteAI Database Sample Query Results\n")
            f.write("="*50 + "\n\n")
            
            cursor = self.conn.cursor()
            for query_name, query_sql in queries.items():
                try:
                    f.write(f"Query: {query_name}\n")
                    f.write("-" * 30 + "\n")
                    f.write(f"SQL: {query_sql.strip()}\n\n")
                    
                    cursor.execute(query_sql)
                    results = cursor.fetchall()
                    column_names = [desc[0] for desc in cursor.description]
                    
                    f.write(f"Results ({len(results)} rows):\n")
                    f.write(" | ".join(column_names) + "\n")
                    f.write("-" * 80 + "\n")
                    
                    for row in results:
                        f.write(" | ".join(str(item) for item in row) + "\n")
                    
                    f.write("\n" + "="*80 + "\n\n")
                    
                except Exception as e:
                    f.write(f"Error executing query: {e}\n\n")
                    
            cursor.close()
            
        print(f"[QUERY] Generated sample query results")
        
    def run_extraction(self):
        """Run complete database extraction"""
        print("Starting RuneLiteAI Database Extraction")
        print("-" * 50)
        
        # Connect to database
        self.connect()
        
        # Create output structure
        self.create_output_structure()
        
        # Get all tables
        tables = self.get_all_tables()
        
        # Collect table information
        tables_info = {}
        for table in tables:
            info = self.get_table_info(table)
            tables_info[table] = info
            print(f"[TABLE] {table}: {info['row_count']:,} rows, {len(info['columns'])} columns")
            
        print("\n" + "="*50)
        print("Exporting Data...")
        print("="*50)
        
        # Export each table
        for table in tables:
            print(f"\nProcessing table: {table}")
            self.export_table_to_csv(table)
            self.export_table_to_json(table)
            
        # Generate summary
        self.generate_summary_report(tables_info)
        
        # Export sample queries
        self.export_sample_queries()
        
        # Close connection
        self.conn.close()
        
        print("\n" + "="*50)
        print("[SUCCESS] Database extraction completed!")
        print(f"[INFO] Results saved to: {self.output_dir}")
        print("="*50)
        
        # Print directory contents
        print(f"\nGenerated files:")
        for root, dirs, files in os.walk(self.output_dir):
            level = root.replace(self.output_dir, '').count(os.sep)
            indent = ' ' * 2 * level
            print(f'{indent}{os.path.basename(root)}/')
            subindent = ' ' * 2 * (level + 1)
            for file in files:
                file_path = os.path.join(root, file)
                file_size = os.path.getsize(file_path)
                if file_size > 1024*1024:  # MB
                    size_str = f"{file_size/(1024*1024):.1f}MB"
                elif file_size > 1024:  # KB
                    size_str = f"{file_size/1024:.1f}KB"
                else:
                    size_str = f"{file_size}B"
                print(f'{subindent}{file} ({size_str})')

if __name__ == "__main__":
    extractor = DatabaseExtractor()
    try:
        extractor.run_extraction()
    except KeyboardInterrupt:
        print("\n[WARN] Extraction cancelled by user")
    except Exception as e:
        print(f"\n[ERROR] Extraction failed: {e}")