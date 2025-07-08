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