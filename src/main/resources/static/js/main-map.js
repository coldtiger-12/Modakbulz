// 카카오맵 API: detail.html과 별개로 동작하는 지도 (추천 캠핑장 마커 + 모든 캠핑장 작은 점 표시)
window.initMainMap = function() {
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

  // ====== 지역별 필터링 함수 ======
  function filterMarkersByRegion(region) {
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
      allMarkers.forEach(function(marker) {
        if (marker.isRecommended) {
          marker.setMap(map);
        }
      });
    } else if (region === 'all') {
      // 전국 선택 시 모든 마커 표시
      allMarkers.forEach(function(marker) {
        marker.setMap(map);
      });
    } else {
      // 선택된 지역의 마커만 표시
      allMarkers.forEach(function(marker, index) {
        if (marker.region === region) {
          marker.setMap(map);
        }
      });
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

  // ====== 드롭다운 이벤트 리스너 ======
  document.getElementById('region-select').addEventListener('change', function() {
    var selectedRegion = this.value;
    filterMarkersByRegion(selectedRegion);
  });

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



  // ====== 지도/로드뷰 모드 전환 ======
  function setActiveMode(mode) {
    var mapBtn = document.getElementById('map-btn');
    var roadviewBtn = document.getElementById('roadview-btn');
    
    if (mode === 'map') {
      mapContainer.style.display = 'block';
      roadviewContainer.style.display = 'none';
      mapBtn.classList.add('active');
      roadviewBtn.classList.remove('active');
      currentMode = 'map';
    } else if (mode === 'roadview') {
      mapContainer.style.display = 'none';
      roadviewContainer.style.display = 'block';
      mapBtn.classList.remove('active');
      roadviewBtn.classList.add('active');
      currentMode = 'roadview';
    }
  }

  // 지도 버튼 클릭 이벤트
  document.getElementById('map-btn').onclick = function() {
    setActiveMode('map');
  };

  // 로드뷰 버튼 클릭 이벤트
  document.getElementById('roadview-btn').onclick = function() {
    if (!roadview) {
      roadview = new kakao.maps.Roadview(roadviewContainer);
    }
    setActiveMode('roadview');
    
    // 로드뷰 초기화 (지도 중심 좌표 기준)
    var roadviewClient = new kakao.maps.RoadviewClient();
    roadviewClient.getNearestPanoId(map.getCenter(), 50, function(panoId) {
      if (panoId) {
        roadview.setPanoId(panoId, map.getCenter());
      } else {
        alert('해당 위치에는 로드뷰가 없습니다.');
        setActiveMode('map');
      }
    });
  };

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
                // 주소 검색이 실패해도 작은 빨간 점으로 표시 (임시 좌표 사용)
                console.log(`임시 좌표로 작은 점 생성: ${camp.facltNm}`);
                createSmallMarker(camp, new kakao.maps.LatLng(36.5, 127.8)); // 대한민국 중심 좌표
              }
            });
          } else {
            // 좌표와 주소가 모두 없으면 임시 좌표로 작은 빨간 점 생성
            console.log(`좌표/주소 없음 - 임시 좌표로 작은 점 생성: ${camp.facltNm}`);
            createSmallMarker(camp, new kakao.maps.LatLng(36.5, 127.8)); // 대한민국 중심 좌표
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

  // 모든 캠핑장 데이터 (작은 빨간 점)
  fetch('/api/camping/all-data')
    .then(response => {
      console.log('전체 캠핑장 API 응답 상태:', response.status);
      return response.json();
    })
    .then(allCamps => {
      console.log('전체 캠핑장 데이터:', allCamps);
      console.log('전체 캠핑장 개수:', allCamps ? allCamps.length : 0);
      if (allCamps && allCamps.length > 0) {
        var smallMarkerCount = 0;
        var geocoder = new kakao.maps.services.Geocoder();

        function createSmallMarker(camp, position) {
          // 카카오맵 음식점처럼 작은 빨간 점 생성
          var svgString = '<svg width="8" height="8" xmlns="http://www.w3.org/2000/svg"><circle cx="4" cy="4" r="4" fill="#ff0000" stroke="#ffffff" stroke-width="1"/></svg>';
          var marker = new kakao.maps.Marker({
            map: map,
            position: position,
            image: new kakao.maps.MarkerImage(
              'data:image/svg+xml;base64,' + btoa(svgString),
              new kakao.maps.Size(8, 8)
            )
          });
          
          // 지역 정보 추가
          var region = extractRegionFromAddress(camp.addr1);
          marker.region = region;
          marker.isRecommended = false; // 일반 캠핑장 표시
          
          smallMarkerCount++;
          
          // 클릭 시 커스텀 오버레이 표시
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
          
          // 마커 배열에 추가 (allMarkers에 통합)
          allMarkers.push(marker);
        }

        allCamps.forEach((camp, index) => {
          if (camp.mapY && camp.mapX) {
            createSmallMarker(camp, new kakao.maps.LatLng(parseFloat(camp.mapY), parseFloat(camp.mapX)));
          } else if (camp.addr1) {
            geocoder.addressSearch(camp.addr1, function(result, status) {
              if (status === kakao.maps.services.Status.OK) {
                var coords = new kakao.maps.LatLng(result[0].y, result[0].x);
                var updatedCamp = {
                  ...camp,
                  mapX: result[0].x,
                  mapY: result[0].y
                };
                createSmallMarker(updatedCamp, coords);
              }
            });
          }
        });
        console.log('생성된 작은 마커 개수:', smallMarkerCount);
      } else {
        console.log('전체 캠핑장 데이터가 없습니다.');
      }
    })
    .catch(error => {
      console.error('전체 캠핑장 데이터를 가져오는 중 오류 발생:', error);
    });

  // 초기화: 페이지 로드 시 추천 캠핑장만 표시
  setTimeout(function() {
    // 드롭다운을 "추천 캠핑장"으로 설정하고 추천 캠핑장만 표시
    var regionSelect = document.getElementById('region-select');
    if (regionSelect) {
      regionSelect.value = 'recommended';
    }
    filterMarkersByRegion('recommended');
  }, 1000); // 1초 후 실행 (데이터 로딩 완료 후)
} 