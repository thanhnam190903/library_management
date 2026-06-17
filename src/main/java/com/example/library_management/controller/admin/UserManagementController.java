package com.example.library_management.controller.admin;

import com.example.library_management.entity.Role;
import com.example.library_management.entity.User;
import com.example.library_management.repository.RoleRepository;
import com.example.library_management.repository.UserRepository;
import com.example.library_management.service.IdGeneratorService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@RequestMapping("/quan-ly")
public class UserManagementController {
    UserRepository userRepository;
    IdGeneratorService idGeneratorService;
    RoleRepository roleRepository;

    @GetMapping("/users")
    public String getAllUsers(@RequestParam(defaultValue = "") String keyword,
                              @RequestParam(defaultValue = "") String role,
                              @RequestParam(required = false) Boolean status,
                              @RequestParam(defaultValue = "0") int page,
                              Model model){

        Map<String, Object> data = new HashMap<>();
        Pageable pageable = PageRequest.of(page, 8, Sort.by("id").descending());
        Page<User> users = userRepository.searchUsers(keyword, role, status, pageable);
        data.put("listUser",users);
        data.put("title","Danh sách người dùng");
        data.put("sub","Quản lý người dùng");
        data.put("activePage","user");
        data.put("currentPage", page);
        data.put("totalPages", users.getTotalPages());
        data.put("totalElements",users.getTotalElements());
        data.put("keyword", keyword);
        data.put("user", new User());
        model.addAllAttributes(data);
        return "admin/user-management";
    }
    @PostMapping("/add-user")
    public String addUser(@ModelAttribute("user") User user,
                          @RequestParam(value = "roleName", required = false) List<String> roleNames,
                          RedirectAttributes redirectAttrs,
                          Model model) {
        if (roleNames == null || roleNames.isEmpty()) {
            model.addAttribute("error", "Phải chọn ít nhất 1 quyền");
            model.addAttribute("showModal", true);
            return "admin/user-management";
        }
        String id = idGeneratorService.generate("USER", "USR");
        List<Role> roles = roleRepository.findByRoleNameIn(roleNames);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String has = passwordEncoder.encode(user.getBirthDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        User newUser = User.builder()
                .id(id)
                .username(id)
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .status(user.isStatus())
                .birthDate(user.getBirthDate())
                .password(has)
                .deleted(false)
                .roles(roles)
                .build();

        userRepository.save(newUser);

        redirectAttrs.addFlashAttribute("success", "Thêm người dùng thành công");
        return "redirect:/quan-ly/users";
    }

    @PostMapping("/update-user")
    public String updateUser(@ModelAttribute User user,RedirectAttributes redirectAttributes,
                             @RequestParam(value = "roleName", required = false) List<String> roleNames){
        User existingUser = userRepository.findById(user.getId()).orElse(null);
        if (existingUser != null){
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            String has = passwordEncoder.encode(user.getBirthDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            List<Role> roles = roleRepository.findByRoleNameIn(roleNames);
            existingUser.setName(user.getName());
            existingUser.setPhone(user.getPhone());
            existingUser.setEmail(user.getEmail());
            existingUser.setPassword(has);
            existingUser.setStatus(user.isStatus());
            existingUser.setBirthDate(user.getBirthDate());
            existingUser.setRoles(roles);
            userRepository.saveAndFlush(existingUser);
            redirectAttributes.addFlashAttribute("success", "Cập nhật nhân viên thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy nhân viên !");
        }
        return "redirect:/quan-ly/users";
    }
    @GetMapping("/delete-user")
    public String deleteUser(@RequestParam("id") String id){
        userRepository.deleteUser(id);
        return "redirect:/quan-ly/users";
    }
}
