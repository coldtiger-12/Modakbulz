// React 앱 초기화
const { useState, useEffect } = React;



// 날씨 위젯 컴포넌트
const WeatherWidget = ({ initialLocation = '서울' }) => {
  const [weatherData, setWeatherData] = useState(null);
  const [location, setLocation] = useState(initialLocation);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // 도시 목록
  const cities = [
    { name: '서울', fullName: '서울특별시' },
    { name: '부산', fullName: '부산광역시' },
    { name: '대구', fullName: '대구광역시' },
    { name: '인천', fullName: '인천광역시' },
    { name: '광주', fullName: '광주광역시' },
    { name: '대전', fullName: '대전광역시' },
    { name: '울산', fullName: '울산광역시' },
    { name: '세종', fullName: '세종특별자치시' },
    { name: '경기', fullName: '경기도' },
    { name: '강원', fullName: '강원도' },
    { name: '충북', fullName: '충청북도' },
    { name: '충남', fullName: '충청남도' },
    { name: '전북', fullName: '전라북도' },
    { name: '전남', fullName: '전라남도' },
    { name: '경북', fullName: '경상북도' },
    { name: '경남', fullName: '경상남도' },
    { name: '제주', fullName: '제주특별자치도' }
  ];

  // 날씨 아이콘 매핑
  const getWeatherIcon = (weatherCode) => {
    const icons = {
      '01': '☀️', // 맑음
      '02': '🌤️', // 구름조금
      '03': '⛅', // 구름많음
      '04': '☁️', // 흐림
      '09': '🌧️', // 소나기
      '10': '🌦️', // 비
      '11': '⛈️', // 번개
      '13': '🌨️', // 눈
      '50': '🌫️'  // 안개
    };
    return icons[weatherCode] || '🌤️';
  };

  // 캠핑 지수 계산
  const getCampingIndex = (temp, humidity, windSpeed, weatherCode) => {
    let score = 5;
    
    // 온도 체크 (15-25도가 최적)
    if (temp < 10 || temp > 30) score -= 2;
    else if (temp < 15 || temp > 25) score -= 1;
    
    // 습도 체크 (40-70%가 최적)
    if (humidity > 80 || humidity < 30) score -= 1;
    
    // 바람 체크 (5m/s 이하가 최적)
    if (windSpeed > 8) score -= 2;
    else if (windSpeed > 5) score -= 1;
    
    // 날씨 체크
    if (weatherCode.startsWith('09') || weatherCode.startsWith('11')) score -= 2;
    else if (weatherCode.startsWith('10')) score -= 1;
    else if (weatherCode.startsWith('13')) score -= 1;
    
    return Math.max(1, score);
  };

  // 날씨 데이터 가져오기 (기상청 API 호출)
  const fetchWeatherData = async () => {
    try {
      console.log('날씨 데이터 요청 시작 - 위치:', location);
      setIsLoading(true);
      setError(null);
      
      // 타임아웃 설정과 함께 API 호출
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 15000); // 15초 타임아웃
      
      const response = await fetch(`/api/weather/current?location=${encodeURIComponent(location)}`, {
        signal: controller.signal
      });
      
      clearTimeout(timeoutId);
      
      if (!response.ok) {
        let errorMessage = `날씨 API 호출에 실패했습니다. (${response.status})`;
        
        try {
          const errorData = await response.json();
          if (errorData.error) {
            errorMessage = errorData.error;
          }
        } catch (parseError) {
          // JSON 파싱 실패 시 텍스트로 읽기 시도
          try {
            const errorText = await response.text();
            console.error('날씨 API 응답 오류:', response.status, errorText);
            if (errorText.includes('<!DOCTYPE')) {
              errorMessage = '서버에서 잘못된 응답을 받았습니다. API 키를 확인해주세요.';
            }
          } catch (textError) {
            console.error('응답 읽기 실패:', textError);
          }
        }
        
        throw new Error(errorMessage);
      }
      
      const weatherData = await response.json();
      
      if (weatherData.error) {
        console.error('날씨 API 에러:', weatherData.error);
        throw new Error(weatherData.error);
      }
      
      setWeatherData(weatherData);
      console.log('날씨 데이터 로드 성공:', weatherData);
    } catch (err) {
      console.error('날씨 데이터 로딩 오류:', err);
      if (err.name === 'AbortError') {
        setError('날씨 정보 로딩 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.');
      } else {
        setError(`날씨 정보를 불러오는데 실패했습니다: ${err.message}`);
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchWeatherData();
    
    // 5분마다 날씨 정보 업데이트
    const interval = setInterval(fetchWeatherData, 300000);
    
    return () => clearInterval(interval);
  }, [location]);

  if (isLoading) {
    return React.createElement('div', { className: 'weather-widget loading' },
      React.createElement('div', { className: 'weather-spinner' }, '🌤️'),
      React.createElement('p', null, '날씨 정보를 불러오는 중...')
    );
  }

  if (error) {
    return React.createElement('div', { className: 'weather-widget error' },
      React.createElement('p', null, error),
      React.createElement('button', { 
        onClick: fetchWeatherData,
        className: 'retry-btn'
      }, '다시 시도')
    );
  }

  if (!weatherData) return null;

  const campingIndex = getCampingIndex(
    weatherData.current.temp,
    weatherData.current.humidity,
    weatherData.current.wind_speed,
    weatherData.current.weather_code
  );

  return React.createElement('div', { className: 'weather-widget' },
    React.createElement('div', { className: 'weather-header' },
      React.createElement('h3', null, '🌤️ 실시간 날씨'),
      React.createElement('div', { className: 'location-selector' },
        React.createElement('select', {
          value: location,
          onChange: (e) => {
            console.log('도시 선택됨:', e.target.value);
            setLocation(e.target.value);
          },
          className: 'location-select'
        },
          cities.map(city =>
            React.createElement('option', { key: city.name, value: city.name }, city.fullName)
          )
        )
      )
    ),
    React.createElement('div', { className: 'weather-current' },
      React.createElement('div', { className: 'weather-main' },
        React.createElement('div', { className: 'weather-icon' }, 
          getWeatherIcon(weatherData.current.weather_code)
        ),
        React.createElement('div', { className: 'weather-info' },
          React.createElement('div', { className: 'temperature' }, 
            `${weatherData.current.temp}°C`
          ),
          React.createElement('div', { className: 'feels-like' }, 
            `체감 ${weatherData.current.feels_like}°C`
          ),
          React.createElement('div', { className: 'description' }, 
            weatherData.current.description
          )
        )
      ),
      React.createElement('div', { className: 'weather-details' },
        React.createElement('div', { className: 'detail-item' },
          React.createElement('span', { className: 'label' }, '습도'),
          React.createElement('span', { className: 'value' }, `${weatherData.current.humidity}%`)
        ),
        React.createElement('div', { className: 'detail-item' },
          React.createElement('span', { className: 'label' }, '바람'),
          React.createElement('span', { className: 'value' }, `${weatherData.current.wind_speed}m/s`)
        )
      )
    ),
    React.createElement('div', { className: 'camping-index' },
      React.createElement('div', { className: 'index-label' }, '캠핑 지수'),
      React.createElement('div', { className: 'index-stars' }, 
        '⭐'.repeat(campingIndex)
      ),
      React.createElement('div', { className: 'index-text' }, 
        campingIndex >= 4 ? '매우 좋음' : 
        campingIndex >= 3 ? '좋음' : 
        campingIndex >= 2 ? '보통' : '나쁨'
      )
    ),
    React.createElement('div', { className: 'weather-forecast' },
      React.createElement('h4', null, '시간별 예보'),
      React.createElement('div', { className: 'forecast-items' },
        weatherData.forecast.map((item, index) =>
          React.createElement('div', { key: index, className: 'forecast-item' },
            React.createElement('span', { className: 'forecast-time' }, item.time),
            React.createElement('span', { className: 'forecast-icon' }, 
              getWeatherIcon(item.weather_code)
            ),
            React.createElement('span', { className: 'forecast-temp' }, `${item.temp}°C`)
          )
        )
      )
    )
  );
};

