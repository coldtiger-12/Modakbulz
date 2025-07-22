import oracledb
import time
from datetime import datetime

def monitor_reviews():
    """실시간 리뷰 수집 현황 모니터링"""
    print("🔍 리뷰 수집 현황 모니터링을 시작합니다...")
    print("Ctrl+C를 눌러서 종료할 수 있습니다.")
    print("-" * 60)
    
    prev_count = 0
    
    try:
        while True:
            try:
                # Oracle DB 연결
                conn = oracledb.connect(
                    user='c##camp',
                    password='camp1234',
                    dsn='localhost:1521/xe'
                )
                
                cursor = conn.cursor()
                
                # 현재 시간
                current_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                
                # 총 리뷰 수 확인
                cursor.execute("SELECT COUNT(*) FROM review")
                current_count = cursor.fetchone()[0]
                
                # 증가량 계산
                increase = current_count - prev_count
                
                print(f"[{current_time}] 📊 총 리뷰 수: {current_count}개", end="")
                if increase > 0:
                    print(f" (+{increase}개 증가)")
                else:
                    print()
                
                # 평점별 통계 (10초마다)
                if current_count > 0 and current_count % 10 == 0:
                    cursor.execute("""
                        SELECT score, COUNT(*) 
                        FROM review 
                        GROUP BY score 
                        ORDER BY score DESC
                    """)
                    score_stats = cursor.fetchall()
                    
                    print("   📈 평점별 분포:")
                    for score, count in score_stats:
                        percentage = (count / current_count) * 100
                        stars = "⭐" * score
                        print(f"      {stars} {score}점: {count}개 ({percentage:.1f}%)")
                
                conn.close()
                prev_count = current_count
                
                # 5초 대기
                time.sleep(5)
                
            except Exception as e:
                print(f"[{current_time}] ❌ 오류: {e}")
                time.sleep(10)
                
    except KeyboardInterrupt:
        print("\n\n🛑 모니터링을 종료합니다.")
        print(f"최종 수집된 리뷰 수: {current_count}개")

if __name__ == "__main__":
    monitor_reviews() 