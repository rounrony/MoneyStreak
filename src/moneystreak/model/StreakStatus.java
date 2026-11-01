package moneystreak.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자의 스트릭(연속 기록) 및 티어/뱃지 상태.
 * 승급/강등/점수 계산 로직 자체는 StreakManager에 있고, 이 클래스는 상태값만 보관한다.
 */
public class StreakStatus {

    private int currentStreakDays;        // 현재 연속 기록 일수
    private int freezeCount;              // 보유 중인 "스트릭 프리즈" 개수
    private Tier currentTier;             // 현재 티어
    private int tierProgressScore;        // 다음 티어 승급까지의 누적 점수 (승급 시 0으로 리셋)
    private int monthlyScore;             // 이번 달 누적 획득 점수 (매월 강등 판정용, 매월 초 0으로 리셋)
    private YearMonth scoringMonth;       // monthlyScore가 집계 중인 달
    private boolean budgetBreachedThisMonth; // 이번 달 중 예산 한도를 이미 한 번 넘겨서 페널티를 받았는지
    private final List<String> badges;    // 획득한 뱃지 목록 (승급/강등 기록 등)
    private LocalDate lastActivityDate;   // 마지막으로 거래를 기록한 날짜
    private LocalDate lastProcessedDate;  // processDay가 마지막으로 처리를 완료한 날짜 (앱 재실행 시 이어서 처리하기 위함)

    public StreakStatus() {
        this.currentStreakDays = 0;
        this.freezeCount = 0;
        this.currentTier = Tier.BRONZE;
        this.tierProgressScore = 0;
        this.monthlyScore = 0;
        this.scoringMonth = null;
        this.budgetBreachedThisMonth = false;
        this.badges = new ArrayList<>();
        this.lastActivityDate = null;
    }

    public int getCurrentStreakDays() {
        return currentStreakDays;
    }

    public void setCurrentStreakDays(int currentStreakDays) {
        this.currentStreakDays = currentStreakDays;
    }

    public int getFreezeCount() {
        return freezeCount;
    }

    public void setFreezeCount(int freezeCount) {
        this.freezeCount = freezeCount;
    }

    public Tier getCurrentTier() {
        return currentTier;
    }

    public void setCurrentTier(Tier currentTier) {
        this.currentTier = currentTier;
    }

    public int getTierProgressScore() {
        return tierProgressScore;
    }

    public void setTierProgressScore(int tierProgressScore) {
        this.tierProgressScore = tierProgressScore;
    }

    public int getMonthlyScore() {
        return monthlyScore;
    }

    public void setMonthlyScore(int monthlyScore) {
        this.monthlyScore = monthlyScore;
    }

    public YearMonth getScoringMonth() {
        return scoringMonth;
    }

    public void setScoringMonth(YearMonth scoringMonth) {
        this.scoringMonth = scoringMonth;
    }

    public boolean isBudgetBreachedThisMonth() {
        return budgetBreachedThisMonth;
    }

    public void setBudgetBreachedThisMonth(boolean budgetBreachedThisMonth) {
        this.budgetBreachedThisMonth = budgetBreachedThisMonth;
    }

    public List<String> getBadges() {
        return badges;
    }

    public void addBadge(String badge) {
        badges.add(badge);
    }

    public LocalDate getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(LocalDate lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    public LocalDate getLastProcessedDate() {
        return lastProcessedDate;
    }

    public void setLastProcessedDate(LocalDate lastProcessedDate) {
        this.lastProcessedDate = lastProcessedDate;
    }
}
