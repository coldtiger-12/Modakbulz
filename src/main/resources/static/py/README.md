# 카카오맵 리뷰 크롤링 (Elasticsearch 저장)

이 스크립트는 카카오맵에서 캠핑장 리뷰를 크롤링하여 Elasticsearch에 저장하는 프로그램입니다.

## 📋 요구사항

- Python 3.7+
- Elasticsearch 8.x
- Chrome 브라우저
- ChromeDriver (자동 설치됨)

## 🚀 설치 및 설정

### 1. Python 패키지 설치

```bash
pip install -r requirements.txt
```

### 2. Elasticsearch 설치 및 실행

#### 방법 1: Docker 사용 (권장)
```bash
docker run -d --name elasticsearch -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch:8.11.0
```

#### 방법 2: 수동 설치
1. [Elasticsearch 다운로드](https://www.elastic.co/downloads/elasticsearch)
2. 압축 해제 후 `bin/elasticsearch.bat` 실행

### 3. 설정 테스트

```bash
python setup_elasticsearch.py
```

## 🎯 사용 방법

### 기본 크롤링 실행

```bash
python kakao_review_crawler.py
```

### Elasticsearch 전용 크롤링 실행

```bash
python kakao_review_crawler_elasticsearch.py
```

## 📊 저장되는 데이터 구조

Elasticsearch에 저장되는 리뷰 데이터:

```json
{
  "revId": null,
  "contentId": 1,
  "memberId": 1,
  "writer": "카카오맵사용자",
  "content": "리뷰 내용...",
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2024-01-01T00:00:00",
  "score": 5,
  "keywordIds": [],
  "campingName": "캠핑장 이름",
  "source": "kakao_map"
}
```

## 🔍 Elasticsearch 검색 예제

### 모든 리뷰 조회
```bash
curl -X GET "localhost:9200/reviews/_search"
```

### 특정 캠핑장 리뷰 검색
```bash
curl -X GET "localhost:9200/reviews/_search" -H "Content-Type: application/json" -d'
{
  "query": {
    "match": {
      "campingName": "영도마리노오토캠핑장"
    }
  }
}'
```

### 높은 평점 리뷰 검색
```bash
curl -X GET "localhost:9200/reviews/_search" -H "Content-Type: application/json" -d'
{
  "query": {
    "range": {
      "score": {
        "gte": 4
      }
    }
  }
}'
```

## 📁 파일 구조

```
py/
├── kakao_review_crawler.py              # 메인 크롤링 스크립트 (Elasticsearch 저장)
├── kakao_review_crawler_elasticsearch.py # Elasticsearch 전용 크롤링 스크립트
├── setup_elasticsearch.py               # Elasticsearch 설정 스크립트
├── requirements.txt                     # Python 패키지 목록
├── README.md                           # 이 파일
└── crawled_data/                       # 크롤링 결과 파일들
    ├── reviews_1_20240101_120000.csv
    ├── reviews_1_20240101_120000.json
    └── all_reviews_20240101_120000.json
```

## ⚠️ 주의사항

1. **Elasticsearch 실행 확인**: 크롤링 전에 Elasticsearch가 실행 중인지 확인하세요.
2. **API 키**: 카카오맵 API 키가 유효한지 확인하세요.
3. **크롤링 간격**: 서버에 부하를 주지 않도록 적절한 간격을 두고 크롤링하세요.
4. **데이터 백업**: 중요한 데이터는 정기적으로 백업하세요.

## 🐛 문제 해결

### Elasticsearch 연결 실패
```bash
# Elasticsearch 상태 확인
curl -X GET "localhost:9200/_cluster/health"
```

### ChromeDriver 오류
- Chrome 브라우저가 최신 버전인지 확인
- `webdriver-manager`가 자동으로 드라이버를 다운로드함

### 메모리 부족
- Elasticsearch 설정에서 힙 메모리 조정
- 크롤링할 캠핑장 수를 줄여서 테스트

## 📞 지원

문제가 발생하면 다음을 확인하세요:
1. Elasticsearch 로그
2. Python 스크립트 오류 메시지
3. 네트워크 연결 상태 