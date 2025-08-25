import requests
import time
import random
import re
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException
from webdriver_manager.chrome import ChromeDriverManager
from datetime import datetime
import oracledb  # Oracle DB 연결용
from elasticsearch import Elasticsearch
import csv
import json
import os
import pandas as pd

class KakaoMapReviewCrawler:
    def __init__(self):
        # Chrome 옵션 설정
        self.chrome_options = webdriver.ChromeOptions()
        self.chrome_options.add_argument('--no-sandbox')
        self.chrome_options.add_argument('--disable-dev-shm-usage')
        self.chrome_options.add_argument('--disable-blink-features=AutomationControlled')
        self.chrome_options.add_experimental_option("excludeSwitches", ["enable-automation"])
        self.chrome_options.add_experimental_option('useAutomationExtension', False)
        self.chrome_options.add_argument('--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36')
        # Windows 환경에서 추가 옵션
        self.chrome_options.add_argument('--disable-gpu')
        self.chrome_options.add_argument('--disable-extensions')
        self.chrome_options.add_argument('--disable-plugins')
        self.chrome_options.add_argument('--disable-images')
        self.chrome_options.add_argument('--headless')  # 헤드리스 모드로 실행
        
        # 카카오맵 API 설정
        self.kakao_api_key = "22a3f23874d2dacc284c6ab7eea89e10"  # 실제 API 키
        self.kakao_search_url = "https://dapi.kakao.com/v2/local/search/keyword.json"
        self.kakao_place_url = "https://place.map.kakao.com/"
        
        # Oracle DB 설정
        self.db_config = {
            'user': 'c##camp',
            'password': 'camp1234',
            'dsn': 'localhost:1521/xe'
        }
        
        # Elasticsearch 설정
        self.es = Elasticsearch(['http://localhost:9200'])
        self.index_name = "reviews"
        
        # 파일 저장 경로 설정
        self.output_dir = "crawled_data"
        if not os.path.exists(self.output_dir):
            os.makedirs(self.output_dir)
        
        self.driver = None
        self.all_reviews = []  # 모든 리뷰를 저장할 리스트
        
    def read_camping_data_from_excel(self):
        """Excel 파일에서 캠핑장 데이터 읽기"""
        try:
            # Excel 파일 경로
            excel_path = "../../camping_data.xlsx"
            
            # Excel 파일 읽기
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
    
    def connect_database(self):
        """데이터베이스 연결"""
        try:
            connection = oracledb.connect(**self.db_config)
            print("데이터베이스 연결 성공!")
            return connection
        except Exception as e:
            print(f"데이터베이스 연결 실패: {e}")
            print("Oracle DB 서버가 실행 중인지 확인해주세요.")
            return None
    
    def check_elasticsearch_connection(self):
        """Elasticsearch 연결 상태 확인"""
        try:
            if self.es.ping():
                print("Elasticsearch 연결 성공!")
                return True
            else:
                print("Elasticsearch 연결 실패!")
                return False
        except Exception as e:
            print(f"Elasticsearch 연결 오류: {e}")
            print("Elasticsearch 서버가 실행 중인지 확인해주세요.")
            return False
    
    def create_index_if_not_exists(self):
        """Elasticsearch 인덱스가 없으면 생성"""
        if not self.es.indices.exists(index=self.index_name):
            mapping = {
                "mappings": {
                    "properties": {
                        "revId": {"type": "long"},
                        "contentId": {"type": "long"},
                        "memberId": {"type": "long"},
                        "writer": {
                            "type": "text",
                            "analyzer": "standard"
                        },
                        "content": {
                            "type": "text",
                            "analyzer": "standard"
                        },
                        "createdAt": {"type": "date"},
                        "updatedAt": {"type": "date"},
                        "score": {"type": "integer"},
                        "keywordIds": {"type": "long"},
                        "campingName": {
                            "type": "text",
                            "analyzer": "standard"
                        },
                        "source": {"type": "keyword"}
                    }
                }
            }
            self.es.indices.create(index=self.index_name, body=mapping)
            print(f"Elasticsearch 인덱스 '{self.index_name}' 생성 완료")
        else:
            print(f"Elasticsearch 인덱스 '{self.index_name}' 이미 존재")
    
    def search_kakao_place(self, camping_name):
        """카카오맵 API로 캠핑장 검색"""
        try:
            headers = {
                'Authorization': f'KakaoAK {self.kakao_api_key}'
            }
            
            params = {
                'query': camping_name,
                'category_group_code': 'AD5',  # 숙박 카테고리
                'size': 1
            }
            
            print(f"카카오맵 API로 검색 중: {camping_name}")
            response = requests.get(self.kakao_search_url, headers=headers, params=params)
            
            if response.status_code == 200:
                data = response.json()
                if data['documents']:
                    place = data['documents'][0]
                    place_id = place['id']
                    # 리뷰 페이지로 직접 이동 (#comment 추가)
                    place_url = f"{self.kakao_place_url}{place_id}#comment"
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
    
    def extract_reviews(self, place_url):
        """카카오맵에서 리뷰 추출 (동적 로딩 처리)"""
        try:
            print(f"리뷰 페이지 접속: {place_url}")
            self.driver.get(place_url)
            time.sleep(random.uniform(5,8))  # 로딩 시간 증가
            
            # 페이지의 모든 iframe 확인
            iframes = self.driver.find_elements(By.TAG_NAME, "iframe")
            print(f"페이지에서 {len(iframes)}개의 iframe을 찾았습니다.")
            
            # iframe으로 전환 시도
            iframe_found = False
            for iframe in iframes:
                try:
                    iframe_id = iframe.get_attribute("id")
                    if iframe_id and ("entry" in iframe_id.lower() or "review" in iframe_id.lower()):
                        self.driver.switch_to.frame(iframe)
                        print(f"iframe {iframe_id}로 전환했습니다.")
                        iframe_found = True
                        break
                except:
                    continue
            
            if not iframe_found:
                print("적절한 iframe을 찾을 수 없습니다. 기본 페이지에서 검색합니다.")
            
            # 리뷰 관련 요소가 로드될 때까지 대기
            try:
                WebDriverWait(self.driver, 10).until(
                    EC.presence_of_element_located((By.CSS_SELECTOR, "li.item_evaluation"))
                )
                print("리뷰 요소가 로드되었습니다.")
            except TimeoutException:
                print("리뷰 요소 로딩 대기 시간 초과")
            
            reviews = []
            page = 1
            max_pages = 3 # 최대 3페이지까지 수집
            
            while page <= max_pages:
                try:
                    # 실제 카카오맵 리뷰 구조에 맞는 선택자
                    review_elements = self.driver.find_elements(By.CSS_SELECTOR, "ul.list_review > li")
                    if not review_elements:
                        print("리뷰 요소를 찾을 수 없습니다.")
                        # 페이지 소스에서 리뷰 관련 텍스트 확인
                        page_source = self.driver.page_source
                        if "후기" in page_source:
                            print("페이지에 '후기' 텍스트가 있습니다.")
                        if "평점" in page_source:
                            print("페이지에 '평점' 텍스트가 있습니다.")
                        if "별점" in page_source:
                            print("페이지에 '별점' 텍스트가 있습니다.")
                        break
                    print(f"페이지 {page}에서 {len(review_elements)}개의 리뷰를 찾았습니다.")
                    for element in review_elements:
                        try:
                            # 리뷰 텍스트 정제 - 실제 리뷰 내용만 추출
                            try:
                                inner_review = element.find_element(By.CSS_SELECTOR, "div.inner_review")
                                review_text = inner_review.text.strip()
                            except:
                                # div.inner_review가 없으면 li 요소에서 직접 텍스트 추출
                                review_text = element.text.strip()
                            
                            # 리뷰 텍스트 정제 (사용자 정보 제거)
                            if review_text:
                                # 줄바꿈으로 분리
                                lines = review_text.split('\n')
                                cleaned_lines = []
                                
                                # 사용자 정보 패턴 제거
                                skip_patterns = [
                                    '골드 레벨', '실버 레벨', '브론즈 레벨',
                                    '후기', '별점평균', '팔로워',
                                    '메뉴 더보기', '레벨'
                                ]
                                
                                for line in lines:
                                    line = line.strip()
                                    if not line:
                                        continue
                                    
                                    # 사용자 정보 패턴이 포함된 줄 제외
                                    skip_line = False
                                    for pattern in skip_patterns:
                                        if pattern in line:
                                            skip_line = True
                                            break
                                    
                                    # 숫자만 있는 줄 제외 (레벨 번호 등)
                                    if line.isdigit() and len(line) <= 3:
                                        skip_line = True
                                    
                                    # 날짜 패턴 제외 (YYYY.MM.DD)
                                    if re.match(r'\d{4}\.\d{2}\.\d{2}', line):
                                        skip_line = True
                                    
                                    if not skip_line:
                                        cleaned_lines.append(line)
                                
                                # 정제된 리뷰 텍스트 생성
                                review_text = ' '.join(cleaned_lines).strip()
                                
                                # 너무 짧은 텍스트 제외 (10자 미만)
                                if len(review_text) < 10:
                                    review_text = ""
                            
                            # 별점 - figure_star on 클래스 개수로 계산
                            rating = 0
                            try:
                                # figure_star on 클래스를 가진 요소들 찾기
                                star_elements = element.find_elements(By.CSS_SELECTOR, "span.figure_star.on")
                                if star_elements:
                                    rating = len(star_elements)
                                    print(f"별점 추출 성공: {rating}점 ({len(star_elements)}개 별)")
                                else:
                                    # 다른 별점 선택자들도 시도
                                    rating_selectors = [
                                        "span.ico_star",
                                        ".ico_star",
                                        ".star_score",
                                        ".rating",
                                        "[aria-label*='별']",
                                        ".star",
                                        "span[class*='star']",
                                        "i[class*='star']",
                                        ".ico_star_on",
                                        ".ico_star_off",
                                        "span[class*='ico_star']",
                                        "i[class*='ico_star']",
                                        ".star_rating",
                                        ".review_star"
                                    ]
                                    
                                    for rating_selector in rating_selectors:
                                        try:
                                            rating_elements = element.find_elements(By.CSS_SELECTOR, rating_selector)
                                            if rating_elements:
                                                print(f"별점 요소 발견: {rating_selector} (개수: {len(rating_elements)})")
                                                for rating_element in rating_elements:
                                                    rating_text = rating_element.get_attribute("aria-label")
                                                    if not rating_text:
                                                        rating_text = rating_element.get_attribute("title")
                                                    if not rating_text:
                                                        rating_text = rating_element.get_attribute("alt")
                                                    if not rating_text:
                                                        rating_text = rating_element.text
                                                    
                                                    if rating_text:
                                                        # 숫자 추출 (1-5 범위)
                                                        match = re.search(r'(\d+)', rating_text)
                                                        if match:
                                                            temp_rating = int(match.group(1))
                                                            if 1 <= temp_rating <= 5:
                                                                rating = temp_rating
                                                                print(f"별점 추출 성공: {rating}")
                                                                break
                                                    
                                                    # 클래스명에서 별점 정보 추출
                                                    class_name = rating_element.get_attribute("class")
                                                    if class_name:
                                                        # ico_star_on 개수로 별점 계산
                                                        if "ico_star_on" in class_name:
                                                            on_count = class_name.count("ico_star_on")
                                                            if on_count > 0:
                                                                rating = on_count
                                                                print(f"클래스에서 별점 추출: {rating}")
                                                                break
                                                if rating > 0:
                                                    break
                                        except Exception as e:
                                            continue
                            except Exception as e:
                                print(f"별점 추출 오류: {e}")
                            
                            # 날짜 - 더 정확한 선택자 사용
                            date_text = "날짜 정보 없음"
                            date_selectors = [
                                "span.time_write",
                                ".time_write",
                                ".date",
                                ".time",
                                ".txt_date",
                                "span[class*='date']",
                                "span[class*='time']"
                            ]
                            
                            for date_selector in date_selectors:
                                try:
                                    date_element = element.find_element(By.CSS_SELECTOR, date_selector)
                                    date_text = date_element.text.strip()
                                    if date_text:
                                        break
                                except:
                                    continue
                            
                            # 작성자명 추출
                            writer = "카카오맵사용자"
                            writer_selectors = [
                                "span.name",
                                ".name",
                                ".writer",
                                ".author",
                                "span[class*='name']",
                                "span[class*='writer']",
                                ".user_name",
                                ".reviewer"
                            ]
                            
                            for writer_selector in writer_selectors:
                                try:
                                    writer_element = element.find_element(By.CSS_SELECTOR, writer_selector)
                                    writer_text = writer_element.text.strip()
                                    if writer_text and len(writer_text) > 1:
                                        writer = writer_text
                                        break
                                except:
                                    continue
                            
                            if review_text and len(review_text) > 10:
                                reviews.append({
                                    'content': review_text,
                                    'rating': rating,
                                    'created_date': date_text,
                                    'writer': writer
                                })
                                print(f"리뷰 수집: {writer} - {review_text[:50]}... (평점: {rating})")
                        except Exception as e:
                            print(f"리뷰 파싱 오류: {e}")
                            continue
                    # 다음 페이지로 이동 (카카오맵은 스크롤 방식일 수 있음, 여기서는 단순히 1페이지만)
                    break
                except Exception as e:
                    print(f"리뷰 추출 중 오류: {e}")
                    break
            
            # iframe에서 기본 컨텍스트로 복귀
            self.driver.switch_to.default_content()
            print(f"총 {len(reviews)}개의 리뷰를 수집했습니다.")
            return reviews
        except Exception as e:
            print(f"리뷰 추출 중 오류: {e}")
            # 오류 발생 시에도 기본 컨텍스트로 복귀
            try:
                self.driver.switch_to.default_content()
            except:
                pass
            return []
    
    def save_reviews(self, camping_id, camping_name, reviews):
        """리뷰를 데이터베이스와 Elasticsearch에 저장"""
        if not reviews:
            print("저장할 리뷰가 없습니다.")
            return
        
        # 1. Oracle DB에 저장
        connection = self.connect_database()
        if connection:
            try:
                with connection.cursor() as cursor:
                    for review in reviews:
                        # 작성자명 길이 제한 (20자)
                        writer = review.get('writer', '카카오맵사용자')
                        if len(writer) > 20:
                            writer = writer[:20]
                            print(f"작성자명이 20자를 초과하여 '{writer}'로 잘라냈습니다.")
                        
                        # Oracle DB용 쿼리 (실제 테이블 구조에 맞게 수정)
                        sql = """
                        INSERT INTO review (rev_id, content_id, member_id, writer, content, score)
                        VALUES (review_rev_id_seq.NEXTVAL, :1, :2, :3, :4, :5)
                        """
                        cursor.execute(sql, (
                            camping_id,
                            1,  # member_id는 임시로 1로 설정
                            writer,
                            review['content'],
                            review['rating']
                        ))
                
                connection.commit()
                print(f"캠핑장 ID {camping_id}: {len(reviews)}개 리뷰 DB 저장 완료")
                
            except Exception as e:
                print(f"DB 리뷰 저장 중 오류: {e}")
                connection.rollback()
            finally:
                connection.close()
        else:
            print("데이터베이스 연결 실패로 DB 저장을 건너뜁니다.")
            print(f"수집된 리뷰 {len(reviews)}개:")
            for i, review in enumerate(reviews[:3], 1):  # 처음 3개만 출력
                print(f"  {i}. 평점: {review['rating']}, 내용: {review['content'][:50]}...")
        
        # 2. Elasticsearch에 저장
        try:
            # Elasticsearch 인덱스 생성 확인
            self.create_index_if_not_exists()
            
            for i, review in enumerate(reviews):
                # 작성자명 길이 제한 (20자)
                writer = review.get('writer', '카카오맵사용자')
                if len(writer) > 20:
                    writer = writer[:20]
                
                # Elasticsearch 문서 생성
                doc = {
                    'revId': None,  # Elasticsearch에서 자동 생성
                    'contentId': camping_id,
                    'memberId': 1,  # 임시 member_id
                    'writer': writer,
                    'content': review['content'],
                    'createdAt': datetime.now().isoformat(),
                    'updatedAt': datetime.now().isoformat(),
                    'score': review['rating'],
                    'keywordIds': [],
                    'campingName': camping_name,
                    'source': 'kakao_map'
                }
                
                # Elasticsearch에 저장
                response = self.es.index(index=self.index_name, body=doc)
                print(f"Elasticsearch 리뷰 {i+1} 저장 완료: {response['result']} (ID: {response['_id']})")
            
            # 인덱스 새로고침
            self.es.indices.refresh(index=self.index_name)
            print(f"캠핑장 '{camping_name}': {len(reviews)}개 리뷰 Elasticsearch 저장 완료")
            
        except Exception as e:
            print(f"Elasticsearch 저장 중 오류: {e}")
    
    def save_to_csv(self, reviews, camping_id, camping_name):
        """리뷰를 CSV 파일로 저장"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"{self.output_dir}/reviews_{camping_id}_{timestamp}.csv"
        
        try:
            with open(filename, 'w', newline='', encoding='utf-8-sig') as csvfile:
                fieldnames = ['camping_id', 'camping_name', 'writer', 'content', 'rating', 'created_date', 'crawled_at']
                writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
                writer.writeheader()
                
                for review in reviews:
                    writer.writerow({
                        'camping_id': camping_id,
                        'camping_name': camping_name,
                        'writer': review.get('writer', '카카오맵사용자'),
                        'content': review['content'],
                        'rating': review['rating'],
                        'created_date': review.get('created_date', '날짜 정보 없음'),
                        'crawled_at': datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                    })
            
            print(f"📄 CSV 파일 저장 완료: {filename}")
            return filename
            
        except Exception as e:
            print(f"❌ CSV 파일 저장 실패: {e}")
            return None
    
    def save_to_json(self, reviews, camping_id, camping_name):
        """리뷰를 JSON 파일로 저장"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"{self.output_dir}/reviews_{camping_id}_{timestamp}.json"
        
        try:
            data = {
                'camping_info': {
                    'camping_id': camping_id,
                    'camping_name': camping_name,
                    'total_reviews': len(reviews),
                    'crawled_at': datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                },
                'reviews': []
            }
            
            for review in reviews:
                data['reviews'].append({
                    'writer': review.get('writer', '카카오맵사용자'),
                    'content': review['content'],
                    'rating': review['rating'],
                    'created_date': review.get('created_date', '날짜 정보 없음')
                })
            
            with open(filename, 'w', encoding='utf-8') as jsonfile:
                json.dump(data, jsonfile, ensure_ascii=False, indent=2)
            
            print(f"📄 JSON 파일 저장 완료: {filename}")
            return filename
            
        except Exception as e:
            print(f"❌ JSON 파일 저장 실패: {e}")
            return None
    
    def save_all_reviews_to_files(self):
        """모든 수집된 리뷰를 통합 파일로 저장"""
        if not self.all_reviews:
            print("❌ 저장할 리뷰가 없습니다.")
            return
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # 통합 CSV 파일 저장
        csv_filename = f"{self.output_dir}/all_reviews_{timestamp}.csv"
        try:
            with open(csv_filename, 'w', newline='', encoding='utf-8-sig') as csvfile:
                fieldnames = ['camping_id', 'camping_name', 'writer', 'content', 'rating', 'created_date', 'crawled_at']
                writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
                writer.writeheader()
                
                for review_data in self.all_reviews:
                    writer.writerow(review_data)
            
            print(f"📄 통합 CSV 파일 저장 완료: {csv_filename}")
            
        except Exception as e:
            print(f"❌ 통합 CSV 파일 저장 실패: {e}")
        
        # 통합 JSON 파일 저장
        json_filename = f"{self.output_dir}/all_reviews_{timestamp}.json"
        try:
            data = {
                'summary': {
                    'total_reviews': len(self.all_reviews),
                    'total_campings': len(set(review['camping_id'] for review in self.all_reviews)),
                    'crawled_at': datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                },
                'reviews': self.all_reviews
            }
            
            with open(json_filename, 'w', encoding='utf-8') as jsonfile:
                json.dump(data, jsonfile, ensure_ascii=False, indent=2)
            
            print(f"📄 통합 JSON 파일 저장 완료: {json_filename}")
            
        except Exception as e:
            print(f"❌ 통합 JSON 파일 저장 실패: {e}")
    
    def run(self, start_index=0, end_index=None):
        """크롤링 실행"""
        print("카카오맵 리뷰 크롤링을 시작합니다...")
        
        # Elasticsearch 연결 확인 (선택사항)
        if not self.check_elasticsearch_connection():
            print("Elasticsearch 연결 실패. DB에만 저장됩니다.")
        
        try:
            # 크롬드라이버 설정 (로컬 chromedriver.exe 사용)
            print("크롬드라이버를 설정하는 중...")
            try:
                # 로컬 chromedriver.exe 사용
                chromedriver_path = r"../../chromedriver.exe"
                service = Service(chromedriver_path)
                self.driver = webdriver.Chrome(service=service, options=self.chrome_options)
                print("크롬드라이버 설정 완료!")
            except Exception as e:
                print(f"로컬 ChromeDriver 설정 실패: {e}")
                print("webdriver-manager로 시도합니다...")
                try:
                    service = Service(ChromeDriverManager().install())
                    self.driver = webdriver.Chrome(service=service, options=self.chrome_options)
                    print("webdriver-manager로 크롬드라이버 설정 완료!")
                except Exception as e2:
                    print(f"webdriver-manager도 실패: {e2}")
                    print("Chrome 브라우저가 설치되어 있는지 확인해주세요.")
                    return
            
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
            
            for i, (camping_id, camping_name, address) in enumerate(camping_sites, 1):
                print(f"[{i}/{len(camping_sites)}] {camping_name} 처리 중...")
                
                # 카카오맵에서 캠핑장 검색
                place_url = self.search_kakao_place(camping_name)
                if not place_url:
                    print(f" - {camping_name}: 검색 결과 없음")
                    continue
                
                # 리뷰 추출
                reviews = self.extract_reviews(place_url)
                if not reviews:
                    print(f" - {camping_name}: 리뷰 없음")
                    continue
                
                print(f" - {camping_name}: {len(reviews)}개 리뷰 수집")
                
                # 데이터베이스와 Elasticsearch에 저장
                self.save_reviews(camping_id, camping_name, reviews)
                
                # 개별 캠핑장 CSV/JSON 파일 저장
                self.save_to_csv(reviews, camping_id, camping_name)
                self.save_to_json(reviews, camping_id, camping_name)
                
                # 전체 리뷰 리스트에 추가
                for review in reviews:
                    self.all_reviews.append({
                        'camping_id': camping_id,
                        'camping_name': camping_name,
                        'writer': review.get('writer', '카카오맵사용자'),
                        'content': review['content'],
                        'rating': review['rating'],
                        'created_date': review.get('created_date', '날짜 정보 없음'),
                        'crawled_at': datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                    })
                
                # 요청 간격 조절
                time.sleep(random.uniform(5, 10))
            
            # 통합 파일 저장
            if self.all_reviews:
                print(f"\n📊 총 {len(self.all_reviews)}개 리뷰 수집 완료!")
                self.save_all_reviews_to_files()
                
        except Exception as e:
            print(f"크롤링 중 오류 발생: {e}")
        finally:
            if self.driver:
                self.driver.quit()
            print("크롤링이 완료되었습니다.")

if __name__ == "__main__":
    crawler = KakaoMapReviewCrawler()
    # 전체 데이터 크롤링 (시간이 오래 걸릴 수 있음)
    # crawler.run()
    
    # 테스트용으로 처음 5개만 크롤링
    crawler.run(start_index=0, end_index=5) 