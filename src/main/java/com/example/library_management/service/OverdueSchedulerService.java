package com.example.library_management.service;

import com.example.library_management.entity.BorrowDetail;
import com.example.library_management.repository.BorrowDetailRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class OverdueSchedulerService {
    BorrowDetailRepository borrowDetailRepository;
    EmailService emailService;

    @Scheduled(cron = "0 0 8 * * *")
    public void sendOverdueMail(){
        List<BorrowDetail> overdueBooks = borrowDetailRepository.findOverdueBooks();
        for(BorrowDetail detail : overdueBooks){

            String email = detail.getBorrowSlip().getLibraryCard().getReader().getEmail();
            String readerName = detail.getBorrowSlip().getLibraryCard().getReader().getName();
            String title = detail.getBookCopy().getBookTitle().getTitle();
            emailService.sendEmail(email, "Thông báo sách quá hạn",
                    "Xin chào " + readerName + ", sách '" + title + "' đã quá hạn trả." +
                            " Vui lòng trả sách càng sớm càng tốt để tránh bị phạt. Cảm ơn bạn đã sử dụng dịch vụ thư viện!");
        }
    }
}
