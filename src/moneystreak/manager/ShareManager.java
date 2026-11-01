package moneystreak.manager;

import moneystreak.model.GroupRanking;
import moneystreak.model.StreakStatus;
import moneystreak.model.Tier;
import moneystreak.model.Transaction;
import moneystreak.store.DataStore;

import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 서버 없이 파일(JSON) 기반으로 동작하는 그룹 경쟁 (F9).
 * 사용자가 자신의 요약 통계를 JSON 파일로 내보내고(export), 서로 파일을 주고받아
 * 가져오기(import)하면 로컬 랭킹 테이블이 갱신된다.
 */
public class ShareManager {

    private static final String RANKING_FILE = "group_ranking.json";

    private final DataStore store;
    private final StreakManager streakManager;
    private final BudgetManager budgetManager;
    private final TransactionManager transactionManager;
    private GroupRanking ranking;

    public ShareManager(DataStore store, StreakManager streakManager,
                         BudgetManager budgetManager, TransactionManager transactionManager) {
        this.store = store;
        this.streakManager = streakManager;
        this.budgetManager = budgetManager;
        this.transactionManager = transactionManager;
        load();
    }

    public GroupRanking getRanking() {
        return ranking;
    }

    /** 현재 내 상태를 바탕으로 내보내기용 Entry를 만든다. */
    private GroupRanking.Entry buildMyEntry(String nickname) {
        StreakStatus status = streakManager.getStatus();
        YearMonth thisMonth = YearMonth.now();
        List<Transaction> txs = transactionManager.getByMonth(thisMonth);
        Double rate = budgetManager.getComplianceRate(thisMonth, txs);
        double complianceRate = (rate == null) ? 1.0 : rate; // 예산 미설정이면 패널티 없이 1.0으로 간주
        return new GroupRanking.Entry(
                nickname,
                status.getCurrentTier(),
                status.getTierProgressScore(),
                complianceRate,
                status.getCurrentStreakDays());
    }

    /** 내 상태를 JSON 파일로 내보낸다 (파일 공유로 그룹원에게 전달). */
    public void exportTo(Path path, String nickname) {
        GroupRanking.Entry entry = buildMyEntry(nickname);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nickname", entry.getNickname());
        m.put("tier", entry.getTier().name());
        m.put("tierProgressScore", entry.getTierProgressScore());
        m.put("budgetComplianceRate", entry.getBudgetComplianceRate());
        m.put("streakDays", entry.getStreakDays());
        DataStore.writeToPath(path, m);
    }

    /** 다른 사람이 보낸 export 파일을 읽어 내 로컬 랭킹 테이블에 반영한다. */
    @SuppressWarnings("unchecked")
    public void importFrom(Path path) {
        Map<String, Object> m = (Map<String, Object>) DataStore.readFromPath(path);
        String nickname = (String) m.get("nickname");
        Tier tier = Tier.valueOf((String) m.get("tier"));
        int tierProgressScore = ((Number) m.get("tierProgressScore")).intValue();
        double complianceRate = ((Number) m.get("budgetComplianceRate")).doubleValue();
        int streakDays = ((Number) m.get("streakDays")).intValue();

        GroupRanking.Entry entry = new GroupRanking.Entry(nickname, tier, tierProgressScore, complianceRate, streakDays);
        ranking.addOrUpdate(entry);
        save();
    }

    /** 공유 카드용으로 보여줄 요약 문구를 만든다 (UI에서 그대로 띄우거나 이미지로 캡처할 수 있음). */
    public String generateShareCardText(String nickname) {
        StreakStatus status = streakManager.getStatus();
        String month = YearMonth.now().format(DateTimeFormatter.ofPattern("M월"));
        return String.format(
                "%s의 %s 머니스트릭 카드%n티어: %s%n연속 기록: %d일%n보유 프리즈: %d개",
                nickname, month, status.getCurrentTier().getDisplayName(),
                status.getCurrentStreakDays(), status.getFreezeCount());
    }

    /** 공유 카드의 한 줄 요약 푸터만 생성한다 (제목 없이 통계만, 미리보기 카드 하단용). */
    public String generateShareCardFooter() {
        StreakStatus status = streakManager.getStatus();
        Double rate = budgetManager.getComplianceRate(YearMonth.now(), transactionManager.getByMonth(YearMonth.now()));
        String rateText = (rate == null) ? "예산 미설정" : String.format("예산 준수율 %.0f%%", rate * 100);
        return String.format("%s · %s · 🔥%d일 연속",
                rateText, status.getCurrentTier().getDisplayName(), status.getCurrentStreakDays());
    }

    // ---------- 영속성 ----------

    private void save() {
        java.util.List<Object> list = new java.util.ArrayList<>();
        for (GroupRanking.Entry e : ranking.getEntries()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nickname", e.getNickname());
            m.put("tier", e.getTier().name());
            m.put("tierProgressScore", e.getTierProgressScore());
            m.put("budgetComplianceRate", e.getBudgetComplianceRate());
            m.put("streakDays", e.getStreakDays());
            list.add(m);
        }
        store.write(RANKING_FILE, list);
    }

    @SuppressWarnings("unchecked")
    private void load() {
        ranking = new GroupRanking();
        Object raw = store.read(RANKING_FILE);
        if (raw == null) return;
        List<Object> list = (List<Object>) raw;
        for (Object o : list) {
            Map<String, Object> m = (Map<String, Object>) o;
            String nickname = (String) m.get("nickname");
            Tier tier = Tier.valueOf((String) m.get("tier"));
            int tierProgressScore = ((Number) m.get("tierProgressScore")).intValue();
            double complianceRate = ((Number) m.get("budgetComplianceRate")).doubleValue();
            int streakDays = ((Number) m.get("streakDays")).intValue();
            ranking.addOrUpdate(new GroupRanking.Entry(nickname, tier, tierProgressScore, complianceRate, streakDays));
        }
    }
}
