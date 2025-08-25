import os
import sys
from datetime import datetime
from kakao_review_crawler_elasticsearch import KakaoMapReviewCrawler

def run_batch_crawling(start_batch=0, batch_size=100):
    """
    배치 단위로 캠핑장 리뷰 크롤링 실행
    
    Args:
        start_batch (int): 시작할 배치 번호 (0부터 시작)
        batch_size (int): 각 배치당 처리할 캠핑장 수
    """
    print(f"배치 크롤링 시작: 배치 {start_batch}, 배치 크기: {batch_size}")
    
    # 전체 캠핑장 수 확인
    crawler = KakaoMapReviewCrawler()
    camping_sites = crawler.read_camping_data_from_excel()
    
    if not camping_sites:
        print("캠핑장 데이터를 읽을 수 없습니다.")
        return
    
    total_campings = len(camping_sites)
    total_batches = (total_campings + batch_size - 1) // batch_size
    
    print(f"전체 캠핑장 수: {total_campings}")
    print(f"전체 배치 수: {total_batches}")
    
    # 시작 배치 계산
    start_index = start_batch * batch_size
    end_index = min(start_index + batch_size, total_campings)
    
    if start_index >= total_campings:
        print(f"시작 배치 {start_batch}가 전체 배치 수 {total_batches}를 초과합니다.")
        return
    
    print(f"처리할 범위: {start_index} ~ {end_index-1} (총 {end_index - start_index}개)")
    
    # 크롤링 실행
    crawler.run(start_index=start_index, end_index=end_index)
    
    print(f"배치 {start_batch} 완료!")

def run_all_batches(batch_size=100):
    """
    모든 배치를 순차적으로 실행
    """
    crawler = KakaoMapReviewCrawler()
    camping_sites = crawler.read_camping_data_from_excel()
    
    if not camping_sites:
        print("캠핑장 데이터를 읽을 수 없습니다.")
        return
    
    total_campings = len(camping_sites)
    total_batches = (total_campings + batch_size - 1) // batch_size
    
    print(f"전체 {total_batches}개 배치를 순차적으로 실행합니다.")
    print(f"각 배치당 {batch_size}개 캠핑장 처리")
    
    for batch_num in range(total_batches):
        print(f"\n{'='*50}")
        print(f"배치 {batch_num + 1}/{total_batches} 시작")
        print(f"{'='*50}")
        
        try:
            run_batch_crawling(start_batch=batch_num, batch_size=batch_size)
        except Exception as e:
            print(f"배치 {batch_num + 1} 실행 중 오류 발생: {e}")
            print("다음 배치로 진행합니다.")
            continue
        
        # 배치 간 휴식 (서버 부하 방지)
        if batch_num < total_batches - 1:
            print("다음 배치 시작 전 30초 대기...")
            import time
            time.sleep(30)
    
    print(f"\n{'='*50}")
    print("모든 배치 완료!")
    print(f"{'='*50}")

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description='카카오맵 리뷰 배치 크롤링')
    parser.add_argument('--batch', type=int, default=0, help='실행할 배치 번호 (0부터 시작)')
    parser.add_argument('--size', type=int, default=100, help='배치 크기 (기본값: 100)')
    parser.add_argument('--all', action='store_true', help='모든 배치를 순차적으로 실행')
    
    args = parser.parse_args()
    
    if args.all:
        run_all_batches(batch_size=args.size)
    else:
        run_batch_crawling(start_batch=args.batch, batch_size=args.size) 