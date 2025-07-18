-- 모닥불스 DB 설계 --
-- 모든 테이블 및 시퀀스 초기화를 위한 DROP 문 (개발 초기 단계에서 유용)
DROP TABLE FILES CASCADE CONSTRAINTS;
DROP TABLE SCRAP CASCADE CONSTRAINTS;
DROP TABLE FAQ_COMMENT CASCADE CONSTRAINTS;
DROP TABLE FAQ CASCADE CONSTRAINTS;
DROP TABLE CO_COMMENT CASCADE CONSTRAINTS;
DROP TABLE COMMUNITY CASCADE CONSTRAINTS;
DROP TABLE REVIEW_KEYWORD CASCADE CONSTRAINTS;
DROP TABLE KEYWORD CASCADE CONSTRAINTS;
DROP TABLE REVIEW CASCADE CONSTRAINTS;
DROP TABLE CAMPSITES CASCADE CONSTRAINTS;
DROP TABLE CAMPING_INFO CASCADE CONSTRAINTS;
DROP TABLE MEMBER CASCADE CONSTRAINTS;

DROP SEQUENCE review_keyword_seq;
DROP SEQUENCE keyword_seq;
DROP SEQUENCE member_member_id_seq;
DROP SEQUENCE review_rev_id_seq;
DROP SEQUENCE community_co_id_seq;
DROP SEQUENCE co_comment_seq;
DROP SEQUENCE camp_faq_id_seq;
DROP SEQUENCE faq_comment_seq;
DROP SEQUENCE camp_scrap_id_seq;
DROP SEQUENCE FILES_SEQ;
-- 1. 회원 정보 DB --------------------------------------------------------------------------

-- 회원 정보 DB 삭제(기존) - 외래키 묶인것들 무시하고 강제 삭제문 추가
DROP TABLE MEMBER CASCADE CONSTRAINTS;

-- 회원 정보 DB 생성
CREATE TABLE MEMBER (
    MEMBER_ID   NUMBER(10) PRIMARY KEY,
    GUBUN       CHAR(1) DEFAULT 'U' NOT NULL CHECK (GUBUN IN ('U', 'A')),
    ID          VARCHAR2(10) NOT NULL UNIQUE CHECK (REGEXP_LIKE(ID, '^[A-Za-z0-9]+$')),
    PWD         VARCHAR2(255) NOT NULL,
    EMAIL       VARCHAR2(255) NOT NULL UNIQUE, -- 제약조건은 DAO에서 암호화 후 처리하므로 여기서는 제거해도 무방합니다.
    TEL         VARCHAR2(255) NOT NULL UNIQUE, -- 개인정보는 암호화되므로 길이를 늘립니다.
    NICKNAME    VARCHAR2(20) NOT NULL UNIQUE,
    GENDER      VARCHAR2(10) CHECK (GENDER IN ('남', '여')),
    REGION      VARCHAR2(10),
    IS_DEL      CHAR(1) DEFAULT 'N' NOT NULL CHECK (IS_DEL IN ('Y', 'N')),
    DEL_DATE    TIMESTAMP
);
CREATE SEQUENCE member_member_id_seq
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

UPDATE MEMBER
SET IS_DEL = 'Y',
    DEL_DATE = SYSTIMESTAMP + INTERVAL '7' DAY
WHERE ID = 'user123' AND IS_DEL = 'N';

BEGIN
  DBMS_SCHEDULER.CREATE_JOB (
    job_name        => 'JOB_DELETE_EXPIRED_MEMBERS',
    job_type        => 'PLSQL_BLOCK',
    job_action      => '
      BEGIN
        DELETE FROM MEMBER
        WHERE IS_DEL = ''Y''
          AND DEL_DATE <= SYSTIMESTAMP;
        COMMIT;
      END;',
    start_date      => SYSTIMESTAMP,
    repeat_interval => 'FREQ=DAILY; BYHOUR=2; BYMINUTE=0; BYSECOND=0',
    enabled         => TRUE,
    comments        => '삭제 예정일이 지난 회원 자동 삭제'
  );
END;

SELECT * FROM MEMBER;
-------------------------------------------------------------------------------------------
-- 2. 캠핑장 정보 저장 DB ---------------------------------------------------------------------

