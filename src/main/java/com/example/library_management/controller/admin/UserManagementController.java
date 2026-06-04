package com.example.library_management.controller.admin;

import com.example.library_management.entity.Role;
import com.example.library_management.entity.User;
import com.example.library_management.repository.RoleRepository;
import com.example.library_management.repository.UserRepository;
import com.example.library_management.service.IdGeneratorService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@RequestMapping("/quan-ly")
public class UserManagementController {
    UserRepository userRepository;
    IdGeneratorService idGeneratorService;
    RoleRepository roleRepository;

    @GetMapping("/users")
    public String getAllUsers(Model model){
        model.addAttribute("listUser",userRepository.getAllUser());
        model.addAttribute("title","Danh sách người dùng");
        model.addAttribute("sub","Quản lý người dùng");
        model.addAttribute("activePage","user");

        model.addAttribute("user", new User());
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

        User newUser = User.builder()
                .id(id)
                .username(id)
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .status(user.isStatus())
                .birthDate(user.getBirthDate())
                .password(user.getBirthDate() != null ? user.getBirthDate().toString() : null)
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
            List<Role> roles = roleRepository.findByRoleNameIn(roleNames);
            existingUser.setName(user.getName());
            existingUser.setPhone(user.getPhone());
            existingUser.setEmail(user.getEmail());
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
