package com.example.library_management.controller;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@FieldDefaults(makeFinal = true,level = lombok.AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class AuthController {

    @GetMapping("/login")
    public String show(){
        return "login";
    }
}
