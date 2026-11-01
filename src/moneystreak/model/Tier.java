package moneystreak.model;

/**
 * 머니스트릭 게임화 시스템의 티어(등급).
 * 리그 오브 레전드 스타일의 9단계: 브론즈 ~ 챌린저.
 * 각 티어는 "다음 티어로 승급하기 위해 필요한 누적 점수"를 가진다 (챌린저는 최고 티어라 없음).
 */
public enum Tier {
    BRONZE("브론즈", 300),
    SILVER("실버", 700),
    GOLD("골드", 1200),
    PLATINUM("플래티넘", 2000),
    EMERALD("에메랄드", 3000),
    DIAMOND("다이아몬드", 4500),
    MASTER("마스터", 8000),
    GRANDMASTER("그랜드마스터", 10000),
    CHALLENGER("챌린저", -1); // 최고 티어, 더 이상 승급 없음

    private final String displayName;
    private final int pointsToNextTier;

    Tier(String displayName, int pointsToNextTier) {
        this.displayName = displayName;
        this.pointsToNextTier = pointsToNextTier;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 이 티어에서 다음 티어로 승급하는 데 필요한 누적 점수. 최고 티어면 -1. */
    public int getPointsToNextTier() {
        return pointsToNextTier;
    }

    public boolean hasNext() {
        return pointsToNextTier > 0;
    }

    public Tier next() {
        if (!hasNext()) return this;
        return Tier.values()[ordinal() + 1];
    }

    /** 바로 아래 티어. 이미 최하위(브론즈)면 null. */
    public Tier previous() {
        if (ordinal() == 0) return null;
        return Tier.values()[ordinal() - 1];
    }

    /**
     * 이 티어를 "유지"하기 위해 매달 필요한 최소 점수.
     * = 바로 아래 티어에서 이 티어로 승급할 때 필요했던 점수와 동일.
     * 브론즈는 더 내려갈 곳이 없으므로 유지 요구치가 없다(0).
     */
    public int getMaintenanceRequirement() {
        Tier prev = previous();
        return (prev == null) ? 0 : prev.getPointsToNextTier();
    }
}
