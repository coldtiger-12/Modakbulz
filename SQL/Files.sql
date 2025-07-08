CREATE TABLE FILES (
    FILE_ID      NUMBER(10) PRIMARY KEY,
    -- 각 파일을 고유하게 식별하기 위한 ID

    ORIGIN_NAME  VARCHAR2(200) NOT NULL,
    -- 사용자가 업로드한 원본 파일명 (예: tent.jpg). UI에 보여줄 때 사용

    SAVE_NAME    VARCHAR2(200) NOT NULL,
    -- 서버에 저장된 실제 파일명 (예: UUID.jpg). 파일명 중복 방지 및 보안 목적

    FILE_PATH    VARCHAR2(250) NOT NULL,
    -- 파일이 저장된 서버 경로 (예: /upload/review/). 파일 정리 및 백업 시 유용

    FILE_URL     VARCHAR2(500) NOT NULL,
    -- 클라이언트에서 접근 가능한 전체 URL (예: /upload/review/uuid.jpg). 이미지 출력 시 사용

    BOARD_TYPE   VARCHAR2(20) NOT NULL 
    CHECK (BOARD_TYPE IN ('REVIEW', 'COMMUNITY', 'FAQ'),
    -- 어떤 게시판에 속한 파일인지 구분 (예: REVIEW, COMMUNITY, INQUIRY). 용도 구분에 필요

    BOARD_ID     NUMBER(10) NOT NULL,
    -- 연결된 게시글 ID. 어떤 게시글에 속한 파일인지 추적
    -- (주의: 타입과 아이디 모두 다대다 구조이므로 외래키 설정이 어렵고, 로직에서 명확히 구분 필요)

    UPLOAD_AT    TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
    -- 업로드 시각 기록. 정렬, 로그 관리 등에 활용
);

CREATE SEQUENCE FILES_SEQ
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;
