package com.example.library_management.controller.librarian;

import com.example.library_management.entity.*;
import com.example.library_management.repository.*;
import com.example.library_management.service.EmailService;
import com.example.library_management.service.GeneratorQRService;
import com.example.library_management.service.IdGeneratorService;
import jakarta.persistence.GeneratedValue;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/quan-ly")
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@RequiredArgsConstructor
public class BorrowRequestManagementController {

    BorrowRequestRepository borrowRequestRepository;
    EmailService emailService;
    GeneratorQRService generatorQRService;
    BookCopyRepository bookCopyRepository;
    IdGeneratorService idGeneratorService;
    LibraryCardRepository libraryCardRepository;
    BorrowDetailRepository borrowDetailRepository;
    BorrowSlipRepository borrowSlipRepository;

    @GetMapping("/reserve")
    public String showPageReserve( @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Integer status,Model model){
        Map<String, Object> data = new HashMap<>();
        data.put("borrowRequest",borrowRequestRepository.findPriorityRequests(keyword,status));
        data.put("title", "Danh mục sách đặt trước");
        data.put("keyword",keyword);
        data.put("sub", "Quản lý đặt sách");
        data.put("activePage","reserves");
        model.addAllAttributes(data);
        return "librarian/reserve";
    }

    @GetMapping("/accept-request")
    public String acceptRequest(@RequestParam("id") String id, RedirectAttributes attributes) throws Exception{
        System.out.println("=== ACCEPT REQUEST ===");
        System.out.println("ID = " + id);
        BorrowRequest request = borrowRequestRepository.findById(id).orElse(null);
        if (request == null) {
            attributes.addFlashAttribute("error", "Không tìm thấy yêu cầu mượn!");
            return "redirect:/qltv/quan-ly/reserve";
        }
        request.setStatus(2);
        request.setRequestDateTime(LocalDateTime.now());
        borrowRequestRepository.save(request);
        byte[] qrBytes = generatorQRService.generateQr(request.getId());
        String email = request.getLibraryCard().getReader().getEmail();
        emailService.sendEmailWithQr( email, "Thông báo mượn sách", request.getId(), qrBytes);
        attributes.addFlashAttribute("success", "Đã xác nhận yêu cầu mượn thành công!");
        return "redirect:/quan-ly/reserve";
    }

    @GetMapping("/reject-request")
    public String rejectRequest(@RequestParam String id, RedirectAttributes attributes) {
        System.out.println(id);
        BorrowRequest request = borrowRequestRepository.findById(id).orElse(null);
        if (request == null) {
            attributes.addFlashAttribute("error", "Không tìm thấy yêu cầu mượn!");
            return "redirect:/quan-ly/reserve";
        }
        request.setStatus(3);
        request.setRequestDateTime(LocalDateTime.now());
        borrowRequestRepository.save(request);
        BookCopy bookCopy = bookCopyRepository.findById(request.getBookCopy().getId()).orElseThrow();
        bookCopy.setCirculationStatus("available");
        bookCopyRepository.save(bookCopy);
        attributes.addFlashAttribute("success", "Đã từ chối yêu cầu mượn thành công!");
        return "redirect:/qltv/quan-ly/reserve";
    }

    @GetMapping("/create-borrow")
    public String createBorrow(@RequestParam String id, RedirectAttributes attributes){

        BorrowRequest borrowRequest = borrowRequestRepository.findById(id).orElseThrow();
        borrowRequest.setStatus(4);
        borrowRequestRepository.save(borrowRequest);
        BorrowSlip borrowSlip = BorrowSlip.builder()
                    .id(idGeneratorService.generate("BORROW_SLIP", "BRS"))
                    .libraryCard(borrowRequest.getLibraryCard())
                    .borrowDate(LocalDate.now())
                    .renewed(false)
                    .dueDate(LocalDate.now().plusDays(borrowRequest.getRequestDays()))
                    .build();
        LibraryCard card = libraryCardRepository
                    .findById(borrowRequest.getLibraryCard().getId()).orElseThrow();
        card.setTotalBorrow(card.getTotalBorrow()+1);
        borrowSlipRepository.save(borrowSlip);
        libraryCardRepository.save(card);
        BookCopy bookCopy = bookCopyRepository
                    .findById(borrowRequest.getBookCopy().getId()).orElseThrow();
        bookCopy.setCirculationStatus("borrowed");
        bookCopyRepository.save(bookCopy);
        BorrowDetail detail = BorrowDetail.builder()
                    .id(idGeneratorService.generate("BORROW_DETAIL", "BRD"))
                    .borrowSlip(borrowSlip)
                    .bookCopy(bookCopy)
                    .status(1)
                    .build();
        borrowDetailRepository.save(detail);
        attributes.addFlashAttribute("success", "Tạo phiếu mượn thành công");
        return "redirect:/quan-ly/borrowing?tab=history-tab";

    }
}
