// camp-map.js
window.addEventListener('DOMContentLoaded', function () {
  console.log('camp-map.js 로드됨');

  var mapContainer = document.getElementById('map-placeholder');
  if (!mapContainer) {
    console.error('map-placeholder 요소를 찾을 수 없습니다.');
    return;
  }

  if (!window.kakao) {
    console.error('카카오맵 API가 로드되지 않았습니다.');
    mapContainer.innerHTML = '카카오맵 API를 불러올 수 없습니다.';
    return;
  }

  // ====== 날씨 API 관련 변수들 ======
  var weatherOverlay = null;
  var weatherMarkers = [];
  var currentWeatherData = null;
  var weatherLayerVisible = false;
  var nationalWeatherMarkers = []; // 전국 날씨 마커들
  var nationalWeatherData = {}; // 전국 날씨 데이터 캐시
  var weatherAnimationInterval = null; // 날씨 애니메이션 인터벌

  // ====== 카테고리 검색 관련 변수들 ======
  var placeOverlay = new kakao.maps.CustomOverlay({zIndex:1}),
      contentNode = document.createElement('div'), // 커스텀 오버레이의 컨텐츠 엘리먼트
      categoryMarkers = [], // 카테고리 검색 마커를 담을 배열
      currCategory = '', // 현재 선택된 카테고리
      ps = null; // 장소 검색 객체

  // ====== 커스텀 오버레이용 CSS 동적 삽입 ======
  (function injectCustomOverlayCSS() {
    var style = document.createElement('style');
    style.innerHTML = `
      .overlay_info {border-radius: 6px; margin-bottom: 12px; float:left;position: relative; border: 1px solid #ccc; border-bottom: 2px solid #ddd;background-color:#fff; z-index: 10;}
      .overlay_info:nth-of-type(n) {border:0; box-shadow: 0px 1px 2px #888;}
      .overlay_info a {display: block; background: #d95050 url(https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/arrow_white.png) no-repeat right 14px center; text-decoration: none; color: #fff; padding:12px 36px 12px 14px; font-size: 14px; border-radius: 6px 6px 0 0}
      .overlay_info a strong {background:url(https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/place_icon.png) no-repeat; padding-left: 27px;}
      .overlay_info .desc {padding:14px;position: relative; min-width: 190px; height: 56px}
      .overlay_info img {vertical-align: top; width:60px; height:40px; object-fit:cover; border-radius:4px;}
      .overlay_info .address {font-size: 12px; color: #333; position: absolute; left: 80px; right: 14px; top: 24px; white-space: normal}
      .overlay_info:after {content:'';position: absolute; margin-left: -11px; left: 50%; bottom: -12px; width: 22px; height: 12px; background:url(https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/vertex_white.png) no-repeat 0 bottom;}
      .overlay_info .actions { padding: 8px 14px 0 0; text-align: right; }
            .overlay_info .directions-btn {
              display: inline-block;
              padding: 6px 12px;
              color: #fff;
              background-color: #FF5722;
              border-radius: 4px;
              text-decoration: none;
              font-size: 13px;
              font-weight: bold;
              transition: background-color 0.2s;
            }
            .overlay_info .directions-btn:hover {
              background-color: #E64A19;
            }
            .overlay_info .current-loc-btn {
                    display: inline-block;
                    padding: 6px 12px;
                    color: #fff;
                    background-color: #2196F3; /* 파란색 계열 */
                    border-radius: 4px;
                    text-decoration: none;
                    font-size: 13px;
                    font-weight: bold;
                    transition: background-color 0.2s;
                    margin-left: 5px; /* 버튼 사이 간격 */
                    border: none;
                    cursor: pointer;
                    font-family: inherit; /* 폰트 상속 */
                  }
                  .overlay_info .current-loc-btn:hover {
                    background-color: #1976D2;
                  }

      /* 카테고리 검색 관련 CSS */
      .map_wrap, .map_wrap * {margin:0; padding:0;font-family:'Malgun Gothic',dotum,'돋움',sans-serif;font-size:12px;}
      .map_wrap {position:relative;width:100%;height:350px;}
      #category {position:absolute;top:10px;left:10px;border-radius: 5px; border:1px solid #909090;box-shadow: 0 1px 1px rgba(0, 0, 0, 0.4);background: #fff;overflow: hidden;z-index: 1000;font-size:10px;}
      #category li {float:left;list-style: none;width:50px;border-right:1px solid #acacac;padding:4px 0;text-align: center; cursor: pointer;font-size:10px;}
      #category li.on {background: #eee;}
      #category li:hover {background: #ffe6e6;border-left:1px solid #acacac;margin-left: -1px;}
      #category li:last-child{margin-right:0;border-right:0;}
      #category li span {display: block;margin:0 auto 3px;width:27px;height: 28px;background-position: center center;}
      #category li .category_bg {background:url(https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/places_category.png) no-repeat;}
      #category li .bank {background-position: -10px 0;}
      #category li .mart {background-position: -10px -36px;}
      #category li .pharmacy {background-position: -10px -72px;}
      #category li .oil {background-position: -10px -108px;}
      #category li .cafe {background-position: -10px -144px;}
      #category li .store {background-position: -10px -180px;}
      #category li.on .category_bg {background-position-x:-46px;}
      .placeinfo_wrap {position:absolute;bottom:28px;left:-150px;width:300px;}
      .placeinfo {position:relative;width:100%;border-radius:6px;border: 1px solid #ccc;border-bottom:2px solid #ddd;padding-bottom: 10px;background: #fff;}
      .placeinfo:nth-of-type(n) {border:0; box-shadow:0px 1px 2px #888;}
      .placeinfo_wrap .after {content:'';position:relative;margin-left:-12px;left:50%;width:22px;height:12px;background:url('https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/vertex_white.png')}
      .placeinfo a, .placeinfo a:hover, .placeinfo a:active{color:#fff;text-decoration: none;}
      .placeinfo a, .placeinfo span {display: block;text-overflow: ellipsis;overflow: hidden;white-space: nowrap;}
      .placeinfo span {margin:5px 5px 0 5px;cursor: default;font-size:13px;}
      .placeinfo .title {font-weight: bold; font-size:14px;border-radius: 6px 6px 0 0;margin: -1px -1px 0 -1px;padding:10px; color: #fff;background: #d95050;background: #d95050 url(https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/arrow_white.png) no-repeat right 14px center;}
      .placeinfo .tel {color:#0f7833;}
      .placeinfo .jibun {color:#999;font-size:11px;margin-top:0;}

      /* 날씨 관련 CSS */
      .weather-controls {position:absolute;top:10px;right:10px;z-index:1000;display:flex;gap:8px;}
      .weather-btn {background:#4CAF50;color:#fff;border:none;border-radius:6px;padding:8px 16px;font-weight:bold;cursor:pointer;font-size:12px;}
      .weather-btn:hover {background:#45a049;}
      .weather-btn.active {background:#2196F3;}
      .weather-overlay {background:rgba(255,255,255,0.95);border-radius:8px;padding:15px;box-shadow:0 4px 12px rgba(0,0,0,0.15);max-width:280px;font-family:'Malgun Gothic',sans-serif;}
      .weather-header {display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;}
      .weather-location {font-weight:bold;font-size:14px;color:#333;}
      .weather-close {background:none;border:none;font-size:18px;cursor:pointer;color:#666;}
      .weather-main {display:flex;align-items:center;gap:12px;margin-bottom:12px;}
      .weather-icon {font-size:2.5rem;}
      .weather-temp {font-size:2rem;font-weight:bold;color:#333;}
      .weather-desc {font-size:13px;color:#666;}
      .weather-details {display:grid;grid-template-columns:1fr 1fr;gap:8px;font-size:12px;}
      .weather-detail {display:flex;justify-content:space-between;}
      .weather-label {color:#666;}
      .weather-value {font-weight:bold;color:#333;}
      .camping-index {background:linear-gradient(135deg,#667eea,#764ba2);color:#fff;border-radius:6px;padding:10px;margin-top:10px;text-align:center;}
      .camping-stars {font-size:1.2rem;margin:5px 0;}

      /* 전국 날씨 마커 스타일 */
      .national-weather-marker {background:rgba(255,255,255,0.9);border-radius:50%;width:60px;height:60px;display:flex;flex-direction:column;align-items:center;justify-content:center;box-shadow:0 2px 8px rgba(0,0,0,0.2);border:2px solid #fff;cursor:pointer;transition:all 0.3s ease;animation:pulse 2s infinite;}
      .national-weather-marker:hover {transform:scale(1.1);box-shadow:0 4px 16px rgba(0,0,0,0.3);}
      .national-weather-marker .weather-icon {font-size:1.5rem;margin-bottom:2px;}
      .national-weather-marker .temp {font-size:0.8rem;font-weight:bold;color:#333;}
      .national-weather-marker .city {font-size:0.6rem;color:#666;margin-top:1px;}

      @keyframes pulse {
        0% { transform: scale(1); }
        50% { transform: scale(1.05); }
        100% { transform: scale(1); }
      }

      /* 날씨 애니메이션 효과 */
      .weather-fade-in {animation: fadeIn 0.5s ease-in;}
      @keyframes fadeIn {
        from { opacity: 0; transform: translateY(-10px); }
        to { opacity: 1; transform: translateY(0); }
      }

      .weather-slide-in {animation: slideIn 0.6s ease-out;}
      @keyframes slideIn {
        from { transform: translateX(-100%); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
      }

      /* 전국 날씨 패널 */
      .national-weather-panel {position:absolute;top:60px;right:10px;background:rgba(255,255,255,0.95);border-radius:8px;padding:15px;box-shadow:0 4px 12px rgba(0,0,0,0.15);max-width:300px;font-family:'Malgun Gothic',sans-serif;z-index:1000;display:none;}
      .national-weather-panel.show {display:block;animation:slideIn 0.3s ease-out;}
      .national-weather-panel h3 {margin:0 0 10px 0;color:#333;font-size:14px;text-align:center;}
      .national-weather-grid {display:grid;grid-template-columns:repeat(3,1fr);gap:8px;}
      .national-weather-item {text-align:center;padding:8px;border-radius:6px;background:rgba(0,0,0,0.05);cursor:pointer;transition:all 0.2s;}
      .national-weather-item:hover {background:rgba(0,0,0,0.1);}
      .national-weather-item .icon {font-size:1.2rem;margin-bottom:2px;}
      .national-weather-item .temp {font-size:0.8rem;font-weight:bold;color:#333;}
      .national-weather-item .city {font-size:0.7rem;color:#666;}
    `;
    document.head.appendChild(style);
  })();

  // ====== 날씨 API 관련 함수들 ======

  // 날씨 데이터 가져오기
  async function fetchWeatherData(lat, lng) {
    try {
      // 위도/경도를 기반으로 가장 가까운 도시 찾기
      const nearestCity = findNearestCity(lat, lng);
      console.log('가장 가까운 도시:', nearestCity);

      const response = await fetch(`/api/weather/current?location=${encodeURIComponent(nearestCity)}`);
      if (!response.ok) {
        throw new Error('날씨 데이터를 가져올 수 없습니다.');
      }

      const weatherData = await response.json();
      if (weatherData.error) {
        throw new Error(weatherData.error);
      }

      return weatherData;
    } catch (error) {
      console.error('날씨 데이터 가져오기 실패:', error);
      return null;
    }
  }

  // 위도/경도로 가장 가까운 도시 찾기
  function findNearestCity(lat, lng) {
    const cities = {
      '서울': { lat: 37.5665, lng: 126.9780 },
      '부산': { lat: 35.1796, lng: 129.0756 },
      '대구': { lat: 35.8714, lng: 128.6014 },
      '인천': { lat: 37.4563, lng: 126.7052 },
      '광주': { lat: 35.1595, lng: 126.8526 },
      '대전': { lat: 36.3504, lng: 127.3845 },
      '울산': { lat: 35.5384, lng: 129.3114 },
      '세종': { lat: 36.4800, lng: 127.2890 },
      '경기': { lat: 37.4138, lng: 127.5183 },
      '강원': { lat: 37.8228, lng: 128.1555 },
      '충북': { lat: 36.8000, lng: 127.7000 },
      '충남': { lat: 36.5184, lng: 126.8000 },
      '전북': { lat: 35.7175, lng: 127.1530 },
      '전남': { lat: 34.8679, lng: 126.9910 },
      '경북': { lat: 36.4919, lng: 128.8889 },
      '경남': { lat: 35.4606, lng: 128.2132 },
      '제주': { lat: 33.4996, lng: 126.5312 }
    };

    let nearestCity = '서울';
    let minDistance = Infinity;

    for (const [city, coords] of Object.entries(cities)) {
      const distance = calculateDistance(lat, lng, coords.lat, coords.lng);
      if (distance < minDistance) {
        minDistance = distance;
        nearestCity = city;
      }
    }

    return nearestCity;
  }

  // 두 지점 간의 거리 계산 (Haversine formula)
  function calculateDistance(lat1, lng1, lat2, lng2) {
    const R = 6371; // 지구의 반지름 (km)
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLng = (lng2 - lng1) * Math.PI / 180;
    const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
              Math.sin(dLng/2) * Math.sin(dLng/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
  }

  // 날씨 아이콘 가져오기
  function getWeatherIcon(weatherCode) {
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
  }

  // 캠핑 지수 계산
  function calculateCampingIndex(temp, humidity, windSpeed, weatherCode) {
    let score = 5; // 기본 점수

    // 온도 점수 (15-25도가 최적)
    if (temp >= 15 && temp <= 25) score += 2;
    else if (temp >= 10 && temp <= 30) score += 1;
    else if (temp < 0 || temp > 35) score -= 2;

    // 습도 점수 (40-70%가 최적)
    if (humidity >= 40 && humidity <= 70) score += 1;
    else if (humidity > 80) score -= 1;

    // 바람 점수 (5m/s 이하가 좋음)
    if (windSpeed <= 5) score += 1;
    else if (windSpeed > 10) score -= 1;

    // 날씨 점수
    if (weatherCode === '01' || weatherCode === '02') score += 2; // 맑음, 구름조금
    else if (weatherCode === '03' || weatherCode === '04') score += 1; // 구름많음, 흐림
    else if (weatherCode === '09' || weatherCode === '10' || weatherCode === '11') score -= 2; // 비, 번개
    else if (weatherCode === '13') score -= 1; // 눈

    return Math.max(1, Math.min(5, score));
  }

  // 날씨 오버레이 생성
  function createWeatherOverlay(weatherData, position) {
    const campingIndex = calculateCampingIndex(
      weatherData.current.temp,
      weatherData.current.humidity,
      weatherData.current.wind_speed,
      weatherData.current.weather_code
    );

    const content = `
      <div class="weather-overlay">
        <div class="weather-header">
          <span class="weather-location">${weatherData.location}</span>
          <button class="weather-close" onclick="closeWeatherOverlay()">&times;</button>
        </div>
        <div class="weather-main">
          <div class="weather-icon">${getWeatherIcon(weatherData.current.weather_code)}</div>
          <div>
            <div class="weather-temp">${weatherData.current.temp}°C</div>
            <div class="weather-desc">${weatherData.current.description}</div>
          </div>
        </div>
        <div class="weather-details">
          <div class="weather-detail">
            <span class="weather-label">체감온도</span>
            <span class="weather-value">${weatherData.current.feels_like}°C</span>
          </div>
          <div class="weather-detail">
            <span class="weather-label">습도</span>
            <span class="weather-value">${weatherData.current.humidity}%</span>
          </div>
          <div class="weather-detail">
            <span class="weather-label">바람</span>
            <span class="weather-value">${weatherData.current.wind_speed}m/s</span>
          </div>
          <div class="weather-detail">
            <span class="weather-label">강수확률</span>
            <span class="weather-value">${weatherData.current.pop || 0}%</span>
          </div>
        </div>
        <div class="camping-index">
          <div>캠핑 지수</div>
          <div class="camping-stars">${'⭐'.repeat(campingIndex)}</div>
          <div>${campingIndex >= 4 ? '매우 좋음' : campingIndex >= 3 ? '좋음' : campingIndex >= 2 ? '보통' : '나쁨'}</div>
        </div>
      </div>
    `;

    return new kakao.maps.CustomOverlay({
      position: position,
      content: content,
      xAnchor: 0.5,
      yAnchor: 1.1
    });
  }

  // 날씨 오버레이 닫기
  window.closeWeatherOverlay = function() {
    if (weatherOverlay) {
      weatherOverlay.setMap(null);
      weatherOverlay = null;
    }
  };

  // 날씨 레이어 토글
  function toggleWeatherLayer() {
    const weatherBtn = document.querySelector('.weather-btn');
    if (weatherLayerVisible) {
      // 날씨 레이어 숨기기
      weatherLayerVisible = false;
      weatherBtn.classList.remove('active');
      weatherBtn.textContent = '날씨 표시';

      // 날씨 마커들 제거
      weatherMarkers.forEach(marker => marker.setMap(null));
      weatherMarkers = [];

      if (weatherOverlay) {
        weatherOverlay.setMap(null);
        weatherOverlay = null;
      }
    } else {
      // 날씨 레이어 표시
      weatherLayerVisible = true;
      weatherBtn.classList.add('active');
      weatherBtn.textContent = '날씨 숨기기';

      // 현재 지도 중심의 날씨 정보 표시
      const center = window.currentMap.getCenter();
      showWeatherAtLocation(center);
    }
  }

  // 특정 위치의 날씨 표시
  async function showWeatherAtLocation(position) {
    const weatherData = await fetchWeatherData(position.getLat(), position.getLng());
    if (weatherData) {
      currentWeatherData = weatherData;

      // 기존 날씨 오버레이 제거
      if (weatherOverlay) {
        weatherOverlay.setMap(null);
      }

      // 새로운 날씨 오버레이 생성
      weatherOverlay = createWeatherOverlay(weatherData, position);
      weatherOverlay.setMap(window.currentMap);
    }
  }

  // 전국 주요 도시 목록
  const NATIONAL_CITIES = [
    { name: '서울', lat: 37.5665, lng: 126.9780 },
    { name: '부산', lat: 35.1796, lng: 129.0756 },
    { name: '대구', lat: 35.8714, lng: 128.6014 },
    { name: '인천', lat: 37.4563, lng: 126.7052 },
    { name: '광주', lat: 35.1595, lng: 126.8526 },
    { name: '대전', lat: 36.3504, lng: 127.3845 },
    { name: '울산', lat: 35.5384, lng: 129.3114 },
    { name: '세종', lat: 36.4800, lng: 127.2890 },
    { name: '경기', lat: 37.4138, lng: 127.5183 },
    { name: '강원', lat: 37.8228, lng: 128.1555 },
    { name: '충북', lat: 36.8000, lng: 127.7000 },
    { name: '충남', lat: 36.5184, lng: 126.8000 },
    { name: '전북', lat: 35.7175, lng: 127.1530 },
    { name: '전남', lat: 34.8679, lng: 126.9910 },
    { name: '경북', lat: 36.4919, lng: 128.8889 },
    { name: '경남', lat: 35.4606, lng: 128.2132 },
    { name: '제주', lat: 33.4996, lng: 126.5312 }
  ];

  // 전국 날씨 데이터 가져오기
  async function fetchNationalWeatherData() {
    const weatherPromises = NATIONAL_CITIES.map(async (city) => {
      try {
        const response = await fetch(`/api/weather/current?location=${encodeURIComponent(city.name)}`);
        if (response.ok) {
          const data = await response.json();
          if (!data.error) {
            return { city: city.name, data: data };
          }
        }
      } catch (error) {
        console.error(`${city.name} 날씨 데이터 가져오기 실패:`, error);
      }
      return null;
    });

    const results = await Promise.all(weatherPromises);
    const validResults = results.filter(result => result !== null);

    validResults.forEach(result => {
      nationalWeatherData[result.city] = result.data;
    });

    return validResults;
  }

  // 전국 날씨 마커 생성
  function createNationalWeatherMarker(city, weatherData, position) {
    const markerContent = document.createElement('div');
    markerContent.className = 'national-weather-marker weather-fade-in';
    markerContent.innerHTML = `
      <div class="weather-icon">${getWeatherIcon(weatherData.current.weather_code)}</div>
      <div class="temp">${weatherData.current.temp}°</div>
      <div class="city">${city}</div>
    `;

    const marker = new kakao.maps.CustomOverlay({
      position: position,
      content: markerContent,
      xAnchor: 0.5,
      yAnchor: 0.5
    });

    // 마커 클릭 이벤트
    markerContent.addEventListener('click', () => {
      showNationalWeatherDetail(city, weatherData, position);
    });

    return marker;
  }

  // 전국 날씨 상세 정보 표시
  function showNationalWeatherDetail(city, weatherData, position) {
    // 기존 오버레이 제거
    if (weatherOverlay) {
      weatherOverlay.setMap(null);
    }

    const campingIndex = calculateCampingIndex(
      weatherData.current.temp,
      weatherData.current.humidity,
      weatherData.current.wind_speed,
      weatherData.current.weather_code
    );

    const content = `
      <div class="weather-overlay weather-slide-in">
        <div class="weather-header">
          <span class="weather-location">${city} 날씨</span>
          <button class="weather-close" onclick="closeWeatherOverlay()">&times;</button>
        </div>
        <div class="weather-main">
          <div class="weather-icon">${getWeatherIcon(weatherData.current.weather_code)}</div>
          <div>
            <div class="weather-temp">${weatherData.current.temp}°C</div>
            <div class="weather-desc">${weatherData.current.description}</div>
          </div>
        </div>
        <div class="weather-details">
          <div class="weather-detail">
            <span class="weather-label">체감온도</span>
            <span class="weather-value">${weatherData.current.feels_like}°C</span>
          </div>
          <div class="weather-detail">
            <span class="weather-label">습도</span>
            <span class="weather-value">${weatherData.current.humidity}%</span>
          </div>
          <div class="weather-detail">
            <span class="weather-label">바람</span>
            <span class="weather-value">${weatherData.current.wind_speed}m/s</span>
          </div>
          <div class="weather-detail">
            <span class="weather-label">강수확률</span>
            <span class="weather-value">${weatherData.current.pop || 0}%</span>
          </div>
        </div>
        <div class="camping-index">
          <div>캠핑 지수</div>
          <div class="camping-stars">${'⭐'.repeat(campingIndex)}</div>
          <div>${campingIndex >= 4 ? '매우 좋음' : campingIndex >= 3 ? '좋음' : campingIndex >= 2 ? '보통' : '나쁨'}</div>
        </div>
      </div>
    `;

    weatherOverlay = new kakao.maps.CustomOverlay({
      position: position,
      content: content,
      xAnchor: 0.5,
      yAnchor: 1.1
    });

    weatherOverlay.setMap(window.currentMap);
  }

  // 전국 날씨 패널 생성
  function createNationalWeatherPanel() {
    const panel = document.createElement('div');
    panel.className = 'national-weather-panel';
    panel.innerHTML = `
      <h3>전국 날씨 요약</h3>
      <div class="national-weather-grid" id="national-weather-grid">
        <div style="text-align:center;padding:20px;color:#666;">날씨 정보 로딩 중...</div>
      </div>
    `;

    mapContainer.appendChild(panel);
    return panel;
  }

  // 전국 날씨 패널 업데이트
  function updateNationalWeatherPanel() {
    const grid = document.getElementById('national-weather-grid');
    if (!grid) return;

    const cities = Object.keys(nationalWeatherData).slice(0, 9); // 최대 9개 도시 표시
    grid.innerHTML = '';

    cities.forEach(city => {
      const weatherData = nationalWeatherData[city];
      const item = document.createElement('div');
      item.className = 'national-weather-item';
      item.innerHTML = `
        <div class="icon">${getWeatherIcon(weatherData.current.weather_code)}</div>
        <div class="temp">${weatherData.current.temp}°</div>
        <div class="city">${city}</div>
      `;

      item.addEventListener('click', () => {
        const cityData = NATIONAL_CITIES.find(c => c.name === city);
        if (cityData) {
          const position = new kakao.maps.LatLng(cityData.lat, cityData.lng);
          showNationalWeatherDetail(city, weatherData, position);
          window.currentMap.setCenter(position);
          window.currentMap.setLevel(6);
        }
      });

      grid.appendChild(item);
    });
  }

  // 전국 날씨 애니메이션 시작
  function startNationalWeatherAnimation() {
    // 기존 마커들 제거
    nationalWeatherMarkers.forEach(marker => marker.setMap(null));
    nationalWeatherMarkers = [];

    // 전국 날씨 데이터 가져오기
    fetchNationalWeatherData().then(() => {
      // 마커들을 순차적으로 생성 (애니메이션 효과)
      NATIONAL_CITIES.forEach((city, index) => {
        const weatherData = nationalWeatherData[city.name];
        if (weatherData) {
          setTimeout(() => {
            const position = new kakao.maps.LatLng(city.lat, city.lng);
            const marker = createNationalWeatherMarker(city.name, weatherData, position);
            marker.setMap(window.currentMap);
            nationalWeatherMarkers.push(marker);
          }, index * 100); // 100ms 간격으로 마커 생성
        }
      });

      // 패널 업데이트
      updateNationalWeatherPanel();
    });
  }

  // 날씨 레이어 토글 (수정)
  function toggleWeatherLayer() {
    const weatherBtn = document.querySelector('.weather-btn');
    const nationalBtn = document.querySelector('.national-weather-btn');

    if (weatherLayerVisible) {
      // 날씨 레이어 숨기기
      weatherLayerVisible = false;
      weatherBtn.classList.remove('active');
      weatherBtn.textContent = '날씨 표시';

      // 날씨 마커들 제거
      weatherMarkers.forEach(marker => marker.setMap(null));
      weatherMarkers = [];

      // 전국 날씨 마커들 제거
      nationalWeatherMarkers.forEach(marker => marker.setMap(null));
      nationalWeatherMarkers = [];

      if (weatherOverlay) {
        weatherOverlay.setMap(null);
        weatherOverlay = null;
      }

      // 전국 날씨 패널 숨기기
      const panel = document.querySelector('.national-weather-panel');
      if (panel) {
        panel.classList.remove('show');
      }

      // 애니메이션 인터벌 정리
      if (weatherAnimationInterval) {
        clearInterval(weatherAnimationInterval);
        weatherAnimationInterval = null;
      }
    } else {
      // 날씨 레이어 표시
      weatherLayerVisible = true;
      weatherBtn.classList.add('active');
      weatherBtn.textContent = '날씨 숨기기';

      // 전국 날씨 애니메이션 시작
      startNationalWeatherAnimation();

      // 전국 날씨 패널 표시
      const panel = document.querySelector('.national-weather-panel');
      if (panel) {
        panel.classList.add('show');
      }

             // 30분마다 날씨 데이터 갱신 (백엔드 스케줄러와 동기화)
       weatherAnimationInterval = setInterval(() => {
         if (weatherLayerVisible) {
           startNationalWeatherAnimation();
         }
       }, 1800000); // 30분 = 1800초 = 1800000ms
    }
  }

  // 날씨 컨트롤 UI 생성 (수정)
  function createWeatherControls() {
    const weatherControls = document.createElement('div');
    weatherControls.className = 'weather-controls';

    const weatherBtn = document.createElement('button');
    weatherBtn.className = 'weather-btn';
    weatherBtn.textContent = '전국 날씨';
    weatherBtn.onclick = toggleWeatherLayer;

    weatherControls.appendChild(weatherBtn);
    mapContainer.appendChild(weatherControls);

    // 전국 날씨 패널 생성
    createNationalWeatherPanel();
  }

  // ====== 카테고리 UI 생성 함수 ======
  function createCategoryUI() {
    var categoryContainer = document.createElement('div');
    categoryContainer.id = 'category';
    categoryContainer.innerHTML = `
      <li id="BK9" data-order="0">
        <span class="category_bg bank"></span>
        은행
      </li>
      <li id="MT1" data-order="1">
        <span class="category_bg mart"></span>
        마트
      </li>
      <li id="PM9" data-order="2">
        <span class="category_bg pharmacy"></span>
        약국
      </li>
      <li id="OL7" data-order="3">
        <span class="category_bg oil"></span>
        주유소
      </li>
      <li id="CE7" data-order="4">
        <span class="category_bg cafe"></span>
        카페
      </li>
      <li id="CS2" data-order="5">
        <span class="category_bg store"></span>
        편의점
      </li>
    `;

    // 카테고리 컨테이너를 지도 컨테이너 안에 직접 추가
    mapContainer.appendChild(categoryContainer);

    // 카테고리 클릭 이벤트 등록
    addCategoryClickEvent();
  }

  // ====== 카테고리 클릭 이벤트 등록 ======
  function addCategoryClickEvent() {
    var category = document.getElementById('category');
    if (!category) return;

    var children = category.children;
    for (var i=0; i<children.length; i++) {
      children[i].onclick = onClickCategory;
    }
  }

  // ====== 카테고리 클릭 핸들러 ======
  function onClickCategory() {
    var id = this.id,
        className = this.className;

    placeOverlay.setMap(null);

    if (className === 'on') {
      currCategory = '';
      changeCategoryClass();
      removeCategoryMarkers();
    } else {
      currCategory = id;
      changeCategoryClass(this);
      searchPlaces();
    }
  }

  // ====== 카테고리 클래스 변경 ======
  function changeCategoryClass(el) {
    var category = document.getElementById('category');
    if (!category) return;

    var children = category.children;
    for (var i=0; i<children.length; i++) {
      children[i].className = '';
    }

    if (el) {
      el.className = 'on';
    }
  }

  // ====== 카테고리 검색 실행 ======
  function searchPlaces() {
    if (!currCategory || !ps) {
      return;
    }

    // 커스텀 오버레이를 숨깁니다
    placeOverlay.setMap(null);

    // 지도에 표시되고 있는 카테고리 마커를 제거합니다
    removeCategoryMarkers();

    ps.categorySearch(currCategory, placesSearchCB, {useMapBounds:true});
  }

  // ====== 장소검색 완료 콜백 ======
  function placesSearchCB(data, status, pagination) {
    if (status === kakao.maps.services.Status.OK) {
      // 정상적으로 검색이 완료됐으면 지도에 마커를 표출합니다
      displayPlaces(data);
    } else if (status === kakao.maps.services.Status.ZERO_RESULT) {
      console.log('검색 결과가 없습니다.');
    } else if (status === kakao.maps.services.Status.ERROR) {
      console.error('검색 중 오류가 발생했습니다.');
    }
  }

  // ====== 장소 마커 표시 ======
  function displayPlaces(places) {
    // 몇번째 카테고리가 선택되어 있는지 얻어옵니다
    var order = document.getElementById(currCategory).getAttribute('data-order');

    for (var i=0; i<places.length; i++) {
      // 마커를 생성하고 지도에 표시합니다
      var marker = addCategoryMarker(new kakao.maps.LatLng(places[i].y, places[i].x), order);

      // 마커와 검색결과 항목을 클릭 했을 때
      // 장소정보를 표출하도록 클릭 이벤트를 등록합니다
      (function(marker, place) {
        kakao.maps.event.addListener(marker, 'click', function() {
          displayPlaceInfo(place);
        });
      })(marker, places[i]);
    }
  }

  // ====== 카테고리 마커 생성 ======
  function addCategoryMarker(position, order) {
    var imageSrc = 'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/places_category.png',
        imageSize = new kakao.maps.Size(27, 28),
        imgOptions = {
          spriteSize : new kakao.maps.Size(72, 208),
          spriteOrigin : new kakao.maps.Point(46, (order*36)),
          offset: new kakao.maps.Point(11, 28)
        },
        markerImage = new kakao.maps.MarkerImage(imageSrc, imageSize, imgOptions),
        marker = new kakao.maps.Marker({
          position: position,
          image: markerImage
        });

    marker.setMap(window.currentMap); // 현재 지도에 마커 표시
    categoryMarkers.push(marker);

    return marker;
  }

  // ====== 카테고리 마커 제거 ======
  function removeCategoryMarkers() {
    for (var i = 0; i < categoryMarkers.length; i++) {
      categoryMarkers[i].setMap(null);
    }
    categoryMarkers = [];
  }

  // ====== 장소 정보 표시 ======
  function displayPlaceInfo(place) {
    var content = '<div class="placeinfo">' +
                    '   <a class="title" href="' + place.place_url + '" target="_blank" title="' + place.place_name + '">' + place.place_name + '</a>';

    if (place.road_address_name) {
      content += '    <span title="' + place.road_address_name + '">' + place.road_address_name + '</span>' +
                  '  <span class="jibun" title="' + place.address_name + '">(지번 : ' + place.address_name + ')</span>';
    } else {
      content += '    <span title="' + place.address_name + '">' + place.address_name + '</span>';
    }

    content += '    <span class="tel">' + place.phone + '</span>' +
                '</div>' +
                '<div class="after"></div>';

    contentNode.innerHTML = content;
    placeOverlay.setPosition(new kakao.maps.LatLng(place.y, place.x));
    placeOverlay.setMap(window.currentMap);
  }

  // ====== 커스텀 오버레이 HTML 생성 함수 ======
  function getCustomOverlayContent() {
      // campName, address, mapY, mapX 변수가 window에 있다고 가정
      var name = window.campName || '캠핑장';
      var addr = window.address || '';
      var img = window.campImgUrl || 'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/place_thumb.png';
      var lat = window.mapY;
      var lng = window.mapX;

      // 위도, 경도 값이 있을 때만 길찾기 버튼 HTML 생성
      var directionsHtml = '';
      if (lat && lng) {
        var encodedName = encodeURIComponent(name);
        var directionsUrl = `https://map.kakao.com/link/to/${encodedName},${lat},${lng}`;
        directionsHtml = `
          <div class="actions">
                <a href="${directionsUrl}" target="_blank" class="directions-btn">🚗 길찾기</a>
                <button id="directions-from-current-loc-btn" class="current-loc-btn">📍 현위치 출발</button>
          </div>
        `;
      }

      // 최종 오버레이 HTML 반환
      return (
        '<div class="overlay_info">' +
        '  <a href="#" id="camp-detail-link" target="_blank"><strong>' + name + '</strong></a>' +
        '  <div class="desc">' +
        '    <img src="' + img + '" alt="캠핑장 이미지">' +
        '    <span class="address">' + addr + '</span>' +
        '  </div>' +
        directionsHtml + // 생성된 길찾기 버튼 HTML 추가
        '</div>'
      );
    }

  // ====== 마커 클릭 시 커스텀 오버레이 표시 ======
  function addMarkerWithCustomOverlay(map, coords) {
    var marker = new kakao.maps.Marker({
      map: map,
      position: coords
    });
    var customOverlay = new kakao.maps.CustomOverlay({
      position: coords,
      content: getCustomOverlayContent(),
      xAnchor: 0.5,
      yAnchor: 1.35 // 마커 위에 자연스럽게 위치
    });
    var isOpen = false;
    kakao.maps.event.addListener(marker, 'click', function() {
      if (isOpen) {
        customOverlay.setMap(null);
        isOpen = false;
      } else {
        customOverlay.setMap(map);
        isOpen = true;
        // 오버레이가 DOM에 추가된 후 이벤트 바인딩
        setTimeout(function() {
          // 기존 캠핑장명 클릭 이벤트
          var detailLink = document.getElementById('camp-detail-link');
          if (detailLink) {
            detailLink.onclick = function(e) {
              e.preventDefault();
              var placeName = window.campName || '캠핑장';
              var address = window.address || '';
              var places = new kakao.maps.services.Places();
              var keyword = placeName + (address ? ' ' + address : '');
              places.keywordSearch(keyword, function(data, status) {
                if (status === kakao.maps.services.Status.OK && data.length > 0) {
                  var place = data[0];
                  if (place.place_url) {
                    window.open(place.place_url, '_blank');
                  } else {
                    alert('상세정보가 없습니다.');
                  }
                } else {
                  places.keywordSearch(placeName, function(data2, status2) {
                    if (status2 === kakao.maps.services.Status.OK && data2.length > 0 && data2[0].place_url) {
                      window.open(data2[0].place_url, '_blank');
                    } else {
                      alert('상세정보가 없습니다.');
                    }
                  });
                }
              });
              return false;
            };
          }
          // '현위치 출발' 버튼 클릭 이벤트
          var currentLocBtn = document.getElementById('directions-from-current-loc-btn');
          if (currentLocBtn) {
            currentLocBtn.onclick = function() {
              // HTML5 Geolocation API 사용
              if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(function(position) {
                  // 위치 정보 얻기 성공
                  var userLat = position.coords.latitude;
                  var userLng = position.coords.longitude;
                  var destLat = window.mapY;
                  var destLng = window.mapX;
                  var destName = encodeURIComponent(window.campName || '캠핑장');

                  // 카카오맵 길찾기 URL 생성 (출발지: 현위치, 도착지: 캠핑장)
                  var url = `https://map.kakao.com/link/to/${destName},${destLat},${destLng}/from/내 위치,${userLat},${userLng}`;

                  // 새 탭에서 길찾기 페이지 열기
                  window.open(url, '_blank');

                }, function(error) {
                  // 위치 정보 얻기 실패 시 오류 처리
                  var errorMsg = "현재 위치를 가져올 수 없습니다. ";
                  switch(error.code) {
                    case error.PERMISSION_DENIED:
                      errorMsg += "브라우저의 위치 정보 접근 권한을 허용해주세요.";
                      break;
                    case error.POSITION_UNAVAILABLE:
                      errorMsg += "사용할 수 없는 위치 정보입니다.";
                      break;
                    case error.TIMEOUT:
                      errorMsg += "요청 시간이 초과되었습니다.";
                      break;
                    default:
                      errorMsg += "알 수 없는 오류가 발생했습니다.";
                      break;
                  }
                  alert(errorMsg);
                });
              } else {
                // Geolocation API를 지원하지 않는 브라우저
                alert("이 브라우저에서는 위치 정보 기능을 지원하지 않습니다.");
              }
            };
          }
        }, 0);
      }
    });
    // 3가지 모드 컨트롤 추가
    addRoadviewControl(coords, map);
  }

  // 로드뷰 컨트롤 추가 함수
  function addRoadviewControl(coords, map) {
    // 로드뷰 컨테이너 생성
    var roadviewContainer = document.createElement('div');
    roadviewContainer.id = 'roadview-container';
    roadviewContainer.style.width = '100%';
    roadviewContainer.style.height = '350px';
    roadviewContainer.style.display = 'none';
    roadviewContainer.style.marginTop = '10px';
    mapContainer.parentNode.insertBefore(roadviewContainer, mapContainer.nextSibling);

    // 버튼 컨테이너 생성
    var buttonContainer = document.createElement('div');
    buttonContainer.style.position = 'absolute';
    buttonContainer.style.top = '10px';
    buttonContainer.style.right = '10px';
    buttonContainer.style.zIndex = 10;
    buttonContainer.style.display = 'flex';
    buttonContainer.style.gap = '8px';
    mapContainer.parentNode.style.position = 'relative';
    mapContainer.parentNode.appendChild(buttonContainer);

    // 지도 버튼 생성
    var mapBtn = document.createElement('button');
    mapBtn.innerText = '지도';
    mapBtn.className = 'map-mode-btn active';
    mapBtn.style.background = '#ff9800';
    mapBtn.style.color = '#fff';
    mapBtn.style.border = 'none';
    mapBtn.style.borderRadius = '6px';
    mapBtn.style.padding = '8px 16px';
    mapBtn.style.fontWeight = 'bold';
    mapBtn.style.cursor = 'pointer';
    buttonContainer.appendChild(mapBtn);

    // 로드뷰 버튼 생성
    var roadBtn = document.createElement('button');
    roadBtn.innerText = '로드뷰';
    roadBtn.className = 'map-mode-btn';
    roadBtn.style.background = '#4CAF50';
    roadBtn.style.color = '#fff';
    roadBtn.style.border = 'none';
    roadBtn.style.borderRadius = '6px';
    roadBtn.style.padding = '8px 16px';
    roadBtn.style.fontWeight = 'bold';
    roadBtn.style.cursor = 'pointer';
    buttonContainer.appendChild(roadBtn);

    // 스카이뷰 버튼 생성
    var skyBtn = document.createElement('button');
    skyBtn.innerText = '스카이뷰';
    skyBtn.className = 'map-mode-btn';
    skyBtn.style.background = '#2196F3';
    skyBtn.style.color = '#fff';
    skyBtn.style.border = 'none';
    skyBtn.style.borderRadius = '6px';
    skyBtn.style.padding = '8px 16px';
    skyBtn.style.fontWeight = 'bold';
    skyBtn.style.cursor = 'pointer';
    buttonContainer.appendChild(skyBtn);

    var roadview = null;
    var roadviewOverlay = null; // 로드뷰용 커스텀 오버레이
    var currentMode = 'map'; // map, roadview, skyview

    function setActive(btn) {
      [mapBtn, roadBtn, skyBtn].forEach(function(b) { b.classList.remove('active'); });
      btn.classList.add('active');
    }

    mapBtn.onclick = function() {
      // 일반 지도 모드
      setActive(mapBtn);
      mapContainer.style.display = 'block';
      roadviewContainer.style.display = 'none';
      map.setMapTypeId(kakao.maps.MapTypeId.ROADMAP);
      currentMode = 'map';
      // 로드뷰 오버레이 제거
      if (roadviewOverlay) roadviewOverlay.setMap(null);
    };

    roadBtn.onclick = function() {
      // 로드뷰 모드
      if (!roadview) {
        roadview = new kakao.maps.Roadview(roadviewContainer);
      }
      var roadviewClient = new kakao.maps.RoadviewClient();
      roadviewClient.getNearestPanoId(coords, 50, function(panoId) {
        if (panoId) {
          setActive(roadBtn);
          roadview.setPanoId(panoId, coords);
          roadviewContainer.style.display = 'block';
          mapContainer.style.display = 'none';
          currentMode = 'roadview';
          // 로드뷰 오버레이 생성 및 표시
          if (roadviewOverlay) roadviewOverlay.setMap(null);
          roadviewOverlay = new kakao.maps.CustomOverlay({
            position: coords,
            content: getCustomOverlayContent(),
            xAnchor: 0.5,
            yAnchor: 1.35 // 마커 위에 자연스럽게 위치
          });
          roadviewOverlay.setMap(roadview);
          // 로드뷰 중심에 오버레이가 오도록 시점 조정
          kakao.maps.event.addListener(roadview, 'init', function() {
            var projection = roadview.getProjection();
            var viewpoint = projection.viewpointFromCoords(roadviewOverlay.getPosition(), roadviewOverlay.getAltitude ? roadviewOverlay.getAltitude() : 0);
            roadview.setViewpoint(viewpoint);
          });
        } else {
          alert('해당 위치에는 로드뷰가 없습니다.');
        }
      });
    };

    skyBtn.onclick = function() {
      // 스카이뷰(위성) 모드
      setActive(skyBtn);
      mapContainer.style.display = 'block';
      roadviewContainer.style.display = 'none';
      map.setMapTypeId(kakao.maps.MapTypeId.HYBRID);
      currentMode = 'skyview';
      // 로드뷰 오버레이 제거
      if (roadviewOverlay) roadviewOverlay.setMap(null);
    };
  }

  // ====== 지도 초기화 및 카테고리 검색 설정 ======
  function initializeMapWithCategorySearch(coords) {
    // 지도 컨테이너에 position: relative 설정
    mapContainer.style.position = 'relative';

    var map = new kakao.maps.Map(mapContainer, {
      center: coords,
      level: 4
    });

    // 전역 변수로 지도 객체 저장
    window.currentMap = map;

    // 장소 검색 객체 생성
    ps = new kakao.maps.services.Places(map);

    // 지도에 idle 이벤트를 등록하여 카테고리 검색 활성화
    kakao.maps.event.addListener(map, 'idle', searchPlaces);

    // 커스텀 오버레이 설정
    contentNode.className = 'placeinfo_wrap';
    placeOverlay.setContent(contentNode);

    // 카테고리 UI 생성
    createCategoryUI();

    // 날씨 컨트롤 UI 생성
    createWeatherControls();

    // 캠핑장 마커 추가
    addMarkerWithCustomOverlay(map, coords);

    // 지도 클릭 이벤트 추가 (날씨 정보 표시)
    kakao.maps.event.addListener(map, 'click', function(mouseEvent) {
      if (weatherLayerVisible) {
        // 전국 날씨 모드에서는 클릭 시 해당 위치의 가장 가까운 도시 날씨 표시
        const nearestCity = findNearestCity(mouseEvent.latLng.getLat(), mouseEvent.latLng.getLng());
        const cityData = NATIONAL_CITIES.find(c => c.name === nearestCity);
        if (cityData && nationalWeatherData[nearestCity]) {
          const position = new kakao.maps.LatLng(cityData.lat, cityData.lng);
          showNationalWeatherDetail(nearestCity, nationalWeatherData[nearestCity], position);
        }
      }
    });

    // 지도 중심 변경 이벤트 추가
    kakao.maps.event.addListener(map, 'center_changed', function() {
      // 전국 날씨 모드에서는 중심 변경 시 자동 업데이트하지 않음
    });

    // 지도 리사이즈 및 중심 설정
    setTimeout(function() {
      map.relayout();
      map.setCenter(coords);
    }, 100);

    return map;
  }

  // 1. 위도/경도 값이 있으면 바로 지도 표시
  if (typeof mapY !== 'undefined' && typeof mapX !== 'undefined' && mapY && mapX) {
    console.log('위도/경도로 지도 표시:', mapY, mapX);
    var coords = new kakao.maps.LatLng(mapY, mapX);
    initializeMapWithCategorySearch(coords);
    return;
  }

  // 2. mapY/mapX 없으면 DOM에서 주소 읽기
  var addressElem = document.getElementById('camp-address');
  var domAddress = addressElem ? addressElem.textContent.trim() : "";
  var useAddress = address || domAddress;

  console.log('주소로 지도 표시:', useAddress);

  if (!useAddress) {
    console.error('주소 정보가 없습니다.');
    mapContainer.innerHTML = '지도를 불러올 수 없습니다. (주소 정보 없음)';
    return;
  }

  var geocoder = new kakao.maps.services.Geocoder();
  geocoder.addressSearch(useAddress, function(result, status) {
    if (status === kakao.maps.services.Status.OK) {
      console.log('주소 검색 성공:', result[0]);
      window.mapX = result[0].x;
      window.mapY = result[0].y;

      var coords = new kakao.maps.LatLng(result[0].y, result[0].x);
      initializeMapWithCategorySearch(coords);
    } else {
      console.error('주소 검색 실패:', status);
      mapContainer.innerHTML = '지도를 불러올 수 없습니다. (주소 검색 실패)';
    }
  });
}); 