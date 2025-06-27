-- Authors 테이블에 데이터 삽입
INSERT INTO Authors (name) VALUES
('허정준'),
('박해선'),
('오렐리앙 제롱'),
('조민정'),
('사이토 고키'),
('김진중'),
('오노 사토시'),
('유호석');

-- Publishers 테이블에 데이터 삽입
INSERT INTO Publishers (name) VALUES
('책만'),
('한빛미디어'),
('리코멘드'),
('제이펍');

-- Books 테이블에 데이터 삽입
INSERT INTO Books (title, author_ref, publisher_ref) VALUES
('LLM을 활용한 실전 AI 애플리케이션 개발', 1, 1),
('혼자 공부하는 머신러닝+딥러닝', 2, 2),
('핸즈온 머신러닝', 3, 2),
('챗GPT 제대로 써먹기', 4, 2),
('밑바닥부터 시작하는 딥러닝', 5, 2),
('최고의 프롬프트 엔지니어링 강의', 6, 3),
('소프트웨어 개발에 ChatGPT 사용하기', 7, 4),
('제가 만든 GPT는 당신이 만든 GPT와 전혀 다릅니다', 8, 3);
