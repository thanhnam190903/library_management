package com.example.library_management.service;

import com.example.library_management.entity.*;
import com.example.library_management.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class OverdueSchedulerService {
    BorrowDetailRepository borrowDetailRepository;
    BorrowSlipRepository borrowSlipRepository;
    BorrowRequestRepository borrowRequestRepository;
    NotificationLogRepository notificationLogRepository;
    BookCopyRepository bookCopyRepository;
    EmailService emailService;

    @Scheduled(cron = "0 0 8 * * *")
    public void sendDueReminder(){

        LocalDate today = LocalDate.now();
        List<BorrowSlip> upcoming = borrowSlipRepository
                .findUpcomingDue(today, today.plusDays(2));
        int upcomingCount = 0;
        for (BorrowSlip slip : upcoming) {
            String email = slip.getLibraryCard().getReader().getEmail();

            emailService.sendEmail(
                    email,
                    "Nhắc hạn trả sách",
                    "Phiếu mượn " + slip.getId() +
                            " sắp đến hạn trả vào " + slip.getDueDate()+
                    "\nVui lòng đến trả sớm để khng bị phạt!"
            );
            upcomingCount++;
        }
        notificationLogRepository.save(
                NotificationLog.builder()
                        .notificationType("UPCOMING_DUE")
                        .totalSent(upcomingCount)
                        .createdAt(LocalDate.now())
                        .build()
        );


        List<BorrowDetail> overdueBooks = borrowDetailRepository.findOverdueBooks();
        int overdueCount = 0;
        for(BorrowDetail detail : overdueBooks){

            String email = detail.getBorrowSlip().getLibraryCard().getReader().getEmail();
            String readerName = detail.getBorrowSlip().getLibraryCard().getReader().getName();
            String title = detail.getBookCopy().getBookTitle().getTitle();
            emailService.sendEmail(email, "Thông báo sách quá hạn",
                    "Xin chào " + readerName + ", sách '" + title + "' đã quá hạn trả." +
                            "\n Vui lòng trả sách càng sớm càng tốt để tránh bị phạt. Cảm ơn bạn đã sử dụng dịch vụ thư viện!");
            overdueCount++;
        }
        notificationLogRepository.save(
                NotificationLog.builder()
                        .notificationType("OVERDUE")
                        .totalSent(overdueCount)
                        .createdAt(LocalDate.now())
                        .build()
        );

        LocalDateTime expiredTime = LocalDateTime.now().minusDays(2);
        List<BorrowRequest> borrowRequests = borrowRequestRepository.findExpiredConfirmedRequests(expiredTime);
        int overRequestDay = 0;
        for (BorrowRequest request : borrowRequests) {
            request.setStatus(3);
            request.setNote("Hệ thống đã tự động hủy đoen của bạn do không đến nhận sách đúng hạn!");
            request.setRequestDateTime(LocalDateTime.now());
            borrowRequestRepository.save(request);
            BookCopy bookCopy = bookCopyRepository.findById(request.getBookCopy().getId()).orElseThrow();
            bookCopy.setCirculationStatus("available");
            bookCopyRepository.save(bookCopy);
            overRequestDay++;
        }
        notificationLogRepository.save(
                NotificationLog.builder()
                        .notificationType("OVERREQUEST")
                        .totalSent(overRequestDay)
                        .createdAt(LocalDate.now())
                        .build()
        );
    }
}
