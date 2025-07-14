// 야놀자 스타일 리뷰 모달 JavaScript

// 모달 열기
function openReviewModal() {
    const modal = document.getElementById('reviewModal');
    const modalTitle = modal.querySelector('.review-modal-header h3');
    const submitBtn = modal.querySelector('.submit-btn');
    
    // 모달 초기화
    resetReviewForm();
    
    // 제목과 버튼 텍스트 설정
    modalTitle.textContent = '리뷰 작성';
    submitBtn.textContent = '등록';
    
    // 모달 표시
    modal.style.display = 'block';
    
    // ESC 키로 모달 닫기
    document.addEventListener('keydown', handleEscKey);
    
    // 모달 외부 클릭으로 닫기
    modal.addEventListener('click', handleModalOutsideClick);
}

// 모달 닫기
function closeReviewModal() {
    const modal = document.getElementById('reviewModal');
    modal.style.display = 'none';
    
    // 이벤트 리스너 제거
    document.removeEventListener('keydown', handleEscKey);
    modal.removeEventListener('click', handleModalOutsideClick);
}

// ESC 키 처리
function handleEscKey(event) {
    if (event.key === 'Escape') {
        closeReviewModal();
    }
}

// 모달 외부 클릭 처리
function handleModalOutsideClick(event) {
    const modal = document.getElementById('reviewModal');
    
    if (event.target === modal) {
        closeReviewModal();
    }
}

// 폼 초기화
function resetReviewForm() {
    const form = document.getElementById('reviewForm');
    const reviewIdInput = document.getElementById('reviewId');
    const contentTextarea = document.getElementById('reviewContent');
    const ratingInputs = document.querySelectorAll('input[name="rating"]');
    const featureInputs = document.querySelectorAll('input[name="features"]');
    
    // 폼 리셋
    form.reset();
    
    // 숨겨진 필드 초기화
    reviewIdInput.value = '';
    
    // 별점 초기화
    ratingInputs.forEach(input => {
        input.checked = false;
    });
    
    // 특징 선택 초기화
    featureInputs.forEach(input => {
        input.checked = false;
    });
    
    // 텍스트 영역 초기화
    contentTextarea.value = '';
    
    // 평점 텍스트 초기화
    const ratingText = document.querySelector('.rating-text');
    if (ratingText) {
        ratingText.textContent = '평점을 선택해주세요';
    }
    
    // 사진 업로드 초기화
    const photoGrid = document.getElementById('photoGrid');
    if (photoGrid) {
        const existingPhotos = photoGrid.querySelectorAll('.photo-item');
        existingPhotos.forEach(photo => photo.remove());
    }
    
    // 글자 수 카운터 초기화
    const charCount = document.getElementById('currentCount');
    if (charCount) {
        charCount.textContent = '0';
    }
}

// 폼 제출 처리
document.addEventListener('DOMContentLoaded', function() {
    const reviewForm = document.getElementById('reviewForm');
    
    if (reviewForm) {
        reviewForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            // 폼 데이터 수집
            const formData = new FormData(reviewForm);
            const campingId = document.getElementById('campingId').value;
            const reviewId = document.getElementById('reviewId').value;
            const rating = formData.get('rating');
            const content = formData.get('content');
            const features = formData.getAll('features');
            
            // 유효성 검사
            if (!rating) {
                showYanoljaMessage('평점을 선택해주세요.', 'warning');
                return;
            }
            
            if (!content || content.trim() === '') {
                showYanoljaMessage('리뷰 내용을 입력해주세요.', 'warning');
                return;
            }
            
            if (content.trim().length < 10) {
                showYanoljaMessage('리뷰 내용은 최소 10자 이상 입력해주세요.', 'warning');
                return;
            }
            
            // 제출 버튼 비활성화
            const submitBtn = reviewForm.querySelector('.submit-btn');
            const originalText = submitBtn.textContent;
            submitBtn.disabled = true;
            submitBtn.textContent = '처리 중...';
            
            // API 호출
            submitYanoljaReview(campingId, reviewId, rating, content, features, submitBtn, originalText);
        });
    }
});

// 야놀자 스타일 리뷰 제출 API 호출
function submitYanoljaReview(campingId, reviewId, rating, content, features, submitBtn, originalText) {
    const url = reviewId ? `/api/reviews/${reviewId}` : '/api/reviews';
    const method = reviewId ? 'PUT' : 'POST';
    
    const requestData = {
        campingId: campingId,
        score: parseInt(rating),
        content: content.trim(),
        features: features
    };
    
    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: JSON.stringify(requestData)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            // 성공 메시지 표시
            showYanoljaMessage('리뷰가 성공적으로 등록되었습니다!', 'success');
            
            // 모달 닫기
            closeReviewModal();
            
            // 페이지 새로고침 (리뷰 목록 업데이트)
            setTimeout(() => {
                location.reload();
            }, 2000);
        } else {
            throw new Error(data.message || '리뷰 등록에 실패했습니다.');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showYanoljaMessage('리뷰 등록에 실패했습니다. 다시 시도해주세요.', 'error');
        
        // 버튼 상태 복원
        submitBtn.disabled = false;
        submitBtn.textContent = originalText;
    });
}

