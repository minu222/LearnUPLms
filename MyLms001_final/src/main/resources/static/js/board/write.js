// 간단한 RTE 명령
function execCmd(cmd, val = null) {
    document.execCommand(cmd, false, val);
}

// 폰트/사이즈 적용
document.getElementById('fontName')?.addEventListener('change', e => {
    execCmd('fontName', e.target.value);
});
document.getElementById('fontSize')?.addEventListener('change', e => {
    // execCommand의 fontSize는 1~7 단계라서 인라인 스타일로 대체
    const size = e.target.value;
    const span = document.createElement('span');
    span.style.fontSize = size;
    wrapSelectionWith(span);
});

// 선택 영역을 특정 요소로 래핑
function wrapSelectionWith(node) {
    const sel = window.getSelection();
    if (!sel || sel.rangeCount === 0) return;
    const range = sel.getRangeAt(0);
    range.surroundContents(node);
}

// 제출 전 유효성/동기화
document.getElementById('writeForm')?.addEventListener('submit', (e) => {
    const title = document.getElementById('title').value.trim();
    const html = document.getElementById('editor').innerHTML.trim();

    if (!title) {
        e.preventDefault();
        alert('제목을 입력하세요.');
        return;
    }
    if (!html || html === '<br>') {
        e.preventDefault();
        alert('내용을 입력하세요.');
        return;
    }

    // 서버로 보낼 내용 복사 (HTML 허용)
    document.getElementById('contentField').value = html;
});