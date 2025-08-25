import oracledb
from datetime import datetime

def connect_database():
    """데이터베이스 연결"""
    try:
        connection = oracledb.connect(
            user='c##camp',
            password='camp1234',
            dsn='localhost:1521/xe'
        )
        print("✅ 데이터베이스 연결 성공!")
        return connection
    except Exception as e:
        print(f"❌ 데이터베이스 연결 실패: {e}")
        return None

def check_total_reviews():
    """총 리뷰 수 확인"""
    conn = connect_database()
    if not conn:
        return
    
    try:
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM review")
        total_count = cursor.fetchone()[0]
        print(f"\n📊 총 수집된 리뷰 수: {total_count}개")
        return total_count
    except Exception as e:
        print(f"❌ 리뷰 수 조회 실패: {e}")
        return 0
    finally:
        conn.close()

def check_recent_reviews(limit=10):
    """최근 리뷰 확인"""
    conn = connect_database()
    if not conn:
        return
    
    try:
        cursor = conn.cursor()
        sql = """
        SELECT r.rev_id, r.content_id, c.facltNm, r.writer, r.score, r.created_at
        FROM review r
        LEFT JOIN camping_info c ON r.content_id = c.contentId
        ORDER BY r.rev_id DESC
        """
        cursor.execute(sql)
        results = cursor.fetchall()
        
        print(f"\n📝 최근 {min(limit, len(results))}개 리뷰:")
        print("=" * 80)
        
        for i, (rev_id, content_id, camping_name, writer, score, created_at) in enumerate(results[:limit], 1):
            stars = "⭐" * score
            camping_display = camping_name if camping_name else f"캠핑장ID: {content_id}"
            print(f"{i}. 리뷰ID: {rev_id}")
            print(f"   캠핑장: {camping_display}")
            print(f"   작성자: {writer}")
            print(f"   평점: {stars} ({score}점)")
            print(f"   작성일: {created_at}")
            print("-" * 80)
            
    except Exception as e:
        print(f"❌ 최근 리뷰 조회 실패: {e}")
    finally:
        conn.close()

def check_camping_reviews():
    """캠핑장별 리뷰 통계"""
    conn = connect_database()
    if not conn:
        return
    
    try:
        cursor = conn.cursor()
        sql = """
        SELECT r.content_id, c.facltNm, COUNT(*) as review_count,
               AVG(r.score) as avg_score, MIN(r.score) as min_score, MAX(r.score) as max_score
        FROM review r
        LEFT JOIN camping_info c ON r.content_id = c.contentId
        GROUP BY r.content_id, c.facltNm
        ORDER BY review_count DESC
        """
        cursor.execute(sql)
        results = cursor.fetchall()
        
        print(f"\n🏕️ 캠핑장별 리뷰 통계:")
        print("=" * 80)
        
        for content_id, camping_name, count, avg_score, min_score, max_score in results:
            camping_display = camping_name if camping_name else f"캠핑장ID: {content_id}"
            avg_display = f"{avg_score:.1f}" if avg_score else "N/A"
            print(f"📌 {camping_display}")
            print(f"   리뷰 수: {count}개")
            print(f"   평균 평점: {avg_display}점")
            print(f"   최저 평점: {min_score}점")
            print(f"   최고 평점: {max_score}점")
            print("-" * 80)
            
    except Exception as e:
        print(f"❌ 캠핑장별 리뷰 통계 조회 실패: {e}")
    finally:
        conn.close()

def check_score_distribution():
    """평점별 분포 확인"""
    conn = connect_database()
    if not conn:
        return
    
    try:
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM review")
        total_count = cursor.fetchone()[0]
        
        if total_count == 0:
            print("\n❌ 수집된 리뷰가 없습니다.")
            return
        
        sql = """
        SELECT score, COUNT(*) as count
        FROM review
        GROUP BY score
        ORDER BY score DESC
        """
        cursor.execute(sql)
        results = cursor.fetchall()
        
        print(f"\n⭐ 평점별 분포:")
        print("=" * 50)
        
        for score, count in results:
            percentage = (count / total_count) * 100
            stars = "⭐" * score
            print(f"{stars} {score}점: {count}개 ({percentage:.1f}%)")
            
    except Exception as e:
        print(f"❌ 평점별 분포 조회 실패: {e}")
    finally:
        conn.close()

