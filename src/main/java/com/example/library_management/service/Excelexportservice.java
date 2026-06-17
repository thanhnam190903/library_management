package com.example.library_management.service;

import com.example.library_management.repository.BookTitleRepository;
import com.example.library_management.repository.BorrowDetailRepository;
import com.example.library_management.repository.ReaderRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Excelexportservice {
    BorrowDetailRepository borrowDetailRepo;
    BookTitleRepository bookTitleRepo;
    ReaderRepository readerRepo;

    private static final DateTimeFormatter VI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // 1. BÁO CÁO THÁNG TỔNG HỢP
    public byte[] exportMonthly(LocalDate from, LocalDate to) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sh = wb.createSheet("Tổng hợp");
            StyleKit sk  = new StyleKit(wb);

            // Title
            writeTitle(sh, sk, "BÁO CÁO TỔNG HỢP HOẠT ĐỘNG THƯ VIỆN", 0, 2);
            writeSubtitle(sh, sk, "Từ " + from.format(VI_DATE) + " đến " + to.format(VI_DATE), 1, 2);

            // KPI section
            int r = 3;
            r = writeSectionHeader(sh, sk, "I. CHỈ SỐ HOẠT ĐỘNG", r, 2);

            long totalBooks    = bookTitleRepo.countByDeletedFalse();
            long borrowing     = borrowDetailRepo.countAllBorrowing();
            long borrowInRange = borrowDetailRepo.countBorrowInRange(from, to);
            long returnInRange = borrowDetailRepo.countReturnInRange(from, to);
            double fineTotal   = borrowDetailRepo.sumFineInRange(from, to);
            long overdueCount  = borrowDetailRepo.countOverdueBooks();

            String[][] kpiData = {
                    {"Tổng đầu sách trong hệ thống",  String.valueOf(totalBooks)},
                    {"Số sách đang được mượn",          String.valueOf(borrowing)},
                    {"Lượt mượn trong kỳ",              String.valueOf(borrowInRange)},
                    {"Lượt trả trong kỳ",               String.valueOf(returnInRange)},
                    {"Số sách quá hạn hiện tại",        String.valueOf(overdueCount)},
                    {"Tổng phí phạt thu được",          String.format("%,.0f VND", fineTotal)},
            };

            writeDataHeader(sh, sk, r++, new String[]{"Chỉ số", "Giá trị"}, 2);
            for (String[] row : kpiData) {
                XSSFRow xr = sh.createRow(r++);
                createCell(xr, 0, row[0], sk.data);
                createCell(xr, 1, row[1], sk.dataRight);
            }
            // Top 10 books section
            r++;
            r = writeSectionHeader(sh, sk, "II. TOP 10 SÁCH ĐƯỢC MƯỢN NHIỀU NHẤT", r, 6);
            writeDataHeader(sh, sk, r++, new String[]{"#", "Tên sách", "Tác giả", "Thể loại", "Tổng lượt mượn", "Đang mượn"}, 6);

            List<Object[]> topBooks = borrowDetailRepo.findTopBorrowedBooks(from, to, PageRequest.of(0, 10));
            int rank = 1;
            for (Object[] row : topBooks) {
                XSSFRow xr = sh.createRow(r++);
                createCell(xr, 0, String.valueOf(rank++),           sk.dataCenter);
                createCell(xr, 1, str(row[1]),                       sk.data);
                createCell(xr, 2, str(row[2]),                       sk.data);
                createCell(xr, 3, str(row[3]),                       sk.data);
                createCell(xr, 4, str(row[4]),                       sk.dataRight);
                createCell(xr, 5, str(row[5]),                       sk.dataRight);
            }

            autoSize(sh, 6);
            sh.setColumnWidth(1, 45 * 256);
            return toBytes(wb);
        }
    }


    // 2. DANH SÁCH PHIẾU MƯỢN
    public byte[] exportBorrows(LocalDate from, LocalDate to) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sh = wb.createSheet("Danh sách mượn");
            StyleKit sk  = new StyleKit(wb);

            writeTitle(sh, sk, "DANH SÁCH PHIẾU MƯỢN SÁCH", 0, 9);
            writeSubtitle(sh, sk, "Từ " + from.format(VI_DATE) + " đến " + to.format(VI_DATE), 1, 9);

            String[] headers = {"Mã CT", "Mã phiếu", "Tên độc giả", "Tên sách",
                    "Ngày mượn", "Hạn trả", "Ngày trả", "Trạng thái", "Phí phạt (VND)"};
            writeDataHeader(sh, sk, 2, headers, headers.length);

            List<Object[]> rows = borrowDetailRepo.findBorrowsForExport(from, to);
            int r = 3;
            for (Object[] row : rows) {
                XSSFRow xr = sh.createRow(r++);
                createCell(xr, 0, str(row[0]), sk.data);
                createCell(xr, 1, str(row[1]), sk.data);
                createCell(xr, 2, str(row[2]), sk.data);
                createCell(xr, 3, str(row[3]), sk.data);
                createCell(xr, 4, formatDate(row[4]), sk.dataCenter);
                createCell(xr, 5, formatDate(row[5]), sk.dataCenter);
                createCell(xr, 6, formatDate(row[6]), sk.dataCenter);

                String status = str(row[7]);
                XSSFCell statusCell = xr.createCell(7);
                statusCell.setCellValue(status);
                statusCell.setCellStyle(
                        "QUÁ HẠN".equals(status) ? sk.statusOverdue
                                : "ĐÃ TRẢ".equals(status)  ? sk.statusReturned
                                : sk.statusBorrowing
                );

                double fine = row[8] instanceof Number ? ((Number) row[8]).doubleValue() : 0;
                createCell(xr, 8, fine > 0 ? String.format("%,.0f", fine) : "—", sk.dataRight);
            }

            autoSize(sh, headers.length);
            sh.setColumnWidth(2, 30 * 256);
            sh.setColumnWidth(3, 45 * 256);
            return toBytes(wb);
        }
    }

    // 3. DANH SÁCH QUÁ HẠN
    public byte[] exportOverdue() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sh = wb.createSheet("Quá hạn");
            StyleKit sk  = new StyleKit(wb);

            writeTitle(sh, sk, "DANH SÁCH SÁCH QUÁ HẠN", 0, 7);
            writeSubtitle(sh, sk, "Tính đến ngày: " + LocalDate.now().format(VI_DATE), 1, 7);

            String[] headers = {"Mã phiếu", "Tên độc giả", "Tên sách",
                    "Ngày mượn", "Hạn trả", "Số ngày quá hạn", "Phí phạt (VND)"};
            writeDataHeader(sh, sk, 2, headers, headers.length);

            List<Object[]> rows = borrowDetailRepo.findOverdueForExport();
            int r = 3;
            for (Object[] row : rows) {
                XSSFRow xr = sh.createRow(r++);
                createCell(xr, 0, str(row[0]),    sk.data);
                createCell(xr, 1, str(row[1]),    sk.data);
                createCell(xr, 2, str(row[2]),    sk.data);
                createCell(xr, 3, formatDate(row[3]), sk.dataCenter);
                createCell(xr, 4, formatDate(row[4]), sk.statusOverdue);  // highlight due date
                createCell(xr, 5, str(row[5]) + " ngày", sk.dataCenter);
                double fine = row[6] instanceof Number ? ((Number) row[6]).doubleValue() : 0;
                createCell(xr, 6, fine > 0 ? String.format("%,.0f", fine) : "—", sk.dataRight);
            }

            // Summary row
            XSSFRow sumRow = sh.createRow(r + 1);
            createCell(sumRow, 0, "TỔNG CỘNG", sk.headerSmall);
            createCell(sumRow, 6, "=SUM(G4:G" + (r) + ")", sk.totalRight);

            autoSize(sh, headers.length);
            sh.setColumnWidth(1, 30 * 256);
            sh.setColumnWidth(2, 45 * 256);
            return toBytes(wb);
        }
    }

    // 4. DANH SÁCH HỘI VIÊN
    public byte[] exportMembers() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sh = wb.createSheet("Hội viên");
            StyleKit sk  = new StyleKit(wb);

            writeTitle(sh, sk, "DANH SÁCH HỘI VIÊN THƯ VIỆN", 0, 7);
            writeSubtitle(sh, sk, "Xuất ngày: " + LocalDate.now().format(VI_DATE), 1, 7);

            String[] headers = {"Mã thẻ", "Họ tên", "Email", "Điện thoại",
                    "Ngày cấp thẻ", "Ngày hết hạn", "Trạng thái"};
            writeDataHeader(sh, sk, 2, headers, headers.length);

            List<Object[]> rows = readerRepo.findAllForExport();
            int r = 3;
            for (Object[] row : rows) {
                XSSFRow xr = sh.createRow(r++);
                createCell(xr, 0, str(row[0]), sk.data);
                createCell(xr, 1, str(row[1]), sk.data);
                createCell(xr, 2, str(row[2]), sk.data);
                createCell(xr, 3, str(row[3]), sk.dataCenter);
                createCell(xr, 4, formatDate(row[4]), sk.dataCenter);
                createCell(xr, 5, formatDate(row[5]), sk.dataCenter);
                String status = str(row[6]);
                XSSFCell sc = xr.createCell(6);
                sc.setCellValue(status);
                sc.setCellStyle("ĐANG HOẠT ĐỘNG".equals(status) ? sk.statusBorrowing : sk.statusOverdue);
            }

            autoSize(sh, headers.length);
            sh.setColumnWidth(1, 30 * 256);
            sh.setColumnWidth(2, 35 * 256);
            return toBytes(wb);
        }
    }


    // 5. KHO SÁCH
    public byte[] exportInventory() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sh = wb.createSheet("Kho sách");
            StyleKit sk  = new StyleKit(wb);

            writeTitle(sh, sk, "BÁO CÁO KHO SÁCH", 0, 5);
            writeSubtitle(sh, sk, "Xuất ngày: " + LocalDate.now().format(VI_DATE), 1, 5);

            String[] headers = {"Tên sách", "Tác giả", "Thể loại",
                    "Số bản sao", "Đang mượn"};
            writeDataHeader(sh, sk, 2, headers, headers.length);

            List<Object[]> rows = bookTitleRepo.findInventoryForExport();
            int r = 3;
            long totalCopies    = 0;
            long totalBorrowing = 0;
            for (Object[] row : rows) {
                XSSFRow xr = sh.createRow(r++);
                createCell(xr, 0, str(row[0]), sk.data);
                createCell(xr, 1, str(row[1]), sk.data);
                createCell(xr, 2, str(row[2]), sk.data);
                long copies    = row[3] instanceof Number ? ((Number) row[3]).longValue() : 0;
                long borrowing = row[4] instanceof Number ? ((Number) row[4]).longValue() : 0;
                totalCopies    += copies;
                totalBorrowing += borrowing;
                createCell(xr, 3, String.valueOf(copies),    sk.dataRight);
                createCell(xr, 4, String.valueOf(borrowing), sk.dataRight);
            }

            // Totals
            XSSFRow sumRow = sh.createRow(r + 1);
            createCell(sumRow, 0, "TỔNG CỘNG", sk.headerSmall);
            createCell(sumRow, 3, "=SUM(D4:D" + r + ")", sk.totalRight);
            createCell(sumRow, 4, "=SUM(E4:E" + r + ")", sk.totalRight);

            autoSize(sh, headers.length);
            sh.setColumnWidth(0, 50 * 256);
            return toBytes(wb);
        }
    }


    // 6. TÀI CHÍNH
    public byte[] exportFinance(LocalDate from, LocalDate to) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sh = wb.createSheet("Tài chính");
            StyleKit sk  = new StyleKit(wb);

            writeTitle(sh, sk, "BÁO CÁO TÀI CHÍNH THƯ VIỆN", 0, 5);
            writeSubtitle(sh, sk, "Từ " + from.format(VI_DATE) + " đến " + to.format(VI_DATE), 1, 5);

            // Summary KPI
            int r = 3;
            r = writeSectionHeader(sh, sk, "I. TỔNG KẾT", r, 5);
            long borrowCount  = borrowDetailRepo.countBorrowInRange(from, to);
            long returnCount  = borrowDetailRepo.countReturnInRange(from, to);
            double totalFine  = borrowDetailRepo.sumFineInRange(from, to);

            writeDataHeader(sh, sk, r++, new String[]{"Chỉ số", "Giá trị"}, 2);
            String[][] kpi = {
                    {"Tổng lượt mượn trong kỳ",    String.valueOf(borrowCount)},
                    {"Tổng lượt trả trong kỳ",     String.valueOf(returnCount)},
                    {"Tổng phí phạt thu được (VND)", String.format("%,.0f", totalFine)},
            };
            for (String[] kRow : kpi) {
                XSSFRow xr = sh.createRow(r++);
                createCell(xr, 0, kRow[0], sk.data);
                createCell(xr, 1, kRow[1], sk.dataRight);
            }

            // Detail
            r += 2;
            r = writeSectionHeader(sh, sk, "II. CHI TIẾT THU PHÍ PHẠT", r, 5);
            String[] headers = {"Mã phiếu", "Tên độc giả", "Ngày trả", "Loại phí", "Số tiền (VND)"};
            writeDataHeader(sh, sk, r++, headers, headers.length);

            List<Object[]> rows = borrowDetailRepo.findFinanceForExport(from, to);
            int startDetailRow = r + 1;
            for (Object[] row : rows) {
                XSSFRow xr = sh.createRow(r++);
                createCell(xr, 0, str(row[0]),    sk.data);
                createCell(xr, 1, str(row[1]),    sk.data);
                createCell(xr, 2, formatDate(row[2]), sk.dataCenter);
                createCell(xr, 3, str(row[3]),    sk.data);
                double fine = row[4] instanceof Number ? ((Number) row[4]).doubleValue() : 0;
                createCell(xr, 4, String.format("%,.0f", fine), sk.dataRight);
            }

            // Totals
            XSSFRow sumRow = sh.createRow(r + 1);
            createCell(sumRow, 0, "TỔNG CỘNG", sk.headerSmall);
            createCell(sumRow, 4, "=SUM(E" + startDetailRow + ":E" + r + ")", sk.totalRight);

            autoSize(sh, headers.length);
            sh.setColumnWidth(1, 30 * 256);
            return toBytes(wb);
        }
    }


    // HELPER METHODS
    private void writeTitle(XSSFSheet sh, StyleKit sk, String text, int rowNum, int colSpan) {
        XSSFRow row = sh.createRow(rowNum);
        row.setHeightInPoints(28);
        XSSFCell cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(sk.title);
        if (colSpan > 1) sh.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, colSpan - 1));
    }

    private void writeSubtitle(XSSFSheet sh, StyleKit sk, String text, int rowNum, int colSpan) {
        XSSFRow row = sh.createRow(rowNum);
        row.setHeightInPoints(18);
        XSSFCell cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(sk.subtitle);
        if (colSpan > 1) sh.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, colSpan - 1));
    }

    private int writeSectionHeader(XSSFSheet sh, StyleKit sk, String text, int rowNum, int colSpan) {
        XSSFRow row = sh.createRow(rowNum);
        row.setHeightInPoints(20);
        XSSFCell cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(sk.sectionHeader);
        if (colSpan > 1) sh.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, colSpan - 1));
        return rowNum + 1;
    }

    private void writeDataHeader(XSSFSheet sh, StyleKit sk, int rowNum, String[] cols, int colCount) {
        XSSFRow row = sh.createRow(rowNum);
        row.setHeightInPoints(18);
        for (int i = 0; i < cols.length; i++) {
            XSSFCell cell = row.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(sk.header);
        }
    }

    private void createCell(XSSFRow row, int col, String value, XSSFCellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        if (style != null) cell.setCellStyle(style);
    }

    private String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private String formatDate(Object o) {
        if (o == null) return "";
        if (o instanceof java.sql.Date)      return ((java.sql.Date) o).toLocalDate().format(VI_DATE);
        if (o instanceof java.time.LocalDate) return ((LocalDate) o).format(VI_DATE);
        String s = o.toString();
        try { return LocalDate.parse(s).format(VI_DATE); } catch (Exception e) { return s; }
    }

    private void autoSize(XSSFSheet sh, int colCount) {
        for (int i = 0; i < colCount; i++) sh.autoSizeColumn(i);
    }

    private byte[] toBytes(XSSFWorkbook wb) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STYLE KIT  — all cell styles defined once
    // ─────────────────────────────────────────────────────────────────────────
    private static class StyleKit {
        final XSSFCellStyle title, subtitle, sectionHeader, headerSmall,
                header, data, dataCenter, dataRight,
                statusBorrowing, statusOverdue, statusReturned,
                totalRight;

        StyleKit(XSSFWorkbook wb) {
            XSSFFont boldLarge = wb.createFont();
            boldLarge.setBold(true);
            boldLarge.setFontHeightInPoints((short) 14);
            boldLarge.setColor(IndexedColors.DARK_BLUE.getIndex());
            boldLarge.setFontName("Arial");

            XSSFFont boldSmall = wb.createFont();
            boldSmall.setBold(true);
            boldSmall.setFontHeightInPoints((short) 11);
            boldSmall.setFontName("Arial");

            XSSFFont boldWhite = wb.createFont();
            boldWhite.setBold(true);
            boldWhite.setColor(IndexedColors.WHITE.getIndex());
            boldWhite.setFontHeightInPoints((short) 11);
            boldWhite.setFontName("Arial");

            XSSFFont normal = wb.createFont();
            normal.setFontHeightInPoints((short) 11);
            normal.setFontName("Arial");

            XSSFFont subtitleFont = wb.createFont();
            subtitleFont.setItalic(true);
            subtitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            subtitleFont.setFontHeightInPoints((short) 11);
            subtitleFont.setFontName("Arial");

            // ── title
            title = wb.createCellStyle();
            title.setFont(boldLarge);
            title.setAlignment(HorizontalAlignment.CENTER);
            title.setVerticalAlignment(VerticalAlignment.CENTER);

            // ── subtitle
            subtitle = wb.createCellStyle();
            subtitle.setFont(subtitleFont);
            subtitle.setAlignment(HorizontalAlignment.CENTER);

            // ── section header (gold background)
            sectionHeader = wb.createCellStyle();
            sectionHeader.setFont(boldSmall);
            sectionHeader.setFillForegroundColor(new XSSFColor(new byte[]{(byte)201,(byte)150,(byte)58}, null));
            sectionHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sectionHeader.setFont(boldWhite);
            sectionHeader.setAlignment(HorizontalAlignment.LEFT);

            // ── table header (dark navy)
            header = wb.createCellStyle();
            header.setFont(boldWhite);
            header.setFillForegroundColor(new XSSFColor(new byte[]{(byte)44,(byte)62,(byte)80}, null));
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(header);

            // ── small header (used in totals label)
            headerSmall = wb.createCellStyle();
            headerSmall.setFont(boldSmall);
            headerSmall.setFillForegroundColor(new XSSFColor(new byte[]{(byte)230,(byte)224,(byte)210}, null));
            headerSmall.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorder(headerSmall);

            // ── data (left-aligned)
            data = wb.createCellStyle();
            data.setFont(normal);
            data.setAlignment(HorizontalAlignment.LEFT);
            data.setWrapText(false);
            setBorder(data);

            // ── data center
            dataCenter = wb.createCellStyle();
            dataCenter.setFont(normal);
            dataCenter.setAlignment(HorizontalAlignment.CENTER);
            setBorder(dataCenter);

            // ── data right
            dataRight = wb.createCellStyle();
            dataRight.setFont(normal);
            dataRight.setAlignment(HorizontalAlignment.RIGHT);
            setBorder(dataRight);

            // ── status: borrowing (green bg)
            statusBorrowing = wb.createCellStyle();
            statusBorrowing.cloneStyleFrom(dataCenter);
            XSSFFont greenFont = wb.createFont();
            greenFont.setBold(true);
            greenFont.setColor(new XSSFColor(new byte[]{(byte)45,(byte)106,(byte)63}, null));
            greenFont.setFontName("Arial");
            greenFont.setFontHeightInPoints((short) 11);
            statusBorrowing.setFont(greenFont);
            statusBorrowing.setFillForegroundColor(new XSSFColor(new byte[]{(byte)230,(byte)242,(byte)236}, null));
            statusBorrowing.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ── status: overdue (red bg)
            statusOverdue = wb.createCellStyle();
            statusOverdue.cloneStyleFrom(dataCenter);
            XSSFFont redFont = wb.createFont();
            redFont.setBold(true);
            redFont.setColor(new XSSFColor(new byte[]{(byte)176,(byte)0,(byte)32}, null));
            redFont.setFontName("Arial");
            redFont.setFontHeightInPoints((short) 11);
            statusOverdue.setFont(redFont);
            statusOverdue.setFillForegroundColor(new XSSFColor(new byte[]{(byte)255,(byte)235,(byte)235}, null));
            statusOverdue.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ── status: returned (grey)
            statusReturned = wb.createCellStyle();
            statusReturned.cloneStyleFrom(dataCenter);
            XSSFFont greyFont = wb.createFont();
            greyFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            greyFont.setFontName("Arial");
            greyFont.setFontHeightInPoints((short) 11);
            statusReturned.setFont(greyFont);

            // ── total right (bold + gold bg)
            totalRight = wb.createCellStyle();
            totalRight.setFont(boldSmall);
            totalRight.setAlignment(HorizontalAlignment.RIGHT);
            totalRight.setFillForegroundColor(new XSSFColor(new byte[]{(byte)255,(byte)243,(byte)205}, null));
            totalRight.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorder(totalRight);
        }

        private static void setBorder(XSSFCellStyle s) {
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderTop(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
            s.setBottomBorderColor(new XSSFColor(new byte[]{(byte)200,(byte)195,(byte)180}, null));
            s.setTopBorderColor(   new XSSFColor(new byte[]{(byte)200,(byte)195,(byte)180}, null));
            s.setLeftBorderColor(  new XSSFColor(new byte[]{(byte)200,(byte)195,(byte)180}, null));
            s.setRightBorderColor( new XSSFColor(new byte[]{(byte)200,(byte)195,(byte)180}, null));
        }
    }
}
