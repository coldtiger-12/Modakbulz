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
    `;
    document.head.appendChild(style);
  })();

  // ====== 커스텀 오버레이 HTML 생성 함수 ======
  function getCustomOverlayContent() {
    // campName, address, campImgUrl 변수가 window에 있다고 가정
    var name = window.campName || '캠핑장';
    var addr = window.address || '';
    var img = window.campImgUrl || 'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/place_thumb.png';
    var link = window.campLink || '#';
    // 캠핑장명 a태그에 id 부여
    return (
      '<div class="overlay_info">' +
      '  <a href="#" id="camp-detail-link" target="_blank"><strong>' + name + '</strong></a>' +
      '  <div class="desc">' +
      '    <img src="' + img + '" alt="">' +
      '    <span class="address">' + addr + '</span>' +
      '  </div>' +
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
        // 오버레이가 DOM에 추가된 후 캠핑장명 클릭 이벤트 바인딩
        setTimeout(function() {
          var detailLink = document.getElementById('camp-detail-link');
          if (detailLink) {
            detailLink.onclick = function(e) {
              e.preventDefault();
              var placeName = window.campName || '캠핑장';
              var address = window.address || '';
              var places = new kakao.maps.services.Places();
              // 캠핑장명+주소로 우선 검색, 없으면 캠핑장명만
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
                  // 이름+주소로 안 나오면 이름만 재검색
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

  // 1. 위도/경도 값이 있으면 바로 지도 표시
  if (typeof mapY !== 'undefined' && typeof mapX !== 'undefined' && mapY && mapX) {
    console.log('위도/경도로 지도 표시:', mapY, mapX);
    var coords = new kakao.maps.LatLng(mapY, mapX);
    var map = new kakao.maps.Map(mapContainer, {
      center: coords,
      level: 4 // 기존 3에서 4로 한 단계 뒤로
    });
    addMarkerWithCustomOverlay(map, coords);
    setTimeout(function() {
      map.relayout();
      map.setCenter(coords);
    }, 100);
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
      var coords = new kakao.maps.LatLng(result[0].y, result[0].x);
      var map = new kakao.maps.Map(mapContainer, {
        center: coords,
        level: 4 // 기존 3에서 4로 한 단계 뒤로
      });
      addMarkerWithCustomOverlay(map, coords);
      setTimeout(function() {
        map.relayout();
        map.setCenter(coords);
      }, 100);
    } else {
      console.error('주소 검색 실패:', status);
      mapContainer.innerHTML = '지도를 불러올 수 없습니다. (주소 검색 실패)';
    }
  });
}); 