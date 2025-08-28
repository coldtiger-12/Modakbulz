// ranking.js (최종 수정본)
document.addEventListener('DOMContentLoaded', () => {
  const popularContainer = document.getElementById('popular-keywords-container');
  const recommendContainer = document.getElementById('recommend-camps-container');
  const searchForm = document.querySelector('.search-form');

  /**
   * 검색 폼 제출 시 검색어 저장
   */
  if (searchForm) {
      searchForm.addEventListener('submit', function(event) {
        // 각 input 요소에서 값을 가져옵니다.
        const keywordInput = this.querySelector('input[name="keyword"]');
        const regionInput = this.querySelector('input[name="region"]');
        const facltNmInput = this.querySelector('input[name="facltNm"]');

        const keyword = keywordInput ? keywordInput.value.trim() : '';
        const region = regionInput ? regionInput.value.trim() : '';
        const facltNm = facltNmInput ? facltNmInput.value.trim() : '';

        // 키워드가 있을 때만 인기 검색어 저장을 시도합니다.
        if (keyword) {
          event.preventDefault(); // 기본 폼 전송을 막습니다.

          const formData = new URLSearchParams();
          formData.append('keyword', keyword);

          fetch('/api/search', {
            method: 'POST',
            body: formData
          })
          .catch(err => console.error('검색어 저장 API 호출 실패:', err))
          .finally(() => {
            // 모든 검색 파라미터를 포함하는 URL을 만듭니다.
            const searchParams = new URLSearchParams({
              keyword: keyword,
              region: region,
              facltNm: facltNm
            });
            // 완성된 URL로 페이지를 이동시킵니다.
            window.location.href = `/camping/search?${searchParams.toString()}`;
          });
        }
        // 키워드가 없으면, 자바스크립트는 아무것도 하지 않고
        // form의 기본 동작(action="/camping/search" method="get")에 따라 모든 파라미터가 전송됩니다.
      });
    }

  /**
   * 인기 검색어 태그 클릭 시 검색어 저장
   */
  if (popularContainer) {
    popularContainer.addEventListener('click', event => {
      if (event.target.classList.contains('tag-link')) {
        event.preventDefault();
        const clickedTag = event.target;
        const href = clickedTag.href;
        const url = new URL(href);
        const keyword = url.searchParams.get('keyword');

        if (keyword) {
          fetch('/api/search', {
            method: 'POST',
            body: new URLSearchParams({ keyword })
          })
          .catch(err => console.error('태그 검색어 저장 API 호출 실패:', err))
          .finally(() => {
            window.location.href = href;
          });
        }
      }
    });
  }

  /**
   * 인기 검색어 로드
   */
  function loadPopularKeywords() {
    // ★★★ 수정된 부분 ★★★: popularContainer가 없으면 함수를 즉시 종료
    if (!popularContainer) {
      return;
    }
    fetch('/api/popular-searches')
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
        popularContainer.innerHTML = '<p>인기 검색어 로드에 실패했습니다.</p>';
      });
  }

  /**
   * 추천 캠핑장 로드
   */
  function loadRecommendedCamps() {
    // ★★★ 수정된 부분 ★★★: recommendContainer가 없으면 함수를 즉시 종료
    if (!recommendContainer) {
      return;
    }
    fetch('/camping/recommendations')
      .then(response => response.json())
      .then(camps => {
        recommendContainer.innerHTML = '';
        if (camps && camps.length > 0) {
          camps.forEach(camp => {
            const campLink = document.createElement('a');
            campLink.className = 'camp';
            campLink.href = `/camping/${camp.contentId}`;
            const defaultImageUrl = '/images/임시사진.png';
            const imageUrl = camp.firstImageUrl || defaultImageUrl;
            campLink.innerHTML = `
              <img src="${imageUrl}" alt="${camp.facltNm}">
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
        recommendContainer.innerHTML = '<p>정보를 불러오는 데 실패했습니다.</p>';
      });
  }

  // 초기 로드: 이제 에러 없이 필요한 페이지에서만 기능이 동작합니다.
  loadPopularKeywords();
  loadRecommendedCamps();
});