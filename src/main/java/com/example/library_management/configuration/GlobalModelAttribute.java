package com.example.library_management.configuration;

import com.example.library_management.entity.Reader;
import com.example.library_management.entity.User;
import com.example.library_management.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@RequiredArgsConstructor
public class GlobalModelAttribute {
    BorrowDetailRepository borrowDetailRepository;
    ReaderRepository readerRepository;
    BorrowRequestRepository borrowRequestRepository;
    BorrowSlipRepository borrowSlipRepository;
    UserRepository userRepository;
    NotificationLogRepository notificationLogRepository;

    @ModelAttribute("readerBorrowingCount")
    public long countBorrowing(Principal principal) {
        if (principal == null) return 0;

        Reader reader = readerRepository.findByEmail(principal.getName()).orElse(null);
        if (reader == null || reader.getCards().isEmpty()) return 0;

        return borrowDetailRepository.countBorrowing(reader.getCards().get(0).getId());
    }

    @ModelAttribute("readerBorrowingRequest")
    public long countBorrowRequest(Principal principal) {
        if (principal == null) return 0;

        Reader reader = readerRepository.findByEmail(principal.getName()).orElse(null);
        if (reader == null || reader.getCards().isEmpty()) return 0;

        return borrowRequestRepository.countByCardIdAndStatus1(reader.getCards().get(0).getId());
    }

    @ModelAttribute("reader")
    public Reader showReaderProfile(Principal principal) {
        if (principal == null) return null;
        return readerRepository.findByEmail(principal.getName()).orElse(null);
    }

    @ModelAttribute("profileAdmin")
    public User showUserProfile(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByEmail(principal.getName()).orElse(null);
    }

    @ModelAttribute("countPendingRequests")
    public long countPendingRequests (Principal principal) {
        return borrowRequestRepository.countPendingRequests();
    }

    @ModelAttribute("overdueCount")
    public long overdueCount() {
        return borrowDetailRepository.countOverdueBooks();
    }

    @ModelAttribute("countPendingRenew")
    public long countPendingRenew() {
        return borrowSlipRepository.countPendingRenew();
    }

    @ModelAttribute("todayReminderMail")
    public Long todayReminderMail() {
        return notificationLogRepository.sumTodayByType("UPCOMING_DUE");
    }

    @ModelAttribute("todayOverdueMail")
    public Long todayOverdueMail() {
        return notificationLogRepository.sumTodayByType("OVERDUE");
    }

    @ModelAttribute("todayAutoCancel")
    public Long todayAutoCancel() {
        return notificationLogRepository.sumTodayByType("OVERREQUEST");
    }
}
