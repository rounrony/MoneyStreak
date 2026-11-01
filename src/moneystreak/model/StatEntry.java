package moneystreak.model;

/**
 * 통계 집계 결과 1건 (예: "1주차" 또는 "2026-01-05" 같은 구간의 수입/지출 합계).
 * 캘린더 조회(F3)와 통계·시각화(F4) 양쪽에서 공통으로 사용한다.
 */
public class StatEntry {

    private final String label;
    private long income;
    private long expense;

    public StatEntry(String label) {
        this.label = label;
        this.income = 0;
        this.expense = 0;
    }

    public String getLabel() {
        return label;
    }

    public long getIncome() {
        return income;
    }

    public long getExpense() {
        return expense;
    }

    public long getNet() {
        return income - expense;
    }

    public void addIncome(long amount) {
        this.income += amount;
    }

    public void addExpense(long amount) {
        this.expense += amount;
    }
}
