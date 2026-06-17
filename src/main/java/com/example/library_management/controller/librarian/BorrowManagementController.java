package com.example.library_management.controller.librarian;

import com.example.library_management.entity.*;
import com.example.library_management.repository.*;
import com.example.library_management.service.EmailService;
import com.example.library_management.service.IdGeneratorService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@RequestMapping("/quan-ly")
public class BorrowManagementController {
    LibraryCardRepository libraryCardRepository;
    BookCopyRepository bookCopyRepository;
    BorrowDetailRepository borrowDetailRepository;
    IdGeneratorService idGeneratorService;
    BorrowSlipRepository borrowSlipRepository;
    LibararyRuleRepository libararyRuleRepository;

    @GetMapping("/borrowing")
    public String showBorrowing(@RequestParam(defaultValue = "borrow-tab") String tab,
                                @RequestParam(defaultValue = "") String filter,
                                @RequestParam(defaultValue = "") String keyword,
                                @RequestParam(defaultValue = "0") int page,Model model) {

        Map<String, Object> data = new HashMap<>();
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<BorrowDetail> historyPage = borrowDetailRepository.searchHistory(keyword, filter, pageable);
        data.put("title", "Cho mượn · Trả sách · Quá hạn");
        data.put("sub", "Quản lý lưu hành");
        data.put("borrowTitle", historyPage);
        data.put("activeTab", tab);
        data.put("keyword",keyword);
        data.put("totalElements", historyPage.getTotalElements());
        data.put("overdueBooks", borrowDetailRepository.findOverdueBooks());
        data.put("returnedToday", borrowDetailRepository.findReturnedToday());
        data.put("renewedBorrow",borrowDetailRepository.findRenewedBorrowDetails());
        data.put("countReturnedToday", borrowDetailRepository.findReturnedToday().size());
        data.put("libraryRule",libararyRuleRepository.findAll());
        double overdueRate = libararyRuleRepository.findByRuleKey("OVERDUE_FINE_PER_DAY")
                .orElseThrow()
                .getRuleValue();
        data.put("overdueRate",overdueRate);

        switch (tab) {
            case "history-tab":
                model.addAttribute("activePage", "borrow");
                break;
            case "return-tab":
                model.addAttribute("activePage", "return");
                break;
            case "overdue-tab":
                model.addAttribute("activePage", "overdue");
                break;
            default:
                model.addAttribute("activePage", "borrow");
        }
        model.addAllAttributes(data);
        return "librarian/borrow";
    }

    @GetMapping("/borrow/find-reader")
    @ResponseBody
    public Map<String,Object> findReader(@RequestParam String cardId){

        LibraryCard card = libraryCardRepository.findById(cardId).orElse(null);
        Map<String,Object> data = new HashMap<>();
        if(card == null){
            data.put("found", false);
            return data;
        }
        data.put("found", true);
        data.put("id", card.getId());
        data.put("expiryDate", card.getExpiryDate());
        data.put("status", card.isStatus());
        data.put("name", card.getReader().getName());
        return data;
    }

