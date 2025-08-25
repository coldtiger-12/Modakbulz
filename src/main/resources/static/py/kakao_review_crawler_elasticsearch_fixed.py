from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException
import time
import re
import random
from datetime import datetime
from elasticsearch import Elasticsearch
import csv
import json
import os
import requests
import pandas as pd
import argparse

class SimpleMoreCrawler:
    def __init__(self):
        # Chrome 옵션 설정
        self.chrome_options = webdriver.ChromeOptions()
        self.chrome_options.add_argument('--no-sandbox')
        self.chrome_options.add_argument('--disable-dev-shm-usage')
        self.chrome_options.add_argument('--disable-blink-features=AutomationControlled')
        self.chrome_options.add_experimental_option("excludeSwitches", ["enable-automation"])
        self.chrome_options.add_experimental_option('useAutomationExtension', False)
        self.chrome_options.add_argument('--headless')  # 헤드리스 모드
        
        # Elasticsearch 설정
        self.es = Elasticsearch(['http://localhost:9200'])
        self.index_name = "reviews"
        
        # 파일 저장 경로 설정
        self.output_dir = "crawled_data"
        if not os.path.exists(self.output_dir):
            os.makedirs(self.output_dir)
        
        self.driver = None
        
    def extract_reviews_with_more(self, place_url):
        """더보기 버튼을 클릭해서 전체 리뷰 내용 추출"""
        try:
            print(f"리뷰 페이지 접속: {place_url}")
            self.driver.get(place_url)
            time.sleep(5)
            
            # 리뷰 요소 찾기
            review_elements = self.driver.find_elements(By.CSS_SELECTOR, "ul.list_review > li")
            print(f"리뷰 개수: {len(review_elements)}")
            
            reviews = []
            for i, element in enumerate(review_elements[:5]):  # 처음 5개만 테스트
                print(f"\n=== 리뷰 {i+1} ===")
                
                # 더보기 버튼 찾기
                more_button = None
                more_button_selectors = [
                    "span.btn_more",
                    "button.btn_more", 
                    ".btn_more",
                    ".more_btn",
                    "[class*='more']",
                    "button[class*='more']",
                    ".btn_more_review",
                    ".more_text",
                    "span.more",
                    "a.more"
                ]
                
                # 더보기 버튼 찾기
                for selector in more_button_selectors:
                    try:
                        more_button = element.find_element(By.CSS_SELECTOR, selector)
                        if more_button and more_button.is_displayed():
                            print(f"더보기 버튼 발견: {selector}")
                            break
                    except:
                        continue
                
                # 더보기 버튼이 있으면 클릭
                if more_button:
                    print("더보기 버튼 클릭합니다.")
                    more_button.click()
                    time.sleep(3)  # 내용 로딩 대기
                
                # 리뷰 텍스트 추출 (더보기 클릭 후 전체 내용)
                try:
                    desc_review = element.find_element(By.CSS_SELECTOR, "p.desc_review")
                    review_text = desc_review.text.strip()
                    print(f"리뷰 텍스트 추출: {review_text[:100]}...")
                except:
                    try:
                        inner_review = element.find_element(By.CSS_SELECTOR, "div.inner_review")
                        review_text = inner_review.text.strip()
                        print(f"리뷰 텍스트 추출 (fallback): {review_text[:100]}...")
                    except:
                        review_text = element.text.strip()
                        print(f"리뷰 텍스트 추출 (fallback2): {review_text[:100]}...")
                
                # 키워드 점수 추출 및 리뷰 텍스트 정제
                keyword_scores = {}
                if review_text:
                    # 키워드 점수 패턴 찾기 (예: "시설 뷰 +3", "가격 시설 +2" 등)
                    keyword_patterns = [
                        r'시설\s*주차\s*\+(\d+)',
                        r'가격\s*시설\s*\+(\d+)',
                        r'가격\s*뷰\s*\+(\d+)',
                        r'시설\s*\+(\d+)',
                        r'주차\s*\+(\d+)',
                        r'청결\s*\+(\d+)',
                        r'가격\s*\+(\d+)',
                        r'뷰\s*\+(\d+)',
                        r'위치\s*\+(\d+)',
                        r'서비스\s*\+(\d+)',
                        r'분위기\s*\+(\d+)',
                        r'편의시설\s*\+(\d+)'
                    ]
                    
                    # 키워드 점수 추출
                    for pattern in keyword_patterns:
                        matches = re.findall(pattern, review_text)
                        if matches:
                            # 패턴에서 키워드 이름 추출
                            if '시설' in pattern and '주차' in pattern:
                                keyword_scores['시설'] = 5  # 기본값
                                keyword_scores['주차'] = 5  # 기본값
                            elif '가격' in pattern and '시설' in pattern:
                                keyword_scores['가격'] = 5  # 기본값
                                keyword_scores['시설'] = 5  # 기본값
                            elif '가격' in pattern and '뷰' in pattern:
                                keyword_scores['가격'] = 5  # 기본값
                                keyword_scores['뷰'] = 5  # 기본값
                            else:
                                # 단일 키워드 패턴
                                keyword_name = pattern.split(r'\s*\+(\d+)')[0].strip()
                                keyword_score = 5  # 기본값
                                keyword_scores[keyword_name] = keyword_score
                            
                            print(f"키워드 점수 발견: {pattern} -> {keyword_scores}")
                            break
                    
                    # 키워드 점수가 없으면 다른 방법으로 시도
                    if not keyword_scores:
                        # 키워드 점수 요소들 찾기
                        keyword_selectors = [
                            "div.wrap_badge",
                            ".wrap_badge",
                            ".badge",
                            "span[class*='badge']",
                            "div[class*='keyword']",
                            ".keyword_score",
                            ".review_keyword"
                        ]
                        
                        for keyword_selector in keyword_selectors:
                            try:
                                keyword_elements = element.find_elements(By.CSS_SELECTOR, keyword_selector)
                                if keyword_elements:
                                    print(f"키워드 요소 발견: {keyword_selector} (개수: {len(keyword_elements)})")
                                    
                                    for keyword_element in keyword_elements:
                                        keyword_text = keyword_element.text.strip()
                                        if keyword_text:
                                            print(f"키워드 텍스트: {keyword_text}")
                                            
                                            # 여러 줄로 분리된 키워드 텍스트 처리
                                            lines = keyword_text.split('\n')
                                            print(f"분리된 라인들: {lines}")
                                            
                                            for line in lines:
                                                line = line.strip()
                                                if not line:
                                                    continue
                                                
                                                print(f"처리 중인 라인: '{line}'")
                                                
                                                # 키워드와 점수 분리
                                                # 예: "시설 5", "주차 4", "청결 5" 등
                                                keyword_match = re.match(r'([가-힣]+)\s*(\d+)', line)
                                                if keyword_match:
                                                    keyword_name = keyword_match.group(1)
                                                    keyword_score = int(keyword_match.group(2))
                                                    keyword_scores[keyword_name] = keyword_score
                                                    print(f"키워드 점수 추출: {keyword_name} = {keyword_score}")
                                                
                                                # 다른 패턴도 시도 (예: "시설5", "주차4" 등)
                                                keyword_match2 = re.match(r'([가-힣]+)(\d+)', line)
                                                if keyword_match2:
                                                    keyword_name = keyword_match2.group(1)
                                                    keyword_score = int(keyword_match2.group(2))
                                                    keyword_scores[keyword_name] = keyword_score
                                                    print(f"키워드 점수 추출: {keyword_name} = {keyword_score}")
                                                
                                                # "+" 패턴 처리 (예: "가격 뷰 +3")
                                                keyword_match3 = re.match(r'([가-힣]+)\s*([가-힣]+)\s*\+(\d+)', line)
                                                if keyword_match3:
                                                    keyword1 = keyword_match3.group(1)
                                                    keyword2 = keyword_match3.group(2)
                                                    score = int(keyword_match3.group(3))
                                                    keyword_scores[keyword1] = score
                                                    keyword_scores[keyword2] = score
                                                    print(f"키워드 점수 추출: {keyword1}, {keyword2} = {score}")
                                                
                                                # 단일 키워드 + 패턴 (예: "시설 +3")
                                                keyword_match4 = re.match(r'([가-힣]+)\s*\+(\d+)', line)
                                                if keyword_match4:
                                                    keyword_name = keyword_match4.group(1)
                                                    score = int(keyword_match4.group(2))
                                                    keyword_scores[keyword_name] = score
                                                    print(f"키워드 점수 추출: {keyword_name} = {score}")
                                                
                                                # "+" 기호만 있는 경우 (다음 라인에서 점수 찾기)
                                                if line == '+':
                                                    print("'+' 기호 발견, 다음 라인에서 점수 찾기")
                                                    # 다음 라인에서 숫자 찾기
                                                    for next_line in lines:
                                                        next_line = next_line.strip()
                                                        # 숫자만 있는지 확인 (예: "3")
                                                        if next_line.isdigit():
                                                            # 이전에 발견된 키워드들에 점수 할당
                                                            score = int(next_line)
                                                            # 이전 라인들에서 키워드 찾기
                                                            prev_keywords = []
                                                            for prev_line in lines:
                                                                prev_line = prev_line.strip()
                                                                if prev_line in ['가격', '뷰', '시설', '주차', '청결', '위치', '서비스', '분위기', '편의시설']:
                                                                    prev_keywords.append(prev_line)
                                                            
                                                            for prev_keyword in prev_keywords:
                                                                keyword_scores[prev_keyword] = score
                                                                print(f"키워드 점수 추출: {prev_keyword} = {score}")
                                                            break
                                                
                                                # "+숫자" 패턴 처리 (예: "+3")
                                                if line.startswith('+') and line[1:].isdigit():
                                                    print(f"'+숫자' 패턴 발견: {line}")
                                                    score = int(line[1:])
                                                    # 이전 라인들에서 키워드 찾기
                                                    prev_keywords = []
                                                    for prev_line in lines:
                                                        prev_line = prev_line.strip()
                                                        if prev_line in ['가격', '뷰', '시설', '주차', '청결', '위치', '서비스', '분위기', '편의시설']:
                                                            prev_keywords.append(prev_line)
                                                    
                                                    for prev_keyword in prev_keywords:
                                                        keyword_scores[prev_keyword] = score
                                                        print(f"키워드 점수 추출: {prev_keyword} = {score}")
                                    
                                    if keyword_scores:
                                        break
                            except Exception as e:
                                continue
                    
                    # 키워드 점수 패턴 제거 (리뷰 텍스트에서)
                    keyword_patterns_to_remove = [
                        r'시설\s*주차\s*\+(\d+)',
                        r'가격\s*시설\s*\+(\d+)',
                        r'가격\s*뷰\s*\+(\d+)',
                        r'시설\s*\+(\d+)',
                        r'주차\s*\+(\d+)',
                        r'청결\s*\+(\d+)',
                        r'가격\s*\+(\d+)',
                        r'뷰\s*\+(\d+)',
                        r'위치\s*\+(\d+)',
                        r'서비스\s*\+(\d+)',
                        r'분위기\s*\+(\d+)',
                        r'편의시설\s*\+(\d+)'
                    ]
                    
                    for pattern in keyword_patterns_to_remove:
                        review_text = re.sub(pattern, '', review_text)
                    
                    # 사용자 정보 제거
                    lines = review_text.split('\n')
                    cleaned_lines = []
                    
                    skip_patterns = [
                        '골드 레벨', '실버 레벨', '브론즈 레벨',
                        '후기', '별점평균', '팔로워',
                        '메뉴 더보기', '레벨'
                    ]
                    
                    for line in lines:
                        line = line.strip()
                        if not line:
                            continue
                        
                        skip_line = False
                        for pattern in skip_patterns:
                            if pattern in line:
                                skip_line = True
                                break
                        
                        if line.isdigit() and len(line) <= 3:
                            skip_line = True
                        
                        if re.match(r'\d{4}\.\d{2}\.\d{2}', line):
                            skip_line = True
                        
                        if not skip_line:
                            cleaned_lines.append(line)
                    
                    review_text = ' '.join(cleaned_lines).strip()
                    
                    # "접기" 텍스트 제거
                    if "접기" in review_text:
                        review_text = review_text.split("접기")[0].strip()
                    
                    # "더보기" 텍스트 제거
                    review_text = re.sub(r'더보기\.?\.?\.?', '', review_text)
                    review_text = re.sub(r'\.\.\.\s*더보기', '', review_text)
                    review_text = re.sub(r'더보기', '', review_text)
                    
                    # 연속된 공백 제거
                    review_text = re.sub(r'\s+', ' ', review_text)
                    review_text = review_text.strip()
                    
                    if len(review_text) >= 5:
                        reviews.append({
                            'content': review_text,
                            'rating': 5,  # 기본값
                            'created_date': datetime.now().strftime("%Y-%m-%d"),
                            'writer': '카카오맵사용자',
                            'keyword_scores': keyword_scores  # 키워드 점수 추가
                        })
                        print(f"리뷰 저장: {review_text[:50]}... (키워드: {keyword_scores})")
                
                print("-" * 50)
            
            print(f"총 {len(reviews)}개의 리뷰를 수집했습니다.")
            return reviews
            
        except Exception as e:
            print(f"리뷰 추출 중 오류: {e}")
            return []
    
    def save_to_elasticsearch(self, reviews, camping_id, camping_name):
        """Elasticsearch에 저장"""
        try:
            # 키워드 점수 통계 계산
            keyword_stats = {}
            for review in reviews:
                keyword_scores = review.get('keyword_scores', {})
                for keyword, score in keyword_scores.items():
                    if keyword not in keyword_stats:
                        keyword_stats[keyword] = {'total_score': 0, 'count': 0}
                    keyword_stats[keyword]['total_score'] += score
                    keyword_stats[keyword]['count'] += 1
            
            # 평균 점수 계산
            for keyword in keyword_stats:
                keyword_stats[keyword]['average_score'] = round(
                    keyword_stats[keyword]['total_score'] / keyword_stats[keyword]['count'], 1
                )
            
            print(f"키워드 점수 통계: {keyword_stats}")
            
            for i, review in enumerate(reviews):
                doc = {
                    'revId': None,
                    'contentId': camping_id,
                    'memberId': 1,
                    'writer': review['writer'],
                    'content': review['content'],
                    'createdAt': datetime.now().isoformat(),
                    'updatedAt': datetime.now().isoformat(),
                    'score': review['rating'],
                    'keywordIds': [],
                    'campingName': camping_name,
                    'source': 'kakao_map',
                    'keywordScores': review.get('keyword_scores', {}),  # 키워드 점수 추가
                    'keywordStats': keyword_stats  # 키워드 통계 추가
                }
                
                response = self.es.index(index=self.index_name, body=doc)
                print(f"Elasticsearch 리뷰 {i+1} 저장 완료: {response['result']}")
            
            self.es.indices.refresh(index=self.index_name)
            print(f"캠핑장 '{camping_name}': {len(reviews)}개 리뷰 Elasticsearch 저장 완료")
            
        except Exception as e:
            print(f"Elasticsearch 저장 중 오류: {e}")
    
    def read_camping_data_from_excel(self):
        """Excel 파일에서 캠핑장 데이터 읽기"""
        try:
            # Excel 파일 경로
            excel_path = "../../camping_data.xlsx"
            
            # Excel 파일 읽기
            import pandas as pd
            df = pd.read_excel(excel_path)
            
            print(f"Excel 파일 읽기 성공!")
            print(f"총 {len(df)}개의 캠핑장 데이터를 읽어왔습니다.")
            
            camping_data = []
            for index, row in df.iterrows():
                camping_id = row['contentId']
                camping_name = row['facltNm']
                address = row['addr1'] if pd.notna(row['addr1']) else "주소 정보 없음"
                
                camping_data.append((camping_id, camping_name, address))
            
            print(f"총 {len(camping_data)}개의 캠핑장 데이터를 추출했습니다.")
            return camping_data
            
        except Exception as e:
            print(f"Excel 파일 읽기 실패: {e}")
            return []
    
    def search_kakao_place(self, camping_name):
        """카카오맵 API로 캠핑장 검색"""
        try:
            import requests
            
            # 카카오맵 API 설정
            kakao_api_key = "22a3f23874d2dacc284c6ab7eea89e10"
            kakao_search_url = "https://dapi.kakao.com/v2/local/search/keyword.json"
            kakao_place_url = "https://place.map.kakao.com/"
            
            headers = {
                'Authorization': f'KakaoAK {kakao_api_key}'
            }
            
            params = {
                'query': camping_name,
                'category_group_code': 'AD5',  # 숙박 카테고리
                'size': 1
            }
            
            print(f"카카오맵 API로 검색 중: {camping_name}")
            response = requests.get(kakao_search_url, headers=headers, params=params)
            
            if response.status_code == 200:
                data = response.json()
                if data['documents']:
                    place = data['documents'][0]
                    place_id = place['id']
                    # 리뷰 페이지로 직접 이동 (#comment 추가)
                    place_url = f"{kakao_place_url}{place_id}#comment"
                    print(f"장소를 찾았습니다: {place['place_name']} (ID: {place_id})")
                    return place_url
                else:
                    print(f"검색 결과가 없습니다: {camping_name}")
                    return None
            else:
                print(f"API 요청 실패: {response.status_code}")
                return None
                
        except Exception as e:
            print(f"카카오맵 검색 중 오류: {e}")
            return None
    
    def run(self, start_index=0, end_index=None, person_number=None):
        """크롤링 실행"""
        # 사람 번호 설정
        self.person_number = person_number or 'unknown'
        print(f"4325개 캠핑장 리뷰 크롤링을 시작합니다... (사람 {self.person_number})")
        
        try:
            # 크롬드라이버 설정
            self.driver = webdriver.Chrome(options=self.chrome_options)
            print("크롬드라이버 설정 완료!")
            
            # Excel 파일에서 캠핑장 데이터 읽기
            camping_sites = self.read_camping_data_from_excel()
            
            if not camping_sites:
                print("캠핑장 데이터를 읽을 수 없습니다.")
                return
            
            # 인덱스 범위 설정
            if end_index is None:
                end_index = len(camping_sites)
            
            camping_sites = camping_sites[start_index:end_index]
            
            print(f"총 {len(camping_sites)}개의 캠핑장을 처리합니다. (인덱스: {start_index}~{end_index-1})")
            
            all_reviews = []  # 모든 리뷰를 저장할 리스트
            
            for i, (camping_id, camping_name, address) in enumerate(camping_sites, 1):
                print(f"[{i}/{len(camping_sites)}] {camping_name} 처리 중...")
                
                # 카카오맵에서 캠핑장 검색
                place_url = self.search_kakao_place(camping_name)
                if not place_url:
                    print(f" - {camping_name}: 검색 결과 없음")
                    continue
                
                # 리뷰 추출
                reviews = self.extract_reviews_with_more(place_url)
                if not reviews:
                    print(f" - {camping_name}: 리뷰 없음")
                    continue
                
                print(f" - {camping_name}: {len(reviews)}개 리뷰 수집")
                
                # Elasticsearch에 저장
                self.save_to_elasticsearch(reviews, camping_id, camping_name)
                
                # 개별 캠핑장 CSV 파일 저장
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                csv_filename = f"{self.output_dir}/reviews_{camping_id}_{timestamp}.csv"
                
                # 키워드 점수 필드들을 동적으로 추가
                all_keywords = set()
                for review in reviews:
                    all_keywords.update(review.get('keyword_scores', {}).keys())
                
                fieldnames = ['camping_id', 'camping_name', 'writer', 'content', 'rating', 'created_date']
                fieldnames.extend([f'keyword_{keyword}' for keyword in sorted(all_keywords)])
                
                with open(csv_filename, 'w', newline='', encoding='utf-8-sig') as csvfile:
                    writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
                    writer.writeheader()
                    
                    for review in reviews:
                        row = {
                            'camping_id': camping_id,
                            'camping_name': camping_name,
                            'writer': review['writer'],
                            'content': review['content'],
                            'rating': review['rating'],
                            'created_date': review['created_date']
                        }
                        
                        # 키워드 점수 추가
                        keyword_scores = review.get('keyword_scores', {})
                        for keyword in sorted(all_keywords):
                            row[f'keyword_{keyword}'] = keyword_scores.get(keyword, '')
                        
                        writer.writerow(row)
                
                print(f"CSV 파일 저장 완료: {csv_filename}")
                
                # 전체 리뷰 리스트에 추가
                for review in reviews:
                    all_reviews.append({
                        'camping_id': camping_id,
                        'camping_name': camping_name,
                        'writer': review['writer'],
                        'content': review['content'],
                        'rating': review['rating'],
                        'created_date': review['created_date'],
                        'keyword_scores': review.get('keyword_scores', {})
                    })
                
                # 요청 간격 조절 (서버 부하 방지)
                import random
                time.sleep(random.uniform(3, 7))
            
            # 통합 CSV 파일 저장
            if all_reviews:
                print(f"\n📊 총 {len(all_reviews)}개 리뷰 수집 완료!")
                
                # 통합 CSV 파일 저장 (사람 번호 포함)
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                # 사람 번호를 파일명에 포함 (전역 변수로 관리)
                person_number = getattr(self, 'person_number', 'unknown')
                csv_filename = f"{self.output_dir}/all_reviews_person{person_number}_{timestamp}.csv"
                
                # 키워드 점수 필드들을 동적으로 추가
                all_keywords = set()
                for review_data in all_reviews:
                    all_keywords.update(review_data.get('keyword_scores', {}).keys())
                
                fieldnames = ['camping_id', 'camping_name', 'writer', 'content', 'rating', 'created_date']
                fieldnames.extend([f'keyword_{keyword}' for keyword in sorted(all_keywords)])
                
                with open(csv_filename, 'w', newline='', encoding='utf-8-sig') as csvfile:
                    writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
                    writer.writeheader()
                    
                    for review_data in all_reviews:
                        row = {
                            'camping_id': review_data['camping_id'],
                            'camping_name': review_data['camping_name'],
                            'writer': review_data['writer'],
                            'content': review_data['content'],
                            'rating': review_data['rating'],
                            'created_date': review_data['created_date']
                        }
                        
                        # 키워드 점수 추가
                        keyword_scores = review_data.get('keyword_scores', {})
                        for keyword in sorted(all_keywords):
                            row[f'keyword_{keyword}'] = keyword_scores.get(keyword, '')
                        
                        writer.writerow(row)
                
                print(f"📄 통합 CSV 파일 저장 완료: {csv_filename}")
                
        except Exception as e:
            print(f"크롤링 중 오류 발생: {e}")
        finally:
            if self.driver:
                self.driver.quit()
            print("크롤링이 완료되었습니다.")

