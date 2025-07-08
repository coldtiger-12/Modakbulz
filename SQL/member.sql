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