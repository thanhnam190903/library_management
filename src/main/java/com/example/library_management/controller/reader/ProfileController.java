package com.example.library_management.controller.reader;

import com.example.library_management.entity.LibraryCard;
import com.example.library_management.entity.Reader;
import com.example.library_management.repository.BorrowDetailRepository;
import com.example.library_management.repository.LibraryCardRepository;
import com.example.library_management.repository.ReaderRepository;
import com.example.library_management.service.IdGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;

@Controller
@RequestMapping("/home")
public class ProfileController {
    @Autowired
    private ReaderRepository readerRepository;
    @Autowired
    private BorrowDetailRepository borrowDetailRepository;
    @Autowired
    private LibraryCardRepository libraryCardRepository;
    @Autowired
    private IdGeneratorService idGeneratorService;

    @GetMapping("/profile")
    public String getProfile(Model model, Principal principal){
        Reader reader = readerRepository.findByEmail(principal.getName()).get();
        model.addAttribute("countBorrowReturn",borrowDetailRepository
                .countBorrowReturn(reader.getCards().get(0).getId()));
        model.addAttribute("activePage","profile");
        model.addAttribute("re",new Reader());
        return "reader/profile";
    }
    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute("re") Reader reader, RedirectAttributes attributes){
        Reader existingReader = readerRepository.findById(reader.getId()).orElse(null);
        System.out.println(reader);
        if (existingReader != null) {
            existingReader.setName(reader.getName());
            existingReader.setGender(reader.getGender());
            existingReader.setPhone(reader.getPhone());
            existingReader.setEmail(reader.getEmail());
            existingReader.setPassword(reader.getPassword());
            existingReader.setReaderType(reader.getReaderType());
            existingReader.setBirthDate(reader.getBirthDate());
            existingReader.setAddress(reader.getAddress());
            readerRepository.save(existingReader);
            attributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
        }else {
            attributes.addFlashAttribute("error", "Có lôi xảy ra!");
        }
        return "redirect:/home/profile";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName, @RequestParam String email,
            @RequestParam String sdt, @RequestParam String diaChi,
            @RequestParam String gender, @RequestParam LocalDate ngaySinh,
            @RequestParam String password, RedirectAttributes attributes) {
        if (readerRepository.findByEmail(email).isPresent()) {
            attributes.addFlashAttribute("registerError", "Email đã tồn tại");
            return "redirect:/login?tab=register";
        }
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        Reader user = Reader.builder()
                .id(idGeneratorService.generate("Reader", "RD"))
                .name(fullName)
                .gender(gender)
                .phone(sdt)
                .email(email)
                .birthDate(ngaySinh)
                .address(diaChi)
                .password(passwordEncoder.encode(password))
                .deleted(false)
                .build();

        readerRepository.save(user);
        LibraryCard card = LibraryCard.builder()
                .id(idGeneratorService.generate("LibraryCard", "DG"))
                .issueDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .maxBooksAllowed(5)
                .totalBorrow(0)
                .overdueCount(0)
                .status(true)
                .locked(false)
                .reader(user)
                .build();
        libraryCardRepository.save(card);
        attributes.addFlashAttribute("success","Đăng ký thành công!");
        return "redirect:/login";
    }
}
