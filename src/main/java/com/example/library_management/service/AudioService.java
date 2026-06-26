package com.example.library_management.service;

import com.example.library_management.entity.DigitalBook;
import com.example.library_management.repository.DigitalBookRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AudioService {

        private final DigitalBookRepository digitalBookRepository;
        private final CloudinaryService cloudinaryService;
        private static final String AUDIO_DIR = "D:/audio/";
        private static final String FFMPEG_PATH = "D:/software/ffmpeg-8.1.1-essentials_build/bin/ffmpeg.exe";
        private static final int CHUNK_SIZE = 4000;
        private static final int MAX_PARALLEL_TTS = 3;

        private ExecutorService ttsExecutor;

    @PostConstruct
    public void setup() {
        new File(AUDIO_DIR).mkdirs();
        ttsExecutor = Executors.newFixedThreadPool(MAX_PARALLEL_TTS);
        System.out.println("Khởi tạo xong thư mục lưu audio");
    }

    @PreDestroy
    public void cleanUpPool() {
        if (ttsExecutor != null) {
            ttsExecutor.shutdown();
        }
    }

    @Async
    public void generateAudio(String bookId) {
        List<String> partFiles = new ArrayList<>();
        String concatListPath = null;
        File mergedFile = null;

        System.out.println("Bắt đầu tạo audio cho sách: " + bookId);

        try {
            DigitalBook book = digitalBookRepository.findById(bookId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sách id=" + bookId));

            String rawText = trichXuatTextTuPdf(book.getFileUrl());
            if (rawText == null || rawText.isBlank()) {
                throw new RuntimeException("File PDF rỗng, không lấy được nội dung");
            }
            System.out.println("Lấy được " + rawText.length() + " ký tự từ PDF");
            String textSach = gomVeMotDong(rawText);

            List<String> cacDoanVan = chiaVanBan(textSach, CHUNK_SIZE);
            System.out.println("Tổng chunk " + cacDoanVan.size());

            partFiles = taoAmThanhChoTatCa(bookId, cacDoanVan);
            System.out.println("Đã tạo " + partFiles.size() + " file mp3");

            String fileGopLai = AUDIO_DIR + UUID.randomUUID() + ".mp3";
            concatListPath = AUDIO_DIR + "ds_ghep_" + bookId + "_" + UUID.randomUUID() + ".txt";
            ghepCacFileMp3(partFiles, fileGopLai, concatListPath);
            System.out.println("Ghép file xong");

            mergedFile = new File(fileGopLai);
            Map ketQuaUpload = cloudinaryService.uploadAudio(mergedFile);
            String urlAudio = ketQuaUpload.get("secure_url").toString();
            book.setAudioGenerated(true);
            book.setAudioUrl(urlAudio);
            digitalBookRepository.save(book);

            System.out.println("Hoàn thành audio ");

        } catch (Exception ex) {
            System.out.println("LỖI khi xử lý sách " + bookId + ": " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            xoaFileTam(partFiles);
            if (concatListPath != null) new File(concatListPath).delete();
            if (mergedFile != null) mergedFile.delete();
        }
    }

    // Đọc toàn bộ nội dung text trong file PDF từ url
    private String trichXuatTextTuPdf(String pdfUrl) throws Exception {
        try (InputStream in = new URL(pdfUrl).openStream();
             PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    // Bỏ xuống dòng để tránh lỗi tách argument khi gọi process trên Windows
    private String gomVeMotDong(String text) {
        return text.replaceAll("\\r\\n|\\r|\\n", " ")
                .replaceAll(" {2,}", " ")
                .trim();
    }

    // Cắt theo câu để không bị cắt giữa câu, mỗi đoạn không vượt quá maxLen
    private List<String> chiaVanBan(String text, int maxLen) {
        List<String> ketQua = new ArrayList<>();
        StringBuilder dangGop = new StringBuilder();
        String[] cauArr = text.split("(?<=[.!?])\\s+");

        for (String cau : cauArr) {
            if (dangGop.length() + cau.length() > maxLen) {
                ketQua.add(dangGop.toString());
                dangGop.setLength(0);
            }
            dangGop.append(cau).append(" ");
        }
        if (!dangGop.isEmpty()) {
            ketQua.add(dangGop.toString());
        }
        return ketQua;
    }

    // Chạy song song các đoạn văn qua ttsExecutor, đợi tất cả xong rồi mới trả kết quả.
    // Nếu 1 đoạn lỗi thì join() sẽ throw, bắn lên cho generateAudio xử lý chung.
    private List<String> taoAmThanhChoTatCa(String bookId, List<String> cacDoanVan) {
        List<CompletableFuture<String>> dsTask = new ArrayList<>();

        for (int i = 0; i < cacDoanVan.size(); i++) {
            int idx = i;
            String doan = cacDoanVan.get(i);
            dsTask.add(CompletableFuture.supplyAsync(
                    () -> chayTtsChoMotDoan(bookId, idx, doan), ttsExecutor));
        }

        return dsTask.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    // Gọi process python -m edge_tts để chuyển 1 đoạn văn thành 1 file mp3
    private String chayTtsChoMotDoan(String bookId, int idx, String noiDung) {
        int soLanThuLai = 3;
        Exception loiCuoiCung = null;

        for (int lan = 1; lan <= soLanThuLai; lan++) {
            try {
                return chayEdgeTtsMotLan(bookId, idx, noiDung);
            } catch (Exception e) {
                loiCuoiCung = e;
                System.out.println("Chunk " + idx + " lỗi lần " + lan + "/" + soLanThuLai + ": " + e.getMessage());
                try {
                    Thread.sleep(2000L * lan); // đợi lâu hơn mỗi lần thử lại
                } catch (InterruptedException ignored) {}
            }
        }
        throw new RuntimeException("Chunk " + idx + " thất bại sau " + soLanThuLai + " lần thử: "
                + loiCuoiCung.getMessage(), loiCuoiCung);
    }

    // Đổi tên hàm cũ chayTtsChoMotDoan -> chayEdgeTtsMotLan, giữ nguyên toàn bộ nội dung bên trong
    private String chayEdgeTtsMotLan(String bookId, int idx, String noiDung) {
        String fileMp3 = AUDIO_DIR + bookId + "_part_" + idx + ".mp3";
        String fileTextTam = AUDIO_DIR + bookId + "_part_" + idx + ".txt";
        Process proc = null;

        try {
            try (Writer w = new OutputStreamWriter(new FileOutputStream(fileTextTam), StandardCharsets.UTF_8)) {
                w.write(noiDung);
            }

            ProcessBuilder pb = new ProcessBuilder(
                    "python", "-m", "edge_tts",
                    "-f", fileTextTam,
                    "--voice", "vi-VN-HoaiMyNeural",
                    "--write-media", fileMp3
            );
            pb.redirectErrorStream(true);
            proc = pb.start();

            String logLoi = docOutputProcess(proc);

            boolean chayXong = proc.waitFor(5, TimeUnit.MINUTES);
            if (!chayXong) {
                proc.destroyForcibly();
                throw new RuntimeException("Chunk " + idx + " chạy quá 5 phút, hủy");
            }
            if (proc.exitValue() != 0) {
                throw new RuntimeException("edge_tts lỗi ở chunk " + idx + ", exit code " + proc.exitValue() + "\n" + logLoi);
            }

            System.out.println("Chunk " + idx + " xong: " + fileMp3);
            return fileMp3;

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Chunk " + idx + " thất bại: " + e.getMessage(), e);
        } finally {
            if (proc != null) proc.destroyForcibly();
            new File(fileTextTam).delete();
        }
    }

    private String docOutputProcess(Process proc) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String dong;
            while ((dong = br.readLine()) != null) {
                sb.append(dong).append("\n");
            }
        }
        return sb.toString();
    }

    // Ghép nhiều mp3 thành 1 bằng ffmpeg concat demuxer (copy thẳng, không encode lại nên rất nhanh)
    private void ghepCacFileMp3(List<String> dsFile, String fileOutput, String duongDanFileList) throws Exception {
        try (PrintWriter pw = new PrintWriter(duongDanFileList)) {
            for (String f : dsFile) {
                String safe = f.replace("'", "'\\''");
                pw.println("file '" + safe + "'");
            }
        }

        List<String> lenh = List.of(
                FFMPEG_PATH, "-f", "concat", "-safe", "0",
                "-i", duongDanFileList, "-c", "copy", "-y", fileOutput
        );
        ProcessBuilder pb = new ProcessBuilder(lenh);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        boolean xong = p.waitFor(5, TimeUnit.MINUTES);
        if (!xong) {
            p.destroyForcibly();
            throw new RuntimeException("Ghép mp3 quá lâu, timeout");
        }
        if (p.exitValue() != 0) {
            throw new RuntimeException("ffmpeg lỗi khi ghép, exit code " + p.exitValue());
        }
    }

    private void xoaFileTam(List<String> files) {
        if (files == null) return;
        for (String f : files) {
            try {
                new File(f).delete();
            } catch (Exception ignored) {
            }
        }
    }
}
