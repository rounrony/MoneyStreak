package moneystreak.manager;

import moneystreak.model.Transaction;
import moneystreak.store.DataStore;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 거래 내역 등록/수정/삭제/조회 (F1).
 * 모든 거래를 메모리에 올려두고, 변경 시마다 JSON 파일로 영속화한다.
 */
public class TransactionManager {

    private static final String FILE_NAME = "transactions.json";

    private final DataStore store;
    private final List<Transaction> transactions = new ArrayList<>();

    public TransactionManager(DataStore store) {
        this.store = store;
        load();
    }

    /** 새 거래를 등록한다. id는 자동 생성. */
    public Transaction add(LocalDate date, Transaction.Type type, String category, long amount, String memo) {
        Transaction t = new Transaction(UUID.randomUUID().toString(), date, type, category, amount, memo);
        transactions.add(t);
        save();
        return t;
    }

    /** id로 거래 1건을 찾는다. 없으면 null. */
    public Transaction findById(String id) {
        for (Transaction t : transactions) {
            if (t.getId().equals(id)) return t;
        }
        return null;
    }

    /** 기존 거래를 수정한다. 변경하지 않을 필드는 null로 넘기면 된다 (amount는 음수를 "변경 없음"으로 취급). */
    public boolean update(String id, LocalDate date, Transaction.Type type, String category, Long amount, String memo) {
        Transaction t = findById(id);
        if (t == null) return false;
        if (date != null) t.setDate(date);
        if (type != null) t.setType(type);
        if (category != null) t.setCategory(category);
        if (amount != null) t.setAmount(amount);
        if (memo != null) t.setMemo(memo);
        save();
        return true;
    }

    /** id로 거래를 삭제한다. */
    public boolean delete(String id) {
        boolean removed = transactions.removeIf(t -> t.getId().equals(id));
        if (removed) save();
        return removed;
    }

    /** 전체 거래 목록 (날짜순). */
    public List<Transaction> getAll() {
        return transactions.stream()
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
    }

    /** 특정 월의 거래 목록. */
    public List<Transaction> getByMonth(YearMonth month) {
        return transactions.stream()
                .filter(t -> YearMonth.from(t.getDate()).equals(month))
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
    }

    /** 특정 월의, 특정 날짜까지(포함) 누적된 거래 목록 (StreakManager/BudgetManager에 넘길 때 사용). */
    public List<Transaction> getByMonthUpTo(LocalDate upToDate) {
        YearMonth month = YearMonth.from(upToDate);
        return transactions.stream()
                .filter(t -> YearMonth.from(t.getDate()).equals(month) && !t.getDate().isAfter(upToDate))
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
    }

    /** 특정 날짜에 거래가 1건이라도 있는지 (스트릭 유지 여부 판정에 사용). */
    public boolean hasActivityOn(LocalDate date) {
        return transactions.stream().anyMatch(t -> t.getDate().equals(date));
    }

    /** 특정 기간의 거래 목록 (통계/캘린더 조회용, F3·F4에서 사용). */
    public List<Transaction> getBetween(LocalDate from, LocalDate to) {
        return transactions.stream()
                .filter(t -> !t.getDate().isBefore(from) && !t.getDate().isAfter(to))
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
    }

    /** 메모 또는 카테고리에 키워드가 포함된 거래 검색 (검색 기능, 추후 F10 확장용으로 미리 제공). */
    public List<Transaction> search(String keyword) {
        String lower = keyword.toLowerCase();
        return transactions.stream()
                .filter(t -> (t.getMemo() != null && t.getMemo().toLowerCase().contains(lower))
                        || (t.getCategory() != null && t.getCategory().toLowerCase().contains(lower)))
                .collect(Collectors.toList());
    }

    // ---------- 영속성 ----------

    private void save() {
        List<Object> list = new ArrayList<>();
        for (Transaction t : transactions) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("date", t.getDate().toString());
            m.put("type", t.getType().name());
            m.put("category", t.getCategory());
            m.put("amount", t.getAmount());
            m.put("memo", t.getMemo());
            list.add(m);
        }
        store.write(FILE_NAME, list);
    }

    @SuppressWarnings("unchecked")
    private void load() {
        Object raw = store.read(FILE_NAME);
        if (raw == null) return;
        List<Object> list = (List<Object>) raw;
        for (Object o : list) {
            Map<String, Object> m = (Map<String, Object>) o;
            String id = (String) m.get("id");
            LocalDate date = LocalDate.parse((String) m.get("date"));
            Transaction.Type type = Transaction.Type.valueOf((String) m.get("type"));
            String category = (String) m.get("category");
            long amount = ((Number) m.get("amount")).longValue();
            String memo = (String) m.get("memo");
            transactions.add(new Transaction(id, date, type, category, amount, memo));
        }
    }
}
