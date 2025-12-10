const courseTypeSelect = document.getElementById("courseType");
const dailyTimeInput = document.getElementById("dailyTime");
const targetField = document.getElementById("targetField");
const mainImage = document.getElementById("mainImage");
const preview = document.getElementById("preview");

function updateFields() {
    const type = courseTypeSelect.value;

    if (type === "VOD") {
        dailyTimeInput.value = "";
        dailyTimeInput.disabled = true;
        document.getElementById("dailyTimeNote").style.display = "block";
    } else {
        dailyTimeInput.disabled = false;
        document.getElementById("dailyTimeNote").style.display = "none";
    }

    targetField.innerHTML = "";
    if (type === "개인강의") {
        targetField.innerHTML = `<input type="text" name="studentId" placeholder="학생 ID 입력">`;
    } else if (type === "다수강의") {
        targetField.innerHTML = `<input type="number" name="students" placeholder="인원수 입력 (20~50)" min="20" max="50">`;
    } else {
        targetField.innerHTML = `<input type="text" disabled placeholder="VOD 선택시 비활성화">`;
    }
}


courseTypeSelect.addEventListener("change", updateFields);
updateFields();

// 이미지 미리보기
mainImage.addEventListener("change", e => {
    const file = e.target.files[0];
    if (file) preview.src = URL.createObjectURL(file);
    else preview.src = "";
});