function filterTable() {
    const input = document.getElementById('searchInput').value.toLowerCase();
    const role = document.getElementById('roleFilter').value;
    const trs = document.querySelectorAll('#messageTable tbody tr');

    trs.forEach(tr => {
        const title = tr.cells[1].textContent.toLowerCase();
        const sender = tr.cells[2].textContent.toLowerCase();
        const trRole = tr.dataset.role;

        let matchesSearch = title.includes(input) || sender.includes(input);
        let matchesRole = role === "" || trRole === role;

        tr.style.display = (matchesSearch && matchesRole) ? "" : "none";
    });
}

function filterTab(tab) {
    const trs = document.querySelectorAll('#messageTable tbody tr');

    trs.forEach(tr => {
        if (tab === '전체') {
            tr.style.display = "";
        } else {
            tr.style.display = (tr.dataset.direction === tab) ? "" : "none";
        }
    });

    document.querySelectorAll('.tabs button').forEach(btn => btn.classList.remove('active'));
    event.target.classList.add('active');
}

function toggleAll(source) {
    const checkboxes = document.querySelectorAll('#messageTable tbody input[type="checkbox"]');
    checkboxes.forEach(cb => cb.checked = source.checked);
}

function deleteRow(button) {
    const row = button.closest('tr');
    row.remove();
}

function deleteSelected() {
    const checkboxes = document.querySelectorAll('#messageTable tbody input[type="checkbox"]');
    checkboxes.forEach(cb => {
        if (cb.checked) {
            cb.closest('tr').remove();
        }
    });
    document.getElementById('selectAll').checked = false;
}