window.addEventListener("DOMContentLoaded",() => {
    const params = new URLSearchParams(window.location.search);
    const tab = params.get("tab");
    if(tab){
        showCircTab(tab);
    }
});

function confirmRemind(element) {
    const cId = element.dataset.id;
    const readerName = element.dataset.name;
    if (confirm("Bạn có chắc chắn muốn nhắc nhở bạn đọc " + readerName + " này?")) {
        window.location.href = "/qltv/quan-ly/send-overdue?id=" + cId;
    }
}

function confirmRemindAll(){
    if(confirm("Bạn có chắc muốn gửi nhắc nhở cho tất cả độc giả quá hạn?")){
        window.location.href ="/qltv/quan-ly/send-overdue-all";
    }
}

function printBorrowList(){
    document.getElementById("print-date").textContent = "Ngày in:" + new Date().toLocaleDateString("vi-VN");
    document.body.classList.add("print-area");
    window.print();
    document.body.classList.remove("print-area");
}

function findReader(){
        const cardId = document.getElementById("card-id").value;
        fetch(`/qltv/quan-ly/borrow/find-reader?cardId=${cardId}`)
            .then(res => res.json())
            .then(card => {
                console.log(card);
                if(card.found == false){
                    document.getElementById("reader-card").style.display = "flex";
                    document.getElementById("reader-name").textContent = "Không tìm thấy độc giả";
                    document.getElementById("reader-info").textContent = "Không có hội viên hoặc sai mã thẻ";
                    document.getElementById("reader-status").textContent = "Vui lòng kiểm tra lại";
                    document.getElementById("borrow-list").innerHTML = "";
                    return;
                }
                document.getElementById("reader-card").style.display = "flex";
                document.getElementById("reader-name").textContent = card.name;
                document.getElementById("reader-info").textContent =`Mã: ${card.id} · Còn hạn đến ${new Date(card.expiryDate).toLocaleDateString("vi-VN")}`;
                document.getElementById("reader-status").textContent =card.status
                    ? "Đủ điều kiện mượn" : "Thẻ đã hết hạn";
                return fetch(`/qltv/quan-ly/borrow/current?cardId=${cardId}`);})
            .then(res => {
                if(!res) return;
                return res.json();
            })
            .then(data => {
                if(!data) return;
                if(data.length > 0){
                    document.getElementById("borrow-count").textContent =`Đang mượn (${data[0]["borrow-count"]} quyển)`;
                    document.getElementById("borrow-limit").textContent =`Tối đa: ${data[0].maxBooksAllowed}`;
                }
                const borrowList = document.getElementById("borrow-list");
                borrowList.innerHTML = "";
                if(data.length === 0){
                    borrowList.innerHTML = `<div style="padding:20px;text-align:center;color:#888;">
                            Không có sách đang mượn</div>`;
                    return;
                }
                data.forEach(book => {
                    let dueClass = "due-ok";
                    let dueText =`Hạn: ${new Date(book.dueDate).toLocaleDateString("vi-VN")}`;
                    // quá hạn
                    if(book.overdueDays > 0){
                        dueClass = "due-over";
                        dueText =`Quá ${book.overdueDays} ngày`;
                    }
                    // sắp tới hạn
                    else {
                        const dueDate = new Date(book.dueDate);
                        const today =new Date();
                        const diffDays =(dueDate - today)/ (1000 * 60 * 60 * 24);
                        if(diffDays <= 3){
                            dueClass = "due-warn";
                        }
                    }

                    borrowList.innerHTML += `
                        <div class="borrow-list-item w-100 row col-md-12">
                            <div class="borrow-book-icon" style="background:#e6f2ec;">📘</div>
                            <div class="borrow-info">
                                <div class="borrow-title-text">${book.title}</div>
                                <div class="borrow-meta">Mượn: ${new Date(book.borrowDate).toLocaleDateString("vi-VN")}</div>
                            </div>
                            <div class="due-date ${dueClass} col-md-6" style="text-align:end;">${dueText}</div>
                        </div>`;
                });

            })
            .catch(err => {
                console.error(err);
                document.getElementById("borrow-list")
                    .innerHTML = `<div style="padding:20px;color:red;text-align:center;">
                            Lỗi tải dữ liệu</div>`;
            });
    }


