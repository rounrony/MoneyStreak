package moneystreak.model;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

/**
 * 특정 월의 예산.
 * 카테고리별 한도(categoryLimits)와 전체 한도(totalLimit)를 가진다.
 */
public class Budget {

    private YearMonth month;
    private long totalLimit;
    private final Map<String, Long> categoryLimits;

    public Budget(YearMonth month, long totalLimit) {
        this.month = month;
        this.totalLimit = totalLimit;
        this.categoryLimits = new HashMap<>();
    }

    public YearMonth getMonth() {
        return month;
    }

    public void setMonth(YearMonth month) {
        this.month = month;
    }

    public long getTotalLimit() {
        return totalLimit;
    }

    public void setTotalLimit(long totalLimit) {
        this.totalLimit = totalLimit;
    }

    public Map<String, Long> getCategoryLimits() {
        return categoryLimits;
    }

    public void setCategoryLimit(String category, long limit) {
        categoryLimits.put(category, limit);
    }

    /** 해당 카테고리에 설정된 한도. 설정이 없으면 -1(무제한)을 반환한다. */
    public long getCategoryLimit(String category) {
        return categoryLimits.getOrDefault(category, -1L);
    }
}
