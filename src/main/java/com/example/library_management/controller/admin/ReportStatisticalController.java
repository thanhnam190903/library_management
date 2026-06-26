package com.example.library_management.controller.admin;

import com.example.library_management.dto.BookDTO;
import com.example.library_management.dto.CategoryDTO;
import com.example.library_management.dto.StatisticsDTO;
import com.example.library_management.entity.DigitalBook;
import com.example.library_management.repository.BookTitleRepository;
import com.example.library_management.repository.BorrowDetailRepository;
import com.example.library_management.repository.DigitalBookRepository;
import com.example.library_management.repository.ReaderRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/quan-ly")
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@RequiredArgsConstructor
public class ReportStatisticalController {

    BookTitleRepository bookTitleRepo;
    BorrowDetailRepository borrowDetailRepo;
    ReaderRepository readerRepo;
    DigitalBookRepository digitalBookRepository;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Set<DayOfWeek> WEEKEND =
            Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    @GetMapping("/statistics")
    public String showReport(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            Model model) {

        // ── Ngày hiện tại & range ─────────────────────────────────────────
        LocalDate today = LocalDate.now();
        DateTimeFormatter viFormatter = DateTimeFormatter
                .ofPattern("EEEE, dd 'tháng' M 'năm' yyyy", new Locale("vi", "VN"));

        LocalDate[] range = resolveDateRange(period, from, to);
        LocalDate dateFrom = range[0];
        LocalDate dateTo   = range[1];

        int year      = dateTo.getYear();
        int month     = dateTo.getMonthValue();
        int prevMonth = month == 1 ? 12     : month - 1;
        int prevYear  = month == 1 ? year-1 : year;

        // ── KPI 1: Tổng đầu sách & diff so tháng trước ───────────────────
        long totalBooks = bookTitleRepo.countByDeletedFalse();
        long booksDiff  = bookTitleRepo.countCreatedBetween(
                LocalDateTime.of(dateTo.withDayOfMonth(1), LocalTime.MIDNIGHT),
                LocalDateTime.of(dateTo, LocalTime.MAX))
                - bookTitleRepo.countCreatedBetween(
                LocalDateTime.of(dateTo.minusMonths(1).withDayOfMonth(1), LocalTime.MIDNIGHT),
                LocalDateTime.of(dateTo.withDayOfMonth(1).minusDays(1), LocalTime.MAX));

        // ── KPI 2: Đang mượn & % vs tuần trước ───────────────────────────
        long currentlyBorrowing = borrowDetailRepo.countAllBorrowing();
        long prevWeekCount = borrowDetailRepo.countBorrowingInRange(
                dateFrom.minusWeeks(1), dateTo.minusWeeks(1));
        double borrowPct = prevWeekCount == 0
                ? (currentlyBorrowing > 0 ? 100.0 : 0.0)
                : ((double)(currentlyBorrowing - prevWeekCount) / prevWeekCount) * 100;

        // ── KPI 3: Hội viên tích cực & đăng ký mới ───────────────────────
        long activeMembers = readerRepo.countActiveMembers(today);
        long newThisMonth  = readerRepo.countNewThisMonth(year, month);

        // ── KPI 4: Lượt mượn trong range & % vs kỳ trước ─────────────────

        long borrowThisMonth = borrowDetailRepo.countByMonth(year, month);
        long borrowPrevMonth = borrowDetailRepo.countByMonth(prevYear, prevMonth);
        System.out.println("this month = " + borrowThisMonth);
        System.out.println("prev month = " + borrowPrevMonth);
        double borrowMonthPct = borrowPrevMonth == 0
                ? (borrowThisMonth > 0 ? 100.0 : 0.0)
                : ((double)(borrowThisMonth - borrowPrevMonth) / borrowPrevMonth) * 100;

        // ── Bar chart: theo ngày (≤31 ngày) hoặc theo tháng (>31 ngày) ───
        List<StatisticsDTO.DailyBorrowDTO> dailyBorrows = new ArrayList<>();
        long daySpan = ChronoUnit.DAYS.between(dateFrom, dateTo);

        if (daySpan <= 31) {
            Map<LocalDate, Long> dayMap = new HashMap<>();
            for (Object[] r : borrowDetailRepo.countByDayInRange(dateFrom, dateTo)) {
                LocalDate d;
                if (r[0] instanceof java.sql.Date)
                    d = ((java.sql.Date) r[0]).toLocalDate();
                else
                    d = LocalDate.parse(r[0].toString());
                dayMap.put(d, ((Number) r[1]).longValue());
            }
            for (LocalDate d = dateFrom; !d.isAfter(dateTo); d = d.plusDays(1)) {
                long cnt = dayMap.getOrDefault(d, 0L);
                String dayType = WEEKEND.contains(d.getDayOfWeek()) ? "weekend"
                        : cnt > 120 ? "peak" : "weekday";
                dailyBorrows.add(new StatisticsDTO.DailyBorrowDTO(d.getDayOfMonth(), cnt, dayType));
            }
        } else {
            Map<String, Long> monthMap = new HashMap<>();
            for (Object[] r : borrowDetailRepo.countByMonthInRange(dateFrom, dateTo)) {
                // r[0]=year, r[1]=month, r[2]=count
                String key = ((Number) r[0]).intValue() + "-" + ((Number) r[1]).intValue();
                monthMap.put(key, ((Number) r[2]).longValue());
            }
            LocalDate cursor = dateFrom.withDayOfMonth(1);
            LocalDate end    = dateTo.withDayOfMonth(1);
            while (!cursor.isAfter(end)) {
                String key = cursor.getYear() + "-" + cursor.getMonthValue();
                long cnt = monthMap.getOrDefault(key, 0L);
                dailyBorrows.add(new StatisticsDTO.DailyBorrowDTO(
                        cursor.getMonthValue(), cnt, "weekday"));
                cursor = cursor.plusMonths(1);
            }
        }

        long maxDailyCount = dailyBorrows.stream()
                .mapToLong(StatisticsDTO.DailyBorrowDTO::getCount)
                .max().orElse(1);

        // ── Donut: phân bố thể loại ───────────────────────────────────────
        List<CategoryDTO> categoryStats = new ArrayList<>();
        long sumTop = 0;
        List<Object[]> rawCat = bookTitleRepo.countByCategory();
        for (int i = 0; i < Math.min(rawCat.size(), 4); i++) {
            long cnt = ((Number) rawCat.get(i)[1]).longValue();
            categoryStats.add(new CategoryDTO(
                    (String) rawCat.get(i)[0], cnt,
                    totalBooks == 0 ? 0 : Math.round(cnt * 1000.0 / totalBooks) / 10.0));
            sumTop += cnt;
        }
        long otherCnt = totalBooks - sumTop;
        if (otherCnt > 0)
            categoryStats.add(new CategoryDTO("Khác", otherCnt,
                    Math.round(otherCnt * 1000.0 / totalBooks) / 10.0));

        // ── Top 10 sách được mượn nhiều nhất ─────────────────────────────
        List<BookDTO> topBooks = borrowDetailRepo
                .findTopBorrowedBooks(dateFrom, dateTo, PageRequest.of(0, 5))
                .stream()
                .filter(r -> r.length >= 6)   // ← thêm dòng này
                .map(r -> new BookDTO(
                        (String) r[0],
                        (String) r[1],
                        r[2] != null ? (String) r[2] : "—",
                        r[3] != null ? (String) r[3] : "—",
                        ((Number) r[4]).longValue(),
                        ((Number) r[5]).longValue(),
                        r.length > 6 && r[6] != null ? (String) r[6] : null))  // ← kiểm tra r.length
                .collect(Collectors.toList());

        // ── Build StatisticsDTO ───────────────────────────────────────────
        StatisticsDTO stats = StatisticsDTO.builder()
                .totalBooks(totalBooks)
                .totalBooksDiff(booksDiff)
                .currentlyBorrowing(currentlyBorrowing)
                .borrowingChangePct(borrowPct)
                .activeMembers(activeMembers)
                .newMembersThisMonth(newThisMonth)
                .borrowCountThisMonth(borrowThisMonth)
                .borrowMonthChangePct(borrowMonthPct)
                .dailyBorrows(dailyBorrows)
                .categoryStats(categoryStats)
                .topBooks(topBooks)
                .fromDate(dateFrom.toString())
                .toDate(dateTo.toString())
                .period(period)
                .build();

        Pageable pageable = PageRequest.of(0, 5);
        List<DigitalBook> topDigital =
                digitalBookRepository.findTopDigitalBooks(pageable);
        model.addAttribute("topDigital",topDigital);
        model.addAttribute("title",     today.format(viFormatter));
        model.addAttribute("sub",       "Thống kê & Báo cáo");
        model.addAttribute("activePage","report-sta");
        model.addAttribute("stats",     stats);
        model.addAttribute("period",    period);
        model.addAttribute("fromDisplay", dateFrom.format(DISPLAY_FMT));
        model.addAttribute("toDisplay",   dateTo.format(DISPLAY_FMT));
        model.addAttribute("fromValue",   dateFrom.toString());
        model.addAttribute("toValue",     dateTo.toString());
        model.addAttribute("currentMonth", month);
        model.addAttribute("currentYear",  year);
        model.addAttribute("prevMonth",    prevMonth);
        model.addAttribute("maxDailyCount", maxDailyCount);
        model.addAttribute("daySpan",      daySpan);

        return "admin/report-statistical";
    }

    private LocalDate[] resolveDateRange(String period, String fromStr, String toStr) {
        LocalDate today = LocalDate.now();
        return switch (period) {
            case "week"   -> new LocalDate[]{ today.with(DayOfWeek.MONDAY), today };
            case "quarter" -> {
                int qs = ((today.getMonthValue() - 1) / 3) * 3 + 1;
                yield new LocalDate[]{
                        today.withMonth(qs).withDayOfMonth(1),
                        today.withMonth(qs + 2).with(TemporalAdjusters.lastDayOfMonth())
                };
            }
            case "year"   -> new LocalDate[]{ today.withDayOfYear(1), today };
            case "custom" -> new LocalDate[]{ LocalDate.parse(fromStr), LocalDate.parse(toStr) };
            default       -> new LocalDate[]{ today.withDayOfMonth(1), today };
        };
    }
}
