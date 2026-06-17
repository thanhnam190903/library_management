package com.example.library_management.controller.admin;


import com.example.library_management.entity.BookTitle;
import com.example.library_management.entity.BorrowDetail;
import com.example.library_management.entity.User;
import com.example.library_management.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@RequestMapping("/quan-ly")
public class AdminController {
    ReaderRepository readerRepository;
    BookCopyRepository bookCopyRepository;
    BorrowDetailRepository borrowDetailRepository;
    BookTitleRepository bookTitleRepository;
    LibraryCardRepository libraryCardRepository;

    @GetMapping("/dashboard")
    public String index(Model model){
        Map<String, Object> data = new HashMap<>();
        LocalDate date = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("EEEE, dd 'tháng' M 'năm' yyyy", new Locale("vi", "VN"));
        String result = date.format(formatter);
        List<BookTitle> books = borrowDetailRepository
                .findTopRecentBorrowedBooks(PageRequest.of(0,5));
        Map<String, Long> availableMap = new HashMap<>();
        for (BookTitle b : books) {
            long available = bookCopyRepository
                    .countAvailableBooks(b.getId());
            availableMap.put(b.getId(), available);
        }
        data.put("title", result);
        data.put("sub","Bảng điều khiển" );
        data.put("activePage", "dashboard");
        data.put("totalBook", bookTitleRepository.countByDeletedFalse());
        data.put("totalReader", readerRepository.count());
        data.put("totalBorrowing", borrowDetailRepository.countAllBorrowing());
        data.put("totalOverdue", borrowDetailRepository.overdueAllBooks());
        data.put("available",availableMap);
        data.put("listBook",books);
        data.put("topReader",libraryCardRepository
                .findTopBorrowers(PageRequest.of(0,5)));
        model.addAllAttributes(data);
        return "admin/index";
    }
   
}
