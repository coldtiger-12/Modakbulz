CREATE TABLE MEMBER (
    MEMBER_ID   NUMBER(10) PRIMARY KEY,
    GUBUN       CHAR(1) DEFAULT 'U' NOT NULL CHECK (GUBUN IN ('U', 'A')),
    ID          VARCHAR2(10) NOT NULL UNIQUE CHECK (REGEXP_LIKE(ID, '^[A-Za-z0-9]+$')),
    PWD VARCHAR2(15) NOT NULL CHECK (
    LENGTH(PWD) >= 8 AND
    REGEXP_LIKE(PWD, '^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+=-]).{8,}$')),
    EMAIL       VARCHAR2(50) NOT NULL UNIQUE CHECK (REGEXP_LIKE(EMAIL, 
                    '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')),
    TEL         VARCHAR2(11) NOT NULL UNIQUE,
    NICKNAME    VARCHAR2(20) NOT NULL UNIQUE,
    GENDER      VARCHAR2(10) CHECK (GENDER IN ('남', '여')),
    REGION      VARCHAR2(10),
    IS_DEL      CHAR(1) DEFAULT 'N' NOT NULL CHECK (IS_DEL IN ('Y', 'N')),
    DEL_DATE    TIMESTAMP
);

-- 멤버 테이블 생성
--(일단 위에 코드로는 비밀번호 무결성 오류가 나는 상황 논의 후 이 테이블 코드 사용 여부 결정 예정)
--CREATE TABLE MEMBER (
--    MEMBER_ID   NUMBER(10) PRIMARY KEY,
--    GUBUN       CHAR(1) DEFAULT 'U' NOT NULL CHECK (GUBUN IN ('U', 'A')),
--    ID          VARCHAR2(10) NOT NULL UNIQUE CHECK (REGEXP_LIKE(ID, '^[A-Za-z0-9]+$')),
--    PWD VARCHAR2(15) NOT NULL CHECK (
--    		LENGTH(PWD) >= 8 AND
--        REGEXP_LIKE(pwd, '.*[A-Z].*') AND     -- 대문자
--        REGEXP_LIKE(pwd, '.*[a-z].*') AND     -- 소문자
--        REGEXP_LIKE(pwd, '.*[0-9].*') AND     -- 숫자
--        REGEXP_LIKE(pwd, '.*[!@#$%^&*()_+=-].*')),  -- 특수문자,
--    EMAIL       VARCHAR2(50) NOT NULL UNIQUE CHECK (REGEXP_LIKE(EMAIL,
--                    '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')),
--    TEL         VARCHAR2(11) NOT NULL UNIQUE,
--    NICKNAME    VARCHAR2(20) NOT NULL UNIQUE,
--    GENDER      VARCHAR2(10) CHECK (GENDER IN ('남', '여')),
--    REGION      VARCHAR2(10),
--    IS_DEL      CHAR(1) DEFAULT 'N' NOT NULL CHECK (IS_DEL IN ('Y', 'N')),
--    DEL_DATE    TIMESTAMP
--);


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

-- 테스트 멤버 생성
INSERT INTO MEMBER(MEMBER_ID, GUBUN, ID, PWD, EMAIL, TEL, NICKNAME, GENDER, REGION, IS_DEL, DEL_DATE)
VALUES(member_member_id_seq.NEXTVAL, 'U', 'test1', 'Test123!@', 'test1@gmail.com', '01011123335', '테스터', '남', '부산', 'N', null);