// CampingIndexCalculator 컴포넌트 정의
const CampingIndexCalculator = () => {
  const [weatherData, setWeatherData] = useState(null);
  const [location, setLocation] = useState('서울');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [campingIndex, setCampingIndex] = useState(null);
  const [detailedScores, setDetailedScores] = useState({});

  // 도시 목록
  const cities = [
    { name: '서울', fullName: '서울특별시' },
    { name: '부산', fullName: '부산광역시' },
    { name: '대구', fullName: '대구광역시' },
    { name: '인천', fullName: '인천광역시' },
    { name: '광주', fullName: '광주광역시' },
    { name: '대전', fullName: '대전광역시' },
    { name: '울산', fullName: '울산광역시' },
    { name: '세종', fullName: '세종특별자치시' },
    { name: '경기', fullName: '경기도' },
    { name: '강원', fullName: '강원도' },
    { name: '충북', fullName: '충청북도' },
    { name: '충남', fullName: '충청남도' },
    { name: '전북', fullName: '전라북도' },
    { name: '전남', fullName: '전라남도' },
    { name: '경북', fullName: '경상북도' },
    { name: '경남', fullName: '경상남도' },
    { name: '제주', fullName: '제주특별자치도' }
  ];

  // 날씨 아이콘 매핑
  const getWeatherIcon = (weatherCode) => {
    const icons = {
      '01': '☀️', // 맑음
      '02': '🌤️', // 구름조금
      '03': '⛅', // 구름많음
      '04': '☁️', // 흐림
      '09': '🌧️', // 소나기
      '10': '🌦️', // 비
      '11': '⛈️', // 번개
      '13': '🌨️', // 눈
      '50': '🌫️'  // 안개
    };
    return icons[weatherCode] || '🌤️';
  };

  // 상세 캠핑 지수 계산
  const calculateDetailedCampingIndex = (temp, humidity, windSpeed, weatherCode) => {
    const scores = {
      temperature: { score: 5, maxScore: 5, details: '' },
      humidity: { score: 5, maxScore: 5, details: '' },
      wind: { score: 5, maxScore: 5, details: '' },
      weather: { score: 5, maxScore: 5, details: '' }
    };

    // 온도 점수 계산 (15-25도가 최적)
    if (temp >= 15 && temp <= 25) {
      scores.temperature.score = 5;
      scores.temperature.details = '캠핑하기 최적의 온도입니다!';
    } else if (temp >= 10 && temp < 15 || temp > 25 && temp <= 30) {
      scores.temperature.score = 3;
      scores.temperature.details = '캠핑 가능하지만 적절한 준비가 필요합니다.';
    } else if (temp >= 5 && temp < 10 || temp > 30 && temp <= 35) {
      scores.temperature.score = 2;
      scores.temperature.details = '캠핑하기 다소 어려운 온도입니다.';
    } else {
      scores.temperature.score = 1;
      scores.temperature.details = '캠핑하기 매우 어려운 온도입니다.';
    }

    // 습도 점수 계산 (40-70%가 최적)
    if (humidity >= 40 && humidity <= 70) {
      scores.humidity.score = 5;
      scores.humidity.details = '캠핑하기 적절한 습도입니다.';
    } else if (humidity >= 30 && humidity < 40 || humidity > 70 && humidity <= 80) {
      scores.humidity.score = 3;
      scores.humidity.details = '습도가 다소 높거나 낮습니다.';
    } else {
      scores.humidity.score = 2;
      scores.humidity.details = '습도가 너무 높거나 낮아 캠핑하기 어렵습니다.';
    }

    // 바람 점수 계산 (5m/s 이하가 최적)
    if (windSpeed <= 5) {
      scores.wind.score = 5;
      scores.wind.details = '바람이 적당하여 캠핑하기 좋습니다.';
    } else if (windSpeed > 5 && windSpeed <= 8) {
      scores.wind.score = 3;
      scores.wind.details = '바람이 다소 강합니다. 텐트 설치에 주의하세요.';
    } else if (windSpeed > 8 && windSpeed <= 12) {
      scores.wind.score = 2;
      scores.wind.details = '바람이 강합니다. 캠핑을 재고해보세요.';
    } else {
      scores.wind.score = 1;
      scores.wind.details = '바람이 매우 강합니다. 캠핑을 피하는 것이 좋습니다.';
    }

    // 날씨 점수 계산
    if (weatherCode.startsWith('01')) {
      scores.weather.score = 5;
      scores.weather.details = '맑은 날씨로 캠핑하기 최적입니다!';
    } else if (weatherCode.startsWith('02') || weatherCode.startsWith('03')) {
      scores.weather.score = 4;
      scores.weather.details = '구름이 있지만 캠핑하기 좋은 날씨입니다.';
    } else if (weatherCode.startsWith('04')) {
      scores.weather.score = 3;
      scores.weather.details = '흐린 날씨입니다. 날씨 변화를 주의하세요.';
    } else if (weatherCode.startsWith('10')) {
      scores.weather.score = 2;
      scores.weather.details = '비가 올 예정입니다. 캠핑을 재고해보세요.';
    } else if (weatherCode.startsWith('09') || weatherCode.startsWith('11') || weatherCode.startsWith('13')) {
      scores.weather.score = 1;
      scores.weather.details = '악천후 예상으로 캠핑을 피하는 것이 좋습니다.';
    }

    return scores;
  };

  // 전체 캠핑 지수 계산
  const calculateOverallIndex = (detailedScores) => {
    const totalScore = Object.values(detailedScores).reduce((sum, score) => sum + score.score, 0);
    const maxTotalScore = Object.values(detailedScores).reduce((sum, score) => sum + score.maxScore, 0);
    return Math.round((totalScore / maxTotalScore) * 5);
  };

  // 캠핑 지수 등급 반환
  const getCampingGrade = (index) => {
    if (index >= 4) return { grade: '매우 좋음', color: '#4CAF50', emoji: '⭐' };
    if (index >= 3) return { grade: '좋음', color: '#8BC34A', emoji: '⭐' };
    if (index >= 2) return { grade: '보통', color: '#FFC107', emoji: '⭐' };
    return { grade: '나쁨', color: '#F44336', emoji: '⭐' };
  };

  // 날씨 데이터 가져오기
  const fetchWeatherData = async () => {
    try {
      console.log('캠핑 지수 계산을 위한 날씨 데이터 요청 - 위치:', location);
      setIsLoading(true);
      setError(null);
      
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 15000);
      
      const response = await fetch(`/api/weather/current?location=${encodeURIComponent(location)}`, {
        signal: controller.signal
      });
      
      clearTimeout(timeoutId);
      
      if (!response.ok) {
        throw new Error(`날씨 API 호출에 실패했습니다. (${response.status})`);
      }
      
      const weatherData = await response.json();
      
      if (weatherData.error) {
        throw new Error(weatherData.error);
      }
      
      setWeatherData(weatherData);
      
      // 상세 캠핑 지수 계산
      const detailedScores = calculateDetailedCampingIndex(
        weatherData.current.temp,
        weatherData.current.humidity,
        weatherData.current.wind_speed,
        weatherData.current.weather_code
      );
      
      setDetailedScores(detailedScores);
      
      // 전체 캠핑 지수 계산
      const overallIndex = calculateOverallIndex(detailedScores);
      setCampingIndex(overallIndex);
      
      console.log('캠핑 지수 계산 완료:', { overallIndex, detailedScores });
    } catch (err) {
      console.error('캠핑 지수 계산 오류:', err);
      if (err.name === 'AbortError') {
        setError('날씨 정보 로딩 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.');
      } else {
        setError(`캠핑 지수 계산에 실패했습니다: ${err.message}`);
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchWeatherData();
    
    // 5분마다 업데이트
    const interval = setInterval(fetchWeatherData, 300000);
    
    return () => clearInterval(interval);
  }, [location]);

  if (isLoading) {
    return React.createElement('div', { className: 'camping-index-calculator loading' },
      React.createElement('div', { className: 'loading-spinner' }, '🏕️'),
      React.createElement('p', null, '캠핑 지수를 계산하는 중...')
    );
  }

  if (error) {
    return React.createElement('div', { className: 'camping-index-calculator error' },
      React.createElement('p', null, error),
      React.createElement('button', { 
        onClick: fetchWeatherData,
        className: 'retry-btn'
      }, '다시 시도')
    );
  }

  if (!weatherData || !campingIndex) return null;

  const grade = getCampingGrade(campingIndex);

  return React.createElement('div', { className: 'camping-index-calculator' },
    React.createElement('div', { className: 'calculator-header' },
      React.createElement('h3', null, '🏕️ 실시간 캠핑 지수'),
      React.createElement('div', { className: 'location-selector' },
        React.createElement('select', {
          value: location,
          onChange: (e) => setLocation(e.target.value),
          className: 'location-select'
        },
          cities.map(city =>
            React.createElement('option', { key: city.name, value: city.name }, city.fullName)
          )
        )
      )
    ),
    React.createElement('div', { className: 'current-weather' },
      React.createElement('div', { className: 'weather-main' },
        React.createElement('div', { className: 'weather-icon' }, 
          getWeatherIcon(weatherData.current.weather_code)
        ),
        React.createElement('div', { className: 'weather-info' },
          React.createElement('div', { className: 'temperature' }, 
            `${weatherData.current.temp}°C`
          ),
          React.createElement('div', { className: 'description' }, 
            weatherData.current.description
          )
        )
      )
    ),
    React.createElement('div', { className: 'camping-index-display' },
      React.createElement('div', { className: 'index-header' },
        React.createElement('h4', null, '캠핑 지수'),
        React.createElement('div', { className: 'index-score', style: { color: grade.color } }, 
          grade.emoji.repeat(campingIndex)
        ),
        React.createElement('div', { className: 'index-grade', style: { color: grade.color } }, 
          grade.grade
        )
      ),
      React.createElement('div', { className: 'detailed-scores' },
        React.createElement('h5', null, '상세 분석'),
        React.createElement('div', { className: 'score-items' },
          React.createElement('div', { className: 'score-item' },
            React.createElement('div', { className: 'score-label' }, '온도'),
            React.createElement('div', { className: 'score-bar' },
              React.createElement('div', { 
                className: 'score-fill', 
                style: { 
                  width: `${(detailedScores.temperature.score / detailedScores.temperature.maxScore) * 100}%`,
                  backgroundColor: detailedScores.temperature.score >= 4 ? '#4CAF50' : 
                                 detailedScores.temperature.score >= 2 ? '#FFC107' : '#F44336'
                }
              })
            ),
            React.createElement('div', { className: 'score-details' },
              detailedScores.temperature.details
            )
          ),
          React.createElement('div', { className: 'score-item' },
            React.createElement('div', { className: 'score-label' }, '습도'),
            React.createElement('div', { className: 'score-bar' },
              React.createElement('div', { 
                className: 'score-fill', 
                style: { 
                  width: `${(detailedScores.humidity.score / detailedScores.humidity.maxScore) * 100}%`,
                  backgroundColor: detailedScores.humidity.score >= 4 ? '#4CAF50' : 
                                 detailedScores.humidity.score >= 2 ? '#FFC107' : '#F44336'
                }
              })
            ),
            React.createElement('div', { className: 'score-details' },
              detailedScores.humidity.details
            )
          ),
          React.createElement('div', { className: 'score-item' },
            React.createElement('div', { className: 'score-label' }, '바람'),
            React.createElement('div', { className: 'score-bar' },
              React.createElement('div', { 
                className: 'score-fill', 
                style: { 
                  width: `${(detailedScores.wind.score / detailedScores.wind.maxScore) * 100}%`,
                  backgroundColor: detailedScores.wind.score >= 4 ? '#4CAF50' : 
                                 detailedScores.wind.score >= 2 ? '#FFC107' : '#F44336'
                }
              })
            ),
            React.createElement('div', { className: 'score-details' },
              detailedScores.wind.details
            )
          ),
          React.createElement('div', { className: 'score-item' },
            React.createElement('div', { className: 'score-label' }, '날씨'),
            React.createElement('div', { className: 'score-bar' },
              React.createElement('div', { 
                className: 'score-fill', 
                style: { 
                  width: `${(detailedScores.weather.score / detailedScores.weather.maxScore) * 100}%`,
                  backgroundColor: detailedScores.weather.score >= 4 ? '#4CAF50' : 
                                 detailedScores.weather.score >= 2 ? '#FFC107' : '#F44336'
                }
              })
            ),
            React.createElement('div', { className: 'score-details' },
              detailedScores.weather.details
            )
          )
        )
      ),
      React.createElement('div', { className: 'camping-tips' },
        React.createElement('h5', null, '캠핑 팁'),
        React.createElement('div', { className: 'tips-content' },
          campingIndex >= 4 ? 
            React.createElement('p', null, '🎉 현재 날씨는 캠핑하기 최적입니다! 즐거운 캠핑 되세요!') :
          campingIndex >= 3 ? 
            React.createElement('p', null, '👍 캠핑 가능하지만 적절한 준비물을 챙기세요.') :
          campingIndex >= 2 ? 
            React.createElement('p', null, '⚠️ 캠핑하기 다소 어려운 날씨입니다. 날씨 변화를 주의하세요.') :
            React.createElement('p', null, '❌ 현재 날씨는 캠핑하기 어렵습니다. 다른 날을 기다리는 것을 추천합니다.')
        )
      )
    )
  );
};