//                if(book.available === 0){
//
//                    document.getElementById("book-card").style.display = "flex";
//
//                    document.getElementById("book-title").textContent = "Sách đã hết";
//
//                    document.getElementById("book-info").textContent ="Không còn quyển nào khả dụng";
//
//                    const statusEl =document.getElementById("book-status");
//
//                    statusEl.className ="bl-badge bl-badge-overdue";
//
//                    statusEl.textContent ="Đã được mượn hết";

let selectedBooks = [];
function findBook(){
    const keyword = document.getElementById("book-id").value.trim();
    if(!keyword) return;
    fetch(`/qltv/quan-ly/borrow/find-book?keyword=${keyword}`)
        .then(res => res.json())
        .then(book => {
            if(book.available === 0){
                alert("Sách đã hết hoặc không tồn tại");
                return;
            }
            addBook(book);
            document.getElementById("book-id").value = "";
        })
        .catch(err => {
            console.error(err);
            alert("Có lỗi xảy ra");
        });
}

function addBook(book){
    const exists = selectedBooks.some(b => b.id === book.id);
    if(exists){
        alert("Sách đã được chọn");
        return;
    }
    selectedBooks.push(book);
    renderSelectedBooks();
}

function removeBook(bookId){
    selectedBooks = selectedBooks.filter(b => b.id !== bookId);
    renderSelectedBooks();
}

function renderSelectedBooks(){

    const container = document.getElementById("selected-books-container");
    container.innerHTML = "";

    selectedBooks.forEach(book => {
        container.innerHTML += `
            <div class="confirm-card" style="border-color:var(--forest); margin-top:10px;">
                <div class="confirm-icon">📗</div>
                <div style="flex:1">
                    <div class="confirm-title">${book.title}</div>

                    <div class="confirm-sub">${book.author} · ISBN ${book.isbn}</div>
                    <div style="margin-top:6px;">
                        <span class="bl-badge bl-badge-available">Còn ${book.available} quyển</span>
                    </div>
                </div>

                <button type="button" onclick="removeBook('${book.id}')" style="background:none;border:none;font-size:18px;cursor:pointer; color:var(--rust);">
                    ✕
                </button>
            </div>
        `;
    });
    renderHiddenInputs();
}

function renderHiddenInputs(){
    const hidden = document.getElementById("hidden-book-inputs");
    hidden.innerHTML = "";
    selectedBooks.forEach(book => {
        hidden.innerHTML += `
            <input type="hidden" name="bookIds" value="${book.id}">`;
    });
}

document.getElementById("borrow-form").addEventListener("submit", function (e) {
    e.preventDefault();
    const errorDiv = document.getElementById("error-input");
    errorDiv.textContent = "";
    const cardId = document.getElementById("card-id").value;
    const borrowDate = document.getElementById("borrow-date").value;
    const returnDate = document.getElementById("return-date").value;

    if (!cardId || !borrowDate || !returnDate) {
        errorDiv.textContent = "Vui lòng nhập đầy đủ thông tin";
        return;
    }
    document.getElementById("hidden-card-id").value = cardId;
    document.getElementById("selected-books").value = selectedBooks.map(book => book.id).join(",");
    if (selectedBooks.length === 0) {
        errorDiv.textContent = "Vui lòng chọn ít nhất 1 cuốn sách";
        return;
    }
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const borrow = new Date(borrowDate + "T00:00:00");

    if (borrow.getTime() !== today.getTime()) {
        errorDiv.textContent = "Ngày mượn phải là hôm nay";
        return;
    }
    const returnD = new Date(returnDate + "T00:00:00");
    const diffDays = (returnD - borrow) / (1000 * 60 * 60 * 24);
    if (diffDays <= 0) {
        errorDiv.textContent = "Ngày trả phải lớn hơn ngày mượn";
        return;
    }
    if (diffDays > 30) {
        errorDiv.textContent = "Thời gian mượn tối đa 30 ngày";
        return;
    }
    Promise.all([
        fetch(`/qltv/quan-ly/borrow/find-reader?cardId=${cardId}`).then(r => r.json()),
        ...selectedBooks.map(book =>
        fetch(`/qltv/quan-ly/borrow/find-book?keyword=${book.isbn}`)
                .then(r => r.json())
        )
    ])
    .then(results => {
        const reader = results[0];
        const books = results.slice(1);
        console.log("Reader:", reader);
            console.log("Books:", books);
        if (!reader.found || !reader.status) {
            errorDiv.textContent ="Thẻ không hợp lệ hoặc đã hết hạn";
            return;
        }
        const unavailableIndex = books.findIndex(book => book.available === 0);

        if (unavailableIndex !== -1) {
            errorDiv.textContent =`Sách "${selectedBooks[unavailableIndex].title}" đã hết hoặc không tồn tại`;
            return;
        }
        document.getElementById("borrow-form").submit();
    })
    .catch(err => {
        console.error(err);
        errorDiv.textContent = "Có lỗi xảy ra khi kiểm tra dữ liệu";
    });
});



