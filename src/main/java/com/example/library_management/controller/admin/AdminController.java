package com.example.library_management.controller.admin;


import com.example.library_management.entity.User;
import com.example.library_management.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
public class AdminController {
    UserRepository userRepository;

    @GetMapping("/ad")
    public String index(){
        return "admin/index";
    }



}
