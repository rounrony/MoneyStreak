package moneystreak.manager;

import moneystreak.model.Budget;
import moneystreak.model.Transaction;
import moneystreak.store.DataStore;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 월별 예산 설정 및 예산 준수 여부 판정 (F5 예산경고).
 * 예산 설정은 선택사항이다 — 특정 월에 Budget이 없으면 "그 달은 예산 미설정"으로 취급하고,
 * StreakManager는 이 경우 레벨 판정에서 예산 준수율 요소를 제외한다.
 */
public class BudgetManager {

    private static final String FILE_NAME = "budgets.json";

    private final DataStore store;
    private final Map<YearMonth, Budget> budgets = new HashMap<>();

    public BudgetManager(DataStore store) {
        this.store = store;
        load();
    }

    /** 특정 월의 예산을 설정(또는 교체)한다. */
    public void setBudget(Budget budget) {
        budgets.put(budget.getMonth(), budget);
        save();
    }

    /** 특정 월에 예산이 설정되어 있는지 확인한다. */
    public boolean hasBudget(YearMonth month) {
        return budgets.containsKey(month);
    }

    public Budget getBudget(YearMonth month) {
        return budgets.get(month);
    }

    /**
     * 해당 월의 거래 목록을 바탕으로 예산 준수율을 계산한다.
     * 설정된 한도(전체 한도 + 카테고리별 한도) 중 지켜진 한도의 비율.
     * 예산이 전혀 설정되지 않았으면 null을 반환한다(평가 대상 없음).
     */
    public Double getComplianceRate(YearMonth month, List<Transaction> monthTransactions) {
        Budget budget = budgets.get(month);
        if (budget == null) {
            return null;
        }

        int totalLimitCount = 0;
        int totalLimitKept = 0;

        // 전체 한도 체크
        if (budget.getTotalLimit() >= 0) {
            long totalSpent = sumExpense(monthTransactions, null);
            totalLimitCount++;
            if (totalSpent <= budget.getTotalLimit()) {
                totalLimitKept++;
            }
        }

        // 카테고리별 한도 체크
        for (Map.Entry<String, Long> e : budget.getCategoryLimits().entrySet()) {
            String category = e.getKey();
            long limit = e.getValue();
            long spent = sumExpense(monthTransactions, category);
            totalLimitCount++;
            if (spent <= limit) {
                totalLimitKept++;
            }
        }

        if (totalLimitCount == 0) {
            // Budget 객체는 있지만 한도를 하나도 설정 안 한 경우도 "미설정"과 동일하게 처리
            return null;
        }

        return (double) totalLimitKept / totalLimitCount;
    }

    /**
     * 카테고리별 한도 사용률(지출/한도, 0 이상)을 계산한다. 한도가 설정된 카테고리만 포함된다.
     * UI에서 예산 경고 배너/진행률 바를 그릴 때 사용한다.
     */
    public Map<String, Double> getCategoryUsageRatios(YearMonth month, List<Transaction> monthTransactions) {
        Map<String, Double> result = new LinkedHashMap<>();
        Budget budget = budgets.get(month);
        if (budget == null) return result;
        for (Map.Entry<String, Long> e : budget.getCategoryLimits().entrySet()) {
            String category = e.getKey();
            long limit = e.getValue();
            long spent = sumExpense(monthTransactions, category);
            result.put(category, limit <= 0 ? 0.0 : (double) spent / limit);
        }
        return result;
    }

    /** category가 null이면 전체 지출 합계, 아니면 해당 카테고리의 지출 합계. */
    private long sumExpense(List<Transaction> transactions, String category) {
        long sum = 0;
        for (Transaction t : transactions) {
            if (t.getType() != Transaction.Type.EXPENSE) continue;
            if (category != null && !category.equals(t.getCategory())) continue;
            sum += t.getAmount();
        }
        return sum;
    }

    // ---------- 영속성 ----------

    private void save() {
        Map<String, Object> root = new LinkedHashMap<>();
        for (Budget b : budgets.values()) {
            Map<String, Object> bm = new LinkedHashMap<>();
            bm.put("totalLimit", b.getTotalLimit());
            Map<String, Object> catLimits = new LinkedHashMap<>(b.getCategoryLimits());
            bm.put("categoryLimits", catLimits);
            root.put(b.getMonth().toString(), bm);
        }
        store.write(FILE_NAME, root);
    }

    @SuppressWarnings("unchecked")
    private void load() {
        Object raw = store.read(FILE_NAME);
        if (raw == null) return;
        Map<String, Object> root = (Map<String, Object>) raw;
        for (Map.Entry<String, Object> e : root.entrySet()) {
            YearMonth month = YearMonth.parse(e.getKey());
            Map<String, Object> bm = (Map<String, Object>) e.getValue();
            long totalLimit = ((Number) bm.get("totalLimit")).longValue();
            Budget budget = new Budget(month, totalLimit);
            Map<String, Object> catLimits = (Map<String, Object>) bm.get("categoryLimits");
            if (catLimits != null) {
                for (Map.Entry<String, Object> ce : catLimits.entrySet()) {
                    budget.setCategoryLimit(ce.getKey(), ((Number) ce.getValue()).longValue());
                }
            }
            budgets.put(month, budget);
        }
    }
}
