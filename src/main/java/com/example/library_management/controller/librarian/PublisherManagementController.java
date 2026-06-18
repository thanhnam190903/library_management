package com.example.library_management.controller.librarian;

import com.example.library_management.entity.Author;
import com.example.library_management.entity.Publisher;
import com.example.library_management.repository.PublisherRepository;
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
import java.util.Map;

@Controller
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@RequestMapping("/quan-ly")
public class PublisherManagementController {
    PublisherRepository publisherRepository;
    IdGeneratorService idGeneratorService;

    @GetMapping("/publisher")
    public String getAllpublisher(@RequestParam(defaultValue = "") String keyword,
                               @RequestParam(defaultValue = "0") int page, Model model){

        Map<String, Object> data = new HashMap<>();
        Pageable pageable = PageRequest.of(page, 10, Sort.by("id").descending());
        Page<Publisher> users = publisherRepository
                .findByDeletedFalseAndPublisherNameContainingIgnoreCase(keyword, pageable);
        data.put("listUser",users);
        data.put("title","Danh sách nhà xuất bản");
        data.put("sub","Quản lý nhà xuất bản");
        data.put("activePage","publishers");
        data.put("currentPage", page);
        data.put("totalPages", users.getTotalPages());
        data.put("totalElements",users.getTotalElements());
        data.put("keyword", keyword);
        data.put("publisher", new Publisher());
        model.addAllAttributes(data);
        return "librarian/publisher-management";
    }
    @PostMapping("/publisher")
    public String createpublisher(@ModelAttribute("publisher") Publisher publisher,
                               RedirectAttributes redirectAttrs, Model model){

        if (publisher.getId() != null && !publisher.getId().isEmpty()) {
            publisherRepository.save(publisher);
            redirectAttrs.addFlashAttribute("success","Thay đổi thông tin thành công !");
            return "redirect:/quan-ly/publisher";
        }
        publisher.setId(idGeneratorService.generate("AUTHOR", "AUTH"));
        redirectAttrs.addFlashAttribute("success","Thêm mới nhà xuất bản thành công !");
        publisherRepository.save(publisher);
        return "redirect:/quan-ly/publisher";
    }
    @GetMapping(value = "/publisher",params = "id")
    public String deletepublisher(@RequestParam("id") String id){
        publisherRepository.deletepublisher(id);
        return "redirect:/quan-ly/publisher";
    }
}