if __name__ == "__main__":
    # 명령행 인수 파싱
    parser = argparse.ArgumentParser(description='카카오맵 리뷰 크롤러')
    parser.add_argument('--batch', type=int, help='배치 번호 (1-7)')
    parser.add_argument('--start', type=int, help='시작 인덱스')
    parser.add_argument('--size', type=int, help='처리할 캠핑장 수')
    parser.add_argument('--test', action='store_true', help='테스트 모드 (처음 10개만)')
    
    args = parser.parse_args()
    
    crawler = SimpleMoreCrawler()
    
    # 7명이 나누어서 크롤링할 인덱스 범위 설정
    total_campings = 4325
    people_count = 7
    camping_per_person = total_campings // people_count
    remainder = total_campings % people_count
    
    # 각 사람별 인덱스 범위 계산
    ranges = []
    start = 0
    for i in range(people_count):
        end = start + camping_per_person
        if i < remainder:  # 나머지를 앞쪽 사람들에게 분배
            end += 1
        ranges.append((start, end))
        start = end
    
    # 명령행 인수가 있으면 사용, 없으면 대화형 모드
    if args.batch or args.start is not None or args.test:
        # 명령행 인수 모드
        if args.test:
            start_index = 0
            end_index = 10
            person_number = "test"
            print(f"테스트 모드: 인덱스 {start_index} ~ {end_index-1}")
        elif args.batch:
            if 1 <= args.batch <= 7:
                person_idx = args.batch - 1
                start_index, end_index = ranges[person_idx]
                person_number = str(args.batch)
                print(f"배치 {args.batch} 선택: 인덱스 {start_index} ~ {end_index-1} ({end_index-start_index}개 캠핑장)")
            else:
                print("배치 번호는 1-7 사이여야 합니다.")
                exit(1)
        elif args.start is not None:
            start_index = args.start
            if args.size:
                end_index = start_index + args.size
            else:
                end_index = start_index + 618  # 기본값
            person_number = f"custom_{start_index}"
            print(f"커스텀 범위: 인덱스 {start_index} ~ {end_index-1} ({end_index-start_index}개 캠핑장)")
        
        # 크롤링 실행
        crawler.run(start_index=start_index, end_index=end_index, person_number=person_number)
        
    else:
        # 대화형 모드 (기존 방식)
        print(f"총 캠핑장 수: {total_campings}")
        print(f"분담 인원: {people_count}명")
        print(f"1인당 처리할 캠핑장 수: {camping_per_person}개")
        print(f"나머지: {remainder}개")
        
        print("\n=== 각 사람별 처리 범위 ===")
        for i, (start_idx, end_idx) in enumerate(ranges, 1):
            count = end_idx - start_idx
            print(f"사람 {i}: 인덱스 {start_idx} ~ {end_idx-1} ({count}개)")
        
        # 사용자 입력으로 처리할 범위 선택
        print("\n=== 크롤링 범위 선택 ===")
        for i, (start_idx, end_idx) in enumerate(ranges, 1):
            count = end_idx - start_idx
            print(f"{i}: 사람 {i} (인덱스 {start_idx}~{end_idx-1}, {count}개)")
        print("0: 테스트용 (처음 10개만)")
        
        try:
            choice = input("\n처리할 범위를 선택하세요 (1-7, 0): ").strip()
            
            if choice == "0":
                # 테스트용 - 처음 10개만
                start_index = 0
                end_index = 10
                person_number = "test"
                print(f"테스트 모드: 인덱스 {start_index} ~ {end_index-1}")
            elif choice in ["1", "2", "3", "4", "5", "6", "7"]:
                person_idx = int(choice) - 1
                start_index, end_index = ranges[person_idx]
                person_number = choice
                print(f"사람 {choice} 선택: 인덱스 {start_index} ~ {end_index-1} ({end_index-start_index}개 캠핑장)")
            else:
                print("잘못된 선택입니다. 기본값(테스트용)으로 실행합니다.")
                start_index = 0
                end_index = 10
                person_number = "test"
            
            # 크롤링 실행
            crawler.run(start_index=start_index, end_index=end_index, person_number=person_number)
            
        except KeyboardInterrupt:
            print("\n사용자에 의해 중단되었습니다.")
        except Exception as e:
            print(f"오류 발생: {e}")