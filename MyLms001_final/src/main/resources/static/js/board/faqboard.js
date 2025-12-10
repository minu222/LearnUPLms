// FAQ 아코디언 기능
document.querySelectorAll('.faq-question').forEach(q => {
    q.addEventListener('click', () => {
        const answer = q.nextElementSibling;
        const isActive = q.classList.contains('active');

        // 모두 닫기
        document.querySelectorAll('.faq-question').forEach(item => item.classList.remove('active'));
        document.querySelectorAll('.faq-answer').forEach(item => item.style.display = 'none');

        // 선택한 항목만 열기
        if(!isActive){
            q.classList.add('active');
            answer.style.display = 'block';
        }
    });
});