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
    public List<Category> getCategory(@RequestParam(required = false) String parent){

        if (parent == null || parent.isEmpty()) {
            for (Category category : categoryRepository.findByParentIsNull()) {
                System.out.println(category.getCategoryName());
            }
            return categoryRepository.findByParentIsNull();

        }
        for (Category category : categoryRepository.findByParent(parent)) {
            System.out.println(category.getCategoryName());
        }
        return categoryRepository.findByParent(parent);
    }
    @PostMapping("/books")
    public String saveBook(@Valid @ModelAttribute("book") BookTitle bookTitle,
            RedirectAttributes redirectAttrs, @RequestParam String author,
            @RequestParam String nxb, Model model) {
        Author authorEntity = authorRepository.findByAuthorName(author);
        if (authorEntity == null) {
            authorEntity = Author.builder()
                    .id(idGeneratorService.generate("AUTHOR", "AUTH"))
                    .authorName(author)
                    .build();
            authorRepository.save(authorEntity);
        }
        bookTitle.setAuthor(authorEntity);

        Publisher publisherEntity = publisherRepository.findByPublisherName(nxb);
        if (publisherEntity == null) {
            publisherEntity = Publisher.builder()
                    .id(idGeneratorService.generate("PUBLISHER", "PUB"))
                    .publisherName(nxb)
                    .build();
            publisherRepository.save(publisherEntity);
        }
        bookTitle.setPublisher(publisherEntity);

        if (bookTitle.getId() == null || bookTitle.getId().isEmpty()) {

            bookTitle.setId(idGeneratorService.generate("BOOK_TITLE", "BKT"));
            bookTitle.setDeleted(false);
            bookTitleRepository.save(bookTitle);

            for (int i = 0; i < bookTitle.getQuantity(); i++) {

                BookCopy bookCopy = BookCopy.builder()
                        .id(idGeneratorService.generate("BOOK_COPY", "BKC"))
                        .barcode(bookTitle.getId() + "-" + (i + 1))
                        .bookTitle(bookTitle)
                        .circulationStatus("available")
                        .build();
                bookCopyRepository.save(bookCopy);
            }
            redirectAttrs.addFlashAttribute("success", "Thêm sách thành công!");
        }
        else {
            BookTitle oldBook = bookTitleRepository.findById(bookTitle.getId()).orElseThrow();
            int oldQuantity = oldBook.getQuantity();
            int newQuantity = bookTitle.getQuantity();
            oldBook.setTitle(bookTitle.getTitle());
            oldBook.setAuthor(authorEntity);
            oldBook.setPublisher(publisherEntity);
            oldBook.setIsbn(bookTitle.getIsbn());
            oldBook.setLanguage(bookTitle.getLanguage());
            oldBook.setPublishYear(bookTitle.getPublishYear());
            oldBook.setCategory(bookTitle.getCategory());
            oldBook.setQuantity(newQuantity);
            oldBook.setPrice(bookTitle.getPrice());
            oldBook.setNote(bookTitle.getNote());

            bookTitleRepository.save(oldBook);

            if (newQuantity > oldQuantity) {
                int total = newQuantity - oldQuantity;
                long countBKC = bookCopyRepository.countByBookTitleId(oldBook.getId());
                for (int i = 1; i <= total; i++) {
                    BookCopy bookCopy = BookCopy.builder()
                            .id(idGeneratorService.generate("BOOK_COPY", "BKC"))
                            .barcode(oldBook.getId() + "-" + (countBKC + i))
                            .bookTitle(oldBook)
                            .circulationStatus("available")
                            .build();
                    bookCopyRepository.save(bookCopy);
                }
            }
            else if (newQuantity < oldQuantity) {
                int total = oldQuantity - newQuantity;

                List<BookCopy> availableCopies = bookCopyRepository
                        .findByBookTitleIdAndCirculationStatus(oldBook.getId(), "available");
                // không đủ available để xóa
                if (availableCopies.size() < total) {
                    redirectAttrs.addFlashAttribute("error", "Không đủ sách khả dụng để giảm số lượng!");
                    return "redirect:/books";
                }
                for (int i = 0; i < total; i++) {
                    bookCopyRepository.delete(availableCopies.get(i));
                }
            }
            redirectAttrs.addFlashAttribute("success", "Cập nhật sách thành công!");
        }
        return "redirect:/books";
    }
    @GetMapping(value = "/books",params = "id")
    public String deletebook(@RequestParam("id") String id){
        bookTitleRepository.deleteBookTitle(id);
        return "redirect:/books";
    }
    @GetMapping("/books-management")
    public String showBooksManagement(@RequestParam("id") String id,  Model model){
        model.addAttribute("title","Danh mục sách");
        model.addAttribute("sub","Quản lý kho sách");
        model.addAttribute("activePage","books");
        model.addAttribute("BookTitle",bookCopyRepository.findByBookTitleId(id));
        return "librarian/book-copy";
    }
}
