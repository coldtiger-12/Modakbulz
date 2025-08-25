#!/usr/bin/env python3
"""
Elasticsearch 설정 및 테스트 스크립트
"""

import subprocess
import sys
import time
from elasticsearch import Elasticsearch

def install_elasticsearch():
    """Elasticsearch 설치 (Windows용)"""
    print("Elasticsearch 설치를 시작합니다...")
    
    try:
        # Chocolatey를 통한 Elasticsearch 설치
        subprocess.run(["choco", "install", "elasticsearch", "-y"], check=True)
        print("Elasticsearch 설치 완료!")
        return True
    except subprocess.CalledProcessError:
        print("Chocolatey를 통한 설치 실패. 수동 설치를 권장합니다.")
        print("1. https://www.elastic.co/downloads/elasticsearch 에서 다운로드")
        print("2. 압축 해제 후 bin/elasticsearch.bat 실행")
        return False
    except FileNotFoundError:
        print("Chocolatey가 설치되지 않았습니다.")
        print("수동 설치를 권장합니다:")
        print("1. https://www.elastic.co/downloads/elasticsearch 에서 다운로드")
        print("2. 압축 해제 후 bin/elasticsearch.bat 실행")
        return False

def check_elasticsearch_running():
    """Elasticsearch 실행 상태 확인"""
    try:
        es = Elasticsearch(['http://localhost:9200'])
        if es.ping():
            print("✅ Elasticsearch가 실행 중입니다!")
            return True
        else:
            print("❌ Elasticsearch 연결 실패")
            return False
    except Exception as e:
        print(f"❌ Elasticsearch 연결 오류: {e}")
        return False

def install_python_packages():
    """필요한 Python 패키지 설치"""
    print("Python 패키지 설치 중...")
    try:
        subprocess.run([sys.executable, "-m", "pip", "install", "-r", "requirements.txt"], check=True)
        print("✅ Python 패키지 설치 완료!")
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ Python 패키지 설치 실패: {e}")
        return False

def test_elasticsearch():
    """Elasticsearch 테스트"""
    print("Elasticsearch 테스트를 시작합니다...")
    
    try:
        es = Elasticsearch(['http://localhost:9200'])
        
        # 연결 테스트
        if not es.ping():
            print("❌ Elasticsearch 연결 실패")
            return False
        
        print("✅ Elasticsearch 연결 성공!")
        
        # 인덱스 생성 테스트
        test_index = "test_reviews"
        if not es.indices.exists(index=test_index):
            mapping = {
                "mappings": {
                    "properties": {
                        "content": {"type": "text"},
                        "score": {"type": "integer"}
                    }
                }
            }
            es.indices.create(index=test_index, body=mapping)
            print(f"✅ 테스트 인덱스 '{test_index}' 생성 완료")
        
        # 문서 저장 테스트
        test_doc = {
            "content": "테스트 리뷰입니다.",
            "score": 5
        }
        response = es.index(index=test_index, body=test_doc)
        print(f"✅ 테스트 문서 저장 완료: {response['result']}")
        
        # 인덱스 새로고침
        es.indices.refresh(index=test_index)
        
        # 검색 테스트
        search_result = es.search(index=test_index, body={"query": {"match": {"content": "테스트"}}})
        print(f"✅ 검색 테스트 완료: {len(search_result['hits']['hits'])}개 결과")
        
        # 테스트 인덱스 삭제
        es.indices.delete(index=test_index)
        print(f"✅ 테스트 인덱스 '{test_index}' 삭제 완료")
        
        print("🎉 Elasticsearch 테스트 완료!")
        return True
        
    except Exception as e:
        print(f"❌ Elasticsearch 테스트 실패: {e}")
        return False

def main():
    """메인 함수"""
    print("=" * 50)
    print("Elasticsearch 설정 및 테스트")
    print("=" * 50)
    
    # 1. Python 패키지 설치
    if not install_python_packages():
        print("Python 패키지 설치 실패로 종료합니다.")
        return
    
    # 2. Elasticsearch 실행 상태 확인
    if not check_elasticsearch_running():
        print("\nElasticsearch가 실행되지 않았습니다.")
        print("다음 중 하나를 선택하세요:")
        print("1. Elasticsearch 수동 설치")
        print("2. Docker를 통한 Elasticsearch 실행")
        print("3. 종료")
        
        choice = input("선택 (1-3): ").strip()
        
        if choice == "1":
            install_elasticsearch()
        elif choice == "2":
            print("Docker 명령어:")
            print("docker run -d --name elasticsearch -p 9200:9200 -p 9300:9300 -e \"discovery.type=single-node\" elasticsearch:8.11.0")
        else:
            print("설정을 종료합니다.")
            return
    
    # 3. Elasticsearch 테스트
    if check_elasticsearch_running():
        test_elasticsearch()
    
    print("\n" + "=" * 50)
    print("설정 완료!")
    print("이제 kakao_review_crawler.py를 실행할 수 있습니다.")
    print("=" * 50)

if __name__ == "__main__":
    main() 