INSERT INTO "C##CAMP"."MEMBER" (MEMBER_ID,GUBUN,ID,PWD,EMAIL,TEL,NICKNAME,GENDER,REGION,IS_DEL,DEL_DATE) VALUES
	 (6,'A','admin2','$2a$10$qgyAXnF5yy2zc8D9pznLjudzSLerc5uJDXNNIhOaKSKMUnKL1TQVC','wb/tCicxZ9XT13iZ9oNswFZhFlCdLXYQJAAPEwKJlng=','JlIdZXTTgfSGeaGh1/MfOGn1/1dBqoc8','관리자2','여','부산','ACTIVE',NULL),
	 (3,'A','admin','$2a$10$vjOO1c.SDzi6WgF4nSg6IuaaAkr3Xi5FtKT8QP..FIRLmdd44yUw.','uVgB4mWSqEqM943mLzyDYy2jzgIqlE7ZfbrplAiBbt4=','r2oUJDuekJhg73IU361jdULMm/5NiyYt','관리자1','여','부산','ACTIVE',NULL),
	 (4,'U','test3','$2a$10$tW1Jg8GNRXOa5IfhZgvx7eO/1W31YvyTeyzf1mS1.Bh1DC/fWPFi.','7FafuWIlwZbd2lpoJIiS//mk2DcciY09kHed5HBrnaU=','wZnVFwS7lRAlqIaeOLPGmgybfqOB4xqZ','태양만세','여','인천','ACTIVE',NULL),
	 (5,'U','test1','$2a$10$ukO3Ky3F1CYQD/.H6EcHAOrZpjod0mLpPyRXnpCwXIMJYBG6plqwq','XbLptYFMND8IYDTHqtPLWQsKUAPqFMd6/bZro/gUGuQ=','SNMtzc9go1V7c+4RvY9fjsPB2JumPEWC','테스트1','남','서울','ACTIVE',NULL);

INSERT INTO "C##CAMP".CAMPSITES (CONTENT_ID,SC_C,VIEW_C,SCORE) VALUES
	 (357,1,0,3);

INSERT INTO "C##CAMP".SCRAP (SCRAP_ID,MEMBER_ID,CONTENT_ID,CREATED_AT,ADDR1,FIRST_IMAGE_URL,FACLT_NM) VALUES
	 (7,5,101769,TIMESTAMP '2025-07-25 14:24:12.136380','충북 제천시 청풍면 용곡길 218','https://gocamping.or.kr/upload/camp/101769/thumb/thumb_720_1408Hw0gkTIQpZoQauheLDx9.jpg','청풍호반 캠핑장'),
	 (2,3,101890,TIMESTAMP '2025-07-25 14:20:23.857792','경기 화성시 동탄순환대로24길 185 (중동)','https://gocamping.or.kr/upload/camp/101890/thumb/thumb_720_8517Map7DYXlDeq7hH7Pyhit.jpg','무봉산 자연휴양림'),
	 (3,3,101880,TIMESTAMP '2025-07-25 14:20:26.140956','경기 연천군 미산면 마동로196번길 77-44','https://gocamping.or.kr/upload/camp/101880/thumb/thumb_720_1693TFQkZ2UU7MhSZoPNq6Ss.jpg','여우별 캠핑장'),
	 (4,3,101850,TIMESTAMP '2025-07-25 14:20:27.659154','전북특별자치도 고창군 상하면 진암구시포로 547-21','https://gocamping.or.kr/upload/camp/101850/thumb/thumb_720_27739USR7SGHMq36EGOZ3W3i.jpg','구시포글램핑'),
	 (5,3,101845,TIMESTAMP '2025-07-25 14:20:29.354577','강원특별자치도 철원군 동송읍 담터길 372','https://gocamping.or.kr/upload/camp/101845/thumb/thumb_720_57017bl4g0mLyXdJzyr6ODs1.jpg','담터휴 캠핑장'),
	 (6,5,101880,TIMESTAMP '2025-07-25 14:24:02.473133','경기 연천군 미산면 마동로196번길 77-44','https://gocamping.or.kr/upload/camp/101880/thumb/thumb_720_1693TFQkZ2UU7MhSZoPNq6Ss.jpg','여우별 캠핑장'),
	 (8,5,357,TIMESTAMP '2025-07-25 16:13:43.746470','경기도 구리시 왕숙천로 11-140','https://gocamping.or.kr/upload/camp/357/thumb/thumb_720_3831p9MDcVndYponaw6mQniB.jpg','구리 토평 가족캠핑장');

