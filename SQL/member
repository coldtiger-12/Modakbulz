DROP TABLE MEMBER;

CREATE TABLE MEMBER (
	MEMBER_ID      NUMBER(10),             -- 내부관리번호 (시퀀스 사용)
	GUBUN          CHAR(1) DEFAULT 'C' NOT NULL,                            -- 회원 구분 (예: 'A' = 관리자, 'C' = 사용자)
	ID             VARCHAR2(10) NOT NULL,              -- 회원 아이디
	PWD            VARCHAR2(13) NOT NULL,              -- 비밀번호
	EMAIL          VARCHAR2(30) NOT NULL,              -- 이메일
	TEL            VARCHAR2(11) NOT NULL,              -- 전화번호
	NICKNAME       VARCHAR2(30) NOT NULL,              -- 별명
	GENDER         VARCHAR2(10),                       -- 성별 ('남자', '여자') - 선택 사항
	REGION         VARCHAR2(10),                       -- 지역 - 선택 사항
	IS_DEL         CHAR(1),       -- 탈퇴 요청 여부 ('Y' / 'N')
	DEL_DATE       TIMESTAMP,     -- 삭제 예정 시간

	-- 유니크 제약조건
	CONSTRAINT UK_MEMBER_ID       UNIQUE (ID),
	CONSTRAINT UK_MEMBER_EMAIL    UNIQUE (EMAIL),
	CONSTRAINT UK_MEMBER_TEL      UNIQUE (TEL),
	CONSTRAINT UK_MEMBER_NICKNAME UNIQUE (NICKNAME)
);


	-- 기본키 추가
	ALTER TABLE MEMBER ADD CONSTRAINT PK_MEMBER_MEMBER_ID PRIMARY KEY(MEMBER_ID);

	-- ID는 영문 대소문자 + 숫자만 허용
	ALTER TABLE MEMBER ADD CONSTRAINT CK_MEMBER_ID_FORMAT CHECK (REGEXP_LIKE(ID, '^[A-Za-z0-9]+$')
);

-- 이메일 형식
ALTER TABLE MEMBER ADD CONSTRAINT CK_MEMBER_EMAIL_FORMAT CHECK
(REGEXP_LIKE(EMAIL, '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'));

-- 비밀번호 형식 (8자 이상, 영문 대소문자 + 숫자 + 특수문자 포함)
ALTER TABLE member
ADD CONSTRAINT CK_MEMBER_PWD_FORMAT
CHECK (
    LENGTH(pwd) >= 8 AND
    REGEXP_LIKE(pwd, '.*[A-Z].*') AND     -- 대문자
    REGEXP_LIKE(pwd, '.*[a-z].*') AND     -- 소문자
    REGEXP_LIKE(pwd, '.*[0-9].*') AND     -- 숫자
    REGEXP_LIKE(pwd, '.*[!@#$%^&*()_+=-].*')  -- 특수문자
);

-- 성별: '남자', '여자'만 허용 구분: 'A', 'C'만 허용
ALTER TABLE MEMBER ADD CONSTRAINT CK_MEMBER_GENDER CHECK (GENDER IN ('남','여') OR GENDER IS NULL);
ALTER TABLE MEMBER ADD CONSTRAINT CK_MEMBER_GUBUN CHECK (GUBUN IN ('A','C'));



--시퀀스 제작 및 삭제
DROP SEQUENCE member_member_id_seq;

CREATE SEQUENCE member_member_id_seq
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

INSERT INTO MEMBER (MEMBER_ID, GUBUN, ID, PWD, EMAIL, TEL, NICKNAME, GENDER, REGION) VALUES (
										member_member_id_seq.NEXTVAL, 'C', 'kds5326','1234Kds!@#$','pbtakcm@naver.com','01011112222','Ronaldo','남','서울');
INSERT INTO MEMBER (MEMBER_ID, GUBUN, ID, PWD, EMAIL, TEL, NICKNAME, GENDER, REGION) VALUES (
										member_member_id_seq.NEXTVAL, 'A', 'kds5327','12345Kds!@#$','pbtakcm1@naver.com','01011112223','산체스','남','부산');

SELECT *
	FROM MEMBER;

SELECT COUNT(*) FROM MEMBER WHERE EMAIL = 'pbtakcm@naver.com';

SELECT MEMBER_ID, GUBUN, ID, EMAIL, TEL, NICKNAME, GENDER, REGION, IS_DEL, DEL_DATE
	FROM MEMBER
 WHERE MEMBER_ID = 1;

SELECT MEMBER_ID, GUBUN, ID, EMAIL, TEL, NICKNAME, GENDER, REGION, IS_DEL, DEL_DATE
	FROM MEMBER
 WHERE GUBUN  = 'A';

SELECT MEMBER_ID, GUBUN, ID, EMAIL, TEL, NICKNAME, GENDER, REGION, IS_DEL, DEL_DATE
	FROM MEMBER
 WHERE EMAIL = 'pbtakcm@naver.com';



UPDATE MEMBER SET
        TEL = 01012547854,
        NICKNAME = '호날두',
        REGION = '중국'
        WHERE MEMBER_ID = 1;


COMMIT;
ROLLBACK;


-- 회원 바로 탈퇴
DELETE FROM MEMBER WHERE id = 'kds5327';

-- 회원 탈퇴
-- 논리 삭제(삭제여부 Y로, 삭제예정일 지정)
UPDATE MEMBER
	 SET IS_DEL = 'Y',
	     DEL_DATE = systimestamp + INTERVAL '7' DAY
 WHERE ID = 'user123' AND IS_DEL = 'Y';

-- 물리 삭제(스케줄러 이용)
BEGIN
	DBMS_SCHEDULER.CREATE_JOB (
		job_name			=> 'JOB_DELETE_EXPIRED_MEMBERS',
		job_type			=> 'PLSQL_BLOCK',
		job_action		=> '
			BEGIN
				DELETE FROM MEMBER
				 WHERE IS_DEL = ''Y''
					AND DEL_DATE <= SYSTIMESTAMP;
			END;',
			start_date	=> SYSTIMESTAMP,
			repeat_interval => 'FREQ=DAILY; BYHOUR=2; BYMINUTE=0; BYSECOND=0', -- 매일 오전 2시
			enabled	=> TRUE,
			comments => '삭제 예정일이 지난 회원 자동 삭제'
	);
END;

/

-- 스케줄러 중단(스케줄러 작동 중엔 삭제 전 중단을 하고 하는것이 안정적이라 투입 시켜놓음)
BEGIN
	DBMS_SCHEDULER.STOP_JOB(
		job_name => 'JOB_DELETE_EXPIRED_MEMBERS'
	);
END;

/

-- 스케줄러 삭제()
BEGIN
  DBMS_SCHEDULER.DROP_JOB (
    job_name => 'JOB_DELETE_EXPIRED_MEMBERS'
  );
END;



 COMMIT;
 ROLLBACK;