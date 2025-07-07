CREATE TABLE CAMPING_TABLE (
    resultCode        VARCHAR2(10),                             -- 결과코드
    resultMsg         VARCHAR2(100),                            -- 결과메시지
    numOfRows         NUMBER(5),                                -- 한 페이지 결과 수
    pageNo            NUMBER(5),                                -- 페이지 번호
    totalCount        NUMBER(10),                               -- 전체 결과 수
    contentId         NUMBER(10)      NOT NULL PRIMARY KEY,     -- 콘텐츠 ID
    facltNm           VARCHAR2(100)   NOT NULL,                 -- 야영장명
    intro             CLOB,                                     -- 소개
    insrncAt          CHAR(1)         CHECK (insrncAt IN ('Y', 'N')), -- 영업배상책임보험 가입여부(Y/N)
    manageSttus       VARCHAR2(20),                             -- 관리상태 (운영, 휴장 등)
    hvofBgnde         DATE,                                     -- 휴가철 시작일
    hvofEnddle        DATE,                                     -- 휴가철 종료일
    featureNm         VARCHAR2(2000),                           -- 특징
    induty            VARCHAR2(100),                            -- 업종 (일반야영장, 자동차야영장 등)
    lctCl             VARCHAR2(100),                            -- 입지구분 (해변, 섬, 산, 숲, 도심 등)
    doNm              VARCHAR2(50),                             -- 도 이름
    sigunguNm         VARCHAR2(50),                             -- 시군구 이름
    zipcode           VARCHAR2(10),                             -- 우편번호
    addr1             VARCHAR2(255),                            -- 기본 주소
    addr2             VARCHAR2(255),                            -- 상세 주소
    direction         VARCHAR2(2000),                           -- 찾아오는 길
    tel               VARCHAR2(20),                             -- 전화번호
    homepage          VARCHAR2(2000),                           -- 홈페이지 URL
    gnrlSiteCo        NUMBER(5)       DEFAULT 0,                -- 주요시설 일반야영장 사이트 수
    autoSiteCo        NUMBER(5)       DEFAULT 0,                -- 주요시설 자동차야영장 사이트 수
    glampSiteCo       NUMBER(5)       DEFAULT 0,                -- 주요시설 글램핑 사이트 수
    caravSiteCo       NUMBER(5)       DEFAULT 0,                -- 주요시설 카라반 사이트 수
    indvdlCaravSiteCo NUMBER(5)       DEFAULT 0,                -- 주요시설 개인 카라반 사이트 수
    sitedStnc         NUMBER(10, 2),                            -- 사이트간 거리 (단위: M)
    glampInnerFclty   VARCHAR2(1000),                           -- 글램핑 내부 시설
    caravInnerFclty   VARCHAR2(1000),                           -- 카라반 내부 시설
    operPdCl          VARCHAR2(100),                            -- 운영기간 (봄, 여름, 가을, 겨울)
    operDeCl          VARCHAR2(100),                            -- 운영일 (평일, 주말, 항시)
    trlerAcmpnyAt     CHAR(1)         CHECK (trlerAcmpnyAt IN ('Y', 'N')), -- 개인 트레일러 동반 여부(Y/N)
    caravAcmpnyAt     CHAR(1)         CHECK (caravAcmpnyAt IN ('Y', 'N')), -- 개인 카라반 동반 여부(Y/N)
    sbrsCl            VARCHAR2(1000),                           -- 부대시설
    themaEnvrnCl      VARCHAR2(255),                            -- 테마환경 (일출/일몰, 수상레저, 체험활동 등)
    eqpmnLendCl       VARCHAR2(255),                            -- 캠핑장비 대여 목록
    animalCmgCl       VARCHAR2(255),                            -- 반려동물 동반 가능 정보
    firstImageUrl     VARCHAR2(2000),                           -- 대표 이미지 URL
    createdtime       TIMESTAMP       DEFAULT SYSTIMESTAMP,     -- 등록일시
    modifiedtime      TIMESTAMP       DEFAULT SYSTIMESTAMP      -- 수정일시
);

SELECT * FROM CAMPING_TABLE;