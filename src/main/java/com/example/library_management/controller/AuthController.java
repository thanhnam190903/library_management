package com.example.library_management.controller;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@FieldDefaults(makeFinal = true,level = lombok.AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class AuthController {

    @GetMapping("/login")
    public String show(@RequestParam(required = false) String tab,
                       Model model){
        model.addAttribute("tab", tab);
        return "login";
    }
}
