// 추천 캠핑장 목록과 지도를 동적으로 업데이트하는 함수
const updateRecommendations = (region = '') => {
  const fetchUrl = region
    ? `/camping/recommendations?region=${encodeURIComponent(region)}`
    : '/camping/recommendations';

  fetch(fetchUrl)
    .then(response => {
      if (!response.ok) throw new Error('Response was not ok');
      return response.json();
    })
    .then(camps => {
      const container = document.getElementById('recommend-camps-container');
      container.innerHTML = ''; // 기존 목록 제거

      if (camps && camps.length > 0) {
        camps.forEach(camp => {
          const campLink = document.createElement('a');
          campLink.className = 'camp';
          campLink.href = `/camping/${camp.contentId}`;

          const defaultImageUrl = '/images/임시사진.png';
          const imageUrl = camp.firstImageUrl ? camp.firstImageUrl : defaultImageUrl;

          campLink.innerHTML = `
            <img src="${imageUrl}" alt="캠핑장 이미지">
            <div class="camp-name">${camp.facltNm}</div>
            <p>${camp.lineIntro || ''}</p>
          `;
          container.appendChild(campLink);
        });
      } else {
        container.innerHTML = '<p>해당 지역의 추천 캠핑장 정보가 없습니다.</p>';
      }

      // ✨ 핵심: 지도 마커를 업데이트하는 함수를 호출합니다.
      if (window.updateMapMarkers) {
        window.updateMapMarkers(camps);
      }
    })
    .catch(error => {
      console.error('추천 캠핑장 정보를 불러오는 데 실패했습니다:', error);
      const container = document.getElementById('recommend-camps-container');
      container.innerHTML = '<p>정보를 불러오는 데 실패했습니다. 잠시 후 다시 시도해주세요.</p>';
    });
};

// 페이지 로딩이 완료되면 아래 코드가 실행됩니다.
document.addEventListener('DOMContentLoaded', () => {
  // 1. 페이지가 처음 열릴 때 기본 추천 캠핑장 목록과 지도를 불러옵니다.
  updateRecommendations();

  const regionInput = document.getElementById('region-input');

  // 2. '지역' 입력 필드에 'input' 이벤트 리스너를 추가합니다.
  if (regionInput) {
    let debounceTimer;

    regionInput.addEventListener('input', () => {
      const region = regionInput.value;

      // 이전 타이머가 있으면 취소
      if (debounceTimer) {
        clearTimeout(debounceTimer);
      }

      // 300ms(0.3초) 후에 검색 실행 (디바운싱)
      debounceTimer = setTimeout(() => {
        updateRecommendations(region);
      }, 300);
    });
  }
});