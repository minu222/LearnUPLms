// emailModal.js
document.addEventListener('DOMContentLoaded', () => {
    // 모달 HTML 생성
    const modalHTML = `
<div class="modal-overlay" id="emailModal">
    <div class="modal-container">
        <button class="modal-close" id="closeEmailModal">&times;</button>
        <div class="modal-header">이메일 보내기</div>
        <form id="emailForm">
            <label for="emailRecipient">받는사람 이메일</label>
            <input type="text" id="emailRecipient" placeholder="example@domain.com" required>

            <label for="emailTitle">제목</label>
            <input type="text" id="emailTitle" placeholder="제목을 입력하세요" required>

            <label for="emailContent">내용</label>
            <div class="toolbar">
                <button type="button" id="boldBtn"><b>B</b></button>
                <button type="button" id="italicBtn"><i>I</i></button>
                <button type="button" id="underlineBtn"><u>U</u></button>
                <button type="button" id="ulistBtn">• 리스트</button>
                <button type="button" id="olistBtn">1. 리스트</button>
                <button type="button" id="leftBtn">왼쪽</button>
                <button type="button" id="centerBtn">가운데</button>
                <button type="button" id="rightBtn">오른쪽</button>
                <select id="fontSizeSelect">
                    <option value="">글자 크기</option>
                    <option value="1">10px</option>
                    <option value="2">13px</option>
                    <option value="3">16px</option>
                    <option value="4">18px</option>
                    <option value="5">24px</option>
                    <option value="6">32px</option>
                    <option value="7">48px</option>
                </select>
            </div>
            <div id="emailContent" class="editor" contenteditable="true"></div>
            <div id="charCount">0 / 1000</div>

            <div class="file-input">
                <label for="emailAttachments">파일 첨부 (최대 5개, 1개당 10MB)</label>
                <input type="file" id="emailAttachments" multiple>
                <ul id="fileList"></ul>
            </div>

            <button type="submit" class="send-button">전송</button>
        </form>
    </div>
</div>
<style>
.modal-overlay {position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.5);display:none;justify-content:center;align-items:center;z-index:1000;}
.modal-container {width:1100px;max-height:90vh;overflow-y:auto;background:#fff;border-radius:8px;padding:30px;box-shadow:0 4px 12px rgba(0,0,0,0.2);position:relative;}
.modal-close {position:absolute;top:15px;right:15px;font-size:1.5rem;border:none;background:none;cursor:pointer;}
.modal-header {font-size:1.8rem;font-weight:bold;margin-bottom:20px;}
form {display:flex;flex-direction:column;gap:15px;}
label {font-weight:bold;margin-bottom:6px;}
input[type="text"] {padding:8px 10px;font-size:1rem;border:1px solid #ccc;border-radius:4px;width:100%;}
.toolbar {display:flex;gap:4px;margin-bottom:5px;flex-wrap:wrap;}
.toolbar button,.toolbar select {padding:4px 8px;border:1px solid #ccc;border-radius:4px;cursor:pointer;background:#f0f0f0;}
.editor {padding:8px;border:1px solid #ccc;border-radius:4px;min-height:200px;overflow:auto;}
#charCount {text-align:right;font-size:0.9rem;color:#555;}
.file-input {display:flex;flex-direction:column;gap:5px;}
.file-input ul {list-style:none;padding-left:0;margin:0;}
.file-input ul li {display:flex;justify-content:space-between;align-items:center;margin-bottom:2px;}
button.send-button {width:120px;padding:10px 0;background:#ffb400;border:none;border-radius:4px;color:#fff;font-weight:bold;cursor:pointer;align-self:flex-start;}
button.removeFileBtn {padding:2px 5px;border:none;border-radius:3px;background:#ff4d4d;color:#fff;cursor:pointer;font-size:0.8rem;}
</style>
    `;
    document.body.insertAdjacentHTML('beforeend', modalHTML);

    const modal = document.getElementById('emailModal');
    const openBtn = document.getElementById('openEmailModal');
    const closeBtn = document.getElementById('closeEmailModal');

    openBtn.addEventListener('click', () => modal.style.display='flex');
    closeBtn.addEventListener('click', () => modal.style.display='none');
    window.addEventListener('click', (e)=>{if(e.target===modal) modal.style.display='none';});

    // ====== 기존 JS 그대로 ======
    const emailContent = document.getElementById('emailContent');
    const emailAttachments = document.getElementById('emailAttachments');
    const fileList = document.getElementById('fileList');
    const form = document.getElementById('emailForm');
    const fontSizeSelect = document.getElementById('fontSizeSelect');
    let selectedFiles = [];
    const maxLength = 1000;

    document.getElementById('boldBtn').addEventListener('click', () => document.execCommand('bold'));
    document.getElementById('italicBtn').addEventListener('click', () => document.execCommand('italic'));
    document.getElementById('underlineBtn').addEventListener('click', () => document.execCommand('underline'));
    document.getElementById('ulistBtn').addEventListener('click', () => document.execCommand('insertUnorderedList'));
    document.getElementById('olistBtn').addEventListener('click', () => document.execCommand('insertOrderedList'));
    document.getElementById('leftBtn').addEventListener('click', () => document.execCommand('justifyLeft'));
    document.getElementById('centerBtn').addEventListener('click', () => document.execCommand('justifyCenter'));
    document.getElementById('rightBtn').addEventListener('click', () => document.execCommand('justifyRight'));

    fontSizeSelect.addEventListener('change', () => {
        const size = fontSizeSelect.value;
        if(size){
            document.execCommand('fontSize', false, size);
            const pxMap = {1:"10px",2:"13px",3:"16px",4:"18px",5:"24px",6:"32px",7:"48px"};
            emailContent.querySelectorAll("font[size]").forEach(f=>{
                f.style.fontSize = pxMap[f.size]||pxMap[size];
                f.removeAttribute("size");
            });
        }
    });

    emailContent.addEventListener('input', ()=>{
        let text = emailContent.innerText;
        if(text.length>maxLength){
            emailContent.innerText=text.substring(0,maxLength);
            alert("내용은 최대 1000글자까지 입력 가능합니다.");
            placeCaretAtEnd(emailContent);
        }
        document.getElementById('charCount').textContent=`${emailContent.innerText.length} / ${maxLength}`;
    });

    function placeCaretAtEnd(el){
        el.focus();
        const range = document.createRange();
        range.selectNodeContents(el);
        range.collapse(false);
        const sel = window.getSelection();
        sel.removeAllRanges();
        sel.addRange(range);
    }

    emailAttachments.addEventListener('change', ()=>{
        const files = Array.from(emailAttachments.files);
        files.forEach(file=>{
            if(selectedFiles.length>=5){alert("첨부파일은 최대 5개까지만 업로드 가능합니다."); return;}
            if(file.size>10*1024*1024){alert(`${file.name} 파일이 10MB를 초과했습니다.`); return;}
            selectedFiles.push(file);
        });
        renderFileList();
        emailAttachments.value="";
    });

    function renderFileList(){
        fileList.innerHTML="";
        selectedFiles.forEach((file,idx)=>{
            const li=document.createElement('li');
            li.textContent=`${file.name} (${Math.round(file.size/1024)} KB) `;
            const btn=document.createElement('button');
            btn.type='button'; btn.textContent='삭제'; btn.className='removeFileBtn';
            btn.onclick=()=>{selectedFiles.splice(idx,1); renderFileList();};
            li.appendChild(btn);
            fileList.appendChild(li);
        });
    }

    form.addEventListener('submit',(e)=>{
        e.preventDefault();
        const recipient=document.getElementById('emailRecipient').value;
        const title=document.getElementById('emailTitle').value;
        const content=emailContent.innerHTML;
        let msg=`받는사람: ${recipient}\n제목: ${title}\n내용:\n${content}`;
        if(selectedFiles.length>0) msg+=`\n첨부파일: ${selectedFiles.map(f=>f.name).join(', ')}`;
        alert("이메일이 전송되었습니다!\n\n"+msg);
        form.reset();
        emailContent.innerHTML="";
        selectedFiles=[];
        renderFileList();
        document.getElementById('charCount').textContent=`0 / ${maxLength}`;
    });
});
