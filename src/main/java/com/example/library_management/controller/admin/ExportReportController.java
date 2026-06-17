package com.example.library_management.controller.admin;

import com.example.library_management.service.Excelexportservice;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/quan-ly/statistics")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExportReportController {
    Excelexportservice excelExportService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "monthly") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to)
            throws IOException {

        // Default range: current month
        LocalDate today = LocalDate.now();
        if (from == null) from = today.withDayOfMonth(1);
        if (to == null)   to   = today;

        byte[] data;
        String filename;

        data = switch (type) {
            case "borrows"   -> excelExportService.exportBorrows(from, to);
            case "overdue"   -> excelExportService.exportOverdue();
            case "members"   -> excelExportService.exportMembers();
            case "inventory" -> excelExportService.exportInventory();
            case "finance"   -> excelExportService.exportFinance(from, to);
            default          -> excelExportService.exportMonthly(from, to);   // "monthly"
        };

        filename = switch (type) {
            case "borrows"   -> "DanhSachMuon_"   + from.format(FMT) + "_" + to.format(FMT) + ".xlsx";
            case "overdue"   -> "QuaHan_"         + today.format(FMT)                        + ".xlsx";
            case "members"   -> "HoiVien_"        + today.format(FMT)                        + ".xlsx";
            case "inventory" -> "KhoSach_"        + today.format(FMT)                        + ".xlsx";
            case "finance"   -> "TaiChinh_"       + from.format(FMT) + "_" + to.format(FMT) + ".xlsx";
            default          -> "BaoCaoThang_"    + from.format(FMT) + "_" + to.format(FMT) + ".xlsx";
        };

        // Encode filename for Content-Disposition (RFC 5987)
        String encoded = java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename)           // ASCII fallback
                .build());
        // RFC 5987 extended header for Vietnamese filenames
        headers.set("Content-Disposition",
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded);

        return ResponseEntity.ok().headers(headers).body(data);
    }
}
