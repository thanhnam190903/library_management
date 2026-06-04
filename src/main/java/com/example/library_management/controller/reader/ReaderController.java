package com.example.library_management.controller.reader;

import com.example.library_management.entity.BookTitle;
import com.example.library_management.entity.BorrowDetail;
import com.example.library_management.entity.Reader;
import com.example.library_management.repository.BookCopyRepository;
import com.example.library_management.repository.BookTitleRepository;
import com.example.library_management.repository.BorrowDetailRepository;
import com.example.library_management.repository.ReaderRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
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
//        System.out.println("Size = " + featuredBooks.size());
        if (principal != null) {
            Reader reader = readerRepository.findByEmail(principal.getName()).orElse(null);
            data.put("reader", reader);
            data.put("borrowBooks", borrowDetailRepository.findBorrowingByCardId(reader.getCards().get(0).getId()));
        }
        data.put("borrowBooks", borrowDetailRepository.findBorrowingByCardId("00002"));
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
        Map<String, Object> data = new HashMap<>();
        data.put("id", book.getId());
        data.put("title", book.getTitle());
        data.put("coverImage", book.getCoverImage());
        data.put("note", book.getNote());
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
        data.put("hasDigitalBook", book.getDigitalBooks() != null && !book.getDigitalBooks().isEmpty());
        String shelfLocation = "Chưa cập nhật";
        if (book.getCopies().get(0).getShelfLocation() != null && !book.getCopies().isEmpty()) {
            shelfLocation = book.getCopies().get(0).getShelfLocation();
        }
        System.out.println("Shelf Location: " + shelfLocation);
        data.put("shelfLocation", shelfLocation);
        return data;
    }
}