-- 캠핑장 정보 저장 테이블 삭제(기존)
DROP TABLE CAMPING_INFO;

-- 캠핑장 정보 저장 테이블 생성
CREATE TABLE CAMPING_INFO (
    contentId          NUMBER(10) PRIMARY KEY,
    resultCode         VARCHAR2(10),
    resultMsg          VARCHAR2(100),
    numOfRows          NUMBER(5),
    pageNo             NUMBER(5),
    totalCount         NUMBER(10),
    facltNm            VARCHAR2(100),
    lineIntro            VARCHAR2(300),
    intro              CLOB,
    insrncAt           CHAR(1) CHECK (insrncAt IN ('Y', 'N')),
    manageSttus        VARCHAR2(20),
    hvofBgnde          DATE,
    hvofEnddle         DATE,
    featureNm          VARCHAR2(2000),
    induty             VARCHAR2(200) DEFAULT '일반야영장',
    lctCl              VARCHAR2(100),
    doNm               VARCHAR2(50),
    sigunguNm          VARCHAR2(50),
    zipcode            VARCHAR2(10),
    addr1              VARCHAR2(255),
    addr2              VARCHAR2(255),
    direction          VARCHAR2(2000),
    tel                VARCHAR2(20),
    homepage           VARCHAR2(2000),
    gnrlSiteCo         NUMBER(5) DEFAULT 0,
    autoSiteCo         NUMBER(5) DEFAULT 0,
    glampSiteCo        NUMBER(5) DEFAULT 0,
    caravSiteCo        NUMBER(5) DEFAULT 0,
    indvdlCaravSiteCo  NUMBER(5) DEFAULT 0,
    sitedStnc          NUMBER(10,2),
    glampInnerFclty    VARCHAR2(1000),
    caravInnerFclty    VARCHAR2(1000),
    operPdCl           VARCHAR2(100),
    operDeCl           VARCHAR2(100),
    trlerAcmpnyAt      CHAR(1) CHECK (trlerAcmpnyAt IN ('Y', 'N')),
    caravAcmpnyAt      CHAR(1) CHECK (caravAcmpnyAt IN ('Y', 'N')),
    sbrsCl             VARCHAR2(1000),
    themaEnvrnCl       VARCHAR2(255),
    eqpmnLendCl        VARCHAR2(255),
    animalCmgCl        VARCHAR2(255),
    firstImageUrl      VARCHAR2(2000),
    createdtime        TIMESTAMP DEFAULT SYSTIMESTAMP,
    modifiedtime       TIMESTAMP DEFAULT SYSTIMESTAMP
);

SELECT * FROM CAMPING_INFO;
-------------------------------------------------------------------------------------------
-- 3. 캠핑장 정보 DB -------------------------------------------------------------------------

-- 캠핑장 정보 테이블 삭제(기존)
DROP TABLE CAMPSITES;

-- 캠핑장 정보 테이블 생성
CREATE TABLE CAMPSITES (
    CONTENT_ID   NUMBER(10) PRIMARY KEY
                REFERENCES CAMPING_INFO(CONTENTID)
                ON DELETE CASCADE,
    SC_C        NUMBER DEFAULT 0 NOT NULL,
    VIEW_C      NUMBER DEFAULT 0 NOT NULL,
    SCORE       NUMBER DEFAULT 0 NOT NULL
                CHECK (SCORE BETWEEN 0 AND 5)
);

-- 캠핑장 동기화 이후 실행
INSERT INTO CAMPSITES (CONTENT_ID, SC_C, VIEW_C, SCORE)
SELECT C.CONTENTID, 0, 0, 1
FROM CAMPING_INFO C
WHERE NOT EXISTS (
  SELECT 1 FROM CAMPSITES S WHERE S.CONTENT_ID = C.CONTENTID
);

SELECT * FROM CAMPSITES;
-------------------------------------------------------------------------------------------
-- 4. 리뷰 게시판 정보 DB ---------------------------------------------------------------------

-- 리뷰 테이블 삭제(기존)
DROP TABLE REVIEW;

