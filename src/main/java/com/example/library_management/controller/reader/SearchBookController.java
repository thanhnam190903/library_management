package com.example.library_management.controller.reader;

import com.example.library_management.entity.BookTitle;
import com.example.library_management.entity.Category;
import com.example.library_management.entity.Reader;
import com.example.library_management.repository.BookCopyRepository;
import com.example.library_management.repository.BookTitleRepository;
import com.example.library_management.repository.CategoryRepository;
import com.example.library_management.repository.ReaderRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/home")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE,makeFinal = true)
@RequiredArgsConstructor
public class SearchBookController {
    CategoryRepository categoryRepository;
    BookTitleRepository bookTitleRepository;
    BookCopyRepository bookCopyRepository;
    ReaderRepository readerRepository;

    @GetMapping("/search")
    public String searchPage(Model model){
        Map<String, Object> data = new HashMap<>();
        List<Category> categories = categoryRepository.findCategoriesHasBooks();
        data.put("categories", categories);
        data.put("activePage", "search");
        model.addAllAttributes(data);
        return "reader/search-book";
    }
    @GetMapping("/api/search")
    @ResponseBody
    public List<Map<String, Object>> searchBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String availability,
            @RequestParam(required = false) String format, Principal principal) {

        boolean cardStatus = false;
        if (principal != null) {
            Optional<Reader> readerOpt = readerRepository.findByEmail(principal.getName());
            if (readerOpt.isPresent()) {
                Reader reader = readerOpt.get();
                if (reader.getCards() != null && !reader.getCards().isEmpty()) {
                    cardStatus = reader.getCards().get(0).isStatus();
                }
            }
        }
        boolean finalCardStatus = cardStatus;
        return bookTitleRepository
                .searchBooks(keyword, category, availability, format, PageRequest.of(0, 12))
                .stream()
                .map(b -> {
                    Map<String, Object> m = new HashMap<>();
                    long availableCopies = bookCopyRepository.countAvailableBooks(b.getId());
                    m.put("id", b.getId());
                    m.put("title", b.getTitle());
                    m.put("coverImage", b.getCoverImage());
                    m.put("authorName", b.getAuthor().getAuthorName());
                    m.put("availableCopies", availableCopies);
                    m.put("cardStatus", finalCardStatus);
                    m.put("hasBookCopy",b.getCopies() != null && !b.getCopies().isEmpty());
                    m.put("hasDigital",b.getDigitalBooks() != null && !b.getDigitalBooks().isEmpty());
                    return m;
                })
                .collect(Collectors.toList());
    }

}
