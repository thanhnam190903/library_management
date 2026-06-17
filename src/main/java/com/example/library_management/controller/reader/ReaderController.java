package com.example.library_management.controller.reader;

import com.example.library_management.entity.BookTitle;
import com.example.library_management.entity.BorrowDetail;
import com.example.library_management.entity.DigitalBook;
import com.example.library_management.entity.Reader;
import com.example.library_management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@FieldDefaults(level = lombok.AccessLevel.PRIVATE,makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/home")
public class ReaderController {

    BorrowDetailRepository borrowDetailRepository;
    BookCopyRepository bookCopyRepository;
    BookTitleRepository bookTitleRepository;
    ReaderRepository readerRepository;
    DigitalBookRepository digitalBookRepository;

    @GetMapping()
    public String home(Principal principal,Model model){

        Map<String, Object> data = new HashMap<>();
        List<BookTitle> featuredBooks =
                borrowDetailRepository.findTopFeaturedBooks(PageRequest.of(0, 4));
        Map<String, Long> availableMap = new HashMap<>();
        for (BookTitle book : featuredBooks) {
            availableMap.put(
                    book.getId(),
                    bookCopyRepository.countAvailableBooks(book.getId())
            );
        }
        if (principal != null) {
            Reader reader = readerRepository.findByEmail(principal.getName()).orElse(null);
            model.addAttribute("cardStatus",
                    (reader.getCards() != null && !reader.getCards().isEmpty())
                            ? reader.getCards().get(0).isStatus()
                            : false
            );
            data.put("borrowBooks", borrowDetailRepository.findBorrowingByCardId(reader.getCards().get(0).getId()));
        }
        data.put("bookFeature", featuredBooks);
        data.put("availableMap", availableMap);
        data.put("activePage", "home");
        model.addAllAttributes(data);
        return "reader/home";
    }

    @GetMapping("/book-featured/{id}")
    @ResponseBody
    public Map<String, Object> getFeaturedBook(@PathVariable String id) {

        BookTitle book = bookTitleRepository.findById(id).orElseThrow();
        DigitalBook latest = book.getDigitalBooks().stream()
                .max(Comparator.comparing(DigitalBook::getCreatedAt))
                .orElse(null);
        Map<String, Object> data = new HashMap<>();
        data.put("id", book.getId());
        data.put("title", book.getTitle());
        data.put("coverImage", book.getCoverImage());
        data.put("note", book.getNote());
        data.put("fileURL", latest != null ? latest.getFileUrl() : null);
        data.put("digitalId", latest != null ? latest.getId() : null);
        data.put("author", book.getAuthor() != null
                        ? book.getAuthor().getAuthorName() : "");
        data.put("category", book.getCategory() != null ? book.getCategory().getCategoryName() : "");
        data.put("language", switch (book.getLanguage()) {
            case "vn" -> "Tiếng Việt";
            case "us" -> "Tiếng Anh";
            case "phap" -> "Tiếng Pháp";
            case "nhat" -> "Tiếng Nhật";
            case "china" -> "Tiếng Trung";
            default -> "Không xác định";
        });
        data.put("availableCopies",
                bookCopyRepository.countAvailableBooks(book.getId()));
        data.put("totalCopies", book.getQuantity());
        data.put("hasCopyBook",book.getCopies() != null && !book.getCopies().isEmpty());
        data.put("hasDigitalBook", book.getDigitalBooks() != null && !book.getDigitalBooks().isEmpty());
        String shelfLocation = "Chưa cập nhật";
        if (!book.getCopies().isEmpty() && book.getCopies().get(0).getShelfLocation() != null ) {
            shelfLocation = book.getCopies().get(0).getShelfLocation();
        }
        System.out.println("Shelf Location: " + shelfLocation);
        data.put("shelfLocation", shelfLocation);
        return data;
    }

    @PostMapping("/increase-view/{id}")
    public ResponseEntity<?> increaseView(@PathVariable String id) {

        DigitalBook book = digitalBookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found"));
        if (book.getViewCount() == null) {
            book.setViewCount(0);
        }
        book.setViewCount(book.getViewCount() + 1);
        digitalBookRepository.save(book);
        return ResponseEntity.ok(book.getViewCount());
    }
}
