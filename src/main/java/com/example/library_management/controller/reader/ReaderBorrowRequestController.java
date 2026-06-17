package com.example.library_management.controller.reader;

import com.example.library_management.entity.*;
import com.example.library_management.repository.*;
import com.example.library_management.service.IdGeneratorService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/home")
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@RequiredArgsConstructor
public class ReaderBorrowRequestController {
    BorrowRequestRepository borrowRequestRepository;
    ReaderRepository readerRepository;
    BookTitleRepository bookTitleRepository;
    BookCopyRepository bookCopyRepository;
    IdGeneratorService idGeneratorService;
    BorrowDetailRepository borrowDetailRepository;

    @GetMapping("/borrow-request")
    public String pageBorrowRequest(Model model, Principal principal){
        Reader reader = readerRepository.findByEmail(principal.getName()).orElseThrow();
        if (reader != null){
            model.addAttribute("requestBorrow",
                    borrowRequestRepository.findByCardIdAndStatusNotZero(reader.getCards().get(0).getId()));
        }
        model.addAttribute("activePage","request");
        return "reader/reader-request";
    }

    @GetMapping(value = "/borrow-request",params = "id")
    public String cancelRequest(@RequestParam("id") String id){
        System.out.println(id);
        borrowRequestRepository.updateStatus(id,0);
        return "redirect:/home/borrow-request";
    }

    @GetMapping("/reader-find-book/{id}")
    @ResponseBody
    public Map<String, Object> getBooks(@PathVariable String id){
        BookTitle book = bookTitleRepository.findById(id).orElseThrow();
        Map<String, Object> data = new HashMap<>();
        data.put("id", book.getId());
        data.put("title", book.getTitle());
        data.put("author", book.getAuthor() != null ? book.getAuthor().getAuthorName() : "");
        data.put("availableCopies",bookCopyRepository.countAvailableBooks(book.getId()));
        data.put("shLocation",book.getCopies().get(0).getShelfLocation());
        return data;
    }

    @PostMapping("/borrow-request")
    public String readerBorrowRequest(@RequestParam("bookTitle") String bookTitleId, @RequestParam("requestDays") Integer requestDays,
                                      @RequestParam("note") String note,Principal principal, RedirectAttributes attributes){
        try {
            Reader reader = readerRepository.findByEmail(principal.getName()).orElseThrow();
            LibraryCard card = reader.getCards().get(0);
            long borrowingCount = borrowDetailRepository.countBorrowing(card.getId());
            if (borrowingCount >= card.getMaxBooksAllowed()) {
                attributes.addFlashAttribute("error", "Bạn đã mượn vượt quá số sách cho phép " +
                        "(" + borrowingCount + "/" + card.getMaxBooksAllowed() + ")");
                return "redirect:/home/borrow-request";
            }
            List<BookCopy> books = bookCopyRepository.findAvailableBooks(bookTitleId);
            if (books.isEmpty()) {
                attributes.addFlashAttribute("error", "Không còn sách khả dụng");
                return "redirect:/home/borrow-request";
            }
            BookCopy book = books.get(0);
            book.setCirculationStatus("reserved");
            bookCopyRepository.save(book);
            BorrowRequest req = BorrowRequest.builder()
                    .id(idGeneratorService.generate("BORROW_REQUEST", "BRR"))
                    .libraryCard(card)
                    .bookCopy(book)
                    .note(note)
                    .requestDays(requestDays)
                    .status(1)
                    .build();
            borrowRequestRepository.save(req);
            attributes.addFlashAttribute("success", "Gửi yêu cầu mượn sách thành công");
            return "redirect:/home/borrow-request";

        } catch (Exception e) {
            attributes.addFlashAttribute("error", "Có lỗi xảy ra");
            return "redirect:/home/borrow-request";
        }
    }
}
