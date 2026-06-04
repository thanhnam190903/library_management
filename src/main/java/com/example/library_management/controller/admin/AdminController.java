package com.example.library_management.controller.admin;


import com.example.library_management.entity.User;
import com.example.library_management.repository.BookCopyRepository;
import com.example.library_management.repository.BorrowDetailRepository;
import com.example.library_management.repository.ReaderRepository;
import com.example.library_management.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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

    @GetMapping("/dashboard")
    public String index(Model model){
        Map<String, Object> data = new HashMap<>();
        LocalDate date = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("EEEE, dd 'tháng' M 'năm' yyyy", new Locale("vi", "VN"));
        String result = date.format(formatter);
        data.put("title", result);
        data.put("sub","Bảng điều khiển" );
        data.put("activePage", "dashboard");
        data.put("totalBook", bookCopyRepository.count());
        data.put("totalReader", readerRepository.count());
        data.put("totalBorrowing", borrowDetailRepository.countAllBorrowing());
        data.put("totalOverdue", borrowDetailRepository.overdueAllBooks());
        model.addAllAttributes(data);
        return "admin/index";
    }



}
