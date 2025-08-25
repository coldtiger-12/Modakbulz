#!/usr/bin/env python3
"""
카카오맵 리뷰 크롤링 테스트 스크립트
"""

from kakao_review_crawler import KakaoMapReviewCrawler
from elasticsearch import Elasticsearch
import json

def test_elasticsearch_connection():
    """Elasticsearch 연결 테스트"""
    print("🔍 Elasticsearch 연결 테스트...")
    try:
        es = Elasticsearch(['http://localhost:9200'])
        if es.ping():
            print("✅ Elasticsearch 연결 성공!")
            
            # 인덱스 정보 확인
            indices = es.indices.get_alias().keys()
            print(f"📊 현재 인덱스: {list(indices)}")
            
            # reviews 인덱스가 있으면 문서 수 확인
            if 'reviews' in indices:
                count = es.count(index='reviews')['count']
                print(f"📝 reviews 인덱스 문서 수: {count}")
            
            return True
        else:
            print("❌ Elasticsearch 연결 실패!")
            return False
    except Exception as e:
        print(f"❌ Elasticsearch 연결 오류: {e}")
        return False

def test_single_camping():
    """단일 캠핑장 크롤링 테스트"""
    print("\n🎯 단일 캠핑장 크롤링 테스트...")
    
    crawler = KakaoMapReviewCrawler()
    
    # Elasticsearch 연결 확인
    if not crawler.check_elasticsearch_connection():
        print("❌ Elasticsearch 연결 실패로 테스트를 중단합니다.")
        return False
    
    try:
        # 테스트용 캠핑장 데이터
        test_camping = (1, "영도마리노오토캠핑장", "부산시 영도구")
        camping_id, camping_name, address = test_camping
        
        print(f"📍 테스트 캠핑장: {camping_name}")
        
        # 카카오맵에서 캠핑장 검색
        place_url = crawler.search_kakao_place(camping_name)
        if not place_url:
            print("❌ 캠핑장 검색 실패")
            return False
        
        print(f"✅ 캠핑장 검색 성공: {place_url}")
        
        # 크롬드라이버 설정
        from selenium import webdriver
        from selenium.webdriver.chrome.service import Service
        from webdriver_manager.chrome import ChromeDriverManager
        
        service = Service(ChromeDriverManager().install())
        crawler.driver = webdriver.Chrome(service=service, options=crawler.chrome_options)
        print("✅ 크롬드라이버 설정 완료")
        
        # 리뷰 추출
        reviews = crawler.extract_reviews(place_url)
        if not reviews:
            print("❌ 리뷰 추출 실패")
            return False
        
        print(f"✅ {len(reviews)}개 리뷰 추출 성공")
        
        # Elasticsearch에 저장
        crawler.save_reviews(camping_id, camping_name, reviews)
        
        # 결과 확인
        es = Elasticsearch(['http://localhost:9200'])
        es.indices.refresh(index='reviews')
        
        # 저장된 리뷰 검색
        search_result = es.search(
            index='reviews',
            body={
                "query": {
                    "match": {
                        "campingName": camping_name
                    }
                },
                "size": 10
            }
        )
        
        print(f"📊 Elasticsearch에 저장된 리뷰: {len(search_result['hits']['hits'])}개")
        
        # 첫 번째 리뷰 내용 출력
        if search_result['hits']['hits']:
            first_review = search_result['hits']['hits'][0]['_source']
            print(f"📝 첫 번째 리뷰:")
            print(f"   작성자: {first_review.get('writer', 'N/A')}")
            print(f"   평점: {first_review.get('score', 'N/A')}")
            print(f"   내용: {first_review.get('content', 'N/A')[:100]}...")
        
        return True
        
    except Exception as e:
        print(f"❌ 테스트 중 오류 발생: {e}")
        return False
    finally:
        if hasattr(crawler, 'driver') and crawler.driver:
            crawler.driver.quit()

def main():
    """메인 테스트 함수"""
    print("=" * 60)
    print("카카오맵 리뷰 크롤링 테스트")
    print("=" * 60)
    
    # 1. Elasticsearch 연결 테스트
    if not test_elasticsearch_connection():
        print("\n❌ Elasticsearch 연결 실패!")
        print("Elasticsearch를 먼저 실행해주세요.")
        print("Docker 명령어: docker run -d --name elasticsearch -p 9200:9200 -p 9300:9300 -e \"discovery.type=single-node\" elasticsearch:8.11.0")
        return
    
    # 2. 단일 캠핑장 크롤링 테스트
    if test_single_camping():
        print("\n🎉 테스트 완료!")
        print("이제 전체 크롤링을 실행할 수 있습니다:")
        print("python kakao_review_crawler.py")
    else:
        print("\n❌ 테스트 실패!")
        print("문제를 해결한 후 다시 시도해주세요.")

if __name__ == "__main__":
    main() 