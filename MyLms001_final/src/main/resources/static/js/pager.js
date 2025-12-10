<!-- 모든 페이지에서 재사용할 공통 페이징 유틸 -->
    (function () {
    if (window.Pager) return; // 중복 로드 방지
    function _render({ totalItems, perPage, current, totalPages, container, onChange }) {
    if (!container) return;

    // totalPages 직접 주거나, totalItems+perPage로 계산
    const tp = totalPages || Math.max(1, Math.ceil(Number(totalItems || 0) / Number(perPage || 10)));
    const cur = Math.min(Math.max(1, Number(current || 1)), tp);

    container.innerHTML = '';
    if (tp <= 1) return;

    function btn(label, page, disabled, active) {
    const b = document.createElement('button');
    b.type = 'button';
    b.textContent = label;
    b.className = active ? 'active' : '';
    if (disabled) b.disabled = true;
    b.addEventListener('click', () => onChange(page));
    container.appendChild(b);
}
    function dots() {
    const s = document.createElement('span');
    s.textContent = '…';
    s.style.padding = '0 4px';
    container.appendChild(s);
}

    btn('«', 1, cur === 1, false);
    btn('‹', Math.max(1, cur - 1), cur === 1, false);

    const win = 2;
    const pages = [];
    for (let p = 1; p <= tp; p++) {
    if (p === 1 || p === tp || (p >= cur - win && p <= cur + win)) pages.push(p);
}
    let prev = 0;
    pages.forEach(p => {
    if (prev && p - prev > 1) dots();
    btn(String(p), p, false, p === cur);
    prev = p;
});

    btn('›', Math.min(tp, cur + 1), cur === tp, false);
    btn('»', tp, cur === tp, false);
}

    window.Pager = {
    render: _render
};
})();
