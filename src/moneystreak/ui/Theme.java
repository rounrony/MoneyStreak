package moneystreak.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 공통 색상 팔레트와 카드형 UI 컴포넌트.
 * CP3 보고서의 GUI 와이어프레임(진한 네이비 헤더/흰 카드/옅은 배경) 톤을 그대로 따른다.
 */
public final class Theme {

    private Theme() {
    }

    public static final Color PRIMARY = new Color(0x1F4E79);      // 헤더, 선택된 네비, 주요 버튼
    public static final Color PRIMARY_LIGHT = new Color(0x4F84C4); // 보조 강조색(그래프 등)
    public static final Color APP_BACKGROUND = new Color(0xEEF1F5); // 콘텐츠 영역 배경
    public static final Color CARD_BACKGROUND = Color.WHITE;
    public static final Color CARD_BORDER = new Color(0xDCE3EA);
    public static final Color NAV_BACKGROUND = Color.WHITE;
    public static final Color DANGER = new Color(0xC0392B);
    public static final Color DANGER_BG = new Color(0xFCEAE8);
    public static final Color SUCCESS = new Color(0x2E7D4F);
    public static final Color TEXT_PRIMARY = new Color(0x1F2937);
    public static final Color TEXT_MUTED = new Color(0x6B7280);
    public static final Color HIGHLIGHT_CELL = new Color(0xDCEAF8);

    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    public static final Font FONT_SECTION = new Font("SansSerif", Font.BOLD, 15);
    public static final Font FONT_BASE = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 13);

    /** 둥근 모서리 + 테두리를 가진 흰색 카드 패널. */
    public static class Card extends JPanel {
        private static final int ARC = 14;

        public Card() {
            setOpaque(false);
            setBorder(new EmptyBorder(14, 16, 14, 16));
        }

        public Card(LayoutManager layout) {
            this();
            setLayout(layout);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD_BACKGROUND);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            g2.setColor(CARD_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 카드 제목 라벨 (네이비, 굵게). */
    public static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SECTION);
        label.setForeground(PRIMARY);
        return label;
    }

    /** 주요 액션 버튼(채워진 네이비 배경, 흰 글씨). */
    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text);
        styleButton(btn, PRIMARY, Color.WHITE, PRIMARY);
        return btn;
    }

    /** 보조 액션 버튼(흰 배경, 네이비 테두리/글씨). */
    public static JButton secondaryButton(String text) {
        JButton btn = new JButton(text);
        styleButton(btn, Color.WHITE, PRIMARY, PRIMARY);
        return btn;
    }

    private static void styleButton(JButton btn, Color bg, Color fg, Color border) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(FONT_BOLD);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                new EmptyBorder(8, 16, 8, 16)));
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static Color progressColor(double ratio) {
        return ratio >= 0.8 ? DANGER : PRIMARY;
    }

    /**
     * 선택 상태에 따라 배경을 직접 그리는 토글 버튼.
     * 일부 Look&amp;Feel에서 JToggleButton의 기본 setBackground()가 선택 시 흐릿하게
     * 섞여 보이는 문제가 있어, paintComponent에서 배경을 직접 채워 확실한 색을 보장한다.
     */
    public static class FlatToggleButton extends JToggleButton {
        public FlatToggleButton(String text, boolean selected) {
            super(text, selected);
            setFocusPainted(false);
            setFont(FONT_BOLD);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setContentAreaFilled(false);
            setBorderPainted(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(isSelected() ? PRIMARY : Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(PRIMARY);
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            g2.dispose();
            setForeground(isSelected() ? Color.WHITE : PRIMARY);
            super.paintComponent(g);
        }
    }
}
