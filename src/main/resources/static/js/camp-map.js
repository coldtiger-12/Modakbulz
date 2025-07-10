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

  console.log('위도/경도 확인:', { mapY, mapX, address });

  // 인포윈도우 내용 생성 함수 (고캠핑 스타일 유사)
  function getInfoWindowContent() {
    return (
      '<div style="background:#fff; border-radius:12px; box-shadow:0 2px 8px rgba(0,0,0,0.13); padding:20px 22px; min-width:220px; max-width:340px; font-size:15px; font-family: \"Noto Sans KR\", sans-serif; line-height:1.8; border:1.5px solid #e0e0e0;">' +
        '<div style="font-weight:700; font-size:18px; color:#222; margin-bottom:10px; letter-spacing:-1px;">' + (campName || '캠핑장') + '</div>' +
        (address ? '<div style="color:#666; margin-bottom:7px; font-size:14px;"><span style="font-size:16px; color:#ff9800; margin-right:4px;">📍</span>' + address + '</div>' : '') +
        '<div style="color:#ff9800; font-weight:600; font-size:15px;"><span style="font-size:16px; color:#ff9800; margin-right:4px;">☎️</span>' + ((campTel && campTel !== 'null' && campTel.trim() !== '') ? campTel : '없음') + '</div>' +
      '</div>'
    );
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
    };
  }

  function addMarkerWithToggle(map, coords) {
    var marker = new kakao.maps.Marker({
      map: map,
      position: coords
    });
    var infowindow = new kakao.maps.InfoWindow({
      content: getInfoWindowContent()
    });
    var isOpen = false;
    kakao.maps.event.addListener(marker, 'click', function() {
      if (isOpen) {
        infowindow.close();
        isOpen = false;
      } else {
        infowindow.open(map, marker);
        isOpen = true;
      }
    });
    // 3가지 모드 컨트롤 추가
    addRoadviewControl(coords, map);
  }

  // 1. 위도/경도 값이 있으면 바로 지도 표시
  if (typeof mapY !== 'undefined' && typeof mapX !== 'undefined' && mapY && mapX) {
    console.log('위도/경도로 지도 표시:', mapY, mapX);
    var coords = new kakao.maps.LatLng(mapY, mapX);
    var map = new kakao.maps.Map(mapContainer, {
      center: coords,
      level: 3
    });
    addMarkerWithToggle(map, coords);
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
        level: 3
      });
      addMarkerWithToggle(map, coords);
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