-- 리뷰 테이블 생성
CREATE TABLE REVIEW (
    REV_ID       NUMBER(10) PRIMARY KEY,
    CONTENT_ID   NUMBER(10) NOT NULL
                 REFERENCES CAMPSITES(CONTENT_ID)
                 ON DELETE CASCADE,
    MEMBER_ID    NUMBER(10) NOT NULL
                 REFERENCES MEMBER(MEMBER_ID)
                 ON DELETE CASCADE,
    WRITER       VARCHAR2(20) NOT NULL,
    CONTENT      CLOB NOT NULL,
    CREATED_AT   TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    UPDATED_AT   TIMESTAMP,
    SCORE        NUMBER(10) DEFAULT 0 NOT NULL
                 CHECK (SCORE BETWEEN 0 AND 5)
);

CREATE SEQUENCE review_rev_id_seq
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_set_review_updated_at
BEFORE UPDATE ON REVIEW
FOR EACH ROW
BEGIN
    :NEW.UPDATED_AT := SYSTIMESTAMP;
END;

SELECT * FROM review;
-------------------------------------------------------------------------------------------
-- 5. 자유 게시판 정보 DB ---------------------------------------------------------------------

-- 자유 게시판 정보 DB 삭제(기존)
DROP TABLE COMMUNITY;

-- 자유 게시판 정보 DB 생성
CREATE TABLE COMMUNITY (
    CO_ID        NUMBER(10) PRIMARY KEY,
    MEMBER_ID    NUMBER(10) NOT NULL
                          REFERENCES MEMBER(member_id)
                          ON DELETE CASCADE,
    TITLE        VARCHAR2(50) NOT NULL,
    WRITER       VARCHAR2(20) NOT NULL,
    CONTENT      CLOB NOT NULL,
    CREATED_AT   TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    UPDATED_AT   TIMESTAMP,
    VIEW_C       NUMBER(10) DEFAULT 0 NOT NULL
);

CREATE SEQUENCE community_co_id_seq
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_set_community_updated_at
BEFORE UPDATE ON COMMUNITY
FOR EACH ROW
BEGIN
    :NEW.UPDATED_AT := SYSTIMESTAMP;
END;

SELECT * FROM community;
-------------------------------------------------------------------------------------------
-- 6. 자유 게시판 댓글 정보 DB -----------------------------------------------------------------

-- 자유 게시판 댓글 정보 DB 삭제(기존)
DROP TABLE CO_COMMENT;

-- 자유 게시판 댓글 정보 DB 생성
CREATE TABLE CO_COMMENT (
C_COM_ID     NUMBER(10) PRIMARY KEY,
CO_ID       NUMBER(10) NOT NULL
                REFERENCES COMMUNITY(co_id)
                ON DELETE CASCADE,
MEMBER_ID    NUMBER(10) NOT NULL
                REFERENCES MEMBER(member_id)
                ON DELETE CASCADE,
WRITER       VARCHAR2(20) NOT NULL,
CONTENT      CLOB NOT NULL,
CREATED_AT   TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
UPDATED_AT   TIMESTAMP,
PRC_COM_ID    NUMBER(10)
                       REFERENCES CO_COMMENT(c_com_id)
                       ON DELETE CASCADE
);

CREATE SEQUENCE co_comment_seq
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_set_co_comment_updated_at
BEFORE UPDATE ON CO_COMMENT
FOR EACH ROW
BEGIN
    :NEW.UPDATED_AT := SYSTIMESTAMP;
END;

SELECT * FROM co_comment;
-------------------------------------------------------------------------------------------
-- 7. 문의사항 게시판 정보 DB ------------------------------------------------------------------

-- 문의사항 게시판 정보 DB 삭제(기존)
DROP TABLE FAQ;

