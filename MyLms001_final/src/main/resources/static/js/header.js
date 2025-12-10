document.addEventListener("DOMContentLoaded", function() {
    // 알림 드롭다운 토글
    const notificationBtn = document.getElementById("notificationBtn");
    if (notificationBtn) {
        notificationBtn.addEventListener("click", function(event) {
            event.stopPropagation();
            const dropdown = this.querySelector(".dropdown");
            dropdown.classList.toggle("show");
        });

        document.addEventListener("click", function() {
            const dropdown = notificationBtn.querySelector(".dropdown");
            if (dropdown) dropdown.classList.remove("show");
        });
    }

    // 메뉴 active 클래스 (클릭 시)
    const headerLinks = document.querySelectorAll(".header-category");
    headerLinks.forEach(link => {
        link.addEventListener("click", function(e) {
            headerLinks.forEach(l => l.classList.remove("active"));
            this.classList.add("active");
        });
    });
});
