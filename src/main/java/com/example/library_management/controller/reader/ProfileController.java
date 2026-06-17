package com.example.library_management.controller.reader;

import com.example.library_management.entity.Reader;
import com.example.library_management.repository.BorrowDetailRepository;
import com.example.library_management.repository.ReaderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/home")
public class ProfileController {
    @Autowired
    private ReaderRepository readerRepository;
    @Autowired
    private BorrowDetailRepository borrowDetailRepository;

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
}