// 야놀자 스타일 메시지 표시 함수
function showYanoljaMessage(message, type) {
    // 기존 메시지 제거
    const existingMessage = document.querySelector('.yanolja-message');
    if (existingMessage) {
        existingMessage.remove();
    }
    
    // 새 메시지 생성
    const messageDiv = document.createElement('div');
    messageDiv.className = `yanolja-message ${type}`;
    
    // 아이콘 설정
    const icons = {
        'success': '✅',
        'error': '❌',
        'warning': '⚠️'
    };
    
    messageDiv.innerHTML = `
        <div class="message-content">
            <span class="message-icon">${icons[type] || 'ℹ️'}</span>
            <span class="message-text">${message}</span>
        </div>
    `;
    
    // 페이지에 추가
    document.body.appendChild(messageDiv);
    
    // 4초 후 자동 제거
    setTimeout(() => {
        messageDiv.classList.add('fade-out');
        setTimeout(() => {
            if (messageDiv.parentNode) {
                messageDiv.remove();
            }
        }, 300);
    }, 4000);
}

// 야놀자 스타일 애니메이션 CSS 추가
const yanoljaStyle = document.createElement('style');
yanoljaStyle.textContent = `
    .yanolja-message {
        position: fixed;
        top: 20px;
        right: 20px;
        background: white;
        border-radius: 8px;
        box-shadow: 0 4px 20px rgba(0,0,0,0.15);
        z-index: 1001;
        animation: slideInRight 0.3s ease-out;
        border-left: 4px solid;
        max-width: 400px;
    }
    
    .yanolja-message.success {
        border-left-color: #28a745;
    }
    
    .yanolja-message.error {
        border-left-color: #dc3545;
    }
    
    .yanolja-message.warning {
        border-left-color: #ffc107;
    }
    
    .message-content {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 16px 20px;
    }
    
    .message-icon {
        font-size: 1.2rem;
    }
    
    .message-text {
        font-size: 0.95rem;
        color: #333;
        font-weight: 500;
    }
    
    .yanolja-message.fade-out {
        animation: slideOutRight 0.3s ease-in;
    }
    
    @keyframes slideInRight {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOutRight {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
`;
document.head.appendChild(yanoljaStyle);

// 리뷰 수정 모달 열기 (기존 리뷰가 있는 경우)
function openEditReviewModal(reviewId, score, content, features = []) {
    const modal = document.getElementById('reviewModal');
    const modalTitle = modal.querySelector('.review-modal-header h3');
    const submitBtn = modal.querySelector('.submit-btn');
    const reviewIdInput = document.getElementById('reviewId');
    const contentTextarea = document.getElementById('reviewContent');
    const ratingInput = document.querySelector(`input[name="rating"][value="${score}"]`);
    
    // 기존 데이터 설정
    reviewIdInput.value = reviewId;
    contentTextarea.value = content;
    if (ratingInput) {
        ratingInput.checked = true;
    }
    
    // 특징 선택 설정
    if (features && features.length > 0) {
        features.forEach(feature => {
            const featureInput = document.querySelector(`input[name="features"][value="${feature}"]`);
            if (featureInput) {
                featureInput.checked = true;
            }
        });
    }
    
    // 제목과 버튼 텍스트 변경
    modalTitle.textContent = '리뷰 수정';
    submitBtn.textContent = '수정';
    
    // 모달 표시
    modal.style.display = 'block';
    
    // 이벤트 리스너 추가
    document.addEventListener('keydown', handleEscKey);
    modal.addEventListener('click', handleModalOutsideClick);
}

// 평점 선택 시 텍스트 변경 (모달용)
function updateRatingText(score) {
    const ratingText = document.querySelector('.rating-text');
    if (ratingText) {
        const texts = {
            '5': '매우 만족',
            '4': '만족',
            '3': '보통',
            '2': '불만족',
            '1': '매우 불만족'
        };
        ratingText.textContent = texts[score] || '평점을 선택해주세요';
    }
}

// 특징 선택 시 카운트 업데이트
function updateFeatureCount(featureValue, isChecked) {
    const featureItem = document.querySelector(`input[name="features"][value="${featureValue}"]`).closest('.feature-item');
    const countElement = featureItem.querySelector('.feature-count');
    const currentCount = parseInt(countElement.textContent);
    
    if (isChecked) {
        countElement.textContent = currentCount + 1;
    } else {
        countElement.textContent = Math.max(0, currentCount - 1);
    }
} 