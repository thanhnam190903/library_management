package com.example.library_management.controller.reader;

import com.example.library_management.entity.BorrowDetail;
import com.example.library_management.entity.Reader;
import com.example.library_management.repository.BorrowDetailRepository;
import com.example.library_management.repository.ReaderRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
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

    @GetMapping("/history")
    @ResponseBody
    public List<Map<String, Object>> getHistory(Principal principal){

        Reader reader = readerRepository.findByEmail(principal.getName()).orElse(null);

        List<BorrowDetail> details = borrowDetailRepository.findBorrowingByCardId(reader.getCards().get(0).getId());

        return details.stream().map(detail -> {

            Map<String, Object> map = new HashMap<>();

            map.put("title", detail.getBookCopy().getBookTitle().getTitle());
            map.put("borrowDate", detail.getBorrowSlip().getBorrowDate());
            map.put("dueDate", detail.getBorrowSlip().getDueDate());
            map.put("returnDate", detail.getReturnDate());
            map.put("oldDueDate", detail.getBorrowSlip().getOldDueDate());

            String status;

            if(slip.getReturnDate() != null){

                if(slip.getReturnDate().after(slip.getDueDate())){
                    status = "late";
                }else{
                    status = "returned";
                }

            }else if(Boolean.TRUE.equals(slip.getRenewed())){
                status = "renewed";
            }else{
                status = "borrowing";
            }

            map.put("status", status);

            return map;

        }).toList();
    }
}
