package com.example.library_management.controller.librarian;

import com.example.library_management.entity.Category;
import com.example.library_management.entity.LibraryCard;
import com.example.library_management.entity.Reader;
import com.example.library_management.repository.BorrowDetailRepository;
import com.example.library_management.repository.LibraryCardRepository;
import com.example.library_management.repository.ReaderRepository;
import com.example.library_management.service.GeneratorQRService;
import com.example.library_management.service.IdGeneratorService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class ReaderManagementController {
    ReaderRepository readerRepository;
    BorrowDetailRepository borrowDetailRepository;
    IdGeneratorService idGeneratorService;
    LibraryCardRepository libraryCardRepository;
    GeneratorQRService generateQrService;

    @GetMapping("/readers")
    public String showReaders(Model model){
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Danh sách & hồ sơ độc giả");
        data.put("sub", "Quản lý hội viên");
        data.put("activePage", "readers");
        data.put("readerList", readerRepository.getAllReader());
        data.put("reader", new Reader());
        Map<String, Long> borrowingMap = new HashMap<>();
        Map<String, Long> overdueMap = new HashMap<>();

        for (Reader r : readerRepository.getAllReader()) {

            if (!r.getCards().isEmpty()) {
                String cardId = r.getCards().get(0).getId();
                borrowingMap.put(
                        r.getId(),
                        borrowDetailRepository.countBorrowing(cardId)
                );
                overdueMap.put(
                        r.getId(),
                        borrowDetailRepository.countOverdue(cardId)
                );
            }
        }
        List<LibraryCard> cards = libraryCardRepository.findAll();
        for (LibraryCard c : cards){
            if(c.getExpiryDate() != null && !c.getExpiryDate().isAfter(LocalDate.now())){
                c.setStatus(false);
            }else{
                c.setStatus(true);
            }
        }
        libraryCardRepository.saveAll(cards);
        data.put("borrowingMap", borrowingMap);
        data.put("overdueMap", overdueMap);
        model.addAllAttributes(data);
        return "librarian/reader";
    }
    @GetMapping("/qr/{id}")
    @ResponseBody
    public byte[] qr(@PathVariable String id) throws Exception {

        return generateQrService.generateQr(id);
    }
    @PostMapping("/readers")
    public String addReader(@ModelAttribute Reader reader , RedirectAttributes redirectAttrs,
                            @RequestParam(value = "idCard",required = false) String idCard,
                            @RequestParam(value = "issuaDate",required = false) LocalDate issuaDate,
                            Model model) {
        if (reader.getId() == null || reader.getId().isEmpty()) {
            reader.setId(idGeneratorService.generate("Reader", "RD"));
            reader.setDeleted(false);
            readerRepository.save(reader);
            LibraryCard card = LibraryCard.builder()
                    .id(idGeneratorService.generate("LibraryCard", "LC"))
                    .issueDate(java.time.LocalDate.now())
                    .expiryDate(java.time.LocalDate.now().plusYears(1))
                    .maxBooksAllowed(5)
                    .totalBorrow(0)
                    .overdueCount(0)
                    .status(true)
                    .locked(false)
                    .reader(reader)
                    .build();
            libraryCardRepository.save(card);
            redirectAttrs.addFlashAttribute("success", "Thêm độc giả thành công!");
            return "redirect:/readers";
        }else {
            Reader existingReader = readerRepository.findById(reader.getId()).orElse(null);
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
                LibraryCard oldCard = libraryCardRepository.findById(idCard).orElse(null);
                if (oldCard != null) {
                    oldCard.setIssueDate(issuaDate);
                    oldCard.setExpiryDate(issuaDate.plusYears(1));
                    libraryCardRepository.save(oldCard);
                    redirectAttrs.addFlashAttribute("success", "Cập nhật độc giả thành công!");
                }else {
                    redirectAttrs.addFlashAttribute("error", "Thẻ thư viện không tồn tại!");
                }
                return "redirect:/readers";
            } else {
                redirectAttrs.addFlashAttribute("error", "Độc giả không tồn tại!");
                return "redirect:/readers";
            }
        }
    }
    @GetMapping(value = "/readers",params = "id")
    public String cardRenewal(@RequestParam String id,RedirectAttributes redirectAttrs){
        LibraryCard card = libraryCardRepository.findById(id).orElse(null);
        if (card != null) {
            card.setExpiryDate(card.getExpiryDate().plusYears(1));
            card.setStatus(true);
            libraryCardRepository.save(card);
        }else {
            redirectAttrs.addFlashAttribute("error", "Thẻ thư viện không tồn tại!");
        }
        return "redirect:/readers";
    }

}
