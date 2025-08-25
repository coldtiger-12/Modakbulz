# 카카오맵 리뷰 크롤러 사용법

## 개요
이 크롤러는 `camping_data.xlsx` 파일에 있는 4325개의 캠핑장 데이터를 읽어와서 카카오맵에서 각 캠핑장의 리뷰를 수집하고 CSV/JSON 파일로 저장합니다.

## 파일 구조
- `kakao_review_crawler_elasticsearch.py`: 메인 크롤러
- `batch_crawler.py`: 배치 처리용 스크립트
- `excel_reader.py`: Excel 파일 읽기 테스트용
- `camping_data.xlsx`: 캠핑장 데이터 (4325개)

## 설치된 패키지
```bash
pip install pandas openpyxl selenium webdriver-manager elasticsearch oracledb
```

## 사용법

### 1. 테스트 실행 (처음 5개 캠핑장)
```bash
python kakao_review_crawler_elasticsearch.py
```

### 2. 특정 배치 실행
```bash
# 배치 0 (처음 100개 캠핑장)
python batch_crawler.py --batch 0 --size 100

# 배치 1 (101-200번째 캠핑장)
python batch_crawler.py --batch 1 --size 100
```

### 3. 전체 데이터 크롤링 (모든 배치 순차 실행)
```bash
# 배치 크기 100으로 전체 실행
python batch_crawler.py --all --size 100

# 배치 크기 50으로 전체 실행 (더 세밀한 제어)
python batch_crawler.py --all --size 50
```

## 출력 파일

### 개별 캠핑장 파일
- `crawled_data/reviews_{캠핑장ID}_{타임스탬프}.csv`
- `crawled_data/reviews_{캠핑장ID}_{타임스탬프}.json`

### 통합 파일
- `crawled_data/all_reviews_{타임스탬프}.csv`
- `crawled_data/all_reviews_{타임스탬프}.json`

## CSV 파일 구조
```csv
camping_id,camping_name,writer,content,rating,created_date,crawled_at
355,구름포오토캠핑장,julia,julia 여긴 진심..1점도 아깝다..,1,날짜 정보 없음,2025-07-31 15:43:20
```

## 배치 처리 전략

### 전체 4325개 캠핑장 처리 시간 예상
- 배치 크기 100: 약 44개 배치
- 각 배치당 평균 30-60분 소요
- 전체 예상 시간: 22-44시간

### 권장 실행 방법
1. **첫 번째 배치 테스트**: `python batch_crawler.py --batch 0 --size 10`
2. **소규모 배치**: `python batch_crawler.py --batch 0 --size 50`
3. **전체 실행**: `python batch_crawler.py --all --size 100`

## 주의사항

### 1. 서버 부하 방지
- 각 캠핑장 처리 후 5-10초 대기
- 배치 간 30초 대기
- 헤드리스 모드로 실행

### 2. 오류 처리
- 개별 캠핑장 오류 시 다음 캠핑장으로 진행
- 배치 오류 시 다음 배치로 진행
- 모든 오류는 로그에 기록

### 3. 데이터베이스 연결
- Oracle DB 연결 실패 시 CSV/JSON 파일만 저장
- Elasticsearch 연결 실패 시 DB에만 저장

## 모니터링

### 진행 상황 확인
```bash
# crawled_data 폴더의 파일 수 확인
ls crawled_data/*.csv | wc -l

# 최신 통합 파일 확인
ls -la crawled_data/all_reviews_*.csv | tail -1
```

### 로그 확인
- 콘솔 출력으로 실시간 진행 상황 확인
- 각 배치별 처리 결과 표시

## 문제 해결

### 1. ChromeDriver 오류
```bash
# webdriver-manager로 자동 설치
pip install webdriver-manager
```

### 2. 메모리 부족
- 배치 크기를 줄여서 실행: `--size 50`
- 더 작은 배치로 실행: `--size 25`

### 3. 네트워크 오류
- 자동 재시도 로직 포함
- 일시적 오류 시 다음 캠핑장으로 진행

## 성능 최적화

### 1. 병렬 처리 (고급)
- 여러 배치를 동시에 실행 가능
- CPU 코어 수에 따라 조정

### 2. 리소스 모니터링
- 메모리 사용량 확인
- 디스크 공간 확인 (CSV 파일 크기)

## 예상 결과

### 수집 가능한 리뷰 수
- 전체 캠핑장: 4325개
- 리뷰 있는 캠핑장: 약 60-70% (약 2600-3000개)
- 예상 총 리뷰 수: 10,000-50,000개

### 파일 크기 예상
- CSV 파일: 10-50MB
- JSON 파일: 15-75MB

## 지원

문제가 발생하면 다음을 확인하세요:
1. Chrome 브라우저 설치 여부
2. 인터넷 연결 상태
3. 디스크 공간 (최소 1GB 권장)
4. 메모리 사용량 