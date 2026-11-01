package moneystreak.model;

import java.time.LocalDate;

/**
 * 거래 1건을 나타내는 데이터 클래스.
 * 수입/지출 여부, 날짜, 금액, 카테고리, 메모를 가진다.
 */
public class Transaction {

    /** 거래 유형 (수입/지출) */
    public enum Type {
        INCOME, EXPENSE
    }

    private final String id;
    private LocalDate date;
    private Type type;
    private String category;
    private long amount; // 원 단위, 항상 양수로 저장
    private String memo;

    public Transaction(String id, LocalDate date, Type type, String category, long amount, String memo) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.memo = memo;
    }

    public String getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s %s %,d원 (%s) - %s",
                id, date, type, amount, category, memo);
    }
}
