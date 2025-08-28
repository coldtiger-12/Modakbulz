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
      const keywordInput = this.querySelector('input[name="keyword"]');
      const keyword = keywordInput ? keywordInput.value.trim() : '';

      if (keyword) {
        event.preventDefault();
        fetch('/api/search', {
          method: 'POST',
          body: new URLSearchParams({ keyword })
        })
        .catch(err => console.error('검색어 저장 API 호출 실패:', err))
        .finally(() => {
          window.location.href = `/camping/search?keyword=${encodeURIComponent(keyword)}`;
        });
      }
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