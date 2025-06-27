-- Authors 테이블 생성 (작가 정보를 저장하는 테이블)
CREATE TABLE Authors (
    id INT NOT NULL AUTO_INCREMENT COMMENT '작가의 고유 ID',
    name VARCHAR(255) NOT NULL COMMENT '작가의 이름',
    PRIMARY KEY (id)
);

-- Publishers 테이블 생성 (출판사 정보를 저장하는 테이블)
CREATE TABLE Publishers (
    id INT NOT NULL AUTO_INCREMENT COMMENT '출판사의 고유 ID',
    name VARCHAR(255) NOT NULL COMMENT '출판사의 이름',
    PRIMARY KEY (id)
);

-- Books 테이블 생성 (책 정보를 저장하는 테이블)
CREATE TABLE Books (
   id INT NOT NULL AUTO_INCREMENT COMMENT '책의 고유 ID',
   title VARCHAR(255) NOT NULL COMMENT '책의 제목',
   author_ref INT NOT NULL COMMENT '책의 저자 ID (Authors 테이블 참조)',
   publisher_ref INT NOT NULL COMMENT '책의 출판사 ID (Publishers 테이블 참조)',
   PRIMARY KEY (id)
);
