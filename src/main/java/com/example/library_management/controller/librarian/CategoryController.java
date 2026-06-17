package com.example.library_management.controller.librarian;

import com.example.library_management.entity.BookTitle;
import com.example.library_management.entity.Category;
import com.example.library_management.entity.User;
import com.example.library_management.repository.CategoryRepository;
import com.example.library_management.service.IdGeneratorService;
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
import java.util.List;
import java.util.Map;

@Controller
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@RequestMapping("/quan-ly")
public class CategoryController {
    CategoryRepository categoryRepository;
    IdGeneratorService idGeneratorService;

    @GetMapping("/categories")
    public String showCategory(@RequestParam(defaultValue = "") String keyword,
                               @RequestParam(required = false) Boolean status,
                               @RequestParam(defaultValue = "0") int page,Model model){
        Map<String, Object> data = new HashMap<>();
        Pageable pageable = PageRequest.of(page, 8, Sort.by("id").descending());

        Page<Category> categories = categoryRepository.searchCategories(keyword, status, pageable);
        data.put("title", "Danh mục thể loại sách");
        data.put("sub", "Quản lý danh mục sách");
        data.put("activePage", "category");
        data.put("categories", categories);
        data.put("keyword", keyword);
        data.put("status", status);
        data.put("currentPage", page);
        data.put("totalPages", categories.getTotalPages());
        data.put("totalElements", categories.getTotalElements());
        data.put("category", new Category());
        model.addAllAttributes(data);
        return "librarian/category";
    }
    @PostMapping("/categories")
    public String createCategory(@ModelAttribute("category") Category category,
                                 RedirectAttributes redirectAttrs,@RequestParam(value = "parent", required = false) String parentId,
                                 Model model){

        category.setParent(parentId);
        category.setDeleted(false);
        if (category.getId() != null && !category.getId().isEmpty()) {
            categoryRepository.save(category);
            redirectAttrs.addFlashAttribute("success","Thay đổi thông tin thành công !");
            return "redirect:/quan-ly/categories";
        }
        category.setId(idGeneratorService.generate("CATEGORY", "CAT"));
        redirectAttrs.addFlashAttribute("success","Thêm mới thể loại thành công !");
        categoryRepository.save(category);
        return "redirect:/quan-ly/categories";
    }
    @GetMapping(value = "/categories",params = "id")
    public String deleteCategory(@RequestParam("id") String id){
        categoryRepository.deleteCategory(id);
        return "redirect:/quan-ly/categories";
    }

}
