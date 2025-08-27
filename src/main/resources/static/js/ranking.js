// ranking.js
document.addEventListener('DOMContentLoaded', () => {
  const popularContainer = document.getElementById('popular-keywords-container');
  const recommendContainer = document.getElementById('recommend-camps-container');
  const searchForm = document.querySelector('form[name="searchForm"]') || document.querySelector('form');

  /**
   * 인기 검색어 로드
   */
  function loadPopularKeywords() {
    fetch('/api/popular-searches') // Spring Boot에서 만든 API
      .then(res => res.json())
      .then(data => {
        popularContainer.innerHTML = '';
        if (data && data.length > 0) {
          data.forEach(item => {
            const tag = document.createElement('a');
            tag.className = 'tag-link';
            tag.href = `/camping/search?keyword=${encodeURIComponent(item.keyword)}`;
            tag.textContent = `#${item.keyword}`;
            popularContainer.appendChild(tag);
          });
        } else {
          popularContainer.innerHTML = '<p>인기 검색어가 없습니다.</p>';
        }
      })
      .catch(err => {
        console.error('인기 검색어 로드 실패', err);
        popularContainer.innerHTML = '<p>인기 검색어 로드 실패</p>';
      });
  }

  /**
   * 추천 캠핑장 로드
   */
  function loadRecommendedCamps() {
    fetch('/camping/recommendations')
      .then(response => response.json())
      .then(camps => {
        recommendContainer.innerHTML = ''; // "불러오는 중" 메시지 제거

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
            recommendContainer.appendChild(campLink);
          });
        } else {
          recommendContainer.innerHTML = '<p>추천 캠핑장 정보가 없습니다.</p>';
        }
      })
      .catch(error => {
        console.error('추천 캠핑장 정보를 불러오는 데 실패했습니다:', error);
        recommendContainer.innerHTML = '<p>정보를 불러오는 데 실패했습니다. 잠시 후 다시 시도해주세요.</p>';
      });
  }

  /**
     * 검색 시 인기 검색어 갱신
     */
    if (searchForm) {
      searchForm.addEventListener('submit', event => {
        // 1. form의 기본 페이지 이동 기능을 막습니다.
        event.preventDefault();

        const keywordInput = searchForm.querySelector('input[name="keyword"]');
        const keyword = keywordInput ? keywordInput.value.trim() : '';

        if (keyword) {
          // FormData를 사용하여 POST 요청 본문을 구성합니다.
          const formData = new URLSearchParams();
          formData.append('keyword', keyword);

          // 2. 올바른 API 주소로 검색어를 저장하는 요청을 보냅니다.
          fetch('/api/search', { // '/api/popular-keywords/increment' -> '/api/search'
            method: 'POST',
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: formData
          })
          .then(res => {
            if (!res.ok) { // 응답이 실패하면 에러를 던집니다.
              throw new Error('Search logging failed');
            }
            // 3. 검색어 저장이 성공하면, 원래 목적지였던 검색 결과 페이지로 이동시킵니다.
            window.location.href = `/camping/search?keyword=${encodeURIComponent(keyword)}`;
          })
          .catch(err => {
            console.error('검색어 저장 실패', err);
            // 저장에 실패하더라도 검색은 되어야 하므로, 페이지를 이동시킵니다.
            window.location.href = `/camping/search?keyword=${encodeURIComponent(keyword)}`;
          });
        } else {
          // 검색어가 없으면 그냥 form의 원래 기능대로 동작하게 합니다.
          searchForm.submit();
        }
      });
    }

  // 초기 로드
  loadPopularKeywords();
  loadRecommendedCamps();
});