def check_review_details(rev_id=None):
    """특정 리뷰 상세 정보 확인"""
    conn = connect_database()
    if not conn:
        return
    
    try:
        cursor = conn.cursor()
        
        if rev_id:
            # 특정 리뷰 조회
            sql = """
            SELECT r.rev_id, r.content_id, c.facltNm, r.writer, r.content, r.score, r.created_at
            FROM review r
            LEFT JOIN camping_info c ON r.content_id = c.contentId
            WHERE r.rev_id = :rev_id
            """
            cursor.execute(sql, rev_id=rev_id)
        else:
            # 최신 리뷰 1개 조회
            sql = """
            SELECT r.rev_id, r.content_id, c.facltNm, r.writer, r.content, r.score, r.created_at
            FROM review r
            LEFT JOIN camping_info c ON r.content_id = c.contentId
            ORDER BY r.rev_id DESC
            """
            cursor.execute(sql)
        
        result = cursor.fetchone()
        
        if result:
            rev_id, content_id, camping_name, writer, content, score, created_at = result
            camping_display = camping_name if camping_name else f"캠핑장ID: {content_id}"
            stars = "⭐" * score
            
            print(f"\n📋 리뷰 상세 정보 (ID: {rev_id}):")
            print("=" * 80)
            print(f"캠핑장: {camping_display}")
            print(f"작성자: {writer}")
            print(f"평점: {stars} ({score}점)")
            print(f"작성일: {created_at}")
            print(f"내용: {content}")
            print("=" * 80)
        else:
            print("❌ 해당 리뷰를 찾을 수 없습니다.")
            
    except Exception as e:
        print(f"❌ 리뷰 상세 정보 조회 실패: {e}")
    finally:
        conn.close()

def check_database_summary():
    """데이터베이스 전체 요약"""
    conn = connect_database()
    if not conn:
        return
    
    try:
        cursor = conn.cursor()
        
        # 각 테이블의 레코드 수 확인
        tables = ['member', 'camping_info', 'campsites', 'review']
        print(f"\n📈 데이터베이스 전체 현황:")
        print("=" * 50)
        
        for table in tables:
            try:
                cursor.execute(f"SELECT COUNT(*) FROM {table}")
                count = cursor.fetchone()[0]
                print(f"{table.upper()}: {count}개 레코드")
            except Exception as e:
                print(f"{table.upper()}: 조회 실패 - {e}")
        
        # 리뷰 관련 추가 통계
        cursor.execute("SELECT COUNT(DISTINCT content_id) FROM review")
        unique_campings = cursor.fetchone()[0]
        
        cursor.execute("SELECT COUNT(DISTINCT writer) FROM review")
        unique_writers = cursor.fetchone()[0]
        
        print(f"\n📊 리뷰 관련 통계:")
        print(f"리뷰가 있는 캠핑장 수: {unique_campings}개")
        print(f"리뷰 작성자 수: {unique_writers}명")
        
    except Exception as e:
        print(f"❌ 데이터베이스 요약 조회 실패: {e}")
    finally:
        conn.close()

def main():
    """메인 함수"""
    print("🔍 모닥불즈 리뷰 데이터베이스 확인 도구")
    print("=" * 50)
    
    while True:
        print("\n📋 메뉴를 선택하세요:")
        print("1. 총 리뷰 수 확인")
        print("2. 최근 리뷰 목록 (10개)")
        print("3. 캠핑장별 리뷰 통계")
        print("4. 평점별 분포 확인")
        print("5. 특정 리뷰 상세 정보")
        print("6. 데이터베이스 전체 요약")
        print("7. 모든 정보 한번에 확인")
        print("0. 종료")
        
        choice = input("\n선택: ").strip()
        
        if choice == "1":
            check_total_reviews()
        elif choice == "2":
            check_recent_reviews()
        elif choice == "3":
            check_camping_reviews()
        elif choice == "4":
            check_score_distribution()
        elif choice == "5":
            rev_id = input("확인할 리뷰 ID (엔터시 최신 리뷰): ").strip()
            if rev_id:
                try:
                    check_review_details(int(rev_id))
                except ValueError:
                    print("❌ 올바른 리뷰 ID를 입력하세요.")
            else:
                check_review_details()
        elif choice == "6":
            check_database_summary()
        elif choice == "7":
            print("\n" + "="*60)
            check_total_reviews()
            check_recent_reviews(5)  # 최근 5개만
            check_camping_reviews()
            check_score_distribution()
            check_database_summary()
            print("="*60)
        elif choice == "0":
            print("👋 프로그램을 종료합니다.")
            break
        else:
            print("❌ 올바른 메뉴를 선택하세요.")

if __name__ == "__main__":
    main() 