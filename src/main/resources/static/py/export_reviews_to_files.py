import oracledb
import csv
import json
import os
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

def export_reviews_to_csv():
    """리뷰 데이터를 CSV 파일로 내보내기"""
    conn = connect_database()
    if not conn:
        return
    
    try:
        cursor = conn.cursor()
        
        # 리뷰 데이터 조회 (캠핑장 이름 포함)
        sql = """
        SELECT r.rev_id, r.content_id, c.facltNm, r.writer, r.content, r.score, r.created_at
        FROM review r
        LEFT JOIN camping_info c ON r.content_id = c.contentId
        ORDER BY r.rev_id
        """
        cursor.execute(sql)
        results = cursor.fetchall()
        
        if not results:
            print("❌ 내보낼 리뷰 데이터가 없습니다.")
            return
        
        # 출력 디렉토리 생성
        output_dir = "exported_data"
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"{output_dir}/reviews_export_{timestamp}.csv"
        
        with open(filename, 'w', newline='', encoding='utf-8-sig') as csvfile:
            fieldnames = ['rev_id', 'content_id', 'camping_name', 'writer', 'content', 'score', 'created_at']
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            
            for rev_id, content_id, camping_name, writer, content, score, created_at in results:
                camping_display = camping_name if camping_name else f"캠핑장ID_{content_id}"
                writer.writerow({
                    'rev_id': rev_id,
                    'content_id': content_id,
                    'camping_name': camping_display,
                    'writer': writer,
                    'content': content,
                    'score': score,
                    'created_at': created_at
                })
        
        print(f"📄 CSV 파일 내보내기 완료: {filename}")
        print(f"📊 총 {len(results)}개 리뷰 내보내기 완료")
        
    except Exception as e:
        print(f"❌ CSV 내보내기 실패: {e}")
    finally:
        conn.close()

def export_reviews_to_json():
    """리뷰 데이터를 JSON 파일로 내보내기"""
    conn = connect_database()
    if not conn:
        return
    
    try:
        cursor = conn.cursor()
        
        # 리뷰 데이터 조회
        sql = """
        SELECT r.rev_id, r.content_id, c.facltNm, r.writer, r.content, r.score, r.created_at
        FROM review r
        LEFT JOIN camping_info c ON r.content_id = c.contentId
        ORDER BY r.rev_id
        """
        cursor.execute(sql)
        results = cursor.fetchall()
        
        if not results:
            print("❌ 내보낼 리뷰 데이터가 없습니다.")
            return
        
        # 캠핑장별 통계 조회
        cursor.execute("""
        SELECT r.content_id, c.facltNm, COUNT(*) as review_count, AVG(r.score) as avg_score
        FROM review r
        LEFT JOIN camping_info c ON r.content_id = c.contentId
        GROUP BY r.content_id, c.facltNm
        ORDER BY review_count DESC
        """)
        camping_stats = cursor.fetchall()
        
        # 출력 디렉토리 생성
        output_dir = "exported_data"
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"{output_dir}/reviews_export_{timestamp}.json"
        
        # JSON 데이터 구조
        data = {
            'export_info': {
                'exported_at': datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                'total_reviews': len(results),
                'total_campings': len(camping_stats)
            },
            'camping_statistics': [],
            'reviews': []
        }
        
        # 캠핑장별 통계 추가
        for content_id, camping_name, review_count, avg_score in camping_stats:
            camping_display = camping_name if camping_name else f"캠핑장ID_{content_id}"
            data['camping_statistics'].append({
                'content_id': content_id,
                'camping_name': camping_display,
                'review_count': review_count,
                'average_score': float(avg_score) if avg_score else 0
            })
        
        # 리뷰 데이터 추가
        for rev_id, content_id, camping_name, writer, content, score, created_at in results:
            camping_display = camping_name if camping_name else f"캠핑장ID_{content_id}"
            data['reviews'].append({
                'rev_id': rev_id,
                'content_id': content_id,
                'camping_name': camping_display,
                'writer': writer,
                'content': content,
                'score': score,
                'created_at': str(created_at) if created_at else None
            })
        
        with open(filename, 'w', encoding='utf-8') as jsonfile:
            json.dump(data, jsonfile, ensure_ascii=False, indent=2)
        
        print(f"📄 JSON 파일 내보내기 완료: {filename}")
        print(f"📊 총 {len(results)}개 리뷰, {len(camping_stats)}개 캠핑장 내보내기 완료")
        
    except Exception as e:
        print(f"❌ JSON 내보내기 실패: {e}")
    finally:
        conn.close()

