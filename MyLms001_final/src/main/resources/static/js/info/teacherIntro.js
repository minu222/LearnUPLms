/* =========================================================
   강사 소개 모듈 (스코프 고립 + 모달 유연 탐색 버전)
   - 카드/검색은 #instructor-root 내부만 사용
   - 모달은 (1) 스코프 내부 → (2) 전역 문서에서 순차 탐색
========================================================= */
(() => {
    'use strict';

    /* [필수] 강사 소개 섹션 루트 */
    const SCOPE = document.getElementById('instructor-root');
    if (!SCOPE) return;

    /* ===== 폴백 이미지 ===== */
    const DEFAULT_PHOTO  = '/images/dw.png';
    const DEFAULT_COURSE = '/images/course_placeholder.jpg';

    /* ===== 강의 카탈로그 ===== */
    const COURSE_CATALOG = [
        { img: '/images/CA/CA01.png', title: '산업안전관리 공통직무 (AI모델-영어)' },
        { img: '/images/CA/CA02.png', title: '화공안전관리 (AI모델-네팔어)' },
        { img: '/images/CA/CA03.png', title: '화공안전관리 (AI모델-베트남어)' },
        { img: '/images/CA/CA04.png', title: '산업안전관리 공통직무 (AI모델-한국어)' },
        { img: '/images/CA/CA05.png', title: '화공안전관리 (AI모델-영어)' },
        { img: '/images/CA/CA06.png', title: '산업안전관리 공통직무 (AI모델-베트남어)' },
        { img: '/images/CA/CA07.png', title: '화공안전관리 (AI모델-한국어)' },
        { img: '/images/CA/CA08.png', title: '건설안전관리 (AI모델-네팔어)' },
        { img: '/images/CA/CA09.png', title: '반도체 패키지 공정 및 제조 기술' },
        { img: '/images/CA/CA10.png', title: '반도체 차세대 패키지' },
        { img: '/images/CA/CA11.png', title: '기계안전관리 (AI모델-한국어)' },
        { img: '/images/CA/CA12.png', title: '화공안전관리 (AI모델-네팔어)' },
        { img: '/images/CA/CA13.png', title: '건설안전관리 (AI모델-한국어)' },
        { img: '/images/CA/CA14.png', title: '전기안전관리 (AI모델-베트남어)' },
        { img: '/images/CA/CA15.png', title: '전기안전관리 (AI모델-네팔어)' },
    ];

    /* ===== 유틸 ===== */
    function shuffle(array) {
        const a = array.slice();
        for (let i = a.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [a[i], a[j]] = [a[j], a[i]];
        }
        return a;
    }
    function pickFourUniqueCourses() {
        return shuffle(COURSE_CATALOG).slice(0, 4);
    }

    /* ===== 강사 데이터 ===== */
    const INSTRUCTORS = [
        { id:'instructor01', name:'김지훈', affiliation:'DW 아카데미', gender:'남', email:'kimjh@dwacademy.co.kr', age:36, motto:'실무로 연결되는 강의만 합니다.', photo:'/images/TA/TA01.png', courses:[] },
        { id:'instructor02', name:'이서연', affiliation:'프리랜서', gender:'여', email:'seoyeon.dev@example.com', age:32, motto:'사용자 경험이 우선입니다.', photo:'/images/TA/TA03.png', courses:[] },
        { id:'instructor03', name:'박민수', affiliation:'데이터랩', gender:'남', email:'minsu.data@example.com', age:38, motto:'데이터는 이야기입니다.', photo:'/images/TA/TA02.png', courses:[] },
        { id:'instructor04', name:'정하늘', affiliation:'클라우드 스쿨', gender:'여', email:'haneul.cloud@example.com', age:34, motto:'자동화로 반복을 없애자.', photo:'/images/TA/TA04.png', courses:[] },
        { id:'instructor05', name:'오준석', affiliation:'모바일랩', gender:'남', email:'junseok.mobile@example.com', age:33, motto:'모바일의 본질은 반응성.', photo:'/images/TA/TA05.png', courses:[] },
        { id:'instructor06', name:'최유리', affiliation:'보안연구소', gender:'여', email:'yuri.sec@example.com', age:37, motto:'보안은 선택이 아니라 기본.', photo:'/images/TA/TA06.png', courses:[] },
        { id:'instructor07', name:'한도윤', affiliation:'게임스튜디오', gender:'남', email:'doyun.game@example.com', age:35, motto:'재미있어야 오래 간다.', photo:'/images/TA/TA07.png', courses:[] },
        { id:'instructor08', name:'백수연', affiliation:'에듀테크', gender:'여', email:'suyeon.edtech@example.com', age:31, motto:'러닝 데이터가 답을 준다.', photo:'/images/TA/TA08.png', courses:[] },
        { id:'instructor09', name:'장현우', affiliation:'스타트업', gender:'남', email:'hyunwoo.start@example.com', age:29, motto:'작게 만들고 빨리 검증.', photo:'/images/TA/TA09.png', courses:[] },
    ];

    /* ===== DOM (카드/검색은 스코프 내부 전용) ===== */
    const grid        = SCOPE.querySelector('#cardGrid');
    const searchBtn   = SCOPE.querySelector('#searchBtn');
    const searchInput = SCOPE.querySelector('#searchInput');
    const filterSel   = SCOPE.querySelector('#filter');

    /* ================================================
       모달은 ‘스코프 내부 → 전역 문서’ 순으로 유연 탐색
       - 일부 프로젝트에서 모달을 레이아웃 최하단 글로벌로 둠
       - 그래서 스코프 내에 없으면 document에서 찾도록 수정
       ------------------------------------------------
       [우선순위]
       1) data-modal 셀렉터 (최우선, 커스텀 훅)
       2) #instructor-modal (권장 고유 ID)
       3) #modal (기존 공통 ID)
       ================================================ */
    const modalSelectorFromAttr = SCOPE.getAttribute('data-modal'); // 예: data-modal="#instructorModal"
    const modalEl  =
        (modalSelectorFromAttr && document.querySelector(modalSelectorFromAttr)) ||
        SCOPE.querySelector('#instructor-modal') ||
        document.getElementById('instructor-modal') ||
        SCOPE.querySelector('#modal') ||
        document.getElementById('modal');

    const modalClose =
        (modalEl && (modalEl.querySelector('#modalClose') || modalEl.querySelector('.modal-close'))) || null;

    // 모달 내부 요소(스코프 내 우선 → 전역 모달 내부 대체)
    const mName   = (SCOPE.querySelector('#mName'))    || (modalEl && modalEl.querySelector('#mName'));
    const mPhoto  = (SCOPE.querySelector('#mPhoto'))   || (modalEl && modalEl.querySelector('#mPhoto'));
    const mIntro  = (SCOPE.querySelector('#mIntro'))   || (modalEl && modalEl.querySelector('#mIntro'));
    const mInfo   = (SCOPE.querySelector('#mInfo'))    || (modalEl && modalEl.querySelector('#mInfo'));
    const mCourses= (SCOPE.querySelector('#mCourses')) || (modalEl && modalEl.querySelector('#mCourses'));

    /* ===== 카드 렌더링 ===== */
    function renderCards(list) {
        if (!grid) return;
        grid.innerHTML = list.map(ins => {
            const photo = ins.photo || DEFAULT_PHOTO;
            return `
        <div class="card">
          <div class="card-info">
            <img src="${photo}" alt="강사 사진" class="photo" onerror="this.src='${DEFAULT_PHOTO}'">
            <div class="info">
              <div class="text">
                <p><strong>이름:</strong> ${ins.name}</p>
                <p><strong>소속:</strong> ${ins.affiliation}</p>
                <p><strong>한마디:</strong> ${ins.motto}</p>
              </div>
              <button class="detailBtn" data-id="${ins.id}">자세히보기</button>
            </div>
          </div>
        </div>`;
        }).join('');

        grid.querySelectorAll('.detailBtn').forEach(btn => {
            btn.addEventListener('click', () => openModal(btn.dataset.id));
        });
    }

    /* ===== 모달 열기/닫기 ===== */
    function openModal(instructorId) {
        const ins = INSTRUCTORS.find(v => v.id === instructorId);
        if (!ins || !modalEl) return; // [FIX] 모달이 전역에만 있어도 안전

        if (mName)  mName.textContent = `${ins.name} 강사`;
        if (mPhoto) {
            mPhoto.src = ins.photo || DEFAULT_PHOTO;
            mPhoto.onerror = function(){ this.src = DEFAULT_PHOTO; };
        }
        if (mIntro) mIntro.textContent = `${ins.name} 강사는 ${ins.affiliation}에서 활동하며, 실제 프로젝트 기반의 커리큘럼을 제공합니다.`;

        if (mInfo) {
            mInfo.innerHTML = `
        <p><strong>이름:</strong> ${ins.name}</p>
        <p><strong>아이디:</strong> ${ins.id}</p>
        <p><strong>소속:</strong> ${ins.affiliation}</p>
        <p><strong>성별:</strong> ${ins.gender}</p>
        <p><strong>이메일:</strong> ${ins.email}</p>
        <p><strong>나이:</strong> ${ins.age}</p>`;
        }

        if (!ins.courses || ins.courses.length === 0) {
            ins.courses = pickFourUniqueCourses();
        } else if (ins.courses.length > 4) {
            ins.courses = ins.courses.slice(0, 4);
        }
        if (mCourses) {
            mCourses.innerHTML = (ins.courses || []).map(c => `
        <div class="course-card">
          <img src="${c.img || DEFAULT_COURSE}" alt="${c.title}" onerror="this.src='${DEFAULT_COURSE}'">
          <div class="title">${c.title}</div>
        </div>
      `).join('');
        }

        // [FIX] 어떤 CSS든 보이도록 둘 다 처리
        modalEl.classList.add('open');
        modalEl.style.display = 'flex';

        if (modalClose && !modalClose._bound) {
            modalClose.addEventListener('click', closeModal);
            modalClose._bound = true;
        }

        // [FIX] 오버레이 클릭 닫기: 자신에게만 반응
        if (!modalEl._overlayBound) {
            modalEl.addEventListener('click', (e) => { if (e.target === modalEl) closeModal(); });
            modalEl._overlayBound = true;
        }
    }

    function closeModal() {
        if (!modalEl) return;
        modalEl.classList.remove('open');
        modalEl.style.display = 'none';
    }

    /* ===== 검색 ===== */
    function doSearch() {
        const keyword = (searchInput?.value || '').trim().toLowerCase();
        const filter  = filterSel?.value || '';
        let list = INSTRUCTORS.slice();

        if (keyword) {
            list = list.filter(v => {
                if (filter === '1') return (v.name || '').toLowerCase().includes(keyword);
                if (filter === '2') return (v.affiliation || '').toLowerCase().includes(keyword);
                return (
                    (v.name || '').toLowerCase().includes(keyword) ||
                    (v.affiliation || '').toLowerCase().includes(keyword) ||
                    (v.motto || '').toLowerCase().includes(keyword)
                );
            });
        }
        renderCards(list);
    }

    /* ===== 초기화 ===== */
    (function init() {
        INSTRUCTORS.forEach(ins => { ins.courses = pickFourUniqueCourses(); });
        renderCards(INSTRUCTORS);

        searchBtn?.addEventListener('click', doSearch);
        searchInput?.addEventListener('keydown', e => { if (e.key === 'Enter') doSearch(); });
    })();
})();
