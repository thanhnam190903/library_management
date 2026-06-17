package com.example.library_management.controller.librarian;

import com.example.library_management.entity.*;
import com.example.library_management.repository.*;
import com.example.library_management.service.AudioService;
import com.example.library_management.service.CloudinaryService;
import com.example.library_management.service.IdGeneratorService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@RequestMapping("/quan-ly")
public class BookController {
    BookTitleRepository bookTitleRepository;
    CategoryRepository categoryRepository;
    AuthorRepository authorRepository;
    PublisherRepository publisherRepository;
    IdGeneratorService idGeneratorService;
    BookCopyRepository bookCopyRepository;
    DigitalBookRepository digitalBookRepository;
    CloudinaryService cloudinaryService;
    AudioService audioService;

    @GetMapping("/books")
    public String showBooks(@RequestParam(defaultValue = "") String keyword,
                            @RequestParam(defaultValue = "") String categoryId,
                            @RequestParam(defaultValue = "0") int page,Model model){

        Pageable pageable = PageRequest.of(page, 6, Sort.by("id").descending());
        Page<BookTitle> booksPage = bookTitleRepository.searchBooks(keyword, categoryId, pageable);
        List<BookTitle> books = booksPage.getContent();
        Map<String, Long> availableMap = new HashMap<>();
        for (BookTitle b : books) {
            long available = bookCopyRepository.countAvailableBooks(b.getId());
            availableMap.put(b.getId(), available);
        }
        Map<String, Object> data = new HashMap<>();

        data.put("title", "Danh mục sách");
        data.put("sub", "Quản lý kho sách");
        data.put("activePage", "books");
        data.put("books", booksPage);
        data.put("categories", categoryRepository.getAllCategory());
        data.put("availableMap", availableMap);
        data.put("book", new BookTitle());
        data.put("keyword", keyword);
        data.put("categoryId", categoryId);
        data.put("currentPage", page);
        data.put("totalPages", booksPage.getTotalPages());
        data.put("totalElements", booksPage.getTotalElements());
        model.addAllAttributes(data);
        return "librarian/book";
    }

