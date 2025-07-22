// 에디터 툴바 JavaScript
document.addEventListener('DOMContentLoaded', function() {
    const textarea = document.getElementById('content');
    const toolbarButtons = document.querySelectorAll('.toolbar-btn');
    const linkBtn = document.getElementById('linkBtn');
    const imageUpload = document.getElementById('imageUpload');
    const colorDropdown = document.querySelector('.dropdown-content');

    // 텍스트 영역을 contenteditable div로 변환
    function convertToRichEditor() {
        const content = textarea.value;
        const editorDiv = document.createElement('div');
        editorDiv.contentEditable = true;
        editorDiv.className = 'editor-textarea';
        editorDiv.style.cssText = textarea.style.cssText;
        editorDiv.style.minHeight = '300px';
        editorDiv.style.padding = '12px 15px';
        editorDiv.style.border = '1px solid #ddd';
        editorDiv.style.borderTop = 'none';
        editorDiv.style.borderRadius = '0 0 4px 4px';
        editorDiv.style.fontFamily = textarea.style.fontFamily || '-apple-system, BlinkMacSystemFont, "Malgun Gothic", "맑은 고딕", sans-serif';
        editorDiv.style.fontSize = '14px';
        editorDiv.style.lineHeight = '1.6';
        editorDiv.style.outline = 'none';
        editorDiv.style.resize = 'vertical';
        editorDiv.style.overflowY = 'auto';
        editorDiv.innerHTML = content;
        
        textarea.parentNode.replaceChild(editorDiv, textarea);
        return editorDiv;
    }

    // 폼 제출 시 contenteditable 내용을 textarea로 복사
    function setupFormSubmission() {
        const form = document.querySelector('.edit-form');
        form.addEventListener('submit', function() {
            const editorDiv = document.querySelector('[contenteditable="true"]');
            if (editorDiv) {
                const hiddenTextarea = document.createElement('textarea');
                hiddenTextarea.name = 'content';
                hiddenTextarea.value = editorDiv.innerHTML;
                hiddenTextarea.style.display = 'none';
                form.appendChild(hiddenTextarea);
            }
        });
    }

    // 기본 툴바 버튼 기능
    function setupToolbarButtons() {
        toolbarButtons.forEach(button => {
            button.addEventListener('click', function(e) {
                e.preventDefault();
                const command = this.getAttribute('data-command');
                
                if (command) {
                    document.execCommand(command, false, null);
                    this.classList.toggle('active');
                }
            });
        });
    }

    // 링크 삽입 기능
    function setupLinkButton() {
        linkBtn.addEventListener('click', function(e) {
            e.preventDefault();
            const url = prompt('링크 URL을 입력하세요:');
            if (url) {
                document.execCommand('createLink', false, url);
            }
        });
    }

    // 이미지 업로드 기능
    function setupImageUpload() {
        imageUpload.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function(e) {
                    const img = `<img src="${e.target.result}" style="max-width: 100%; height: auto;" alt="업로드된 이미지">`;
                    document.execCommand('insertHTML', false, img);
                };
                reader.readAsDataURL(file);
            }
        });
    }

    // 색상 선택 기능
    function setupColorPicker() {
        if (colorDropdown) {
            colorDropdown.addEventListener('click', function(e) {
                if (e.target.classList.contains('dropdown-item')) {
                    e.preventDefault();
                    const color = e.target.getAttribute('data-color');
                    document.execCommand('foreColor', false, color);
                }
            });
        }
    }

    // 실행 취소/다시 실행 상태 관리
    function setupUndoRedo() {
        const undoBtn = document.querySelector('[data-command="undo"]');
        const redoBtn = document.querySelector('[data-command="redo"]');

        if (undoBtn && redoBtn) {
            undoBtn.addEventListener('click', function(e) {
                e.preventDefault();
                document.execCommand('undo', false, null);
            });

            redoBtn.addEventListener('click', function(e) {
                e.preventDefault();
                document.execCommand('redo', false, null);
            });
        }
    }

    // 선택된 텍스트에 대한 버튼 활성화/비활성화
    function updateButtonStates() {
        toolbarButtons.forEach(button => {
            const command = button.getAttribute('data-command');
            if (command) {
                if (document.queryCommandState(command)) {
                    button.classList.add('active');
                } else {
                    button.classList.remove('active');
                }
            }
        });
    }

    // 에디터 초기화
    function initEditor() {
        const editorDiv = convertToRichEditor();
        
        // 이벤트 리스너 추가
        editorDiv.addEventListener('input', updateButtonStates);
        editorDiv.addEventListener('keyup', updateButtonStates);
        editorDiv.addEventListener('mouseup', updateButtonStates);
        editorDiv.addEventListener('focus', updateButtonStates);
        
        // 툴바 기능 설정
        setupToolbarButtons();
        setupLinkButton();
        setupImageUpload();
        setupColorPicker();
        setupUndoRedo();
        setupFormSubmission();
    }

    // 에디터 초기화 실행
    if (textarea) {
        initEditor();
    }
}); 