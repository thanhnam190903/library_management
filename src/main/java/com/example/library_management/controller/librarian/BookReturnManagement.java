package com.example.library_management.controller.librarian;

import com.example.library_management.entity.*;
import com.example.library_management.repository.*;
import com.example.library_management.service.EmailService;
import com.example.library_management.service.IdGeneratorService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@RequestMapping("/quan-ly")
public class BookReturnManagement {
    BookCopyRepository bookCopyRepository;
    BorrowDetailRepository borrowDetailRepository;
    BorrowSlipRepository borrowSlipRepository;
    LibraryCardRepository libraryCardRepository;
    LibararyRuleRepository libararyRuleRepository;
    EmailService mailService;

    @GetMapping("/find-return-slip")
    @ResponseBody
    public Map<String,Object> findReturnSlip(@RequestParam String keyword){
        List<BorrowDetail> details = borrowDetailRepository.findBorrowingBooks(keyword);
        Map<String, Object> res = new HashMap<>();
        if (details == null || details.isEmpty()) {
            res.put("found", false);
            return res;
        }
        BorrowSlip slip = details.get(0).getBorrowSlip();
        long overdueDays = ChronoUnit.DAYS.between(slip.getDueDate(), LocalDate.now());
        overdueDays = Math.max(overdueDays, 0);

        double overdueRate = libararyRuleRepository.findByRuleKey("OVERDUE_FINE_PER_DAY")
                .orElseThrow()
                .getRuleValue();
        System.out.println("quá hạn:" + overdueRate);
        double lightDamageFine = libararyRuleRepository.findByRuleKey("LIGHT_DAMAGE_FINE")
                .orElseThrow()
                .getRuleValue();
        System.out.println("Hỏng nhẹ:"+ lightDamageFine);

        double fine = overdueDays * overdueRate;
        res.put("found", true);
        res.put("borrowDate", slip.getBorrowDate());
        res.put("readerName", slip.getLibraryCard().getReader().getName());
        res.put("dueDate", slip.getDueDate());
        res.put("overdueDays", overdueDays);
        res.put("fine", fine);
        res.put("lightDamageFine",lightDamageFine);
        List<Map<String, Object>> list = new ArrayList<>();
        for (BorrowDetail d : details) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", d.getId());
            item.put("title", d.getBookCopy().getBookTitle().getTitle());
            item.put("priceBook", d.getBookCopy().getBookTitle().getPrice());
            list.add(item);
        }
        res.put("details", list);
        return res;
    }

    @PostMapping("/update-rule")
    public String updateRule(@RequestParam("rule-id") Integer id,
                             @RequestParam("ruleValue") Double ruleValue, RedirectAttributes attributes){
        LibraryRule libraryRule = libararyRuleRepository.findById(id).orElseThrow();
        libraryRule.setRuleValue(ruleValue);
        libararyRuleRepository.save(libraryRule);
        attributes.addFlashAttribute("s", "Thay đổi phí phạt thành công!");
        return "redirect:/quan-ly/borrowing?tab=return-tab";
    }

    @PostMapping("/return-book")
    public String returnBook(@RequestParam List<String> detailIds, @RequestParam List<String> bookConditions,
                             @RequestParam List<Double> fineAmounts, RedirectAttributes attributes) {

        BorrowSlip slip = null;
        double totalPrice = 0;
        for (int i = 0; i < detailIds.size(); i++) {

            BorrowDetail detail = borrowDetailRepository.findById(detailIds.get(i)).orElseThrow();
            if (slip == null) {
                slip = detail.getBorrowSlip();
            }
            detail.setStatus(0);
            detail.setReturnDate(LocalDate.now());
            detail.setReturnHour(LocalTime.now());
            Double damageFine = fineAmounts.get(i);

            if (damageFine == null) damageFine = 0.0;
            detail.setFineAmount(damageFine);
            totalPrice += damageFine;
            borrowDetailRepository.save(detail);
            BookCopy bookCopy = detail.getBookCopy();
            applyBookCondition(bookCopy, bookConditions.get(i));
            bookCopyRepository.save(bookCopy);
        }
        long overdueDays = ChronoUnit.DAYS.between(slip.getDueDate(), LocalDate.now());
        overdueDays = Math.max(overdueDays, 0);
        if (overdueDays > 0) {
            LibraryCard card = slip.getLibraryCard();
            Integer overdueCount = card.getOverdueCount();
            if (overdueCount == null) {
                overdueCount = 0;
            }
            card.setOverdueCount(overdueCount + 1);
            libraryCardRepository.save(card);
        }
        double overdueRate = libararyRuleRepository.findByRuleKey("OVERDUE_FINE_PER_DAY")
                .orElseThrow()
                .getRuleValue();
        double overdueFine = overdueDays * overdueRate;
        double totalFine = overdueFine + totalPrice;

        slip.setTotalFine(totalFine);
        borrowSlipRepository.save(slip);
        attributes.addFlashAttribute("success", "Trả sách thành công!");
        return "redirect:/quan-ly/borrowing?tab=history-tab";
    }

    @GetMapping("/send-overdue-all")
    public String sendOverdueAll(RedirectAttributes attributes){

        List<BorrowDetail> overdueBooks = borrowDetailRepository.findOverdueBooks();

        int sent = 0;
        for(BorrowDetail detail : overdueBooks){
            try{
                String email = detail.getBorrowSlip().getLibraryCard().getReader().getEmail();
                String readerName = detail.getBorrowSlip().getLibraryCard().getReader().getName();
                String title = detail.getBookCopy().getBookTitle().getTitle();
                long overdueDays = ChronoUnit.DAYS.between(detail.getBorrowSlip().getDueDate(), LocalDate.now());

                mailService.sendEmail(email, "Thông báo sách quá hạn", "Xin chào " + readerName
                        + ", sách '" + title + "' đã quá hạn " + overdueDays + " ngày."
                        + " Vui lòng trả sách càng sớm càng tốt để tránh bị phạt." +
                        " Cảm ơn bạn đã sử dụng dịch vụ thư viện!");
                sent++;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        attributes.addFlashAttribute("success", "Đã gửi email cho " + sent + " trường hợp");
        return "redirect:/quan-ly/borrowing?tab=overdue-tab";
    }

    @GetMapping("/send-overdue")
    public String sendOverdue(@RequestParam String id, RedirectAttributes attributes){

        BorrowDetail detail = borrowDetailRepository.findById(id).orElseThrow();
        String email = detail.getBorrowSlip().getLibraryCard().getReader().getEmail();
        String title = detail.getBookCopy().getBookTitle().getTitle();
        mailService.sendEmail(email, "Thông báo quá hạn",
                "Sách '" + title + "' đã quá hạn trả."
                        + " Vui lòng trả sách càng sớm càng tốt để tránh bị phạt. Cảm ơn bạn đã sử dụng dịch vụ thư viện!"
        );
        attributes.addFlashAttribute("success", "Đã gửi email nhắc nhở");
        return "redirect:/quan-ly/borrowing?tab=overdue-tab";
    }

    private void applyBookCondition(BookCopy bookCopy, String condition) {
        switch (condition) {
            case "light" -> {
                bookCopy.setBookCondition("Hư hỏng nhẹ");
                bookCopy.setCirculationStatus("available");
            }
            case "heavy" -> {
                bookCopy.setBookCondition("Hư hỏng nặng");
                bookCopy.setCirculationStatus("available");
            }
            case "lost" -> {
                bookCopy.setBookCondition("Mất");
                bookCopy.setCirculationStatus("lost");
                bookCopy.setStatus(false);
            }
            default -> bookCopy.setCirculationStatus("available");
        }
    }

    @GetMapping("/renew-approve/{id}")
    public String approveRenew(@PathVariable String id, RedirectAttributes redirectAttributes) {

        BorrowSlip borrowSlip = borrowSlipRepository.findById(id).orElseThrow();
        borrowSlip.setStatusRenewed(1);
        borrowSlip.setOldDueDate(borrowSlip.getDueDate());
        borrowSlip.setDueDate(borrowSlip.getDueDate().plusDays(borrowSlip.getRenewDays()));
        borrowSlipRepository.save(borrowSlip);
        redirectAttributes.addFlashAttribute("succe", "Gia hạn thành công");
        return "redirect:/quan-ly/borrowing?tab=renewed-tab";
    }
    @GetMapping("/renew-reject/{id}")
    public String rejectRenew(@PathVariable String id,
                              RedirectAttributes redirectAttributes) {
        BorrowSlip borrowSlip = borrowSlipRepository.findById(id).orElseThrow();
        borrowSlip.setStatusRenewed(2);
        borrowSlipRepository.save(borrowSlip);
        redirectAttributes.addFlashAttribute("succe", "Đã từ chối yêu cầu gia hạn");
        return "redirect:/quan-ly/borrowing?tab=renewed-tab";
    }
}
