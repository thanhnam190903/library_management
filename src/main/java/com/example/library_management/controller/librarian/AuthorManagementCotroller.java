package com.example.library_management.controller.librarian;

import com.example.library_management.entity.Author;
import com.example.library_management.entity.Category;
import com.example.library_management.entity.User;
import com.example.library_management.repository.AuthorRepository;
import com.example.library_management.service.IdGeneratorService;
import jakarta.persistence.Id;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@RequestMapping("/quan-ly")
public class AuthorManagementCotroller {
    AuthorRepository authorRepository;
    IdGeneratorService idGeneratorService;

    @GetMapping("/author")
    public String getAllAuthor(@RequestParam(defaultValue = "") String keyword,
                              @RequestParam(defaultValue = "0") int page, Model model){

        Map<String, Object> data = new HashMap<>();
        Pageable pageable = PageRequest.of(page, 10, Sort.by("id").descending());
        Page<Author> users = authorRepository
                .findByDeletedFalseAndAuthorNameContainingIgnoreCase(keyword, pageable);
        data.put("listUser",users);
        data.put("title","Danh sách tác giả");
        data.put("sub","Quản lý tác giả");
        data.put("activePage","authors");
        data.put("currentPage", page);
        data.put("totalPages", users.getTotalPages());
        data.put("totalElements",users.getTotalElements());
        data.put("keyword", keyword);
        data.put("author", new Author());
        model.addAllAttributes(data);
        return "librarian/author-management";
    }
    @PostMapping("/author")
    public String createauthor(@ModelAttribute("author") Author author,
                                 RedirectAttributes redirectAttrs, Model model){

        if (author.getId() != null && !author.getId().isEmpty()) {
            authorRepository.save(author);
            redirectAttrs.addFlashAttribute("success","Thay đổi thông tin thành công !");
            return "redirect:/quan-ly/author";
        }
        author.setId(idGeneratorService.generate("AUTHOR", "AUTH"));
        redirectAttrs.addFlashAttribute("success","Thêm mới tác giả thành công !");
        authorRepository.save(author);
        return "redirect:/quan-ly/author";
    }
    @GetMapping(value = "/author",params = "id")
    public String deleteauthor(@RequestParam("id") String id){
        authorRepository.deleteauthor(id);
        return "redirect:/quan-ly/author";
    }
}
