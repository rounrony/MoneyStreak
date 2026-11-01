package moneystreak.manager;

import moneystreak.model.Category;
import moneystreak.store.DataStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 메모 텍스트 기반 거래 자동 분류 (F2).
 * 콜드스타트 문제를 줄이기 위해 기본 키워드 매핑을 미리 넣어두고,
 * 사용자가 키워드를 추가/수정하면 그 내용을 JSON으로 영속화한다.
 */
public class AutoClassifier {

    private static final String FILE_NAME = "categories.json";
    public static final String UNCATEGORIZED = "기타";

    private final DataStore store;
    private final List<Category> categories = new ArrayList<>();

    public AutoClassifier(DataStore store) {
        this.store = store;
        load();
        if (categories.isEmpty()) {
            seedDefaults();
            save();
        }
    }

    public List<Category> getCategories() {
        return categories;
    }

    public Category findByName(String name) {
        for (Category c : categories) {
            if (c.getName().equals(name)) return c;
        }
        return null;
    }

    /** 새 카테고리를 추가한다 (이미 있으면 무시). */
    public void addCategory(String name) {
        if (findByName(name) == null) {
            categories.add(new Category(name));
            save();
        }
    }

    /** 카테고리에 키워드를 추가한다. 카테고리가 없으면 새로 만든다. */
    public void addKeyword(String categoryName, String keyword) {
        Category c = findByName(categoryName);
        if (c == null) {
            c = new Category(categoryName);
            categories.add(c);
        }
        c.addKeyword(keyword);
        save();
    }

    /**
     * 메모 텍스트를 기반으로 카테고리를 자동 분류한다.
     * 일치하는 카테고리가 없으면 "기타"를 반환한다.
     */
    public String classify(String memo) {
        for (Category c : categories) {
            if (c.matches(memo)) {
                return c.getName();
            }
        }
        return UNCATEGORIZED;
    }

    private void seedDefaults() {
        addDefault("카페", "스타벅스", "카페", "커피", "이디야", "투썸", "메가커피");
        addDefault("식비", "식당", "배달", "편의점", "마트", "음식점", "국밥", "김밥", "배달의민족", "요기요");
        addDefault("교통", "버스", "지하철", "택시", "주유", "교통카드", "기차", "ktx");
        addDefault("쇼핑", "쿠팡", "올리브영", "다이소", "백화점", "온라인쇼핑", "지마켓");
        addDefault("문화생활", "영화", "넷플릭스", "콘서트", "전시", "유튜브프리미엄");
        addDefault("통신", "통신비", "휴대폰", "인터넷요금", "스마트폰");
        addDefault("주거", "월세", "관리비", "전기요금", "가스요금", "수도요금");
        categories.add(new Category(UNCATEGORIZED));
    }

    private void addDefault(String name, String... keywords) {
        categories.add(new Category(name, Arrays.asList(keywords)));
    }

    // ---------- 영속성 ----------

    private void save() {
        List<Object> list = new ArrayList<>();
        for (Category c : categories) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", c.getName());
            m.put("keywords", new ArrayList<Object>(c.getKeywords()));
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
            String name = (String) m.get("name");
            List<Object> keywordsRaw = (List<Object>) m.get("keywords");
            List<String> keywords = new ArrayList<>();
            if (keywordsRaw != null) {
                for (Object k : keywordsRaw) keywords.add((String) k);
            }
            categories.add(new Category(name, keywords));
        }
    }
}