let overdueFine = 0;
let bookPrice = 0;
let lightDamageFine = 0;
let returnReader = "";
let returnBookNames = [];
function findReturnBook() {
    const keyword = document.getElementById("return-keyword").value.trim();
    if (!keyword) {
        alert("Vui lòng nhập mã");
        return;
    }
    fetch(`/qltv/quan-ly/find-return-slip?keyword=${keyword}`)
        .then(res => res.json())
        .then(data => {
            const returnCard = document.getElementById("return-card");
            const overdueAlert = document.getElementById("overdue-alert");
            overdueAlert.style.display = "none";
            if (!data.found) {
                returnCard.style.display = "flex";
                returnCard.classList.add("confirm-card");
                returnCard.innerHTML = `
                    <div class="confirm-icon">❌</div>
                    <div>
                        <div class="confirm-title">
                            Không tìm thấy phiếu mượn
                        </div>
                        <div class="confirm-sub">
                            Kiểm tra lại mã
                        </div>
                    </div>
                `;
                return;
            }
            returnReader = data.readerName;
            returnBooks = data.details.map(d => d.title);

            overdueFine = data.fine || 0;
            lightDamageFine = data.lightDamageFine || 0;
            returnCard.style.display = "block";
            returnCard.innerHTML = "";
            data.details.forEach((d, index) => {
                returnCard.innerHTML += `
                <div class="confirm-card" style="margin-bottom:10px;">
                    <div class="confirm-icon">📕</div>
                    <div>
                        <div class="confirm-title">${d.title}</div>
                        <div class="confirm-sub">
                            ${data.readerName}
                            · Mượn ${new Date(data.borrowDate).toLocaleDateString("vi-VN")}
                            · Hạn ${new Date(data.dueDate).toLocaleDateString("vi-VN")}
                        </div>
                    </div>
                </div>
                <div class="bl-form-row" style="margin-bottom:20px;">
                    <input type="hidden" name="detailIds" value="${d.id}">
                    <input type="hidden" id="price-${index}" value="${d.priceBook}">
                    <div class="bl-form-group">
                        <div class="bl-form-label">Tình trạng sách</div>
                        <select class="bl-form-input" name="bookConditions" onchange="updateFine(${index}, ${data.fine})">
                            <option value="good">Tốt</option>
                            <option value="light">Có hư hỏng nhẹ</option>
                            <option value="heavy">Hư hỏng nặng</option>
                            <option value="lost">Mất sách</option>
                        </select>
                    </div>
                    <div class="bl-form-group">
                        <div class="bl-form-label">Phí phạt (VNĐ)</div>
                        <input class="bl-form-input" id="fine-${index}" name="fineAmounts" readonly value="${data.fine}">
                    </div>
                </div>
                `;
            });
            setTimeout(() => {
                document.querySelectorAll("[name='bookConditions']")
                    .forEach((_, index) => {
                        updateFine(index);
                    });

                calculateTotalFine();
            }, 0);
            if (data.overdueDays > 0) {
                overdueAlert.style.display = "flex";
                overdueAlert.textContent =
                    `⚠️ Quá ${data.overdueDays} ngày - Phí ${data.fine.toLocaleString("vi-VN")}đ`;
            }
        })
        .catch(err => {
            console.error(err);
            alert("Có lỗi xảy ra");
        });
}

