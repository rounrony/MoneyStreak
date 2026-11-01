package moneystreak.manager;

import moneystreak.model.StreakStatus;
import moneystreak.model.Tier;
import moneystreak.model.Transaction;
import moneystreak.store.DataStore;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 스트릭 기록, 티어 점수 계산, 승급/강등 판정 (F7).
 *
 * 하루 단위 점수 규칙:
 *  - 그 날 거래를 기록함 (스트릭 유지):
 *      예산 미설정 달 → +100점
 *      예산 설정된 달 → 스트릭 +60점, 그리고 이번 달 한도를 아직 하나도 안 넘겼으면 예산 +40점 추가
 *  - 그 날 거래를 기록하지 않음 (스트릭 끊김):
 *      보유한 프리즈가 있으면 1개 소모하고 페널티 없음
 *      프리즈가 없으면 연속일수 0으로 리셋, -100점(실제 차감)
 *  - 이번 달 중 예산 한도를 처음 넘기는 순간 -100점(1회성, 반복 차감 없음)
 *
 * 승급: 누적 점수(tierProgressScore)가 현재 티어의 getPointsToNextTier()에 도달하면
 *       승급하고 초과분은 다음 티어로 이어짐.
 * 강등: 매월 말, 그 달에 번 점수(monthlyScore)가 현재 티어의 유지 요구치(getMaintenanceRequirement())
 *       미달이면 한 단계 강등.
 */
public class StreakManager {

    private static final String FILE_NAME = "streak.json";

    private final DataStore store;
    private final BudgetManager budgetManager;
    private StreakStatus status;

    public StreakManager(DataStore store, BudgetManager budgetManager) {
        this.store = store;
        this.budgetManager = budgetManager;
        load();
    }

    public StreakStatus getStatus() {
        return status;
    }

    /**
     * 특정 날짜 하루에 대한 활동을 처리한다.
     * 날짜 순서대로(과거 -> 현재) 호출되어야 하며, 중간에 빠진 날짜가 있으면 안 된다
     * (빠진 날은 loggedThatDay=false로 각각 호출해서 처리해야 함).
     *
     * @param date                     처리할 날짜
     * @param loggedThatDay            그 날 거래를 1건 이상 기록했는지
     * @param monthTransactionsUpToDate 그 날짜가 속한 달의, 그 날짜까지(포함) 누적된 거래 목록
     */
    public void processDay(LocalDate date, boolean loggedThatDay, List<Transaction> monthTransactionsUpToDate) {
        handleMonthRollover(YearMonth.from(date));

        Double complianceRate = budgetManager.getComplianceRate(YearMonth.from(date), monthTransactionsUpToDate);

        if (loggedThatDay) {
            status.setCurrentStreakDays(status.getCurrentStreakDays() + 1);
            status.setLastActivityDate(date);

            int streakPoints = (complianceRate == null) ? 100 : 60;
            addPoints(streakPoints);

            if (complianceRate != null) {
                if (complianceRate >= 1.0) {
                    addPoints(40);
                } else if (!status.isBudgetBreachedThisMonth()) {
                    addPoints(-100);
                    status.setBudgetBreachedThisMonth(true);
                }
            }
        } else {
            if (status.getFreezeCount() > 0) {
                status.setFreezeCount(status.getFreezeCount() - 1);
                // 프리즈로 보호됨: 페널티 없음 (연속일수는 늘지도 끊기지도 않음)
            } else {
                status.setCurrentStreakDays(0);
                addPoints(-100);
            }
        }

        save();
    }

    private void addPoints(int delta) {
        status.setTierProgressScore(Math.max(0, status.getTierProgressScore() + delta));
        status.setMonthlyScore(Math.max(0, status.getMonthlyScore() + delta));
        checkPromotion();
    }

    private void checkPromotion() {
        Tier tier = status.getCurrentTier();
        while (tier.hasNext() && status.getTierProgressScore() >= tier.getPointsToNextTier()) {
            status.setTierProgressScore(status.getTierProgressScore() - tier.getPointsToNextTier());
            tier = tier.next();
            status.addBadge(tier.getDisplayName() + " 승급");
        }
        status.setCurrentTier(tier);
    }

