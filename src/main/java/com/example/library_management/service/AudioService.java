package com.example.library_management.service;

import com.example.library_management.entity.DigitalBook;
import com.example.library_management.repository.DigitalBookRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AudioService {
    private final DigitalBookRepository digitalBookRepository;
    private final CloudinaryService cloudinaryService;
    private static final String AUDIO_DIR = "D:/audio/";

    // ~15-20 phút audio
    private static final int MAX_PREVIEW_LENGTH = 18000;

    @PostConstruct
    public void init() {
        new File(AUDIO_DIR).mkdirs();
        System.out.println("Thư mục audio đã sẵn sàng: " + AUDIO_DIR);
    }

    @Async
    public void generateAudio(String bookId) {
        List<String> mp3Files = new ArrayList<>();
        Process process = null;
        try {
            System.out.println("📚 BookId: " + bookId);
            DigitalBook book = digitalBookRepository.findById(bookId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sách"));
            // Đọc PDF
            String text;
            try (InputStream in = new URL(book.getFileUrl()).openStream();
                 PDDocument doc = Loader.loadPDF(in.readAllBytes())) {

                PDFTextStripper stripper = new PDFTextStripper();
                text = stripper.getText(doc);
            }
            if (text == null || text.isBlank()) {
                throw new RuntimeException("PDF không có nội dung");
            }
            System.out.println("Tổng ký tự PDF: " + text.length());
            // Chỉ lấy phần đầu để tạo audio preview
            if (text.length() > MAX_PREVIEW_LENGTH) {
                text = text.substring(0, MAX_PREVIEW_LENGTH);
            }
            System.out.println("Ký tự dùng để tạo audio: " + text.length());
            // Chia chunk nhỏ hơn
            List<String> chunks = splitSmart(text, 4000);
            System.out.println("Số chunk: " + chunks.size());

            for (int i = 0; i < chunks.size(); i++) {
                String mp3Path = AUDIO_DIR + bookId + "_part_" + i + ".mp3";
                System.out.println("Đang xử lý chunk " + (i + 1) + "/" + chunks.size());
                ProcessBuilder pb = new ProcessBuilder(
                        "python",
                        "-m",
                        "edge_tts",
                        "-t",
                        chunks.get(i),
                        "--voice",
                        "vi-VN-HoaiMyNeural",
                        "--write-media",
                        mp3Path
                );
                pb.redirectErrorStream(true);
                process = pb.start();
                boolean finished = process.waitFor(5, TimeUnit.MINUTES);
                if (!finished) {
                    process.destroyForcibly();
                    throw new RuntimeException(
                            "Timeout chunk " + i);
                }
                if (process.exitValue() != 0) {
                    throw new RuntimeException(
                            "Lỗi TTS chunk " + i);
                }
                mp3Files.add(mp3Path);
            }
            // Ghép file
            String finalMp3 = AUDIO_DIR + UUID.randomUUID() + ".mp3";
            System.out.println("Đang ghép MP3...");
            mergeMp3(mp3Files, finalMp3);
            System.out.println("Ghép xong!");
            // Upload Cloudinary
            File audioFile = new File(finalMp3);
            Map upload = cloudinaryService.uploadAudio(audioFile);
            String audioUrl = upload.get("secure_url").toString();
            // Update DB
            book.setAudioGenerated(true);
            book.setAudioUrl(audioUrl);
            digitalBookRepository.save(book);
            System.out.println("Hoàn thành!");
            System.out.println("Audio URL: " + audioUrl);
            // Xóa file tạm
            cleanup(mp3Files);
            audioFile.delete();

        } catch (Exception e) {
            System.out.println("Lỗi tạo audio:");
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    private List<String> splitSmart(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String[] sentences = text.split("(?<=[.!?])\\s+");
        for (String sentence : sentences) {
            if (current.length() + sentence.length() > maxLength) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            current.append(sentence).append(" ");
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private void mergeMp3(
            List<String> inputs,
            String output) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        for (String in : inputs) {
            cmd.add("-i");
            cmd.add(in);
        }
        cmd.add("-filter_complex");
        cmd.add("concat=n=" + inputs.size() + ":v=0:a=1");
        cmd.add("-y");
        cmd.add(output);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        boolean finished = p.waitFor(10, TimeUnit.MINUTES);
        if (!finished) {
            p.destroyForcibly();
            throw new RuntimeException(
                    "Ghép MP3 timeout");
        }
    }

    private void cleanup(List<String> files) {
        for (String file : files) {
            try {
                new File(file).delete();
            } catch (Exception ignored) {
            }
        }
    }
}