INSERT INTO "C##CAMP".CAMPING_INFO (CONTENTID,RESULTCODE,RESULTMSG,NUMOFROWS,PAGENO,TOTALCOUNT,FACLTNM,LINEINTRO,INTRO,INSRNCAT,MANAGESTTUS,HVOFBGNDE,HVOFENDDLE,FEATURENM,INDUTY,LCTCL,DONM,SIGUNGUNM,ZIPCODE,ADDR1,ADDR2,DIRECTION,TEL,HOMEPAGE,GNRLSITECO,AUTOSITECO,GLAMPSITECO,CARAVSITECO,INDVDLCARAVSITECO,SITEDSTNC,GLAMPINNERFCLTY,CARAVINNERFCLTY,OPERPDCL,OPERDECL,TRLERACMPNYAT,CARAVACMPNYAT,SBRSCL,THEMAENVRNCL,EQPMNLENDCL,ANIMALCMGCL,FIRSTIMAGEURL,CREATEDTIME,MODIFIEDTIME) VALUES
	 (357,NULL,NULL,NULL,NULL,NULL,'구리 토평 가족캠핑장','한강과 왕숙천 만나는 토평동에 위치한 구리토평가족캠핑장!',TO_CLOB('오늘, 일상이 지루할 때 가볍게 떠나 야영을 즐길 수 있는 도심속 캠핑장입니다. 물이 흐르는 지형을 그대로 보존하여 조성한 친환경 캠핑장으로, 주변에 왕숙천과 한강 그리고 체험 · 교육 시설이 풍부한 차별화된 휴식을 제공하는 공간입니다.'),NULL,NULL,NULL,NULL,'차량 진출입로 및 공간이 협소하여 카라반, 트레일러의 출입이 불가합니다.','자동차야영장','강,도심',NULL,NULL,NULL,'경기도 구리시 왕숙천로 11-140',NULL,NULL,'031-550-3788','https://guriuc.or.kr/cmsManage/contents.do?cmsSeq=101',3,19,0,0,0,NULL,NULL,NULL,'봄,여름,가을,겨울',NULL,NULL,NULL,'전기,무선인터넷,온수,놀이터,산책로','일몰명소,걷기길',NULL,NULL,'https://gocamping.or.kr/upload/camp/357/thumb/thumb_720_3831p9MDcVndYponaw6mQniB.jpg',TIMESTAMP '2025-07-25 14:01:01.138340',TIMESTAMP '2025-07-25 14:01:01.138340');

INSERT INTO "C##CAMP".COMMUNITY (CO_ID,MEMBER_ID,TITLE,WRITER,CONTENT,CREATED_AT,UPDATED_AT,VIEW_C) VALUES
	 (9,5,'커뮤니티 테스트2','테스트1',TO_CLOB('커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트 태양만세'),TIMESTAMP '2025-07-25 16:14:40.136636',TIMESTAMP '2025-07-25 16:14:43.939169',1),
	 (8,5,'커뮤니티 테스트','테스트1',TO_CLOB('커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트커뮤니티 테스트'),TIMESTAMP '2025-07-25 16:14:31.230314',TIMESTAMP '2025-07-25 16:15:13.595544',4);

INSERT INTO "C##CAMP".CO_COMMENT (C_COM_ID,CO_ID,MEMBER_ID,WRITER,CONTENT,CREATED_AT,UPDATED_AT,PRC_COM_ID) VALUES
	 (3,8,5,'테스트1',TO_CLOB('댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트'),TIMESTAMP '2025-07-25 16:14:57.527821',NULL,NULL),
	 (4,8,5,'테스트1',TO_CLOB('대댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트댓글 테스트'),TIMESTAMP '2025-07-25 16:15:00.896310',TIMESTAMP '2025-07-25 16:15:13.585163',3);

INSERT INTO "C##CAMP".FAQ (FAQ_ID,MEMBER_ID,WRITER,CONTENT,CREATED_AT) VALUES
	 (3,5,'테스트1',TO_CLOB('관리자 재밌음 관리자 재밌음 관리자 재밌음 관리자 재밌음 관리자 재밌음 관리자 재밌음 관리자 재밌음 관리자 재밌음 관리자 재밌음 관리자 재밌음 관리자 재밌음 관리자 재밌음 관리자 재밌음 관리자 재밌음 관리자 재밌음 '),TIMESTAMP '2025-07-25 16:14:14.511774');

INSERT INTO "C##CAMP".REVIEW (REV_ID,CONTENT_ID,MEMBER_ID,WRITER,CONTENT,CREATED_AT,UPDATED_AT,SCORE) VALUES
	 (4,357,5,'테스트1',TO_CLOB('캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 캠핑장 별로임 '),TIMESTAMP '2025-07-25 16:13:55.142073',NULL,2),
	 (2,101880,5,'테스트1',TO_CLOB('리뷰 테스트 중입니다'),TIMESTAMP '2025-07-25 14:23:56.763312',NULL,5),
	 (3,357,5,'테스트1',TO_CLOB('캠핑장 재밌음캠핑장 재밌음캠핑장 재밌음캠핑장 재밌음캠핑장 재밌음캠핑장 재밌음캠핑장 재밌음캠핑장 재밌음캠핑장 재밌음캠핑장 재밌음캠핑장 재밌음캠핑장 재밌음캠핑장 재밌음캠핑장 재밌음'),TIMESTAMP '2025-07-25 16:13:37.918662',NULL,4);
