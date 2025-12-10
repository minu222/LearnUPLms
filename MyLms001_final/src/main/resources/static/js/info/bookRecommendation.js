// 도서추천: 더미 9개 + 이미지 폴백 체인 + 강사 CRUD(임시 표시)
(function () {
    'use strict';
    document.addEventListener('DOMContentLoaded', init);

    function init() {
        /* ========== 로그인/권한 (개발용 스위치) ========== */
        const DEV_FORCE_ROLE = 'STUDENT';  // 'INSTRUCTOR' | 'STUDENT' | null
        const DEV_FORCE_USER = 'teacher1'; // 필요시 변경
        const roleFromQuery  = new URLSearchParams(location.search).get('role');
        const resolvedRole   = (DEV_FORCE_ROLE ?? roleFromQuery ?? window.LOGIN_ROLE ?? 'STUDENT');
        const currentUserId  = (DEV_FORCE_USER ?? window.LOGIN_USER_ID ?? 'guest');
        const isInstructor   = (resolvedRole === 'INSTRUCTOR');

        /* ========== 이미지 폴백 유틸 ========== */
        const IMG_BASES   = ['/images/CL', '/images/CA', '/images']; // 가능한 폴더
        const PREFIXES    = ['CL', 'CA'];                             // 가능한 접두사
        const EXTENSIONS  = ['.png', '.jpg', '.jpeg', '.webp'];      // 가능한 확장자
        const DEFAULT_FALLBACK = ['/images/dw.png'];                  // 최종 폴백

        const pad2 = n => String(n).padStart(2, '0');

        // n(1~9) → 후보 경로들 생성
        function coverCandidatesByIndex(n) {
            const id = pad2(n);
            const combos = [];
            for (const base of IMG_BASES) {
                for (const pre of PREFIXES) {
                    for (const ext of EXTENSIONS) {
                        combos.push(`${base}/${pre}${id}${ext}`);
                    }
                }
            }
            return combos;
        }

        // 주어진 단일 경로(또는 dataURL) → 후보 경로들로 확장
        function candidatesFromAny(srcMaybe) {
            // dataURL이면 그대로 사용
            if (typeof srcMaybe === 'string' && srcMaybe.startsWith('data:')) {
                return [srcMaybe];
            }
            // 일반 경로면 그 경로부터 시도 → 확장자/폴더 변형 → 최종 폴백
            const list = [];
            if (srcMaybe) {
                list.push(srcMaybe);
                // 같은 파일명으로 다른 확장자/경로도 시도
                try {
                    const m = srcMaybe.match(/(.*\/)?([A-Za-z]+)(\d{2})(\.[a-zA-Z0-9]+)?$/);
                    if (m) {
                        const num = m[3];
                        coverCandidatesByIndex(Number(num)).forEach(p => {
                            if (!list.includes(p)) list.push(p);
                        });
                    }
                } catch {}
            }
            DEFAULT_FALLBACK.forEach(p => list.push(p));
            return list;
        }

        // <img>에 폴백 체인 적용
        function applyImageWithFallback(imgEl, candidates) {
            let i = 0;
            const tryNext = () => {
                if (i >= candidates.length) {
                    imgEl.onerror = null;
                    return;
                }
                imgEl.src = candidates[i++];
            };
            imgEl.onerror = tryNext;
            tryNext();
        }

        /* ========== DOM ========== */
        const cardGrid       = document.getElementById('cardGrid');
        const paginationEl   = document.getElementById('pagination');
        const searchBtn      = document.getElementById('searchBtn');
        const searchInput    = document.getElementById('searchInput');
        const filterSelect   = document.getElementById('filter');
        const teacherActions = document.getElementById('teacherActions');

        const modalOverlay   = document.getElementById('modalOverlay');
        const modalClose     = document.getElementById('modalClose');
        const myEditOverlay  = document.getElementById('myEditOverlay');
        const myEditClose    = document.getElementById('myEditClose');
        const deleteOverlay  = document.getElementById('deleteOverlay');
        const deleteClose    = document.getElementById('deleteClose');

        const registerBtn    = document.getElementById('registerBtn');
        const editBtn        = document.getElementById('editBtn');
        const deleteBtn      = document.getElementById('deleteBtn');
        const saveBtn        = document.getElementById('saveBtn');
        const deleteConfirmBtn = document.getElementById('deleteConfirmBtn');

        const instructorInput = document.getElementById('instructorInput');
        const titleInput      = document.getElementById('titleInput');
        const descInput       = document.getElementById('descInput');
        const imgInput        = document.getElementById('imgInput');

        if (!cardGrid || !paginationEl) return;

        /* ========== 데이터: 더미 9개 + 임시 등록 배열(메모리만) ========== */
        // 더미 9개 (항상 보임, 삭제/수정 대상 아님) + coverIdx로 표지 인덱스 명시
        const DUMMIES = [
            { title:'HTML + CSS + 자바스크립트 (입문서)',              instructor:'강사A', desc:'기초부터 반응형 웹까지 입문자용.', ownerId:'system', coverIdx:1 },
            { title:'제로초의 자바스크립트 입문',                      instructor:'강사B', desc:'만들면서 배우는 JS 입문.',         ownerId:'system', coverIdx:2 },
            { title:'Vue.js 프론트엔드 개발 입문',                     instructor:'강사C', desc:'기초부터 실무 프로젝트까지.',      ownerId:'system', coverIdx:3 },
            { title:'자바 입문',                                       instructor:'강사A', desc:'문법과 개념을 쉽게 설명.',          ownerId:'system', coverIdx:4 },
            { title:'컴퓨터 구조와 운영체제',                          instructor:'강사D', desc:'체계적으로 정리한 CS 자습서.',      ownerId:'system', coverIdx:5 },
            { title:'혼자 공부하는 첫 프로그래밍 with 파이썬',         instructor:'강사E', desc:'비전공자용 파이썬 첫걸음.',        ownerId:'system', coverIdx:6 },
            { title:'자료구조와 알고리즘 입문',                         instructor:'강사F', desc:'핵심 개념을 그림과 예제로.',       ownerId:'system', coverIdx:7 },
            { title:'혼공 컴퓨터구조 + 운영체제',                       instructor:'강사F', desc:'전공자를 위한 필수 CS 요약.',      ownerId:'system', coverIdx:8 },
            { title:'면접을 위한 CS 전공지식 노트',                     instructor:'강사G', desc:'OS/네트워크/자료구조 총정리.',     ownerId:'system', coverIdx:9 }
        ];

        // 강사가 등록한 임시 도서 (페이지 벗어나면 소멸)
        let tempBooks = [];
        let nextCoverIdx = 0; // 업로드 없을 때 자동 커버 순환(1~9)

        /* ========== 렌더링 ========== */
        const PER_PAGE = 9;
        let currentPage = 1;
        let viewBooks = [];

        function baseItems() {
            return isInstructor ? [...tempBooks, ...DUMMIES] : DUMMIES.slice();
        }

        function applySearchFilter() {
            const src = baseItems();
            const kw = (searchInput?.value || '').trim().toLowerCase();
            const ft = filterSelect?.value || '';
            viewBooks = src.filter(b => {
                const t = (b.title || '').toLowerCase();
                const i = (b.instructor || '').toLowerCase();
                if (!kw) return true;
                if (ft === 'title') return t.includes(kw);
                if (ft === 'instructor') return i.includes(kw);
                return t.includes(kw) || i.includes(kw);
            });
            const maxPage = Math.max(1, Math.ceil(viewBooks.length / PER_PAGE));
            if (currentPage > maxPage) currentPage = maxPage;
        }

        function renderCards() {
            cardGrid.innerHTML = '';
            const start = (currentPage - 1) * PER_PAGE;
            const end   = start + PER_PAGE;
            const pageItems = viewBooks.slice(start, end);

            pageItems.forEach((b) => {
                const card = document.createElement('div');
                card.className = 'card bookcard-row';

                // 왼쪽 이미지(고정 박스 + cover)
                const left = document.createElement('div');
                left.className = 'bookcard-left';

                const im = document.createElement('img');
                im.alt = b.title || 'book';
                im.className = 'bookcard-thumb';

                // 후보 경로 구성
                const candidates = (b.img)
                    ? candidatesFromAny(b.img)
                    : (typeof b.coverIdx === 'number'
                        ? [...coverCandidatesByIndex(b.coverIdx), ...DEFAULT_FALLBACK]
                        : DEFAULT_FALLBACK.slice());

                applyImageWithFallback(im, candidates);
                left.appendChild(im);
                card.appendChild(left);

                // 오른쪽 텍스트
                const right = document.createElement('div');
                right.className = 'bookcard-right';

                const h4 = document.createElement('h4');
                h4.className = 'bookcard-title';
                h4.textContent = b.title || '(제목 없음)';
                right.appendChild(h4);

                const meta = document.createElement('div');
                meta.className = 'meta';
                const own = b.ownerId === currentUserId ? '내 소유(임시)' : '추천';
                meta.innerHTML = `강사: ${b.instructor || '-'} <span class="tag">${own}</span>`;
                right.appendChild(meta);

                const p = document.createElement('div');
                p.className = 'desc';
                p.textContent = b.desc || '';
                right.appendChild(p);

                card.appendChild(right);
                cardGrid.appendChild(card);
            });
        }

        function renderPagination() {
            paginationEl.innerHTML = '';
            const total = viewBooks.length;
            const pages = Math.max(1, Math.ceil(total / PER_PAGE));
            for (let p = 1; p <= pages; p++) {
                const btn = document.createElement('button');
                btn.textContent = p;
                if (p === currentPage) btn.classList.add('active');
                btn.addEventListener('click', () => {
                    currentPage = p;
                    renderCards();
                    renderPagination();
                });
                paginationEl.appendChild(btn);
            }
        }

        function render() {
            applySearchFilter();
            renderCards();
            renderPagination();
        }

        /* ========== 모달/CRUD(임시만) ========== */
        function openCreate() {
            if (!isInstructor) return;
            instructorInput && (instructorInput.value = '');
            titleInput && (titleInput.value = '');
            descInput && (descInput.value = '');
            imgInput && (imgInput.value = '');
            modalOverlay && (modalOverlay.style.display = 'flex');
        }

        function closeAllModals() {
            modalOverlay  && (modalOverlay.style.display  = 'none');
            myEditOverlay && (myEditOverlay.style.display = 'none');
            deleteOverlay && (deleteOverlay.style.display = 'none');
        }

        function openMyListForEdit() {
            if (!isInstructor) return;
            const list = document.getElementById('myEditList');
            if (!list) return;
            list.innerHTML = '';
            if (tempBooks.length === 0) { alert('현재 페이지에서 등록한 임시 도서가 없습니다.'); return; }
            tempBooks.forEach((b, i) => {
                const btn = document.createElement('button');
                btn.className = 'my-edit-item';
                btn.textContent = `${b.title} (${b.instructor})`;
                btn.addEventListener('click', () => {
                    instructorInput && (instructorInput.value = b.instructor || '');
                    titleInput && (titleInput.value = b.title || '');
                    descInput && (descInput.value = b.desc || '');
                    imgInput && (imgInput.value = '');
                    closeAllModals();
                    saveBtn.dataset.editIndex = String(i);
                    modalOverlay && (modalOverlay.style.display = 'flex');
                });
                list.appendChild(btn);
            });
            myEditOverlay && (myEditOverlay.style.display = 'flex');
        }

        function openDeleteModal() {
            if (!isInstructor) return;
            const listDiv = document.getElementById('deleteList');
            if (!listDiv) return;
            listDiv.innerHTML = '';
            if (tempBooks.length === 0) { alert('삭제할 수 있는 임시 도서가 없습니다.'); return; }
            tempBooks.forEach((b, i) => {
                const lab = document.createElement('label');
                lab.style.display='flex';
                lab.style.alignItems='center';
                lab.style.gap='8px';
                lab.innerHTML = `<input type="checkbox" value="${i}"> ${b.title} (${b.instructor})`;
                listDiv.appendChild(lab);
            });
            deleteOverlay && (deleteOverlay.style.display = 'flex');
        }

        // 저장(등록/수정) — 모두 "임시(tempBooks)"에만 반영
        saveBtn?.addEventListener('click', () => {
            if (!isInstructor) return;

            const data = {
                instructor: (instructorInput?.value || '').trim(),
                title:      (titleInput?.value || '').trim(),
                desc:       (descInput?.value || '').trim(),
            };

            const file = imgInput?.files && imgInput.files[0];
            const finish = (imgData) => {
                const editIndexStr = saveBtn.dataset.editIndex;
                if (editIndexStr != null) {
                    const idx = parseInt(editIndexStr, 10);
                    const prev = tempBooks[idx];
                    if (prev) {
                        tempBooks[idx] = { ...prev, ...data, img: imgData ?? prev.img };
                    }
                    delete saveBtn.dataset.editIndex;
                } else {
                    // 업로드 없으면 표지 자동 할당(1~9 순환)
                    const autoIdx = ((nextCoverIdx % 9) + 1);
                    nextCoverIdx++;
                    const finalImg = (imgData && imgData.length) ? imgData : null;
                    tempBooks.unshift({
                        ...data,
                        img: finalImg,          // 있으면 dataURL, 없으면 렌더링 때 자동 후보로
                        coverIdx: autoIdx,
                        ownerId: currentUserId
                    });
                    currentPage = 1;
                }

                closeAllModals();
                render();
            };

            if (file) {
                const reader = new FileReader();
                reader.onload = e => finish(e.target.result);
                reader.readAsDataURL(file);
            } else {
                finish(undefined);
            }
        });

        deleteConfirmBtn?.addEventListener('click', () => {
            if (!isInstructor) return;
            const listDiv = document.getElementById('deleteList');
            if (!listDiv) return;
            const checked = Array.from(listDiv.querySelectorAll('input[type="checkbox"]:checked'));
            if (checked.length === 0) { alert('삭제할 도서를 선택하세요.'); return; }
            const idxs = checked.map(c => parseInt(c.value, 10)).sort((a,b)=>b-a);
            idxs.forEach(i => { if (tempBooks[i]) tempBooks.splice(i,1); });
            closeAllModals();
            render();
        });

        /* ========== 일반 이벤트/초기화 ========== */
        searchBtn?.addEventListener('click', () => { currentPage = 1; render(); });
        searchInput?.addEventListener('keydown', (e) => { if (e.key === 'Enter') { currentPage = 1; render(); } });
        filterSelect?.addEventListener('change', () => { currentPage = 1; render(); });

        if (isInstructor) {
            teacherActions && (teacherActions.style.display = 'flex');
            registerBtn?.addEventListener('click', openCreate);
            editBtn?.addEventListener('click', openMyListForEdit);
            deleteBtn?.addEventListener('click', openDeleteModal);
        } else {
            teacherActions && (teacherActions.style.display = 'none');
        }

        modalClose?.addEventListener('click', closeAllModals);
        myEditClose?.addEventListener('click', closeAllModals);
        deleteClose?.addEventListener('click', closeAllModals);
        window.addEventListener('click', (e) => {
            if (e.target === modalOverlay || e.target === myEditOverlay || e.target === deleteOverlay) closeAllModals();
        });

        // 최초 렌더
        render();
    }
})();