    /** scoringMonth부터 targetMonth 이전 달까지, 달이 바뀔 때마다 강등/프리즈 지급 처리를 한다. */
    private void handleMonthRollover(YearMonth targetMonth) {
        if (status.getScoringMonth() == null) {
            status.setScoringMonth(targetMonth);
            return;
        }
        while (!status.getScoringMonth().equals(targetMonth)) {
            closeOutMonth(status.getScoringMonth());
            status.setScoringMonth(status.getScoringMonth().plusMonths(1));
        }
    }

    private void closeOutMonth(YearMonth closingMonth) {
        Tier currentTier = status.getCurrentTier();
        int maintenanceRequirement = currentTier.getMaintenanceRequirement();
        if (maintenanceRequirement > 0 && status.getMonthlyScore() < maintenanceRequirement) {
            Tier demoted = currentTier.previous();
            if (demoted != null) {
                status.setCurrentTier(demoted);
                status.setTierProgressScore(0);
                status.addBadge(demoted.getDisplayName() + "로 강등");
            }
        }

        boolean hadBudget = budgetManager.hasBudget(closingMonth);
        if (hadBudget && !status.isBudgetBreachedThisMonth()) {
            status.setFreezeCount(status.getFreezeCount() + 1);
        }

        status.setMonthlyScore(0);
        status.setBudgetBreachedThisMonth(false);
    }

    // ---------- 영속성 ----------

    private void save() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("currentStreakDays", status.getCurrentStreakDays());
        m.put("freezeCount", status.getFreezeCount());
        m.put("currentTier", status.getCurrentTier().name());
        m.put("tierProgressScore", status.getTierProgressScore());
        m.put("monthlyScore", status.getMonthlyScore());
        m.put("scoringMonth", status.getScoringMonth() == null ? null : status.getScoringMonth().toString());
        m.put("budgetBreachedThisMonth", status.isBudgetBreachedThisMonth());
        m.put("badges", new ArrayList<Object>(status.getBadges()));
        m.put("lastActivityDate", status.getLastActivityDate() == null ? null : status.getLastActivityDate().toString());
        m.put("lastProcessedDate", status.getLastProcessedDate() == null ? null : status.getLastProcessedDate().toString());
        store.write(FILE_NAME, m);
    }

    @SuppressWarnings("unchecked")
    private void load() {
        status = new StreakStatus();
        Object raw = store.read(FILE_NAME);
        if (raw == null) return;
        Map<String, Object> m = (Map<String, Object>) raw;

        status.setCurrentStreakDays(((Number) m.get("currentStreakDays")).intValue());
        status.setFreezeCount(((Number) m.get("freezeCount")).intValue());
        status.setCurrentTier(Tier.valueOf((String) m.get("currentTier")));
        status.setTierProgressScore(((Number) m.get("tierProgressScore")).intValue());
        status.setMonthlyScore(((Number) m.get("monthlyScore")).intValue());

        Object scoringMonthRaw = m.get("scoringMonth");
        if (scoringMonthRaw != null) {
            status.setScoringMonth(YearMonth.parse((String) scoringMonthRaw));
        }

        status.setBudgetBreachedThisMonth(Boolean.TRUE.equals(m.get("budgetBreachedThisMonth")));

        Object badgesRaw = m.get("badges");
        if (badgesRaw instanceof List) {
            for (Object b : (List<Object>) badgesRaw) {
                status.addBadge((String) b);
            }
        }

        Object lastActivityRaw = m.get("lastActivityDate");
        if (lastActivityRaw != null) {
            status.setLastActivityDate(LocalDate.parse((String) lastActivityRaw));
        }

        Object lastProcessedRaw = m.get("lastProcessedDate");
        if (lastProcessedRaw != null) {
            status.setLastProcessedDate(LocalDate.parse((String) lastProcessedRaw));
        }
    }

    /**
     * 앱이 실행될 때마다 호출: 마지막으로 처리된 날짜의 다음날부터 오늘까지
     * 하루씩 processDay를 호출해 밀린 날들을 전부 반영한다 (처음 실행이면 오늘 하루만 처리).
     */
    public void catchUpTo(LocalDate today, TransactionManager transactionManager) {
        LocalDate from = (status.getLastProcessedDate() == null) ? today : status.getLastProcessedDate().plusDays(1);
        LocalDate date = from;
        while (!date.isAfter(today)) {
            boolean logged = transactionManager.hasActivityOn(date);
            processDay(date, logged, transactionManager.getByMonthUpTo(date));
            status.setLastProcessedDate(date);
            save();
            date = date.plusDays(1);
        }
    }
}
