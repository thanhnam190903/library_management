package com.example.library_management.controller.librarian;

import com.example.library_management.entity.BookTitle;
import com.example.library_management.entity.Category;
import com.example.library_management.entity.User;
import com.example.library_management.repository.CategoryRepository;
import com.example.library_management.service.IdGeneratorService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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
public class CategoryController {
    CategoryRepository categoryRepository;
    IdGeneratorService idGeneratorService;

    @GetMapping("/categories")
    public String showCategory(Model model){
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Danh mục thể loại sách");
        data.put("sub", "Quản lý danh mục sách");
        data.put("activePage", "category");
        data.put("categories", categoryRepository.getAllCategory());
        data.put("category", new Category());
        model.addAllAttributes(data);
        return "librarian/category";
    }
    @PostMapping("/categories")
    public String createCategory(@ModelAttribute("category") Category category,
                                 RedirectAttributes redirectAttrs,@RequestParam(value = "parent", required = false) String parentId,
                                 Model model){
        if (parentId != null && !parentId.equals("0")) {
            category.setParent(parentId);
        } else {
            category.setParent(null);
        }
        category.setDeleted(false);
        if (category.getId() != null && !category.getId().isEmpty()) {
            categoryRepository.save(category);
            return "redirect:/categories";
        }
        category.setId(idGeneratorService.generate("CATEGORY", "CAT"));

        categoryRepository.save(category);
        return "redirect:/categories";
    }
    @GetMapping(value = "/categories",params = "id")
    public String deleteCategory(@RequestParam("id") String id){
        categoryRepository.deleteCategory(id);
        return "redirect:/categories";
    }

}
