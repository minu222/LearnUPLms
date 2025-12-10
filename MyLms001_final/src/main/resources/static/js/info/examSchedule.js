// ==============================
// Info Center - 시험일정 스크립트 (A안: Hover Popover)
// ==============================
const currentUser = "teacher1";
const exams = [
    { course: "JAVA",   name: "중간고사", date: "2025-10-03", author: "teacher1" },
    { course: "React",  name: "기말고사", date: "2025-10-05", author: "teacher2" },
    { course: "Python", name: "퀴즈",   date: "2025-10-03", author: "teacher1" },
    // 테스트용 더미
    { course: "JAVA",   name: "실습평가", date: "2025-10-03", author: "teacher1" },
    { course: "React",  name: "모듈평가", date: "2025-10-03", author: "teacher1" },
];

const calendar      = document.getElementById("calendar");
const registerModal = document.getElementById("registerModal");
const editModal     = document.getElementById("editModal");
const deleteModal   = document.getElementById("deleteModal");

const courseInput   = document.getElementById("courseInput");
const examNameInput = document.getElementById("examName");
const examDateInput = document.getElementById("examDate");

let editingIndex = null;

/* ===== Hover Popover (전역 1개만 운용) ===== */
let _popover;
function ensurePopover() {
    if (_popover) return _popover;
    _popover = document.createElement("div");
    _popover.className = "popover";
    document.body.appendChild(_popover);
    _popover.addEventListener("mouseleave", hidePopover);
    return _popover;
}
function showPopover(html, x, y) {
    const p = ensurePopover();
    p.innerHTML = html;
    // 먼저 표시하여 실제 크기 확보
    p.classList.add("visible");
    const padding = 8;
    const vw = window.innerWidth, vh = window.innerHeight;
    const rect = p.getBoundingClientRect();
    const left = Math.min(x, vw - rect.width - padding);
    const top  = Math.min(y, vh - rect.height - padding);
    p.style.left = left + "px";
    p.style.top  = top  + "px";
}
function hidePopover() {
    if (_popover) _popover.classList.remove("visible");
}
window.addEventListener("scroll", hidePopover, { passive: true });
window.addEventListener("resize", hidePopover);

/* ===== 날짜에서 '일' 추출 (로컬 타임존 안전) ===== */
function getDayOfMonth(dateStr) {
    const d = new Date(dateStr + "T00:00:00");
    return d.getDate();
}

/* ===== 단일 시험 표시용 칩 ===== */
function makeExamChip(exam) {
    const div = document.createElement("div");
    div.className = "exam";
    div.textContent = `${exam.course} 시험`;
    div.title = `${exam.date} • ${exam.course} - ${exam.name}`;
    return div;
}

/* ===== 렌더 ===== */
function renderCalendar() {
    calendar.innerHTML = "";
    const monthDays = 31; // 필요 시 실제 월 길이 계산으로 교체 가능

    for (let day = 1; day <= monthDays; day++) {
        const dayDiv = document.createElement("div");
        dayDiv.className = "day";
        dayDiv.innerHTML = `<h4>${day}일</h4>`;

        const dayExams = exams.filter(e => getDayOfMonth(e.date) === day);
        if (dayExams.length === 0) {
            calendar.appendChild(dayDiv);
            continue;
        }

        // 즉시 2개만 표시
        const visible = dayExams.slice(0, 2);
        visible.forEach(e => dayDiv.appendChild(makeExamChip(e)));

        const hiddenCount = dayExams.length - visible.length;
        if (hiddenCount > 0) {
            // +N건 배지 (hover 시 팝오버)
            const more = document.createElement("span");
            more.className = "more-badge";
            more.textContent = `+${hiddenCount}건`;

            const hiddenList = dayExams.slice(2);
            const popHtml =
                `<h5>추가 일정</h5>` +
                hiddenList
                    .map(e => `<div class="item">${e.date} • ${e.course} - ${e.name}</div>`)
                    .join("");

            let hoverTimer;
            more.addEventListener("mouseenter", ev => {
                clearTimeout(hoverTimer);
                const r = ev.target.getBoundingClientRect();
                showPopover(popHtml, r.right + 6, r.bottom + 6); // 배지 오른쪽-아래에 표시
            });
            more.addEventListener("mouseleave", () => {
                // 팝오버로 이동할 시간 약간 부여
                hoverTimer = setTimeout(hidePopover, 120);
            });

            dayDiv.appendChild(more);
        }

        calendar.appendChild(dayDiv);
    }
}

/* ===== 모달 공통 ===== */
function openModal(m) { m.style.display = "flex"; }
function closeModal(m) { m.style.display = "none"; }

/* 등록 */
document.getElementById("registerBtn").onclick = () => {
    courseInput.value = "";
    examNameInput.value = "";
    examDateInput.value = "";
    editingIndex = null;
    openModal(registerModal);
};

/* 수정 */
document.getElementById("editBtn").onclick = () => {
    const listDiv = document.getElementById("editList");
    listDiv.innerHTML = "";
    exams.forEach((e, i) => {
        if (e.author === currentUser) {
            const div = document.createElement("div");
            div.className = "modal-exam-item";
            div.textContent = `${e.date} - ${e.course} - ${e.name}`;
            div.style.cursor = "pointer";
            div.onclick = () => {
                courseInput.value = e.course;
                examNameInput.value = e.name;
                examDateInput.value = e.date;
                editingIndex = i;
                closeModal(editModal);
                openModal(registerModal);
            };
            listDiv.appendChild(div);
        }
    });
    openModal(editModal);
};

/* 삭제 */
document.getElementById("deleteBtn").onclick = () => {
    const listDiv = document.getElementById("deleteList");
    listDiv.innerHTML = "";
    exams.forEach((e, i) => {
        if (e.author === currentUser) {
            const div = document.createElement("div");
            div.className = "modal-exam-item";
            const checkbox = document.createElement("input");
            checkbox.type = "checkbox";
            checkbox.className = "delete-checkbox";
            checkbox.value = i;
            const label = document.createElement("span");
            label.textContent = `${e.date} - ${e.course} - ${e.name}`;
            div.appendChild(checkbox);
            div.appendChild(label);
            listDiv.appendChild(div);
        }
    });
    openModal(deleteModal);
};

/* 저장 */
document.getElementById("submitExam").onclick = () => {
    const course = courseInput.value.trim();
    const name   = examNameInput.value.trim();
    const date   = examDateInput.value;
    if (!course || !name || !date) { alert("모든 항목을 입력해주세요"); return; }

    if (editingIndex !== null) {
        exams[editingIndex] = { course, name, date, author: currentUser };
        editingIndex = null;
    } else {
        exams.push({ course, name, date, author: currentUser });
    }
    closeModal(registerModal);
    hidePopover();
    renderCalendar();
};

/* 삭제 확정 */
document.getElementById("deleteConfirmBtn").onclick = () => {
    const checkboxes = document.querySelectorAll('#deleteList input[type="checkbox"]:checked');
    const indexes = [...checkboxes].map(cb => parseInt(cb.value, 10)).sort((a, b) => b - a);
    indexes.forEach(i => exams.splice(i, 1));
    closeModal(deleteModal);
    hidePopover();
    renderCalendar();
};

/* 모달 닫기 */
document.querySelectorAll(".modal-close").forEach(btn => {
    btn.onclick = () => closeModal(btn.parentElement.parentElement);
});
document.querySelectorAll(".modal-overlay").forEach(mod => {
    mod.onclick = e => { if (e.target === mod) closeModal(mod); };
});

renderCalendar();
