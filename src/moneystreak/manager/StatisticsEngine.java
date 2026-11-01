package moneystreak.manager;

import moneystreak.model.StatEntry;
import moneystreak.model.Transaction;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 기간별 거래 집계 (F3 캘린더 조회, F4 통계 및 시각화).
 * 좁은 기간은 날짜별 표(캘린더)로, 넓은 기간은 주 단위 그래프로 보여주는 데 쓰인다.
 */
public class StatisticsEngine {

    private final TransactionManager transactionManager;

    public StatisticsEngine(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * 한 달치 날짜별 수입/지출 합계 (캘린더 조회용).
     * 거래가 없는 날짜도 0원짜리 StatEntry로 채워서 반환한다.
     */
    public Map<LocalDate, StatEntry> getMonthlyCalendar(YearMonth month) {
        Map<LocalDate, StatEntry> result = new TreeMap<>();
        int daysInMonth = month.lengthOfMonth();
        for (int d = 1; d <= daysInMonth; d++) {
            LocalDate date = month.atDay(d);
            result.put(date, new StatEntry(date.toString()));
        }
        for (Transaction t : transactionManager.getByMonth(month)) {
            StatEntry entry = result.get(t.getDate());
            if (t.getType() == Transaction.Type.INCOME) {
                entry.addIncome(t.getAmount());
            } else {
                entry.addExpense(t.getAmount());
            }
        }
        return result;
    }

    /**
     * 한 달을 주 단위(1주, 2주, ...)로 나눠 수입/지출 합계를 낸다 (통계 화면의 막대그래프용).
     * 1일이 속한 요일과 무관하게, "그 달의 1~7일=1주, 8~14일=2주, ..." 식으로 단순 분할한다.
     */
    public List<StatEntry> getWeeklyBreakdown(YearMonth month) {
        int daysInMonth = month.lengthOfMonth();
        int weekCount = (int) Math.ceil(daysInMonth / 7.0);
        StatEntry[] weeks = new StatEntry[weekCount];
        for (int i = 0; i < weekCount; i++) {
            weeks[i] = new StatEntry((i + 1) + "주");
        }
        for (Transaction t : transactionManager.getByMonth(month)) {
            int dayOfMonth = t.getDate().getDayOfMonth();
            int weekIndex = (dayOfMonth - 1) / 7;
            StatEntry entry = weeks[weekIndex];
            if (t.getType() == Transaction.Type.INCOME) {
                entry.addIncome(t.getAmount());
            } else {
                entry.addExpense(t.getAmount());
            }
        }
        return java.util.Arrays.asList(weeks);
    }

    /**
     * 한 달을 주 단위로 나누되, 라벨을 "MM/dd - MM/dd" 실제 날짜 범위로 표기한다
     * (통계 화면의 "기간별 내역(표)" 카드용).
     */
    public List<StatEntry> getWeeklyBreakdownDated(YearMonth month) {
        List<StatEntry> weekly = getWeeklyBreakdown(month);
        List<StatEntry> result = new java.util.ArrayList<>();
        int daysInMonth = month.lengthOfMonth();
        for (int i = 0; i < weekly.size(); i++) {
            int startDay = i * 7 + 1;
            int endDay = Math.min(startDay + 6, daysInMonth);
            LocalDate start = month.atDay(startDay);
            LocalDate end = month.atDay(endDay);
            String label = String.format("%02d/%02d - %02d/%02d",
                    start.getMonthValue(), start.getDayOfMonth(), end.getMonthValue(), end.getDayOfMonth());
            StatEntry dated = new StatEntry(label);
            dated.addIncome(weekly.get(i).getIncome());
            dated.addExpense(weekly.get(i).getExpense());
            result.add(dated);
        }
        return result;
    }

    /**
     * 임의의 날짜 범위(from~to, 포함)를 7일 단위로 나눠 수입/지출 합계를 낸다.
     * 월에 묶이지 않는 범용 버전 — 통계 화면 "사용자 지정" 기간 모드에서 사용.
     */
    public List<StatEntry> getWeeklyBreakdownDated(LocalDate from, LocalDate to) {
        List<StatEntry> result = new java.util.ArrayList<>();
        if (to.isBefore(from)) return result;
        LocalDate chunkStart = from;
        while (!chunkStart.isAfter(to)) {
            LocalDate chunkEnd = chunkStart.plusDays(6);
            if (chunkEnd.isAfter(to)) chunkEnd = to;
            String label = String.format("%02d/%02d - %02d/%02d",
                    chunkStart.getMonthValue(), chunkStart.getDayOfMonth(), chunkEnd.getMonthValue(), chunkEnd.getDayOfMonth());
            StatEntry entry = new StatEntry(label);
            for (Transaction t : transactionManager.getBetween(chunkStart, chunkEnd)) {
                if (t.getType() == Transaction.Type.INCOME) entry.addIncome(t.getAmount());
                else entry.addExpense(t.getAmount());
            }
            result.add(entry);
            chunkStart = chunkEnd.plusDays(1);
        }
        return result;
    }

    /**
     * 기준 월을 포함해 과거 monthsBack개월치 월별 수입/지출 합계
     * (통계 화면 "월간" 모드의 추이 표/그래프용).
     */
    public List<StatEntry> getMonthlyTrend(YearMonth centerMonth, int monthsBack) {
        List<StatEntry> result = new java.util.ArrayList<>();
        for (int i = monthsBack - 1; i >= 0; i--) {
            YearMonth m = centerMonth.minusMonths(i);
            StatEntry entry = new StatEntry(m.getMonthValue() + "월");
            for (Transaction t : transactionManager.getByMonth(m)) {
                if (t.getType() == Transaction.Type.INCOME) entry.addIncome(t.getAmount());
                else entry.addExpense(t.getAmount());
            }
            result.add(entry);
        }
        return result;
    }

    /** 특정 기간(from~to, 포함) 동안 카테고리별 지출 합계. */
    public Map<String, Long> getCategoryBreakdown(LocalDate from, LocalDate to) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Transaction t : transactionManager.getBetween(from, to)) {
            if (t.getType() != Transaction.Type.EXPENSE) continue;
            result.merge(t.getCategory(), t.getAmount(), Long::sum);
        }
        return result;
    }

    /** 특정 기간의 총 수입/지출/순수익 요약. */
    public StatEntry getSummary(LocalDate from, LocalDate to) {
        StatEntry summary = new StatEntry(from + " ~ " + to);
        for (Transaction t : transactionManager.getBetween(from, to)) {
            if (t.getType() == Transaction.Type.INCOME) {
                summary.addIncome(t.getAmount());
            } else {
                summary.addExpense(t.getAmount());
            }
        }
        return summary;
    }
}
