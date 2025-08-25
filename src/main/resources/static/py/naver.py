import pandas as pd
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import NoSuchElementException, TimeoutException
import re
import time
import os

# --- 설정 ---
EXCEL_FILE_PATH = "캠핑장.xlsx"
CSV_FILE_PATH = 'camping_keywords.csv'
NAVER_MAP_URL = 'https://map.naver.com'
COLUMN_NAME = 'facltNm'
IMPLICIT_WAIT = 10
EXPLICIT_WAIT = 10

def get_camping_list(file_path, column_name):
    """Excel 파일에서 캠핑장 목록을 읽어옵니다."""
    try:
        df = pd.read_excel(file_path)
        camping_list = df[column_name].dropna().tolist()
        print(f"총 {len(camping_list)}개의 캠핑장 목록을 불러왔습니다.")
        return camping_list
    except FileNotFoundError:
        print(f"에러: '{file_path}' 파일을 찾을 수 없습니다.")
        return []
    except KeyError:
        print(f"에러: '{column_name}' 열을 찾을 수 없습니다.")
        return []

def initialize_driver():
    """웹 드라이버를 초기화하고 반환합니다."""
    chrome_options = Options()
    chrome_options.add_experimental_option('detach', True)  # 브라우저 자동 종료 방지
    driver = webdriver.Chrome(options=chrome_options)
    driver.implicitly_wait(IMPLICIT_WAIT)
    driver.maximize_window()
    return driver

def search_keyword(driver, keyword):
    """네이버 지도에서 특정 키워드로 검색을 수행합니다."""
    try:
        driver.get(NAVER_MAP_URL)
        search_input = WebDriverWait(driver, EXPLICIT_WAIT).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, 'input.input_search'))
        )
        search_input.clear()
        search_input.send_keys(keyword)
        search_input.send_keys(Keys.ENTER)
        time.sleep(2)  # 검색 결과 로딩 대기
        return True
    except TimeoutException:
        print(f"[{keyword}] 검색창을 찾지 못했습니다.")
        return False
    except Exception as e:
        print(f"[{keyword}] 검색 중 에러 발생: {e}")
        return False

def click_search_result(driver, keyword):
    """검색 결과 목록에서 정확히 일치하는 항목을 클릭합니다."""
    try:
        driver.switch_to.frame("searchIframe")
        xpath = f'//*[@id="_pcmap_list_scroll_container"]//span[text()="{keyword}"]'
        target_element = WebDriverWait(driver, EXPLICIT_WAIT).until(
            EC.presence_of_element_located((By.XPATH, xpath))
        )
        driver.execute_script("arguments[0].click();", target_element)
        print(f"[{keyword}] 검색 결과에서 '{keyword}'를 클릭했습니다.")
        driver.switch_to.default_content()
        return True
    except TimeoutException:
        print(f"[{keyword}] 검색 결과 목록에서 일치하는 항목을 찾지 못했습니다.")
        driver.switch_to.default_content()
        return False
    except NoSuchElementException:
        print(f"[{keyword}] 'searchIframe'을 찾지 못했습니다.")
        driver.switch_to.default_content()
        return False

def click_review_tab(driver, keyword):
    """장소 상세 페이지에서 '리뷰' 탭을 클릭하고, '더보기'를 모두 클릭합니다."""
    try:
        driver.switch_to.frame(WebDriverWait(driver, EXPLICIT_WAIT).until(
            EC.presence_of_element_located((By.ID, "entryIframe"))
        ))
        
        review_tab_xpath = "//span[contains(text(), '리뷰')]/parent::a"
        review_tab = WebDriverWait(driver, EXPLICIT_WAIT).until(
            EC.element_to_be_clickable((By.XPATH, review_tab_xpath))
        )
        driver.execute_script("arguments[0].click();", review_tab)
        print(f"[{keyword}] 리뷰 탭을 클릭했습니다.")
        time.sleep(1) # 리뷰 로딩 대기

        # '더보기' 버튼이 사라질 때까지 반복 클릭
        more_button_xpath = '//a[contains(., "더보기")]'
        while True:
            try:
                more_button = WebDriverWait(driver, 3).until(
                    EC.element_to_be_clickable((By.XPATH, more_button_xpath))
                )
                driver.execute_script("arguments[0].click();", more_button)
                print(f"[{keyword}] '더보기' 버튼을 클릭했습니다.")
                time.sleep(0.5) # 추가 로딩 대기
            except TimeoutException:
                print(f"[{keyword}] '더보기' 버튼이 더 이상 없거나, 모든 키워드를 로드했습니다.")
                break
        return True

    except TimeoutException:
        print(f"[{keyword}] 리뷰 탭 또는 'entryIframe'을 시간 내에 찾지 못했습니다.")
        return False
    except Exception as e:
        print(f"[{keyword}] 리뷰 탭 처리 중 에러 발생: {e}")
        return False
    finally:
        driver.switch_to.default_content()

