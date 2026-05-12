package com.example.library_management.controller.librarian;

import com.example.library_management.entity.*;
import com.example.library_management.repository.*;
import com.example.library_management.service.IdGeneratorService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class BookController {
    BookTitleRepository bookTitleRepository;
    CategoryRepository categoryRepository;
    AuthorRepository authorRepository;
    PublisherRepository publisherRepository;
    IdGeneratorService idGeneratorService;
    BookCopyRepository bookCopyRepository;

    @GetMapping("/books")
    public String showBooks(Model model){
        model.addAttribute("title","Danh mục sách");
        model.addAttribute("sub","Quản lý kho sách");
        model.addAttribute("activePage","books");
        model.addAttribute("books",bookTitleRepository.getAllBookTitle());
        model.addAttribute("categories",categoryRepository.getAllCategory());
        model.addAttribute("book",new BookTitle());
        return "librarian/book";
    }
    @GetMapping("/get-category")
    @ResponseBody
    public List<Category> getCategory(@RequestParam String parent){
        if (parent == null) {
            return categoryRepository.findByParentIsNull();
        }
        return categoryRepository.findByParent(parent);
    }
    @PostMapping("/books")
    public String addBook(@Valid @ModelAttribute("book") BookTitle bookTitle, RedirectAttributes redirectAttrs,
                          @RequestParam String author, @RequestParam String nxb, Model model) {
        if (authorRepository.findByAuthorName(author) == null) {
            Author newAuthor = Author.builder()
                    .id(idGeneratorService.generate("AUTHOR", "AUTH"))
                    .authorName(author)
                    .build();
            authorRepository.save(newAuthor);
            bookTitle.setAuthor(newAuthor);
        }else {
            bookTitle.setAuthor(authorRepository.findByAuthorName(author));
        }
        if (publisherRepository.findByPublisherName(nxb) == null) {
            Publisher newPublisher = Publisher.builder()
                    .id(idGeneratorService.generate("PUBLISHER", "PUB"))
                    .publisherName(nxb)
                    .build();
            publisherRepository.save(newPublisher);
            bookTitle.setPublisher(newPublisher);
        }else {
            bookTitle.setPublisher(publisherRepository.findByPublisherName(nxb));
        }
        bookTitle.setId(idGeneratorService.generate("BOOK_TITLE", "BKT"));
        for (int i = 0; i < bookTitle.getQuantity(); i++) {
            BookCopy bookCopy = BookCopy.builder()
                    .id(idGeneratorService.generate("BOOK_COPY", "BKC"))
                    .barcode("barcode-" + (i + 1))
                    .bookTitle(bookTitle)
                    .circulationStatus("available")
                    .build();
            bookCopyRepository.save(bookCopy);
        }
        bookTitle.setDeleted(false);
        bookTitleRepository.save(bookTitle);
        return "redirect:/books";
    }
    @GetMapping(value = "/books",params = "id")
    public String deleteCategory(@RequestParam("id") String id){
        bookTitleRepository.deleteBookTitle(id);
        return "redirect:/books";
    }
    @GetMapping("/books-management")
    public String showBooksManagement(@RequestParam("id") String id,  Model model){
        model.addAttribute("title","Danh mục sách");
        model.addAttribute("sub","Quản lý kho sách");
        model.addAttribute("activePage","books");
        model.addAttribute("BookTitle",bookCopyRepository.findAll());
        return "librarian/book-copy";
    }
}