-- 문의사항 게시판 정보 DB 생성
CREATE TABLE FAQ (
    FAQ_ID    NUMBER(10) PRIMARY KEY,
    MEMBER_ID   NUMBER(10) NOT NULL
                REFERENCES MEMBER(MEMBER_ID)
                ON DELETE CASCADE,
    WRITER    VARCHAR2(20) NOT NULL,
    CONTENT    CLOB NOT NULL,
    CREATED_AT  TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE SEQUENCE camp_faq_id_seq
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

SELECT * FROM faq;
-------------------------------------------------------------------------------------------
-- 8. 문의사항 게시판 댓글 정보 DB --------------------------------------------------------------

-- 문의사항 게시판 댓글 정보 DB 삭제(기존)
DROP TABLE FAQ_COMMENT;

-- 문의사항 게시판 댓글 정보 DB 생성
CREATE TABLE FAQ_COMMENT (
F_COM_ID     NUMBER(10) PRIMARY KEY,
FAQ_ID       NUMBER(10) NOT NULL
                REFERENCES FAQ(faq_id)
                ON DELETE CASCADE,
MEMBER_ID    NUMBER(10) NOT NULL
                REFERENCES MEMBER(member_id)
                ON DELETE CASCADE,
WRITER       VARCHAR2(20) NOT NULL,
CONTENT      CLOB NOT NULL,
CREATED_AT   TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
UPDATED_AT   TIMESTAMP,
PRC_COM_ID    NUMBER(10)
                       REFERENCES FAQ_COMMENT(f_com_id)
                       ON DELETE CASCADE
);

CREATE SEQUENCE faq_comment_seq
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_set_faq_comment_updated_at
BEFORE UPDATE ON FAQ_COMMENT
FOR EACH ROW
BEGIN
    :NEW.UPDATED_AT := SYSTIMESTAMP;
END;

SELECT * FROM faq_comment;
-------------------------------------------------------------------------------------------
-- 9. 스크랩 정보 DB -------------------------------------------------------------------------

-- 스크랩 정보 DB 삭제(기존)
DROP TABLE SCRAP;

-- 스크랩 정보 DB 생성
CREATE TABLE SCRAP (
    SCRAP_ID    NUMBER(10) PRIMARY KEY,
    MEMBER_ID   NUMBER(10) NOT NULL
                REFERENCES MEMBER(MEMBER_ID)
                ON DELETE CASCADE,
    CONTENT_ID  NUMBER(10) NOT NULL
                REFERENCES CAMPSITES(CONTENT_ID)
                ON DELETE CASCADE,
    CREATED_AT  TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    ADDR1 VARCHAR2(255),
    FIRST_IMAGE_URL VARCHAR2(1000),
    FACLT_NM VARCHAR2(200)
);

CREATE SEQUENCE camp_scrap_id_seq
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

SELECT * FROM scrap;
-------------------------------------------------------------------------------------------
-- 10. 파일 관리 정보 DB ---------------------------------------------------------------------

-- 파일 관리 정보 DB 삭제(기존)
DROP TABLE FILES;

-- 파일 관리 정보 DB 생성
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
    CHECK (BOARD_TYPE IN ('REVIEW', 'COMMUNITY', 'FAQ')),
    -- 어떤 게시판에 속한 파일인지 구분 (예: REVIEW, COMMUNITY, FAQ). 용도 구분에 필요
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

SELECT * FROM files;
-------------------------------------------------------------------------------------------
-- 11. 지도 정보 DB--------------------------------------------------------------------------
CREATE TABLE CAMP_LOCATION (
    location_id   NUMBER(10) PRIMARY KEY,
    contentId     NUMBER(10) NOT NULL REFERENCES CAMPING_INFO(contentId) ON DELETE CASCADE,
    mapY          VARCHAR2(30),
    mapX          VARCHAR2(30),
    address       VARCHAR2(255)
);
SELECT * FROM camp_location;
-------------------------------------------------------------------------------------------
-- 12. 키워드 정보 DB------------------------------------------------------------------------
CREATE TABLE KEYWORD (
    KEYWORD_ID NUMBER(10) PRIMARY KEY,
    WORDS      VARCHAR2(255) UNIQUE NOT NULL
);

CREATE SEQUENCE keyword_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
-------------------------------------------------------------------------------------------
-- 13. 리뷰 키워드 정보 DB--------------------------------------------------------------------
CREATE TABLE REVIEW_KEYWORD (
    REV_ID     NUMBER(10) NOT NULL REFERENCES REVIEW(REV_ID) ON DELETE CASCADE,
    KEYWORD_ID NUMBER(10) NOT NULL REFERENCES KEYWORD(KEYWORD_ID) ON DELETE CASCADE,
    PRIMARY KEY (REV_ID, KEYWORD_ID)
    );

CREATE SEQUENCE review_keyword_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
