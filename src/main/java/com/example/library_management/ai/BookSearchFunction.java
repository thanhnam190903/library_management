package com.example.library_management.ai;

import com.example.library_management.entity.BookTitle;
import com.example.library_management.repository.BookCopyRepository;
import com.example.library_management.repository.BookTitleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BookSearchFunction {
    private final BookTitleRepository bookTitleRepository;
    private final BookCopyRepository bookCopyRepository;
    private final ThreadLocal<String> lastBooksJson = new ThreadLocal<>();
    private final ThreadLocal<String> lastKeyword   = new ThreadLocal<>();
    private final ThreadLocal<Boolean> searchCalled = new ThreadLocal<>();

    public String getLastBooksJson() {
        return lastBooksJson.get();
    }
    public String getLastKeyword()   {
        return lastKeyword.get();
    }
    public boolean isSearchCalled() {
        return Boolean.TRUE.equals(searchCalled.get());
    }

    public void clearLastBooksJson() {
        lastKeyword.remove();
        lastBooksJson.remove();
        searchCalled.remove();
    }
    @Tool(description = """
    Tìm kiếm sách trong thư viện BiblioLink theo từ khóa, thể loại, tình trạng.
    Gọi hàm này bất cứ khi nào user đề cập đến sách, chủ đề học tập, thể loại đọc.
    KHÔNG gọi khi user: hỏi câu hỏi chung không liên quan đến thông tin sách.
    Tự suy luận keyword từ nội dung hội thoại, không cần user nói rõ tên sách.
    """)
    public String searchBooks(
            @ToolParam(description = "Từ khóa tìm kiếm: tên sách, tác giả, chủ đề, thể loại")
            String keyword) {

        Pageable pageable = PageRequest.of(0, 5);
        System.out.println("Tool called, keyword = " + keyword);
        if (keyword != null) keyword = keyword.trim();
        System.out.println("Tool called, keyword = " + keyword);
        lastKeyword.set(keyword);
        searchCalled.set(true);
        List<BookTitle> books = bookTitleRepository
                .searchSmart(keyword, pageable).getContent();

        if (books.isEmpty()) {
            lastBooksJson.set(null); // ✅ Không set gì cả
            return "{\"books\":[], \"message\":\"Không tìm thấy sách nào phù hợp.\"}";
        }

        StringBuilder json = new StringBuilder("{\"books\":[");
        for (int i = 0; i < books.size(); i++) {
            BookTitle book = books.get(i);
            long avail = bookCopyRepository.countAvailableBooks(book.getId());
            int qty = book.getQuantity() != null ? book.getQuantity() : 0;

            String status, statusKey;
            if (qty == 0 && avail == 0) {
                status = "E-book 📱"; statusKey = "ebook";
            } else if (avail > 0) {
                status = "Có thể mượn"; statusKey = "available";
            } else {
                status = "Đang cho mượn hết"; statusKey = "unavailable";
            }

            String cover  = book.getCoverImage() != null ? book.getCoverImage() : "";
            String author = book.getAuthor()     != null
                    ? book.getAuthor().getAuthorName() : "Không rõ";

            json.append("{")
                    .append("\"id\":\"")       .append(escapeJson(book.getId()))    .append("\",")
                    .append("\"title\":\"")    .append(escapeJson(book.getTitle())) .append("\",")
                    .append("\"author\":\"")   .append(escapeJson(author))          .append("\",")
                    .append("\"cover\":\"")    .append(escapeJson(cover))           .append("\",")
                    .append("\"status\":\"")   .append(status)                      .append("\",")
                    .append("\"statusKey\":\"").append(statusKey)                   .append("\"")
                    .append("}");

            if (i < books.size() - 1) json.append(",");
        }
        json.append("]}");

        String result = json.toString();
        lastBooksJson.set(result); // ✅ lưu lại để Controller lấy
        return result;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