    @GetMapping("/get-category")
    @ResponseBody
    public List<Category> getCategory(@RequestParam(required = false) String parent){

        if (parent == null || parent.isEmpty()) {
            for (Category category : categoryRepository.findByParentIsNull()) {
                System.out.println("cate:"+category.getCategoryName());
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
            @RequestParam(required = false) String nxb, @RequestParam("image") MultipartFile image) throws IOException {

        Author authorEntity = authorRepository.findByAuthorName(author);
        if (authorEntity == null) {
            authorEntity = Author.builder()
                    .id(idGeneratorService.generate("AUTHOR", "AUTH"))
                    .authorName(author)
                    .build();
            authorRepository.save(authorEntity);
        }
        bookTitle.setAuthor(authorEntity);

        Publisher publisherEntity = null;
        if (nxb != null && !nxb.trim().isEmpty()) {
            publisherEntity = publisherRepository.findByPublisherName(nxb);

            if (publisherEntity == null) {
                publisherEntity = Publisher.builder()
                        .id(idGeneratorService.generate("PUBLISHER", "PUB"))
                        .publisherName(nxb)
                        .build();
                publisherRepository.save(publisherEntity);
            }
        }
        bookTitle.setPublisher(publisherEntity);

        if (bookTitle.getId() == null || bookTitle.getId().isEmpty()) {

            if (image == null || image.isEmpty()) {
                redirectAttrs.addFlashAttribute("error", "Vui lòng chọn ảnh bìa!");
                return "redirect:/quan-ly/books";
            }

            Category category = categoryRepository.findById(bookTitle.getCategory().getId()).orElseThrow();
            String categoryName = category.getCategoryName();
            String categoryCode = getCategoryCode(categoryName);
            System.out.println("cate"+ categoryCode);
            if (bookTitle.getIsbn() != null && !bookTitle.getIsbn().isBlank()) {
                if (bookTitleRepository.existsById(bookTitle.getIsbn())) {
                    redirectAttrs.addFlashAttribute("error", "ISBN đã tồn tại!");
                    return "redirect:/quan-ly/books";
                }
                bookTitle.setId(bookTitle.getIsbn());
            } else {
                bookTitle.setId(idGeneratorService.generate("BOOK_TITLE", categoryCode));
            }
            Map uploadResult = cloudinaryService.uploadFile(image);
            bookTitle.setCoverImage(uploadResult.get("secure_url").toString());
            bookTitle.setDeleted(false);
            bookTitleRepository.save(bookTitle);
            int quantity = bookTitle.getQuantity() == null ? 0 : bookTitle.getQuantity();
            System.out.println("quan:"+quantity);
            for (int i = 0; i < quantity; i++) {
                BookCopy bookCopy = BookCopy.builder()
                        .id(idGeneratorService.generate("BOOK_COPY", "BKC"))
                        .barcode("BC" + String.format("%03d", i + 1))
                        .bookTitle(bookTitle)
                        .circulationStatus("available")
                        .status(true)
                        .deleted(false)
                        .build();

                bookCopyRepository.save(bookCopy);
            }

            redirectAttrs.addFlashAttribute("success", "Thêm sách thành công!");
        } else {

            BookTitle oldBook = bookTitleRepository.findById(bookTitle.getId()).orElseThrow();
            int oldQuantity = oldBook.getQuantity() == null ? 0 : oldBook.getQuantity();
            int newQuantity = bookTitle.getQuantity() == null ? 0 : bookTitle.getQuantity();

            oldBook.setTitle(bookTitle.getTitle());
            oldBook.setAuthor(authorEntity);
            oldBook.setPublisher(publisherEntity);
            oldBook.setIsbn(bookTitle.getIsbn());
            oldBook.setLanguage(bookTitle.getLanguage());
            oldBook.setPublishYear(bookTitle.getPublishYear());
            oldBook.setCategory(bookTitle.getCategory());
            oldBook.setQuantity(bookTitle.getQuantity());
            oldBook.setPrice(bookTitle.getPrice());
            oldBook.setNote(bookTitle.getNote());

            if (image != null && !image.isEmpty()) {
                Map uploadResult = cloudinaryService.uploadFile(image);
                oldBook.setCoverImage(uploadResult.get("secure_url").toString());
            }
            bookTitleRepository.save(oldBook);
            if (newQuantity > oldQuantity) {

                int total = newQuantity - oldQuantity;
                long countBKC = bookCopyRepository.countByBookTitleId(oldBook.getId());
                String categoryCode =
                        getCategoryCode(oldBook.getCategory().getCategoryName());
                for (int i = 1; i <= total; i++) {

                    BookCopy bookCopy = BookCopy.builder()
                            .id(idGeneratorService.generate("BOOK_COPY", "BKC"))
                            .barcode("BC"+String.format("%03d", countBKC + i))
                            .bookTitle(oldBook)
                            .circulationStatus("available")
                            .status(true)
                            .deleted(false)
                            .build();
                    bookCopyRepository.save(bookCopy);
                }
            } else if (newQuantity < oldQuantity) {

                int total = oldQuantity - newQuantity;
                List<BookCopy> availableCopies =
                        bookCopyRepository.findByBookTitleIdAndCirculationStatus(
                                oldBook.getId(), "available");
                if (availableCopies.size() < total) {
                    redirectAttrs.addFlashAttribute("error", "Không đủ sách khả dụng để giảm số lượng!");
                    return "redirect:/quan-ly/books";
                }
                for (int i = 0; i < total; i++) {
                    bookCopyRepository.delete(availableCopies.get(i));
                }
            }
            redirectAttrs.addFlashAttribute("success", "Cập nhật sách thành công!");
        }
        return "redirect:/quan-ly/books";
    }

    @GetMapping(value = "/books",params = "id")
    public String deletebook(@RequestParam("id") String id){
        bookTitleRepository.deleteBookTitle(id);
        return "redirect:/quan-ly/books";
    }

    @GetMapping("/books-management")
    public String showBooksManagement(@RequestParam("id") String id,
                                      @RequestParam(defaultValue = "") String keyword,
                                      @RequestParam(defaultValue = "") String circulationStatus,
                                      @RequestParam(defaultValue = "0") int page,  Model model){
        Map<String, Object> data = new HashMap<>();
        Pageable pageable = PageRequest.of(page, 8, Sort.by("id").descending());

        Page<BookCopy> bookCopies = bookCopyRepository
                .searchBookCopies(id, keyword, circulationStatus, pageable);
        data.put("title", "Danh mục sách");
        data.put("sub", "Quản lý kho sách");
        data.put("activePage", "books");
        data.put("bookCopy", bookCopies);
        data.put("keyword", keyword);
        data.put("currentPage", page);
        data.put("totalPages", bookCopies.getTotalPages());
        data.put("totalElements", bookCopies.getTotalElements());
        data.put("digitalBook", digitalBookRepository.findByBookTitleId(id));
        data.put("bookTittleId",id);
        model.addAllAttributes(data);
        return "librarian/book-copy";
    }

    @PostMapping("/book-copys/update-copy")
    public String updateBookCopy(@RequestParam List<String> ids, @RequestParam String bookCondition,
            @RequestParam String shelfLocation,RedirectAttributes redirectAttrs) {

        String bookTitleId = null;
        for (String id : ids) {
            BookCopy copy = bookCopyRepository.findById(id).orElseThrow();
            copy.setBookCondition(bookCondition);
            copy.setShelfLocation(shelfLocation);
            bookCopyRepository.save(copy);
            if (bookTitleId == null) {
                bookTitleId = copy.getBookTitle().getId();
            }
        }
        redirectAttrs.addFlashAttribute("succe", "Cập nhật thông tin sách thành công!");
        return "redirect:/quan-ly/books-management?id=" + bookTitleId;
    }

    @PostMapping("/digital-book")
    public String save(@RequestParam(required = false) MultipartFile file,
                       @RequestParam String bookTittleId,
                       @ModelAttribute DigitalBook digitalBook,
                       RedirectAttributes attributes) throws IOException {

        BookTitle title = bookTitleRepository.findById(bookTittleId)
                .orElseThrow(() -> new RuntimeException("BookTitle not found"));

        DigitalBook book;
        if (digitalBook.getId() == null || digitalBook.getId().isEmpty()) {
            Map uploadResult = cloudinaryService.uploadRawFile(file);
            book = DigitalBook.builder()
                    .id(idGeneratorService.generate("DIGITAL_BOOK", "BDG"))
                    .bookTitle(title)
                    .fileName(digitalBook.getFileName())
                    .fileType(digitalBook.getFileType())
                    .fileUrl(uploadResult.get("secure_url").toString())
                    .audioUrl(null)
                    .status(digitalBook.getStatus())
                    .viewCount(0)
                    .build();

            attributes.addFlashAttribute("success", "Thêm tài liệu thành công!");
        } else {
            book = digitalBookRepository.findById(digitalBook.getId())
                    .orElseThrow(() -> new RuntimeException("DigitalBook not found"));
            book.setFileName(digitalBook.getFileName());
            book.setStatus(digitalBook.getStatus());
            book.setFileType(digitalBook.getFileType());
            if (file != null && !file.isEmpty()) {
                Map uploadResult = cloudinaryService.uploadFile(file);
                book.setFileUrl(uploadResult.get("secure_url").toString());
            }
            attributes.addFlashAttribute("success", "Cập nhật tài liệu thành công!");
        }
        digitalBookRepository.save(book);
//        audioService.generateAudio(book.getId());
        return "redirect:/quan-ly/books-management?id=" + bookTittleId + "&view=digital";
    }

    private String getCategoryCode(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return "XX";
        }
        String[] words = categoryName.trim().split("\\s+");
        StringBuilder code = new StringBuilder();
        for (String word : words) {
            code.append(Character.toUpperCase(word.charAt(0)));
        }
        return code.toString();
    }
    @GetMapping("/book-copys/delete")
    public String deleteBookCopy(@RequestParam String id, RedirectAttributes redirectAttrs) {

        BookCopy copy = bookCopyRepository.findById(id).orElseThrow();
        BookTitle bookTitle = bookTitleRepository.findById(copy.getBookTitle().getId()).orElseThrow();
        if ("borrowed".equals(copy.getCirculationStatus())
                || "reserved".equals(copy.getCirculationStatus())) {
            redirectAttrs.addFlashAttribute("err", "Sách đang được mượn hoặc đặt trước nên không thể xóa!");
            return "redirect:/quan-ly/books-management?id=" + bookTitle.getId();
        }
        bookTitle.setQuantity(bookTitle.getQuantity()-1);
        bookTitleRepository.save(bookTitle);
        bookCopyRepository.deleteBookCopy(id);

        redirectAttrs.addFlashAttribute("succe", "Xóa thành công!");
        return "redirect:/quan-ly/books-management?id=" + bookTitle.getId();
    }

    @GetMapping("/digital/delete")
    public String deleteDigital(@RequestParam String id, RedirectAttributes redirectAttrs) {
        DigitalBook digitalBook = digitalBookRepository.findById(id).orElseThrow();
        BookTitle bookTitle = bookTitleRepository.findById(digitalBook.getBookTitle().getId()).orElseThrow();
        if (digitalBook.getId() == null){
            redirectAttrs.addFlashAttribute("error", "Không tìm thấy sách!");
            return "redirect:/quan-ly/books-management?id=" + bookTitle.getId() +"&view=digital";
        }
        digitalBookRepository.deleteById(id);
        redirectAttrs.addFlashAttribute("succes", "Xóa tài liệu thành công!");
        return "redirect:/quan-ly/books-management?id=" + bookTitle.getId() +"&view=digital";
    }
}