def export_by_camping():
    """캠핑장별로 개별 파일로 내보내기"""
    conn = connect_database()
    if not conn:
        return
    
    try:
        cursor = conn.cursor()
        
        # 캠핑장별 리뷰 조회
        sql = """
        SELECT r.content_id, c.facltNm, COUNT(*) as review_count
        FROM review r
        LEFT JOIN camping_info c ON r.content_id = c.contentId
        GROUP BY r.content_id, c.facltNm
        ORDER BY review_count DESC
        """
        cursor.execute(sql)
        camping_list = cursor.fetchall()
        
        if not camping_list:
            print("❌ 내보낼 캠핑장 데이터가 없습니다.")
            return
        
        # 출력 디렉토리 생성
        output_dir = "exported_data/by_camping"
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        for content_id, camping_name, review_count in camping_list:
            camping_display = camping_name if camping_name else f"캠핑장ID_{content_id}"
            
            # 해당 캠핑장의 리뷰 조회
            cursor.execute("""
            SELECT r.rev_id, r.writer, r.content, r.score, r.created_at
            FROM review r
            WHERE r.content_id = :content_id
            ORDER BY r.rev_id
            """, content_id=content_id)
            
            reviews = cursor.fetchall()
            
            # CSV 파일 저장
            csv_filename = f"{output_dir}/{camping_display}_{timestamp}.csv"
            with open(csv_filename, 'w', newline='', encoding='utf-8-sig') as csvfile:
                fieldnames = ['rev_id', 'writer', 'content', 'score', 'created_at']
                writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
                writer.writeheader()
                
                for rev_id, writer, content, score, created_at in reviews:
                    writer.writerow({
                        'rev_id': rev_id,
                        'writer': writer,
                        'content': content,
                        'score': score,
                        'created_at': created_at
                    })
            
            # JSON 파일 저장
            json_filename = f"{output_dir}/{camping_display}_{timestamp}.json"
            data = {
                'camping_info': {
                    'content_id': content_id,
                    'camping_name': camping_display,
                    'total_reviews': review_count,
                    'exported_at': datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                },
                'reviews': []
            }
            
            for rev_id, writer, content, score, created_at in reviews:
                data['reviews'].append({
                    'rev_id': rev_id,
                    'writer': writer,
                    'content': content,
                    'score': score,
                    'created_at': str(created_at) if created_at else None
                })
            
            with open(json_filename, 'w', encoding='utf-8') as jsonfile:
                json.dump(data, jsonfile, ensure_ascii=False, indent=2)
            
            print(f"📄 {camping_display}: {review_count}개 리뷰 내보내기 완료")
        
        print(f"📁 캠핑장별 파일 저장 위치: {output_dir}")
        
    except Exception as e:
        print(f"❌ 캠핑장별 내보내기 실패: {e}")
    finally:
        conn.close()

def main():
    """메인 함수"""
    print("📤 모닥불즈 리뷰 데이터 내보내기 도구")
    print("=" * 50)
    
    while True:
        print("\n📋 내보내기 옵션을 선택하세요:")
        print("1. 전체 리뷰를 CSV로 내보내기")
        print("2. 전체 리뷰를 JSON으로 내보내기")
        print("3. 캠핑장별로 개별 파일로 내보내기")
        print("4. 모든 형식으로 내보내기")
        print("0. 종료")
        
        choice = input("\n선택: ").strip()
        
        if choice == "1":
            export_reviews_to_csv()
        elif choice == "2":
            export_reviews_to_json()
        elif choice == "3":
            export_by_camping()
        elif choice == "4":
            print("\n" + "="*60)
            export_reviews_to_csv()
            export_reviews_to_json()
            export_by_camping()
            print("="*60)
            print("✅ 모든 형식으로 내보내기 완료!")
        elif choice == "0":
            print("👋 프로그램을 종료합니다.")
            break
        else:
            print("❌ 올바른 메뉴를 선택하세요.")

if __name__ == "__main__":
    main() 