// 날씨 위젯을 렌더링할 컨테이너 찾기
const weatherContainer = document.getElementById('weather-widget-container');

if (weatherContainer) {
  const weatherRoot = ReactDOM.createRoot(weatherContainer);
  weatherRoot.render(React.createElement(WeatherWidget));
  
  // 날씨 위젯 로드 완료 후 로딩 화면 숨기기
  setTimeout(() => {
    clearInterval(progressInterval);
    hideLoadingScreen();
  }, 2000); // 2초 후 로딩 화면 숨김
}

// 로딩 화면 관리 함수
const showLoadingScreen = () => {
  const overlay = document.getElementById('loading-overlay');
  const progressFill = document.getElementById('progress-fill');
  const progressText = document.getElementById('progress-text');
  
  // 진행률 애니메이션
  let progress = 0;
  const progressInterval = setInterval(() => {
    progress += Math.random() * 15;
    if (progress > 90) progress = 90;
    
    if (progressFill) progressFill.style.width = `${progress}%`;
    if (progressText) progressText.textContent = `${Math.round(progress)}%`;
  }, 200);
  
  return { overlay, progressInterval };
};

const hideLoadingScreen = () => {
  const overlay = document.getElementById('loading-overlay');
  const mainContainer = document.querySelector('.main-container');
  
  if (overlay) {
    overlay.style.opacity = '0';
    setTimeout(() => {
      overlay.style.display = 'none';
    }, 500);
  }
  
  if (mainContainer) {
    mainContainer.style.opacity = '1';
  }
};

// 로딩 화면 진행률 시작
const { overlay, progressInterval } = showLoadingScreen();

// 캠핑 지수 계산기를 렌더링할 컨테이너 찾기
const campingIndexContainer = document.getElementById('camping-index-calculator-container');

if (campingIndexContainer) {
  const campingIndexRoot = ReactDOM.createRoot(campingIndexContainer);
  campingIndexRoot.render(React.createElement(CampingIndexCalculator));
}
