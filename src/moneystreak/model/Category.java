package moneystreak.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 거래 분류용 카테고리.
 * 자동 분류(AutoClassifier)가 메모 텍스트와 매칭할 키워드 목록을 가진다.
 */
public class Category {

    private String name;
    private final List<String> keywords;

    public Category(String name) {
        this.name = name;
        this.keywords = new ArrayList<>();
    }

    public Category(String name, List<String> keywords) {
        this.name = name;
        this.keywords = new ArrayList<>(keywords);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void addKeyword(String keyword) {
        if (!keywords.contains(keyword)) {
            keywords.add(keyword);
        }
    }

    /** 주어진 텍스트(메모)가 이 카테고리의 키워드 중 하나를 포함하는지 확인한다. */
    public boolean matches(String memo) {
        if (memo == null) return false;
        String lower = memo.toLowerCase();
        for (String kw : keywords) {
            if (lower.contains(kw.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