function updateFine(index){
    const select = document.querySelectorAll("[name='bookConditions']")[index];
    const price = Number(document.getElementById(`price-${index}`).value);
    let fine = 0;
    if(select.value === "light"){
        fine = lightDamageFine;
    }
    else if(select.value === "heavy"){
        fine = price/2;
    }
    else if(select.value === "lost"){
        fine = price;
    }
    document.getElementById(`fine-${index}`).value = fine;
    calculateTotalFine();
}

function calculateTotalFine(){
    let bookTotal = 0;
    document.querySelectorAll("[name='fineAmounts']")
        .forEach(input => {
            bookTotal += Number(input.value) || 0;
        });

    const total = overdueFine + bookTotal;
    document.getElementById("total-fine").value =
        total.toLocaleString("vi-VN");
}

function confirmReturn(){
    const detailIds = document.querySelectorAll("[name='detailIds']");
    if(detailIds.length === 0){
        document.getElementById("errorId").textContent = "Vui lòng tra cứu phiếu hợp lệ";
        return;
    }
    if(confirm("Bạn có muốn in biên lai không?")){
        printReceipt();
    }
    document.getElementById("form-borrow").submit();
}

function printReceipt(){
        const fineEl = document.getElementById("total-fine");
        if(!fineEl){
            alert("Không tìm thấy dữ liệu trả sách");
            return;
        }
        const fine = fineEl.value || "0";
        const reader = returnReader || "Không rõ";
        const bookNames = returnBooks || [];
        if(bookNames.length === 0){
            alert("Không có sách để in");
            return;
        }
        document.getElementById("receipt-reader").textContent = reader;
        document.getElementById("receipt-book").innerHTML =
            bookNames.map(b => `📕 ${b}`).join("<br>");
        document.getElementById("receipt-date").textContent =
            new Date().toLocaleDateString("vi-VN");
        document.getElementById("receipt-fine").textContent = fine;
        const area = document.getElementById("receipt-print-area");
        if(!area){
            alert("Không tìm thấy vùng in");
            return;
        }
        document.body.classList.add("print-receipt");
        area.style.display = "block";
        window.print();
        area.style.display = "none";
        document.body.classList.remove("print-receipt");
}

async function printBorrowSlip(btn) {

    const slipId = btn.dataset.slip;

    const res = await fetch(`/qltv/quan-ly/borrow-slip/${slipId}`);
    const data = await res.json();

    let rows = "";

    data.books.forEach((book, index) => {
        rows += `
            <tr>
                <td>${index + 1}</td>
                <td>${book.bookId}</td>
                <td>${book.title}</td>
                <td>${book.author}</td>
                <td>${book.category}</td>
            </tr>
        `;
    });

    const printWindow = window.open("", "_blank");

    printWindow.document.write(`
        <html>
        <head>
            <title>Phiếu mượn sách</title>
            <style>
                body{
                    font-family: Arial,sans-serif;
                    padding:20px;
                }
                table{
                    width:100%;
                    border-collapse:collapse;
                }
                th,td{
                    border:1px solid #000;
                    padding:8px;
                }
                h2{
                    text-align:center;
                }
                .qr{
                    text-align:center;
                    margin-top:20px;
                }
            </style>
        </head>
        <body>

            <h2>PHIẾU MƯỢN SÁCH</h2>

            <p><b>Mã phiếu:</b> ${data.slipId}</p>
            <p><b>Độc giả:</b> ${data.readerName}</p>
            <p><b>Ngày mượn:</b> ${data.borrowDate}</p>
            <p><b>Hạn trả:</b> ${data.dueDate}</p>

            <table>
                <thead>
                    <tr>
                        <th>STT</th>
                        <th>Mã bản sách</th>
                        <th>Tên sách</th>
                        <th>Tác giả</th>
                        <th>Thể loại</th>
                    </tr>
                </thead>
                <tbody>
                    ${rows}
                </tbody>
            </table>

            <p>Vui lòng cầm phiếu này khi đến trả sách</p>

            <div class="qr">
                <img src="/qltv/quan-ly/qr/${slipId}" width="250">
            </div>

        </body>
        </html>
    `);

    printWindow.document.close();

    setTimeout(() => {
        printWindow.print();
        printWindow.close();
    }, 1000);
}