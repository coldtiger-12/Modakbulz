// 카카오맵 API: detail.html과 별개로 동작하는 지도 (추천 캠핑장 마커 + 모든 캠핑장 작은 점 표시)
window.initMainMap = function() {
  // CSS 애니메이션 스타일 추가
  const style = document.createElement('style');
  style.innerHTML = `
    @keyframes pulse {
      0% { transform: scale(1); }
      50% { transform: scale(1.05); }
      100% { transform: scale(1); }
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-10px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .weather-fade-in {
      animation: fadeIn 0.5s ease-in;
    }
  `;
  document.head.appendChild(style);
  var mapContainer = document.getElementById('main-map');
  var roadviewContainer = document.getElementById('roadview-container');
  if (!mapContainer) return;

  var mapOption = {
    center: new kakao.maps.LatLng(36.5, 127.8), // 대한민국 중심 좌표(임의)
    level: 13
  };
  var map = new kakao.maps.Map(mapContainer, mapOption);
  var roadview = null;
  var roadviewOverlay = null;
  var currentMode = 'map';
  var currentOpenOverlay = null; // 현재 열린 오버레이를 추적
  window.currentOpenOverlay = currentOpenOverlay; // 전역 접근을 위해 window 객체에 추가

  // 마커 관리를 위한 변수들
  var allMarkers = []; // 모든 마커를 저장 (추천 + 일반)
  var currentRegion = 'all'; // 현재 선택된 지역

  // 전국 날씨 관련 변수들
  var nationalWeatherMarkers = []; // 전국 날씨 마커들
  var nationalWeatherData = {}; // 전국 날씨 데이터 캐시
  var weatherAnimationInterval = null; // 날씨 애니메이션 인터벌
  var weatherLayerVisible = false; // 날씨 레이어 표시 상태

  // ====== 전국 날씨 관련 함수들 ======

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
    markerContent.style.cssText = `
      background: rgba(255,255,255,0.9);
      border-radius: 50%;
      width: 60px;
      height: 60px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      box-shadow: 0 2px 8px rgba(0,0,0,0.2);
      border: 2px solid #fff;
      cursor: pointer;
      transition: all 0.3s ease;
      animation: pulse 2s infinite;
    `;
    markerContent.innerHTML = `
      <div style="font-size:1.5rem;margin-bottom:2px;">${getWeatherIcon(weatherData.current.weather_code)}</div>
      <div style="font-size:0.8rem;font-weight:bold;color:#333;">${weatherData.current.temp}°</div>
      <div style="font-size:0.6rem;color:#666;margin-top:1px;">${city}</div>
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
    const content = `
      <div style="background:rgba(255,255,255,0.95);border-radius:8px;padding:15px;box-shadow:0 4px 12px rgba(0,0,0,0.15);max-width:280px;font-family:'Malgun Gothic',sans-serif;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
          <span style="font-weight:bold;font-size:14px;color:#333;">${city} 날씨</span>
          <button onclick="this.parentElement.parentElement.parentElement.remove()" style="background:none;border:none;font-size:18px;cursor:pointer;color:#666;">&times;</button>
        </div>
        <div style="display:flex;align-items:center;gap:12px;margin-bottom:12px;">
          <div style="font-size:2.5rem;">${getWeatherIcon(weatherData.current.weather_code)}</div>
          <div>
            <div style="font-size:2rem;font-weight:bold;color:#333;">${weatherData.current.temp}°C</div>
            <div style="font-size:13px;color:#666;">${weatherData.current.description}</div>
          </div>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;font-size:12px;">
          <div style="display:flex;justify-content:space-between;">
            <span style="color:#666;">체감온도</span>
            <span style="font-weight:bold;color:#333;">${weatherData.current.feels_like}°C</span>
          </div>
          <div style="display:flex;justify-content:space-between;">
            <span style="color:#666;">습도</span>
            <span style="font-weight:bold;color:#333;">${weatherData.current.humidity}%</span>
          </div>
          <div style="display:flex;justify-content:space-between;">
            <span style="color:#666;">바람</span>
            <span style="font-weight:bold;color:#333;">${weatherData.current.wind_speed}m/s</span>
          </div>
          <div style="display:flex;justify-content:space-between;">
            <span style="color:#666;">강수확률</span>
            <span style="font-weight:bold;color:#333;">${weatherData.current.pop || 0}%</span>
          </div>
        </div>
      </div>
    `;

    const overlay = new kakao.maps.CustomOverlay({
      position: position,
      content: content,
      xAnchor: 0.5,
      yAnchor: 1.1
    });

    overlay.setMap(map);

    // 5초 후 자동으로 닫기
    setTimeout(() => {
      overlay.setMap(null);
    }, 5000);
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
            marker.setMap(map);
            nationalWeatherMarkers.push(marker);
          }, index * 100); // 100ms 간격으로 마커 생성
        }
      });
    });
  }

  // 날씨 레이어 토글
  function toggleWeatherLayer() {
    if (weatherLayerVisible) {
      // 날씨 레이어 숨기기
      weatherLayerVisible = false;
      const weatherBtn = document.querySelector('.weather-btn');
      if (weatherBtn) {
        weatherBtn.classList.remove('active');
        weatherBtn.textContent = '전국 날씨';
      }

      // 전국 날씨 마커들 제거
      nationalWeatherMarkers.forEach(marker => marker.setMap(null));
      nationalWeatherMarkers = [];

      // 애니메이션 인터벌 정리
      if (weatherAnimationInterval) {
        clearInterval(weatherAnimationInterval);
        weatherAnimationInterval = null;
      }
    } else {
      // 날씨 레이어 표시
      weatherLayerVisible = true;
      const weatherBtn = document.querySelector('.weather-btn');
      if (weatherBtn) {
        weatherBtn.classList.add('active');
        weatherBtn.textContent = '날씨 숨기기';
      }

      // 전국 날씨 애니메이션 시작
      startNationalWeatherAnimation();

             // 30분마다 날씨 데이터 갱신 (백엔드 스케줄러와 동기화)
       weatherAnimationInterval = setInterval(() => {
         if (weatherLayerVisible) {
           startNationalWeatherAnimation();
         }
       }, 1800000); // 30분 = 1800초 = 1800000ms
    }
  }

  // 날씨 컨트롤 버튼 생성
  function createWeatherButton() {
    const weatherBtn = document.createElement('button');
    weatherBtn.className = 'weather-btn';
    weatherBtn.textContent = '전국 날씨';
    weatherBtn.style.cssText = `
      background: #4CAF50;
      color: #fff;
      border: none;
      border-radius: 6px;
      padding: 8px 16px;
      font-weight: bold;
      cursor: pointer;
      font-size: 12px;
      margin-left: 8px;
    `;
    weatherBtn.onclick = toggleWeatherLayer;

    const controlsContainer = document.querySelector('.map-controls');
    if (controlsContainer) {
      controlsContainer.appendChild(weatherBtn);
    }
  }

  // ====== 지역별 필터링 함수 ======
  function filterMarkersByRegion(region) {
    console.log('지역 필터링 시작:', region);
    console.log('전체 마커 개수:', allMarkers.length);

    // 기존 오버레이 닫기
    if (currentOpenOverlay) {
      currentOpenOverlay.setMap(null);
      currentOpenOverlay = null;
      window.currentOpenOverlay = null;
    }

    // 모든 마커 숨기기
    allMarkers.forEach(function(marker) {
      marker.setMap(null);
    });

    if (region === 'recommended') {
      // 추천 캠핑장 선택 시 추천 마커만 표시
      var recommendedCount = 0;
      allMarkers.forEach(function(marker) {
        if (marker.isRecommended === true) {
          marker.setMap(map);
          recommendedCount++;
        }
      });
      console.log('추천 마커 표시 개수:', recommendedCount);
      currentRegion = 'recommended';
    } else if (region === 'all') {
      // 전국 선택 시 모든 마커 표시
      allMarkers.forEach(function(marker) {
        marker.setMap(map);
      });
      console.log('전체 마커 표시 완료');
      currentRegion = 'all';
         } else {
       // 기존 주소 기반 필터링 방식 사용 (즉시 실행)
       var regionNameMap = {
         '서울': ['서울특별시', '서울시', '서울'],
         '부산': ['부산광역시', '부산시', '부산'],
         '대구': ['대구광역시', '대구시', '대구'],
         '인천': ['인천광역시', '인천시', '인천'],
         '광주': ['광주광역시', '광주시', '광주'],
         '대전': ['대전광역시', '대전시', '대전'],
         '울산': ['울산광역시', '울산시', '울산'],
         '세종': ['세종특별자치시', '세종시', '세종'],
         '경기': ['경기도', '경기'],
         '강원': ['강원도', '강원'],
         '충북': ['충청북도', '충북'],
         '충남': ['충청남도', '충남'],
         '전북': ['전라북도', '전북'],
         '전남': ['전라남도', '전남'],
         '경북': ['경상북도', '경북'],
         '경남': ['경상남도', '경남'],
         '제주': ['제주특별자치도', '제주도', '제주']
       };

       var targetRegions = regionNameMap[region];
       console.log('선택된 지역:', region, '->', targetRegions);

       if (targetRegions) {
         var inRegionCount = 0;

         // 각 마커의 지역 정보를 확인하여 필터링
         allMarkers.forEach(function(marker) {
           var markerRegion = marker.region;
           console.log(`마커 지역 확인: ${markerRegion} vs ${targetRegions}`);

           // 해당 지역에 속하는지 확인
           if (targetRegions.includes(markerRegion)) {
             marker.setMap(map);
             inRegionCount++;
             console.log(`✅ ${markerRegion} 지역 마커 추가 (총 ${inRegionCount}개)`);
           }
         });

         console.log(`최종 결과: ${region} 지역 내 마커 ${inRegionCount}개 표시`);
         currentRegion = region;
       } else {
         console.log('매핑되지 않은 지역:', region);
         currentRegion = region;
       }
     }

    currentRegion = region;
  }

  // ====== 주소에서 지역 추출 함수 ======
  function extractRegionFromAddress(address) {
    if (!address) return 'unknown';

    // 주소를 공백으로 분리하여 첫 번째 부분(시/도) 확인
    var parts = address.split(' ');
    if (parts.length === 0) return 'unknown';

    var firstPart = parts[0];

    // 더 정확한 지역 매칭을 위한 규칙
    if (firstPart === '서울특별시' || firstPart === '서울시' || firstPart === '서울') {
      return '서울';
    }
    if (firstPart === '부산광역시' || firstPart === '부산시' || firstPart === '부산') {
      return '부산';
    }
    if (firstPart === '대구광역시' || firstPart === '대구시' || firstPart === '대구') {
      return '대구';
    }
    if (firstPart === '인천광역시' || firstPart === '인천시' || firstPart === '인천') {
      return '인천';
    }
    if (firstPart === '광주광역시' || firstPart === '광주시' || firstPart === '광주') {
      return '광주';
    }
    if (firstPart === '대전광역시' || firstPart === '대전시' || firstPart === '대전') {
      return '대전';
    }
    if (firstPart === '울산광역시' || firstPart === '울산시' || firstPart === '울산') {
      return '울산';
    }
    if (firstPart === '세종특별자치시' || firstPart === '세종시' || firstPart === '세종') {
      return '세종';
    }
    if (firstPart === '경기도' || firstPart === '경기') {
      return '경기';
    }
    if (firstPart === '강원도' || firstPart === '강원') {
      return '강원';
    }
    if (firstPart === '충청북도' || firstPart === '충북') {
      return '충북';
    }
    if (firstPart === '충청남도' || firstPart === '충남') {
      return '충남';
    }
    if (firstPart === '전라북도' || firstPart === '전북') {
      return '전북';
    }
    if (firstPart === '전라남도' || firstPart === '전남') {
      return '전남';
    }
    if (firstPart === '경상북도' || firstPart === '경북') {
      return '경북';
    }
    if (firstPart === '경상남도' || firstPart === '경남') {
      return '경남';
    }
    if (firstPart === '제주특별자치도' || firstPart === '제주도' || firstPart === '제주') {
      return '제주';
    }

    return 'unknown';
  }



  // ====== 커스텀 오버레이용 CSS 동적 삽입 ======
  (function injectCustomOverlayCSS() {
    var style = document.createElement('style');
    style.innerHTML = `
      .overlay_info {
        border-radius: 6px;
        margin-bottom: 12px;
        float: left;
        position: relative;
        border: 1px solid #ccc;
        border-bottom: 2px solid #ddd;
        background-color: #fff;
        z-index: 10;
        box-shadow: 0px 1px 2px #888;
        min-width: 200px;
      }
      .overlay_info a {
        display: block;
        background: #d95050 url(https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/arrow_white.png) no-repeat right 14px center;
        text-decoration: none;
        color: #fff;
        padding: 12px 36px 12px 14px;
        font-size: 14px;
        border-radius: 6px 6px 0 0;
        font-weight: bold;
      }
      .overlay_info a strong {
        background: url(https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/place_icon.png) no-repeat;
        padding-left: 27px;
      }
      .overlay_info .desc {
        padding: 14px;
        position: relative;
        min-width: 190px;
        min-height: 56px;
        display: flex;
        align-items: flex-start;
        gap: 10px;
      }
      .overlay_info img {
        vertical-align: top;
        width: 60px;
        height: 40px;
        object-fit: cover;
        border-radius: 4px;
        flex-shrink: 0;
      }
      .overlay_info .address {
        font-size: 12px;
        color: #333;
        flex: 1;
        line-height: 1.4;
        word-break: keep-all;
      }
      .overlay_info .coordinates {
        font-size: 11px;
        color: #666;
        margin-top: 4px;
      }
      .overlay_info:after {
        content: '';
        position: absolute;
        margin-left: -11px;
        left: 50%;
        bottom: -12px;
        width: 22px;
        height: 12px;
        background: url(https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/vertex_white.png) no-repeat 0 bottom;
      }
      .overlay_info .close {
        position: absolute;
        top: 5px;
        right: 5px;
        width: 20px;
        height: 20px;
        background: rgba(0,0,0,0.3);
        color: white;
        border: none;
        border-radius: 50%;
        cursor: pointer;
        font-size: 12px;
        line-height: 1;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .overlay_info .close:hover {
        background: rgba(0,0,0,0.5);
      }
      .map-mode-btn.active {
        opacity: 1;
      }
      .map-mode-btn:not(.active) {
        opacity: 0.7;
      }
    `;
    document.head.appendChild(style);
  })();

  // ====== 커스텀 오버레이 HTML 생성 함수 ======
  function getCustomOverlayContent(campData) {
    var name = campData.facltNm || '캠핑장';
    var addr1 = campData.addr1 || '';
    var addr2 = campData.addr2 || '';
    var tel = campData.tel || '';
    var img = campData.firstImageUrl || 'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/place_thumb.png';

    // 디버깅: 전화번호 데이터 확인
    console.log('캠핑장 데이터:', campData);
    console.log('전화번호:', tel);

    // 주소 정보 조합
    var fullAddress = addr1;
    if (addr2 && addr2.trim()) {
      fullAddress += ' ' + addr2;
    }

    // 전화번호 정보
    var telInfo = '';
    if (tel && tel.trim()) {
      telInfo = `<div class="coordinates">📞 ${tel}</div>`;
    }

    return (
      '<div class="overlay_info">' +
      '  <button class="close" onclick="this.parentElement.parentElement.setMap(null); window.currentOpenOverlay = null;">×</button>' +
      '  <a href="#" id="camp-detail-link" target="_blank"><strong>' + name + '</strong></a>' +
      '  <div class="desc">' +
      '    <img src="' + img + '" alt="캠핑장 이미지">' +
      '    <div class="address">' + fullAddress + telInfo + '</div>' +
      '  </div>' +
      '</div>'
    );
  }



//  // ====== 지도/로드뷰 모드 전환 ======
//  function setActiveMode(mode) {
//    var mapBtn = document.getElementById('map-btn');
//    var roadviewBtn = document.getElementById('roadview-btn');
//
//    if (mode === 'map') {
//      mapContainer.style.display = 'block';
//      roadviewContainer.style.display = 'none';
//      mapBtn.classList.add('active');
//      roadviewBtn.classList.remove('active');
//      currentMode = 'map';
//    } else if (mode === 'roadview') {
//      mapContainer.style.display = 'none';
//      roadviewContainer.style.display = 'block';
//      mapBtn.classList.remove('active');
//      roadviewBtn.classList.add('active');
//      currentMode = 'roadview';
//    }
//  }
//
//  // 지도 버튼 클릭 이벤트
//  document.getElementById('map-btn').onclick = function() {
//    setActiveMode('map');
//  };
//
//  // 로드뷰 버튼 클릭 이벤트
//  document.getElementById('roadview-btn').onclick = function() {
//    if (!roadview) {
//      roadview = new kakao.maps.Roadview(roadviewContainer);
//    }
//    setActiveMode('roadview');
//
//    // 로드뷰 초기화 (지도 중심 좌표 기준)
//    var roadviewClient = new kakao.maps.RoadviewClient();
//    roadviewClient.getNearestPanoId(map.getCenter(), 50, function(panoId) {
//      if (panoId) {
//        roadview.setPanoId(panoId, map.getCenter());
//      } else {
//        alert('해당 위치에는 로드뷰가 없습니다.');
//        setActiveMode('map');
//      }
//    });
//  };

  // 추천 캠핑장 8개 (기본 마커)
  fetch('/camping/recommendations')
    .then(response => response.json())
    .then(camps => {
      console.log('추천 캠핑장 데이터:', camps);
      if (camps && camps.length > 0) {
        var bounds = new kakao.maps.LatLngBounds();
        var markerCount = 0;
        var geocoder = new kakao.maps.services.Geocoder();

        function createRecommendationMarker(camp, position) {
          // 추천 캠핑장은 기본 마커 사용
          var marker = new kakao.maps.Marker({
            map: map,
            position: position
          });

          // 지역 정보 추가
          var region = extractRegionFromAddress(camp.addr1);
          marker.region = region;
          marker.isRecommended = true; // 추천 캠핑장 표시

          var customOverlay = new kakao.maps.CustomOverlay({
            position: position,
            content: getCustomOverlayContent(camp),
            xAnchor: 0.5,
            yAnchor: 1.35
          });

          var isOpen = false;

          kakao.maps.event.addListener(marker, 'click', function() {
            if (isOpen) {
              customOverlay.setMap(null);
              isOpen = false;
              currentOpenOverlay = null;
              window.currentOpenOverlay = null;
            } else {
              // 기존에 열린 오버레이가 있으면 닫기
              if (currentOpenOverlay && currentOpenOverlay !== customOverlay) {
                currentOpenOverlay.setMap(null);
              }

              // 현재 모드에 따라 오버레이 표시
              if (currentMode === 'map') {
                customOverlay.setMap(map);
              } else if (currentMode === 'roadview' && roadview) {
                customOverlay.setMap(roadview);
              }

              isOpen = true;
              currentOpenOverlay = customOverlay;
              window.currentOpenOverlay = customOverlay;

              // 오버레이가 DOM에 추가된 후 이벤트 바인딩
              setTimeout(function() {
                var detailLink = document.getElementById('camp-detail-link');
                if (detailLink) {
                  detailLink.onclick = function(e) {
                    e.preventDefault();
                    // 캠핑장 상세 페이지로 이동
                    if (camp.contentId) {
                      window.open('/camping/' + camp.contentId, '_blank');
                    } else {
                      // contentId가 없으면 카카오맵 검색
                      var placeName = camp.facltNm || '캠핑장';
                      var address = camp.addr1 || '';
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
                    }
                    return false;
                  };
                }
              }, 0);
            }
          });

          // 마커 배열에 추가
          allMarkers.push(marker);

          bounds.extend(position);
          markerCount++;
        }

        camps.forEach((camp, index) => {
          console.log(`캠핑장 ${index + 1}:`, camp.facltNm, 'mapY:', camp.mapY, 'mapX:', camp.mapX, 'addr1:', camp.addr1);
          if (camp.mapY && camp.mapX) {
            createRecommendationMarker(camp, new kakao.maps.LatLng(camp.mapY, camp.mapX));
          } else if (camp.addr1) {
            geocoder.addressSearch(camp.addr1, function(result, status) {
              if (status === kakao.maps.services.Status.OK) {
                console.log(`주소 검색 성공 (${camp.facltNm}):`, result[0]);
                var coords = new kakao.maps.LatLng(result[0].y, result[0].x);
                var updatedCamp = {
                  ...camp,
                  mapX: result[0].x,
                  mapY: result[0].y
                };
                createRecommendationMarker(updatedCamp, coords);
                if (markerCount > 0) map.setBounds(bounds);
              } else {
                console.log(`주소 검색 실패 (${camp.facltNm}):`, status);
                // 주소 검색이 실패하면 마커를 생성하지 않음
              }
            });
          } else {
            // 좌표와 주소가 모두 없으면 마커를 생성하지 않음
            console.log(`좌표/주소 없음 - 마커 생성하지 않음: ${camp.facltNm}`);
          }
        });
        if (markerCount > 0) map.setBounds(bounds);
        console.log('생성된 추천 마커 개수:', markerCount);
      } else {
        console.log('추천 캠핑장 데이터가 없습니다.');
      }
    })
    .catch(error => {
      console.error('추천 캠핑장 데이터를 가져오는 중 오류 발생:', error);
    });



  // 날씨 버튼 생성
  createWeatherButton();

  // 초기화: 페이지 로드 시 추천 캠핑장만 표시
  setTimeout(function() {
    // 추천 캠핑장만 표시
    filterMarkersByRegion('recommended');

    console.log('초기화 완료: 추천 캠핑장만 표시');
  }, 3000); // 3초 후 실행 (데이터 로딩 완료 후)

  // 전역 함수로 마커 업데이트 기능 추가
  window.updateMapMarkers = function(camps) {
    console.log('지도 마커 업데이트 시작:', camps);

    // 기존 추천 마커들 제거
    allMarkers.forEach(function(marker) {
      if (marker.isRecommended) {
        marker.setMap(null);
      }
    });

    // 새로운 추천 마커들 생성
    if (camps && camps.length > 0) {
      var bounds = new kakao.maps.LatLngBounds();
      var markerCount = 0;
      var geocoder = new kakao.maps.services.Geocoder();

      function createNewRecommendationMarker(camp, position) {
        var marker = new kakao.maps.Marker({
          map: map,
          position: position
        });

        var region = extractRegionFromAddress(camp.addr1);
        marker.region = region;
        marker.isRecommended = true;

        var customOverlay = new kakao.maps.CustomOverlay({
          position: position,
          content: getCustomOverlayContent(camp),
          xAnchor: 0.5,
          yAnchor: 1.35
        });

        var isOpen = false;

        kakao.maps.event.addListener(marker, 'click', function() {
          if (isOpen) {
            customOverlay.setMap(null);
            isOpen = false;
          } else {
            if (window.currentOpenOverlay) {
              window.currentOpenOverlay.setMap(null);
            }
            customOverlay.setMap(map);
            isOpen = true;
            window.currentOpenOverlay = customOverlay;
          }
        });

        allMarkers.push(marker);
        bounds.extend(position);
        markerCount++;
      }

      camps.forEach((camp, index) => {
        if (camp.mapY && camp.mapX) {
          createNewRecommendationMarker(camp, new kakao.maps.LatLng(camp.mapY, camp.mapX));
        } else if (camp.addr1) {
          geocoder.addressSearch(camp.addr1, function(result, status) {
            if (status === kakao.maps.services.Status.OK) {
              var coords = new kakao.maps.LatLng(result[0].y, result[0].x);
              var updatedCamp = {
                ...camp,
                mapX: result[0].x,
                mapY: result[0].y
              };
              createNewRecommendationMarker(updatedCamp, coords);
              if (markerCount > 0) map.setBounds(bounds);
            }
          });
        }
      });

      if (markerCount > 0) {
        map.setBounds(bounds);
        console.log('새로운 추천 마커 생성 완료:', markerCount);
      }
    }
  };
}