package moneystreak.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 파일 기반 그룹 경쟁의 랭킹 테이블.
 * 각 Entry는 한 사용자가 공유(export)한 요약 통계 1건을 나타낸다.
 */
public class GroupRanking {

    /** 공유된 사용자 1명의 요약 통계 */
    public static class Entry {
        private String nickname;
        private Tier tier;
        private int tierProgressScore;       // 같은 티어 내에서의 진행 점수 (동률 비교용)
        private double budgetComplianceRate; // 0.0 ~ 1.0, 예산 준수율 (예산 미설정이면 1.0)
        private int streakDays;

        public Entry(String nickname, Tier tier, int tierProgressScore, double budgetComplianceRate, int streakDays) {
            this.nickname = nickname;
            this.tier = tier;
            this.tierProgressScore = tierProgressScore;
            this.budgetComplianceRate = budgetComplianceRate;
            this.streakDays = streakDays;
        }

        public String getNickname() {
            return nickname;
        }

        public Tier getTier() {
            return tier;
        }

        public int getTierProgressScore() {
            return tierProgressScore;
        }

        public double getBudgetComplianceRate() {
            return budgetComplianceRate;
        }

        public int getStreakDays() {
            return streakDays;
        }
    }

    private final List<Entry> entries;

    public GroupRanking() {
        this.entries = new ArrayList<>();
    }

    public List<Entry> getEntries() {
        return entries;
    }

    /** 같은 닉네임의 기존 기록이 있으면 교체하고, 없으면 새로 추가한다. */
    public void addOrUpdate(Entry entry) {
        entries.removeIf(e -> e.getNickname().equals(entry.getNickname()));
        entries.add(entry);
        // 티어 내림차순 -> 같은 티어면 진행점수 내림차순 -> 같으면 예산 준수율 내림차순
        entries.sort((a, b) -> {
            if (b.getTier().ordinal() != a.getTier().ordinal()) {
                return Integer.compare(b.getTier().ordinal(), a.getTier().ordinal());
            }
            if (b.getTierProgressScore() != a.getTierProgressScore()) {
                return Integer.compare(b.getTierProgressScore(), a.getTierProgressScore());
            }
            return Double.compare(b.getBudgetComplianceRate(), a.getBudgetComplianceRate());
        });
    }
}
