package com.example.library_management.controller.librarian;

import com.example.library_management.entity.*;
import com.example.library_management.repository.BookCopyRepository;
import com.example.library_management.repository.BorrowDetailRepository;
import com.example.library_management.repository.BorrowSlipRepository;
import com.example.library_management.repository.LibraryCardRepository;
import com.example.library_management.service.IdGeneratorService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class BorrowManagementController {
    LibraryCardRepository libraryCardRepository;
    BookCopyRepository bookCopyRepository;
    BorrowDetailRepository borrowDetailRepository;
    IdGeneratorService idGeneratorService;
    BorrowSlipRepository borrowSlipRepository;

    @GetMapping("/borrowing")
    public String showBorrowing(Model model) {
        model.addAttribute("title", "Quản lý lưu hành");
        model.addAttribute("sub", "Cho mượn · Trả sách · Quá hạn");
        model.addAttribute("activePage", "borrowing");
        model.addAttribute("borrowTitle", borrowDetailRepository.findAll());
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
                                  @RequestParam String bookId, Model model, RedirectAttributes attributes) {

        LibraryCard card = libraryCardRepository.findById(cardId).orElse(null);
        long borrowingCount = borrowDetailRepository.countBorrowing(cardId);
        if (borrowingCount >= card.getMaxBooksAllowed()) {
            attributes.addFlashAttribute("error", "Đã vượt quá số sách được mượn");
            return "redirect:/qltv/borrowing";
        }
        card.setTotalBorrow(card.getTotalBorrow() + 1);
        libraryCardRepository.save(card);
        borrowSlip.setId(idGeneratorService.generate("BORROW_SLIP", "BRS"));
        borrowSlip.setLibraryCard(card);
        borrowSlipRepository.save(borrowSlip);
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
        attributes.addFlashAttribute("success", "Tạo phiếu mượn thành công!");
        return "redirect:/borrowing?tab=history";
    }
}
