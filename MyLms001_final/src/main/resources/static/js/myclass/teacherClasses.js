/* ===============================
 *  Teacher - My Classes (List + Edit)
 *  REST API:
 *    GET   /api/teacher/courses?status=published|closed&q=...
 *    PATCH /api/teacher/courses/{courseId}
 *  서버는 세션의 instructor_id로 자동 필터링
 * =============================== */

(function () {
    'use strict';

    // ---------- 중복 실행 방지 ----------
    if (window.__teacherClassesInitialized) {
        console.log('teacherClasses.js already initialized — skip');
        return;
    }
    window.__teacherClassesInitialized = true;
    console.log('teacherClasses.js initialized');

    document.addEventListener('DOMContentLoaded', () => {

        // ---------- DOM ----------
        const grid         = document.getElementById('courseGrid');
        const statusFilter = document.getElementById('statusFilter');
        const searchBox    = document.getElementById('searchBox');
        const searchBtn    = document.getElementById('searchBtn');

        const editModal    = document.getElementById('editModal');
        const editForm     = document.getElementById('editForm');
        const editName     = document.getElementById('editName');
        const editPeriod   = document.getElementById('editPeriod');
        const editType     = document.getElementById('editType');
        const editStatus   = document.getElementById('editStatus');
        const editIdInput  = document.getElementById('editId');

        let editingCourseId = null;
        let lastCourses = [];

        // ---------- CSRF ----------
        function csrfHeaders() {
            const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const headerName = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
            return token && headerName ? { [headerName]: token } : {};
        }

        // ---------- Utils ----------
        const $ = (sel, p = document) => p.querySelector(sel);

        function fmtDateOnly(d) {
            if (!d) return '';
            const m = String(d).match(/^(\d{4}-\d{2}-\d{2})/);
            return m ? m[1] : String(d);
        }

        function mapStatusToUi(db) {
            if (!db) return '정상';
            return String(db).toLowerCase() === 'closed' ? '중지' : '정상';
        }

        function mapFilterToDb(ui) {
            if (!ui || ui === '전체') return '';
            if (ui === '중지') return 'closed';
            return 'published';
        }

        function buildPeriod(created_at, expiry_date) {
            const left  = fmtDateOnly(created_at);
            const right = fmtDateOnly(expiry_date);
            if (left && right) return `${left} ~ ${right}`;
            return right || left || '';
        }

        function escapeHtml(s) {
            return String(s)
                .replaceAll('&', '&amp;')
                .replaceAll('<', '&lt;')
                .replaceAll('>', '&gt;')
                .replaceAll('"', '&quot;')
                .replaceAll("'", '&#39;');
        }

        function toast(msg) {
            try {
                const t = document.createElement('div');
                t.textContent = msg;
                t.style.cssText =
                    'position:fixed;left:50%;top:70px;transform:translateX(-50%);' +
                    'background:#333;color:#fff;padding:10px 14px;border-radius:8px;' +
                    'z-index:9999;opacity:.95';
                document.body.appendChild(t);
                setTimeout(() => t.remove(), 1800);
            } catch { alert(msg); }
        }

        // ---------- Data fetch ----------
        async function fetchCourses({ status = '', q = '' } = {}) {
            const params = new URLSearchParams();
            if (status) params.set('status', status);
            if (q)      params.set('q', q);

            const url = '/api/teacher/courses' + (params.toString() ? `?${params}` : '');
            const res = await fetch(url, { method: 'GET' });
            if (!res.ok) throw new Error('강의 목록을 불러오지 못했습니다.');
            return res.json();
        }

        // ---------- Render ----------
        function renderCourses(courses) {
            lastCourses = Array.isArray(courses) ? courses : [];
            grid.innerHTML = '';

            if (lastCourses.length === 0) {
                const empty = document.createElement('div');
                empty.textContent = '표시할 강의가 없습니다.';
                empty.style.cssText = 'padding:24px;color:#666;';
                grid.appendChild(empty);
                return;
            }

            lastCourses.forEach(c => {
                const { course_id, title, category, status, created_at, expiry_date } = c;

                const card = document.createElement('div');
                card.className = 'course-card';
                card.dataset.courseId = course_id;

                const period = buildPeriod(created_at, expiry_date);
                const statusUi = mapStatusToUi(status);

                card.innerHTML = `
                    <div class="course-title">${escapeHtml(title || '')}</div>
                    <div class="course-meta">
                        <div>기간: ${escapeHtml(period)}</div>
                        <div>유형: ${escapeHtml(category || 'VOD')}</div>
                        <div>상태: ${escapeHtml(statusUi)}</div>
                    </div>
                    <div class="course-actions">
                        <button class="btn-edit">수정</button>
                        <button class="btn-students">학생 리스트</button>
                        <button class="btn-delete" data-id="${escapeHtml(String(course_id))}">삭제</button>
                    </div>
                `;

                grid.appendChild(card);

                $('.btn-edit', card)?.addEventListener('click', () => openEditModal(course_id));
                $('.btn-students', card)?.addEventListener('click', () => {
                    toast('학생 리스트는 준비 중입니다.');
                });
            });
        }

        // ---------- Modal ----------
        function openEditModal(courseId) {
            const course = lastCourses.find(c => String(c.course_id) === String(courseId));
            if (!course) { alert('강의를 찾을 수 없습니다.'); return; }

            editingCourseId = courseId;
            if (editIdInput) editIdInput.value = String(courseId);

            editName.value   = course.title || '';
            editType.value   = course.category || 'VOD';
            editStatus.value = mapStatusToUi(course.status);
            editPeriod.value = buildPeriod(course.created_at, course.expiry_date);

            if (editModal) editModal.style.display = 'block';
        }

        function closeEditModal() {
            editingCourseId = null;
            if (editIdInput) editIdInput.value = '';
            if (editModal) editModal.style.display = 'none';
        }
        window.closeEditModal = closeEditModal;

        document.querySelectorAll('#editModal .closeModal')
            .forEach(btn => btn.addEventListener('click', closeEditModal));

        window.addEventListener('click', (e) => { if (e.target === editModal) closeEditModal(); });

        editForm?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const idFromDom = editIdInput?.value?.trim();
            const courseId = idFromDom || editingCourseId;
            if (!courseId) { alert('수정할 강의를 찾을 수 없습니다.'); return; }

            const payload = {
                title:    editName.value?.trim(),
                period:   editPeriod.value?.trim(),
                category: editType.value,
                status:   editStatus.value
            };
            if (!payload.title)  return alert('강의명을 입력하세요.');
            if (!payload.period) return alert('기간을 입력하세요.');

            try {
                const res = await fetch(`/api/teacher/courses/${courseId}`, {
                    method: 'PATCH',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                const data = await res.json().catch(() => ({}));
                if (!res.ok || data.ok === false) throw new Error(data.message || '수정에 실패했습니다.');

                closeEditModal();
                toast('수정되었습니다.');
                await loadCourses();
            } catch (err) {
                alert(err.message || '오류가 발생했습니다.');
            }
        });

        // ---------- Load courses ----------
        async function loadCourses() {
            const status = mapFilterToDb(statusFilter?.value || '전체');
            const q = searchBox?.value?.trim() || '';
            try {
                const list = await fetchCourses({ status, q });
                renderCourses(list);
            } catch (e) {
                grid.innerHTML = '<div style="padding:24px;color:#c00;">목록을 불러오는 중 오류가 발생했습니다.</div>';
                console.error(e);
            }
        }
        window.loadCourses = loadCourses;

        statusFilter?.addEventListener('change', loadCourses);
        searchBtn?.addEventListener('click', loadCourses);
        searchBox?.addEventListener('keydown', (e) => { if (e.key === 'Enter') loadCourses(); });

        // ---------- Delete event ----------
        document.removeEventListener('click', handleDeleteClick);
        document.addEventListener('click', handleDeleteClick);

        let isDeleting = false;

        function handleDeleteClick(e) {
            const btn = e.target.closest('.btn-delete');
            if (!btn) return;

            // 중복 클릭 방지
            if (isDeleting) return;
            isDeleting = true;
            setTimeout(() => isDeleting = false, 1000);

            const id = btn.dataset.id || btn.closest('.course-card')?.dataset.courseId;
            if (!id) return alert('삭제할 강의 ID를 찾을 수 없습니다.');
            if (
                !confirm('정말 이 강의를 삭제하시겠습니까?'),
                console.log('Delete listener removed')
            ) return;

            (async () => {
                try {
                    const res = await fetch(`/api/teacher/courses/${encodeURIComponent(id)}`, {
                        method: 'DELETE',
                        headers: { 'Accept': 'text/plain', ...csrfHeaders() }
                    });

                    if (res.status === 204) {
                        btn.closest('.course-card')?.remove();
                        toast('삭제되었습니다.');
                        return;
                    }

                    let msg = '';
                    try { msg = await res.text(); } catch {}
                    if (res.status === 404) throw new Error(msg || '강의를 찾을 수 없습니다.');
                    if (res.status === 409) throw new Error(msg || '참조 데이터가 있어 삭제할 수 없습니다.');
                    throw new Error(msg || '삭제에 실패했습니다.');
                } catch (err) {
                    alert(err.message || '삭제 중 오류가 발생했습니다.');
                }
            })();

            console.log('Delete listener added');
            console.log(document.querySelectorAll('.btn-delete').length);

        }



        // ---------- Initial load ----------
        loadCourses();
    });
})();
