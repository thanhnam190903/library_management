package com.example.library_management.controller.reader;

import com.example.library_management.entity.BorrowDetail;
import com.example.library_management.entity.BorrowSlip;
import com.example.library_management.entity.Reader;
import com.example.library_management.repository.BorrowDetailRepository;
import com.example.library_management.repository.BorrowSlipRepository;
import com.example.library_management.repository.ReaderRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/home")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE,makeFinal = true)
@RequiredArgsConstructor
public class HistoryController {
    BorrowDetailRepository borrowDetailRepository;
    ReaderRepository readerRepository;
    BorrowSlipRepository borrowSlipRepository;

    @GetMapping("/history")
    public String historyPage(Model model, Principal principal) {
        Map<String, Object> data = new HashMap<>();
        Reader reader = readerRepository.findByEmail(principal.getName()).orElse(null);
        List<BorrowDetail> details =
                borrowDetailRepository.findBorrowByCardId(reader.getCards().get(0).getId());
        List<BorrowDetail> lateList = details.stream()
                .filter(d ->
                        d.getStatus() == 0
                                && d.getReturnDate() != null
                                && d.getReturnDate().isAfter(d.getBorrowSlip().getDueDate())
                )
                .toList();
        List<BorrowDetail> returnedList = details.stream()
                .filter(d ->
                        d.getStatus() == 0 &&
                                d.getReturnDate() != null &&
                                (d.getReturnDate().isBefore(d.getBorrowSlip().getDueDate())
                                        || d.getReturnDate().isEqual(d.getBorrowSlip().getDueDate()))
                )
                .toList();
        List<BorrowDetail> renewedList = borrowDetailRepository.findByCardIdAndRenewedTrue("00002");
        data.put("detailsAll", details);
        data.put("lateList", lateList);
        data.put("returnedList", returnedList);
        data.put("renewedList", renewedList);
        data.put("activePage", "history");

        model.addAllAttributes(data);

        return "reader/history";
    }
    @GetMapping("/my-book")
    public String myBook(Model model, Principal principal){
        Map<String, Object> data = new HashMap<>();
        Reader reader = readerRepository.findByEmail(principal.getName()).orElseThrow();
        List<BorrowDetail> details = borrowDetailRepository
                .findBorrowingByCardId(reader.getCards().get(0).getId());
        data.put("details", details);
        data.put("countBorrowing", details.size());
        data.put("activePage", "my-book");
        model.addAllAttributes(data);
        return "reader/my-book";
    }
    @PostMapping("/renew-book")
    public String renewBook(@RequestParam String borrowSlipId,
                            @RequestParam Integer renewDays,
                            RedirectAttributes redirectAttributes) {
        BorrowSlip borrowSlip = borrowSlipRepository.findById(borrowSlipId).orElseThrow();
        borrowSlip.setRenewed(true);
        borrowSlip.setStatusRenewed(0);
        borrowSlip.setRenewDays(renewDays);
        borrowSlipRepository.save(borrowSlip);
        redirectAttributes.addFlashAttribute(
                "success",
                "Đã gửi yêu cầu gia hạn " + renewDays + " ngày"
        );
        return "redirect:/home/my-book";
    }
}
