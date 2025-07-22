import oracledb

def check_reviews():
    """수집된 리뷰 데이터 확인"""
    try:
        # Oracle DB 연결
        conn = oracledb.connect(
            user='c##camp',
            password='camp1234',
            dsn='localhost:1521/xe'
        )
        
        cursor = conn.cursor()
        
        # 총 리뷰 수 확인
        cursor.execute("SELECT COUNT(*) FROM review")
        total_count = cursor.fetchone()[0]
        print(f"📊 총 수집된 리뷰 수: {total_count}개")
        
        if total_count > 0:
            # 최근 10개 리뷰 확인
            cursor.execute("""
                SELECT content_id, writer, content, score, created_at 
                FROM review 
                ORDER BY rev_id DESC
            """)
            results = cursor.fetchall()
            
            print(f"\n📝 최근 {min(10, len(results))}개 리뷰:")
            print("-" * 80)
            
            for i, (content_id, writer, content, score, created_at) in enumerate(results[:10], 1):
                print(f"{i}. 캠핑장ID: {content_id}")
                print(f"   작성자: {writer}")
                print(f"   평점: {'⭐' * score}")
                print(f"   내용: {content[:100]}...")
                print(f"   작성일: {created_at}")
                print("-" * 80)
            
            # 평점별 통계
            cursor.execute("""
                SELECT score, COUNT(*) 
                FROM review 
                GROUP BY score 
                ORDER BY score DESC
            """)
            score_stats = cursor.fetchall()
            
            print(f"\n📈 평점별 통계:")
            for score, count in score_stats:
                print(f"   {score}점: {count}개 ({count/total_count*100:.1f}%)")
            
            # 캠핑장별 리뷰 수
            cursor.execute("""
                SELECT content_id, COUNT(*) 
                FROM review 
                GROUP BY content_id 
                ORDER BY COUNT(*) DESC
            """)
            camping_stats = cursor.fetchall()
            
            print(f"\n🏕️ 캠핑장별 리뷰 수 (상위 10개):")
            for content_id, count in camping_stats[:10]:
                print(f"   캠핑장ID {content_id}: {count}개")
        
        else:
            print("❌ 아직 수집된 리뷰가 없습니다.")
            print("크롤러가 실행 중이거나 아직 완료되지 않았을 수 있습니다.")
        
        conn.close()
        
    except Exception as e:
        print(f"❌ 데이터베이스 연결 오류: {e}")
        print("Oracle DB 서버가 실행 중인지 확인해주세요.")

if __name__ == "__main__":
    check_reviews() 