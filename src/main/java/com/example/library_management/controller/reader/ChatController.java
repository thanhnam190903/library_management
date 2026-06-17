package com.example.library_management.controller.reader;

import com.example.library_management.ai.BookSearchFunction;
import com.example.library_management.dto.ChatRequest;
import com.example.library_management.dto.ChatResponse;
import com.example.library_management.dto.MessageDTO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/home")
public class ChatController {
    private final ChatClient chatClient;
    private final BookSearchFunction bookSearchFunction;
    private static final String SYSTEM_PROMPT = """
    Bạn là trợ lý thư viện BiblioLink, hỗ trợ người dùng bằng tiếng Việt.
    
    QUY TẮC BẮT BUỘC:
    - TUYỆT ĐỐI không được tự tạo, bịa, hoặc đề xuất tên sách khi chưa gọi tool
    - Khi user hỏi về sách, tác giả, chủ đề, thể loại → PHẢI gọi tool searchBooks trước
    - Chỉ được nhắc đến sách nếu sách đó có trong kết quả trả về từ tool
    - Nếu tool trả về rỗng → nói "Không tìm thấy sách phù hợp" và gợi ý keyword khác
    - Khi user hỏi tóm tắt/mô tả sách đã hiển thị → trả lời dựa trên tên sách, KHÔNG gọi tool lại
    - Trả lời ngắn gọn, tự nhiên, thân thiện
    
    CÁCH SUY LUẬN keyword:
    - "học lập trình" → "lập trình"
    - "lịch sử Việt Nam" → "lịch sử"
    - "sách thiếu nhi" → "thiếu nhi"
    - "tâm lý học" → "tâm lý"
    - "đọc truyện" -> "truyện"
    """;

    public ChatController(ChatClient.Builder builder, BookSearchFunction bookSearchFunction) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
        this.bookSearchFunction = bookSearchFunction;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            String userMsg = request.message();

            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

            if (request.history() != null) {
                for (MessageDTO msg : request.history()) {
                    // ✅ Bỏ qua tin nhắn lỗi trong history
                    if (msg.content() == null) continue;
                    if (msg.content().startsWith("Có lỗi xảy ra:")) continue;
                    if (msg.content().startsWith("Kết nối thất bại")) continue;

                    if ("user".equalsIgnoreCase(msg.role())) {
                        messages.add(new UserMessage(msg.content()));
                    } else {
                        String content = msg.content();
                        if (msg.keyword() != null) {
                            content += " [đã tìm keyword: " + msg.keyword() + "]";
                        }
                        messages.add(new AssistantMessage(content));
                    }
                }
            }
            messages.add(new UserMessage(userMsg));

            bookSearchFunction.clearLastBooksJson();

            String reply = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .messages(messages)
                    .tools(bookSearchFunction)
                    .call()
                    .content();

            String booksJson = bookSearchFunction.getLastBooksJson();
            String lastKeyword = bookSearchFunction.getLastKeyword();
            boolean searchCalled = bookSearchFunction.isSearchCalled();
            boolean hasBooks = booksJson != null && !booksJson.contains("\"books\":[]");

            return ResponseEntity.ok(new ChatResponse(
                    reply,
                    hasBooks ? booksJson : null,
                    lastKeyword,
                    searchCalled
            ));

        } catch (Exception e) {
            e.printStackTrace();
            // ✅ Không expose lỗi kỹ thuật ra ngoài
            return ResponseEntity.ok(new ChatResponse(
                    "Xin lỗi, tôi đang gặp sự cố. Bạn vui lòng thử lại sau nhé!",
                    null, null, false
            ));
        }
    }

}