    @GetMapping("/borrow/current")
    @ResponseBody
    public List<Map<String, Object>> currentBorrow(@RequestParam String cardId) {

        List<BorrowDetail> details = borrowDetailRepository.findBorrowingByCardId(cardId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (BorrowDetail detail : details) {
            Map<String, Object> map = new HashMap<>();
            map.put("maxBooksAllowed", detail.getBorrowSlip().getLibraryCard().getMaxBooksAllowed());
            map.put("borrow-count",borrowDetailRepository.countBorrowing(cardId));
            map.put("title", detail.getBookCopy().getBookTitle().getTitle());
            map.put("borrowDate", detail.getBorrowSlip().getBorrowDate());
            map.put("dueDate", detail.getBorrowSlip().getDueDate());

            long overdue = ChronoUnit.DAYS.between(detail.getBorrowSlip().getDueDate(),
                    LocalDate.now());
            map.put("overdueDays", Math.max(overdue, 0));
            result.add(map);
        }
        return result;
    }

    @GetMapping("/borrow/find-book")
    @ResponseBody
    public Map<String,Object> findBook(@RequestParam String keyword){

        List<BookCopy> books = bookCopyRepository.findAvailableBooks(keyword);
        Map<String,Object> data = new HashMap<>();
        if(books.isEmpty()){
            data.put("available", 0);
            data.put("title", keyword);
            return data;
        }
        BookCopy book = books.get(0);
        data.put("id", book.getId());
        data.put("title", book.getBookTitle().getTitle());
        data.put("isbn", book.getBookTitle().getIsbn());
        data.put("author", book.getBookTitle().getAuthor().getAuthorName());
        data.put("available", books.size());
        return data;
    }

    @PostMapping("/borrowing")
    public String createBorrowing(@ModelAttribute BorrowSlip borrowSlip, @RequestParam String cardId,
                                  @RequestParam List<String> bookIds, Model model, RedirectAttributes attributes) {

        LibraryCard card = libraryCardRepository.findById(cardId).orElse(null);
        long borrowingCount = borrowDetailRepository.countBorrowing(cardId);
        if (borrowingCount + bookIds.size() > card.getMaxBooksAllowed()) {
            attributes.addFlashAttribute("error", "Đã vượt quá số sách được mượn");
            return "redirect:/borrowing?tab=borrow-tab";
        }

        borrowSlip.setId(idGeneratorService.generate("BORROW_SLIP", "BRS"));
        borrowSlip.setLibraryCard(card);
        borrowSlipRepository.save(borrowSlip);
        card.setTotalBorrow(card.getTotalBorrow() + bookIds.size());
        libraryCardRepository.save(card);
        for(String bookId : bookIds){
            BookCopy bookCopy = bookCopyRepository.findById(bookId).orElseThrow();
            bookCopy.setCirculationStatus("borrowed");
            bookCopyRepository.save(bookCopy);
            BorrowDetail detail = BorrowDetail.builder()
                            .id(idGeneratorService.generate("BORROW_DETAIL", "BRD"))
                            .borrowSlip(borrowSlip)
                            .bookCopy(bookCopy)
                            .status(1)
                            .build();
            borrowDetailRepository.save(detail);
        }

        attributes.addFlashAttribute("success", "Tạo phiếu mượn thành công");
        model.addAttribute("brs", borrowSlip);
        model.addAttribute("details", borrowDetailRepository.findByBorrowSlipId(borrowSlip.getId()));
        return "redirect:/quan-ly/borrowing?tab=history-tab";
    }

    @GetMapping("/borrow-slip/{id}")
    @ResponseBody
    public Map<String, Object> getBorrowSlip(@PathVariable String id) {
        BorrowSlip slip = borrowSlipRepository.findById(id).orElseThrow();
        Map<String, Object> map = new HashMap<>();
        map.put("slipId", slip.getId());
        map.put("readerName", slip.getLibraryCard().getReader().getName());
        map.put("borrowDate", slip.getBorrowDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        map.put("dueDate", slip.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        List<Map<String, Object>> books = slip.getDetails()
                .stream()
                .map(d -> {
                    Map<String, Object> b = new HashMap<>();
                    b.put("bookId", d.getBookCopy().getId());
                    b.put("title", d.getBookCopy().getBookTitle().getTitle());
                    b.put("author", d.getBookCopy().getBookTitle().getAuthor().getAuthorName());
                    b.put("category", d.getBookCopy().getBookTitle().getCategory().getCategoryName());
                    return b;
                }).toList();
        map.put("books", books);
        return map;
    }

    @GetMapping("/borrowing/export-excel")
    public void exportExcel(HttpServletResponse response) throws IOException {

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=lich-su-muon-tra.xlsx");

        List<BorrowDetail> list = borrowDetailRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Lich su muon tra");

        Row header = sheet.createRow(0);

        header.createCell(0).setCellValue("Mã phiếu");
        header.createCell(1).setCellValue("Độc giả");
        header.createCell(2).setCellValue("Tên sách");
        header.createCell(3).setCellValue("Ngày mượn");
        header.createCell(4).setCellValue("Hạn trả");
        header.createCell(5).setCellValue("Trạng thái");

        int rowNum = 1;
        for (BorrowDetail bd : list) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(bd.getBorrowSlip().getId());
            row.createCell(1).setCellValue(bd.getBorrowSlip().getLibraryCard()
                                    .getReader().getName());
            row.createCell(2).setCellValue(bd.getBookCopy().getBookTitle().getTitle());
            row.createCell(3).setCellValue(bd.getBorrowSlip().getBorrowDate().toString());
            row.createCell(4).setCellValue(bd.getBorrowSlip().getDueDate().toString());

            String status = bd.getStatus() == 0 ? "Đã trả" : (bd.getBorrowSlip().getDueDate().isBefore(LocalDate.now())
                            ? "Quá hạn" : "Đang mượn");
            row.createCell(5).setCellValue(status);
        }
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}
