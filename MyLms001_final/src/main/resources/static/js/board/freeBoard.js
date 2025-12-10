    (function(){
    const PAGE_SIZE = 10;

    const $ = (s, p=document) => p.querySelector(s);
    const $$ = (s, p=document) => Array.from(p.querySelectorAll(s));

    const table = $('#freeBoardTable');
    const tbody = table ? table.tBodies[0] : null;
    const paginationEl = $('#pagination');

    const form = $('#freeBoardSearchForm');
    const input = $('#searchInput');

    if (!tbody) return;

    // 원본 행 스냅샷(서버가 내려준 현재 페이지의 행들 기준)
    const allRows = $$('#freeBoardTable tbody tr')
    .filter(tr => tr.querySelector('td') && !tr.querySelector('.empty-state'));

    // 비어있을 때 표시용
    function ensureEmptyRow(){
    let r = tbody.querySelector('tr.client-empty');
    if (!r) {
    r = document.createElement('tr');
    r.className = 'client-empty';
    r.innerHTML = '<td colspan="5" class="empty-state"><div class="icon">🔎</div>조건에 맞는 게시글이 없습니다.</td>';
    tbody.appendChild(r);
}
    return r;
}

    function parseDate(yyyyMMdd){
    const [y,m,d] = (yyyyMMdd||'').split('-').map(n=>parseInt(n,10));
    if (!y || !m || !d) return new Date(0);
    return new Date(y, m-1, d);
}

    function rowData(tr){
    const titleA   = tr.querySelector('td:nth-child(2) a');
    const title    = titleA ? titleA.textContent.trim() : '';
    const authorTd = tr.querySelector('.col-author') || tr.children[2];
    const author   = authorTd ? authorTd.textContent.trim() : '';
    const dateTd   = tr.querySelector('.col-date') || tr.children[4];
    const dateStr  = dateTd ? dateTd.textContent.trim() : '';
    return { tr, title, author, dateStr, date: parseDate(dateStr) };
}

    const original = allRows.map(rowData);

    function apply(){
    const q = (input?.value || '').trim().toLowerCase();

    // 1) 검색(제목/작성자)
    let list = original.slice();
    if (q) {
    list = list.filter(it =>
    it.title.toLowerCase().includes(q) ||
    it.author.toLowerCase().includes(q)
    );
}

    // 2) 정렬(기본: 최신순)
    list.sort((a,b) => b.date - a.date);

    // 3) 페이지네이션
    const total = list.length;
    const pages = Math.max(1, Math.ceil(total / PAGE_SIZE));

    let current = parseInt(paginationEl?.dataset.page || '1', 10);
    if (current > pages) current = pages;
    if (current < 1) current = 1;

    render(list, current, pages, total);
}

    function render(list, current, pages, total){
    // 모든 행 숨김
    original.forEach(it => { it.tr.style.display = 'none'; });

    // 현재 페이지 범위
    const start = (current - 1) * PAGE_SIZE;
    const end   = start + PAGE_SIZE;

    // 데이터 없는 경우
    if (total === 0) {
    ensureEmptyRow().style.display = '';
} else {
    const empty = tbody.querySelector('tr.client-empty');
    if (empty) empty.style.display = 'none';

    // 해당 페이지 행 보여주고 NO 재계산
    list.slice(start, end).forEach((it, idx) => {
    it.tr.style.display = '';
    const noTd = it.tr.querySelector('.col-no') || it.tr.children[0];
    if (noTd) noTd.textContent = String(start + idx + 1);
});
}

    // 페이지네이션 UI
    buildPagination(current, pages);
}

    function buildPagination(current, pages){
    if (!paginationEl) return;
    paginationEl.dataset.page = String(current);
    paginationEl.innerHTML = '';

    function btn(label, page, disabled=false, active=false){
    const b = document.createElement('button');
    b.type = 'button';
    b.textContent = label;
    if (active) b.classList.add('active');
    if (disabled) b.disabled = true;
    b.addEventListener('click', () => {
    paginationEl.dataset.page = String(page);
    apply();
});
    return b;
}

    // « prev
    paginationEl.appendChild(btn('«', Math.max(1, current-1), current===1));

    // 숫자 버튼(최대 7개)
    const windowSize = 7;
    let start = Math.max(1, current - Math.floor(windowSize/2));
    let end   = Math.min(pages, start + windowSize - 1);
    if (end - start + 1 < windowSize) start = Math.max(1, end - windowSize + 1);

    for (let p=start; p<=end; p++){
    paginationEl.appendChild(btn(String(p), p, false, p===current));
}

    // next »
    paginationEl.appendChild(btn('»', Math.min(pages, current+1), current===pages));
}

    // 이벤트
    form?.addEventListener('submit', e => { e.preventDefault(); paginationEl.dataset.page = '1'; apply(); });
    input?.addEventListener('keyup', e => { if (e.key === 'Enter') { e.preventDefault(); paginationEl.dataset.page = '1'; apply(); } });

    // 초기 렌더
    paginationEl.dataset.page = '1';
    apply();
})();