def extract_keywords(driver, keyword):
    """리뷰 탭에서 키워드와 선택 인원 수를 추출합니다."""
    keyword_data = []
    try:
        driver.switch_to.frame("entryIframe")
        
        # 키워드 목록을 포함하는 ul 태그를 찾습니다.
        keyword_list_xpath = '//*[@id="app-root"]//ul[.//span[contains(@class, "AjLUF")]]' # 키워드 텍스트를 포함하는 span 기준
        ul_element = WebDriverWait(driver, EXPLICIT_WAIT).until(
            EC.presence_of_element_located((By.XPATH, keyword_list_xpath))
        )
        
        li_elements = ul_element.find_elements(By.XPATH, './li')

        if not li_elements:
            print(f"[{keyword}] 키워드 데이터를 찾을 수 없습니다.")
            return []

        for item in li_elements:
            try:
                keyword_text = item.find_element(By.XPATH, './/span[1]').text.strip()
                count_text = item.find_element(By.XPATH, './/span[2]').text.strip()
                count_number = int(re.search(r'\d+', count_text).group()) if re.search(r'\d+', count_text) else 0
                
                keyword_data.append({
                    '캠핑장명': keyword,
                    '키워드': keyword_text,
                    '선택인원': count_number
                })
            except (NoSuchElementException, AttributeError):
                continue
        
        print(f"[{keyword}] 총 {len(keyword_data)}개의 키워드를 추출했습니다.")
        return keyword_data

    except TimeoutException:
        print(f"[{keyword}] 키워드 목록을 찾을 수 없습니다.")
        return []
    except Exception as e:
        print(f"[{keyword}] 키워드 추출 중 에러 발생: {e}")
        return []
    finally:
        driver.switch_to.default_content()

def save_to_csv(data, file_path):
    """추출된 데이터를 CSV 파일에 저장합니다."""
    if not data:
        return
    
    df = pd.DataFrame(data)
    file_exists = os.path.isfile(file_path)
    
    try:
        with open(file_path, 'a', encoding='utf-8-sig', newline='') as f:
            df.to_csv(f, header=not file_exists, index=False)
        print(f"데이터가 '{file_path}'에 성공적으로 저장(추가)되었습니다.")
    except Exception as e:
        print(f"CSV 저장 중 오류 발생: {e}")

def main():
    """메인 실행 함수"""
    camping_list = get_camping_list(EXCEL_FILE_PATH, COLUMN_NAME)
    if not camping_list:
        return

    driver = initialize_driver()
    
    try:
        for keyword in camping_list:
            print(f"\n--- '{keyword}' 캠핑장 처리 시작 ---")
            if not search_keyword(driver, keyword):
                continue
            
            if not click_search_result(driver, keyword):
                # 단일 결과 페이지일 경우 바로 리뷰 탭 처리 시도
                print(f"[{keyword}] 단일 검색 결과로 가정하고 리뷰 탭으로 바로 진행합니다.")
            
            if not click_review_tab(driver, keyword):
                continue
                
            keyword_data = extract_keywords(driver, keyword)
            save_to_csv(keyword_data, CSV_FILE_PATH)
            
            time.sleep(2) # 다음 검색을 위한 대기

    finally:
        driver.quit()
        print("\n--- 모든 작업 완료. 웹 드라이버를 종료합니다. ---")

if __name__ == "__main__":
    main()