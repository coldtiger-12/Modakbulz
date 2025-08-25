import pandas as pd
import os

def read_camping_data():
    """Excel 파일에서 캠핑장 데이터 읽기"""
    try:
        # Excel 파일 경로
        excel_path = "../../camping_data.xlsx"
        
        # Excel 파일 읽기
        df = pd.read_excel(excel_path)
        
        print(f"Excel 파일 읽기 성공!")
        print(f"총 {len(df)}개의 행이 있습니다.")
        print(f"컬럼명: {list(df.columns)}")
        
        # 처음 5개 행 출력
        print("\n처음 5개 행:")
        print(df.head())
        
        # 캠핑장 데이터 추출 (컬럼명에 따라 조정 필요)
        camping_data = []
        
        # 일반적인 컬럼명들 시도
        possible_id_columns = ['contentId', 'content_id', 'id', 'camping_id', '번호']
        possible_name_columns = ['facltNm', 'faclt_nm', 'name', 'camping_name', '캠핑장명', '시설명']
        possible_address_columns = ['addr1', 'addr_1', 'address', '주소', 'addr']
        
        id_column = None
        name_column = None
        address_column = None
        
        # ID 컬럼 찾기
        for col in possible_id_columns:
            if col in df.columns:
                id_column = col
                break
        
        # 이름 컬럼 찾기
        for col in possible_name_columns:
            if col in df.columns:
                name_column = col
                break
        
        # 주소 컬럼 찾기
        for col in possible_address_columns:
            if col in df.columns:
                address_column = col
                break
        
        print(f"\n사용할 컬럼:")
        print(f"ID 컬럼: {id_column}")
        print(f"이름 컬럼: {name_column}")
        print(f"주소 컬럼: {address_column}")
        
        if id_column and name_column:
            for index, row in df.iterrows():
                camping_id = row[id_column]
                camping_name = row[name_column]
                address = row[address_column] if address_column else "주소 정보 없음"
                
                camping_data.append((camping_id, camping_name, address))
            
            print(f"\n총 {len(camping_data)}개의 캠핑장 데이터를 추출했습니다.")
            
            # 처음 10개 출력
            print("\n처음 10개 캠핑장:")
            for i, (camping_id, camping_name, address) in enumerate(camping_data[:10], 1):
                print(f"{i}. ID: {camping_id}, 이름: {camping_name}, 주소: {address}")
            
            return camping_data
        else:
            print("필요한 컬럼을 찾을 수 없습니다.")
            return []
            
    except Exception as e:
        print(f"Excel 파일 읽기 실패: {e}")
        return []

if __name__ == "__main__":
    camping_data = read_camping_data() 