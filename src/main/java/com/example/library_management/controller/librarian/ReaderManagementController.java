package com.example.library_management.controller.librarian;

import com.example.library_management.entity.Category;
import com.example.library_management.entity.LibraryCard;
import com.example.library_management.entity.Reader;
import com.example.library_management.repository.BorrowDetailRepository;
import com.example.library_management.repository.LibraryCardRepository;
import com.example.library_management.repository.ReaderRepository;
import com.example.library_management.service.GeneratorQRService;
import com.example.library_management.service.IdGeneratorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@RequestMapping("/quan-ly")
public class ReaderManagementController {
    ReaderRepository readerRepository;
    BorrowDetailRepository borrowDetailRepository;
    IdGeneratorService idGeneratorService;
    LibraryCardRepository libraryCardRepository;
    GeneratorQRService generateQrService;

    private static final int PAGE_SIZE = 10;

    @GetMapping("/readers")
    public String showReaders(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        updateCardStatuses();
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<Reader> readerPage = readerRepository.searchReaders(keyword.trim(), status, pageable);
        Map<String, Long> borrowingMap = new HashMap<>();
        Map<String, Long> overdueMap   = new HashMap<>();
        for (Reader r : readerPage.getContent()) {
            if (!r.getCards().isEmpty()) {
                String cardId = r.getCards().get(0).getId();
                borrowingMap.put(r.getId(), borrowDetailRepository.countBorrowing(cardId));
                overdueMap.put(r.getId(),   borrowDetailRepository.countOverdue(cardId));
            }
        }
        model.addAttribute("title",       "Danh sách & hồ sơ độc giả");
        model.addAttribute("sub",         "Quản lý hội viên");
        model.addAttribute("activePage",  "readers");
        model.addAttribute("readerPage",  readerPage);
        model.addAttribute("reader",      new Reader());
        model.addAttribute("borrowingMap", borrowingMap);
        model.addAttribute("overdueMap",   overdueMap);
        model.addAttribute("keyword",      keyword);
        model.addAttribute("status",       status);
        model.addAttribute("currentPage",  page);
        model.addAttribute("totalPages",   readerPage.getTotalPages());
        model.addAttribute("totalElements",readerPage.getTotalElements());
        return "librarian/reader";
    }

    @GetMapping("/qr/{id}")
    @ResponseBody
    public byte[] qr(@PathVariable String id) throws Exception {
        return generateQrService.generateQr(id);
    }

    @PostMapping("/readers")
    public String saveReader(@RequestParam(required = false) String id,
                             @RequestParam String name,
                             @RequestParam String gender,
                             @RequestParam String phone,
                             @RequestParam(required = false) String email,
                             @RequestParam(required = false) String readerType,
                             @RequestParam String birthDate,
                             @RequestParam(required = false) String address,
                             RedirectAttributes redirectAttrs) {

        if (name == null || name.trim().isEmpty()
                || phone == null || phone.trim().isEmpty()) {
            redirectAttrs.addFlashAttribute("error", "Dữ liệu không hợp lệ!");
            return "redirect:/quan-ly/readers";
        }

        LocalDate birth = LocalDate.parse(birthDate);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        boolean isNew = (id == null || id.isBlank());

        if (isNew) {
            String rawPassword = birth.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String encodedPassword = passwordEncoder.encode(rawPassword);

            Reader newReader = Reader.builder()
                    .id(idGeneratorService.generate("Reader", "RD"))
                    .name(name)
                    .gender(gender)
                    .phone(phone)
                    .email(email)
                    .readerType(readerType)
                    .birthDate(birth)
                    .address(address)
                    .password(encodedPassword)
                    .deleted(false)
                    .build();

            readerRepository.save(newReader);

            LibraryCard card = LibraryCard.builder()
                    .id(idGeneratorService.generate("LibraryCard", "DG"))
                    .issueDate(LocalDate.now())
                    .expiryDate(LocalDate.now().plusYears(1))
                    .maxBooksAllowed(5)
                    .totalBorrow(0)
                    .overdueCount(0)
                    .status(true)
                    .locked(false)
                    .reader(newReader)
                    .build();

            libraryCardRepository.save(card);

            redirectAttrs.addFlashAttribute("success", "Thêm độc giả thành công!");

        } else {
            Reader existing = readerRepository.findById(id).orElse(null);

            if (existing == null) {
                redirectAttrs.addFlashAttribute("error", "Độc giả không tồn tại!");
                return "redirect:/quan-ly/readers";
            }
            String rawPassword = birth.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String encodedPassword = passwordEncoder.encode(rawPassword);
            existing.setName(name);
            existing.setGender(gender);
            existing.setPhone(phone);
            existing.setEmail(email);
            existing.setReaderType(readerType);
            existing.setBirthDate(birth);
            existing.setPassword(encodedPassword);
            existing.setAddress(address);
            readerRepository.save(existing);

            redirectAttrs.addFlashAttribute("success", "Cập nhật thành công!");
        }

        return "redirect:/quan-ly/readers";
    }

    @GetMapping(value = "/readers",params = "id")
    public String cardRenewal(@RequestParam String id,RedirectAttributes redirectAttrs){
        LibraryCard card = libraryCardRepository.findById(id).orElse(null);
        if (card != null) {
            card.setExpiryDate(card.getExpiryDate().plusYears(1));
            card.setStatus(true);
            libraryCardRepository.save(card);
            redirectAttrs.addFlashAttribute("success", "Gia hạn thẻ thành công!");
        }else {
            redirectAttrs.addFlashAttribute("error", "Thẻ thư viện không tồn tại!");
        }
        return "redirect:/quan-ly/readers";
    }
    private void updateCardStatuses() {
        List<LibraryCard> cards = libraryCardRepository.findAll();
        LocalDate today = LocalDate.now();
        cards.forEach(c -> c.setStatus(c.getExpiryDate() != null && !c.getExpiryDate().isBefore(today)));
        libraryCardRepository.saveAll(cards);
    }

}
