package moneystreak.ui;

import moneystreak.manager.AutoClassifier;
import moneystreak.manager.BudgetManager;
import moneystreak.manager.StatisticsEngine;
import moneystreak.manager.TransactionManager;
import moneystreak.model.Budget;
import moneystreak.model.Category;
import moneystreak.model.StatEntry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 화면3: 통계 및 시각화 (F4), 예산 설정/경고 (F5).
 * CP3 GUI 와이어프레임 "통계 및 예산" 화면 레이아웃(기간 탭 + 표/원형/막대 카드 + 예산 진행률)을 재구성.
 * 주간 / 월간 / 사용자 지정 세 가지 기간 모드를 토글로 전환할 수 있다.
 */
public class StatisticsPanel extends JPanel {

    private enum PeriodMode { WEEKLY, MONTHLY, CUSTOM }

    private final StatisticsEngine statisticsEngine;
    private final BudgetManager budgetManager;
    private final AutoClassifier classifier;
    private final TransactionManager transactionManager;
    private YearMonth currentMonth;

    private PeriodMode mode = PeriodMode.WEEKLY;
    private LocalDate customFrom = YearMonth.now().atDay(1);
    private LocalDate customTo = YearMonth.now().atEndOfMonth();

    private final JPanel contentColumn = new JPanel();

    public StatisticsPanel(StatisticsEngine statisticsEngine, BudgetManager budgetManager,
                            AutoClassifier classifier, TransactionManager transactionManager) {
        this.statisticsEngine = statisticsEngine;
        this.budgetManager = budgetManager;
        this.classifier = classifier;
        this.transactionManager = transactionManager;
        this.currentMonth = YearMonth.now();

        setLayout(new BorderLayout());
        setBackground(Theme.APP_BACKGROUND);

        add(buildHeader(), BorderLayout.NORTH);

        contentColumn.setLayout(new BoxLayout(contentColumn, BoxLayout.Y_AXIS));
        contentColumn.setBackground(Theme.APP_BACKGROUND);
        contentColumn.setBorder(new EmptyBorder(14, 20, 16, 20));
        JScrollPane scroll = new JScrollPane(contentColumn);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        refresh();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.APP_BACKGROUND);
        header.setBorder(new EmptyBorder(16, 20, 0, 20));

        JLabel title = Theme.sectionTitle("통계 및 시각화 조회");
        title.setFont(Theme.FONT_TITLE);
        header.add(title, BorderLayout.WEST);

        JButton budgetBtn = Theme.primaryButton("예산 설정");
        budgetBtn.addActionListener(e -> openBudgetDialog());
        JPanel navRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        navRight.setOpaque(false);
        navRight.add(budgetBtn);
        header.add(navRight, BorderLayout.EAST);

        return header;
    }

    /** 주간/월간/사용자지정 토글 + 모드별 기간 이동 컨트롤. 모드가 바뀔 때마다 다시 빌드된다. */
    private Theme.Card buildPeriodModeBar() {
        Theme.Card card = new Theme.Card(new BorderLayout(10, 8));

        ButtonGroup group = new ButtonGroup();
        Theme.FlatToggleButton weeklyToggle = new Theme.FlatToggleButton("주간", mode == PeriodMode.WEEKLY);
        Theme.FlatToggleButton monthlyToggle = new Theme.FlatToggleButton("월간", mode == PeriodMode.MONTHLY);
        Theme.FlatToggleButton customToggle = new Theme.FlatToggleButton("사용자 지정", mode == PeriodMode.CUSTOM);
        group.add(weeklyToggle);
        group.add(monthlyToggle);
        group.add(customToggle);
        weeklyToggle.addActionListener(e -> { mode = PeriodMode.WEEKLY; refresh(); });
        monthlyToggle.addActionListener(e -> { mode = PeriodMode.MONTHLY; refresh(); });
        customToggle.addActionListener(e -> { mode = PeriodMode.CUSTOM; refresh(); });

        JPanel modeRow = new JPanel(new GridLayout(1, 3, 0, 0));
        modeRow.setPreferredSize(new Dimension(330, 34));
        modeRow.add(weeklyToggle);
        modeRow.add(monthlyToggle);
        modeRow.add(customToggle);
        card.add(modeRow, BorderLayout.WEST);

        card.add(buildModeControls(), BorderLayout.EAST);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        return card;
    }

    private JPanel buildModeControls() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);

        if (mode == PeriodMode.CUSTOM) {
            JTextField fromField = new JTextField(customFrom.toString(), 10);
            JTextField toField = new JTextField(customTo.toString(), 10);
            JButton applyBtn = Theme.secondaryButton("조회");
            applyBtn.addActionListener(e -> {
                try {
                    LocalDate from = LocalDate.parse(fromField.getText().trim());
                    LocalDate to = LocalDate.parse(toField.getText().trim());
                    if (to.isBefore(from)) {
                        JOptionPane.showMessageDialog(this, "종료일이 시작일보다 앞설 수 없습니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    customFrom = from;
                    customTo = to;
                    refresh();
                } catch (DateTimeParseException ex) {
                    JOptionPane.showMessageDialog(this, "날짜 형식이 올바르지 않습니다 (예: 2026-11-01)", "입력 오류", JOptionPane.ERROR_MESSAGE);
                }
            });
            controls.add(new JLabel("시작일"));
            controls.add(fromField);
            controls.add(new JLabel("종료일"));
            controls.add(toField);
            controls.add(applyBtn);
        } else {
            JButton prevBtn = Theme.secondaryButton("◀ 이전달");
            JLabel monthLabel = new JLabel(currentMonth.getYear() + "년 " + currentMonth.getMonthValue() + "월");
            monthLabel.setFont(Theme.FONT_BOLD);
            JButton nextBtn = Theme.secondaryButton("다음달 ▶");
            prevBtn.addActionListener(e -> { currentMonth = currentMonth.minusMonths(1); refresh(); });
            nextBtn.addActionListener(e -> { currentMonth = currentMonth.plusMonths(1); refresh(); });
            controls.add(prevBtn);
            controls.add(monthLabel);
            controls.add(nextBtn);
        }
        return controls;
    }

    public void refresh() {
        contentColumn.removeAll();

        Theme.Card modeBar = buildPeriodModeBar();
        modeBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentColumn.add(modeBar);
        contentColumn.add(Box.createVerticalStrut(14));

        List<StatEntry> periodList;
        String tableTitle;
        String trendTitle;
        LocalDate pieFrom, pieTo;

        switch (mode) {
            case MONTHLY:
                periodList = statisticsEngine.getMonthlyTrend(currentMonth, 6);
                tableTitle = "최근 6개월 내역 (표)";
                trendTitle = "최근 6개월 추이 (막대)";
                pieFrom = currentMonth.atDay(1);
                pieTo = currentMonth.atEndOfMonth();
                break;
            case CUSTOM:
                periodList = statisticsEngine.getWeeklyBreakdownDated(customFrom, customTo);
                tableTitle = "지정 기간 내역 (표)";
                trendTitle = "지정 기간 추이 (막대)";
                pieFrom = customFrom;
                pieTo = customTo;
                break;
            case WEEKLY:
            default:
                periodList = statisticsEngine.getWeeklyBreakdownDated(currentMonth);
                tableTitle = "주간 내역 (표)";
                trendTitle = "기간별 추이 (막대)";
                pieFrom = currentMonth.atDay(1);
                pieTo = currentMonth.atEndOfMonth();
                break;
        }

        JPanel row1 = new JPanel(new GridLayout(1, 2, 16, 0));
        row1.setOpaque(false);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        row1.setMinimumSize(new Dimension(400, 230));
        row1.setPreferredSize(new Dimension(860, 230));
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
        row1.add(buildPeriodTableCard(periodList, tableTitle));
        row1.add(buildPieCard(pieFrom, pieTo));
        contentColumn.add(row1);
        contentColumn.add(Box.createVerticalStrut(14));

        Theme.Card trendCard = buildTrendCard(periodList, trendTitle);
        trendCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        trendCard.setMinimumSize(new Dimension(400, 260));
        trendCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        contentColumn.add(trendCard);
        contentColumn.add(Box.createVerticalStrut(14));

        Theme.Card progressCard = buildBudgetProgressCard();
        progressCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentColumn.add(progressCard);

        contentColumn.revalidate();
        contentColumn.repaint();
    }

    private Theme.Card buildPeriodTableCard(List<StatEntry> entries, String title) {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 8));
        card.add(Theme.sectionTitle(title), BorderLayout.NORTH);

        String[] cols = {"기간", "수입", "지출"};
        Object[][] rows = new Object[entries.size()][3];
        for (int i = 0; i < entries.size(); i++) {
            StatEntry e = entries.get(i);
            rows[i][0] = e.getLabel();
            rows[i][1] = String.format("+%,d", e.getIncome());
            rows[i][2] = String.format("-%,d", e.getExpense());
        }
        JTable table = new JTable(rows, cols);
        styleTable(table);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setMinimumSize(new Dimension(150, 80));
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private Theme.Card buildPieCard(LocalDate from, LocalDate to) {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 8));
        card.add(Theme.sectionTitle("카테고리별 비율 (원형)"), BorderLayout.NORTH);
        Map<String, Long> breakdown = statisticsEngine.getCategoryBreakdown(from, to);
        PieChart pie = new PieChart(breakdown);
        card.add(pie, BorderLayout.CENTER);
        card.add(buildPieLegend(breakdown), BorderLayout.EAST);
        return card;
    }

    /** 원형 차트 우측에 색상-카테고리 범례를 표시한다. */
    private JPanel buildPieLegend(Map<String, Long> data) {
        JPanel legend = new JPanel();
        legend.setOpaque(false);
        legend.setLayout(new BoxLayout(legend, BoxLayout.Y_AXIS));
        legend.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 2));

        long total = data.values().stream().mapToLong(Long::longValue).sum();
        int colorIdx = 0;
        for (Map.Entry<String, Long> e : data.entrySet()) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 3));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel swatch = new JPanel();
            swatch.setPreferredSize(new Dimension(11, 11));
            swatch.setBackground(PieChart.PALETTE[colorIdx % PieChart.PALETTE.length]);
            row.add(swatch);

            int pct = total > 0 ? (int) Math.round(100.0 * e.getValue() / total) : 0;
            JLabel label = new JLabel("  " + e.getKey() + " (" + pct + "%)");
            label.setFont(label.getFont().deriveFont(12f));
            label.setForeground(Theme.TEXT_MUTED);
            row.add(label);

            legend.add(row);
            colorIdx++;
        }
        return legend;
    }

    private Theme.Card buildTrendCard(List<StatEntry> entries, String title) {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 8));
        card.add(Theme.sectionTitle(title), BorderLayout.NORTH);
        WeekChart chart = new WeekChart();
        chart.setData(entries);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private Theme.Card buildBudgetProgressCard() {
        Theme.Card card = new Theme.Card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel title = Theme.sectionTitle("카테고리별 예산 진행률");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(10));

        if (mode == PeriodMode.CUSTOM) {
            JLabel empty = new JLabel("사용자 지정 기간에는 예산 진행률을 표시하지 않습니다. 예산은 월 단위 개념이라 주간/월간 모드에서 확인해주세요.");
            empty.setForeground(Theme.TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(empty);
            return card;
        }

        if (!budgetManager.hasBudget(currentMonth)) {
            JLabel empty = new JLabel("이번 달은 예산이 설정되지 않았습니다 (선택사항). 예산을 설정하면 카테고리별 진행률이 여기에 표시됩니다.");
            empty.setForeground(Theme.TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(empty);
            return card;
        }

        Map<String, Double> ratios = budgetManager.getCategoryUsageRatios(currentMonth, transactionManager.getByMonth(currentMonth));
        if (ratios.isEmpty()) {
            JLabel empty = new JLabel("한도가 설정된 카테고리가 없습니다.");
            empty.setForeground(Theme.TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(empty);
            return card;
        }

        for (Map.Entry<String, Double> e : ratios.entrySet()) {
            card.add(Box.createVerticalStrut(6));
            card.add(progressRow(e.getKey(), e.getValue()));
        }
        return card;
    }

    private JPanel progressRow(String category, double ratio) {
        JPanel row = new JPanel(new BorderLayout(8, 2));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JPanel labelRow = new JPanel(new BorderLayout());
        labelRow.setOpaque(false);
        JLabel nameLabel = new JLabel(category);
        nameLabel.setFont(Theme.FONT_BOLD);
        JLabel pctLabel = new JLabel(String.format("%.0f%%", ratio * 100));
        pctLabel.setFont(Theme.FONT_BOLD);
        labelRow.add(nameLabel, BorderLayout.WEST);
        labelRow.add(pctLabel, BorderLayout.EAST);
        row.add(labelRow, BorderLayout.NORTH);

        ProgressBar bar = new ProgressBar(ratio);
        bar.setPreferredSize(new Dimension(100, 10));
        row.add(bar, BorderLayout.CENTER);
        return row;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(26);
        table.setShowGrid(false);
        table.setFont(Theme.FONT_BASE);
        JTableHeader header = table.getTableHeader();
        header.setBackground(Theme.PRIMARY);
        header.setForeground(Color.WHITE);
        header.setFont(Theme.FONT_BOLD);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF5F8FB));
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    private void openBudgetDialog() {
        Budget existing = budgetManager.getBudget(currentMonth);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "예산 설정 - " + currentMonth, true);
        dialog.getContentPane().setBackground(Theme.APP_BACKGROUND);
        Theme.Card card = new Theme.Card(new GridBagLayout());
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Theme.APP_BACKGROUND);
        outer.setBorder(new EmptyBorder(16, 16, 16, 16));
        outer.add(card, BorderLayout.CENTER);
        dialog.add(outer);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        JTextField totalField = new JTextField(existing != null && existing.getTotalLimit() >= 0 ? String.valueOf(existing.getTotalLimit()) : "", 10);
        c.gridx = 0; c.gridy = 0; card.add(new JLabel("전체 한도(원, 비워두면 미설정)"), c);
        c.gridx = 1; card.add(totalField, c);

        Map<String, JTextField> categoryFields = new HashMap<>();
        int row = 1;
        for (Category cat : classifier.getCategories()) {
            long limit = existing != null ? existing.getCategoryLimit(cat.getName()) : -1;
            JTextField field = new JTextField(limit >= 0 ? String.valueOf(limit) : "", 10);
            categoryFields.put(cat.getName(), field);
            c.gridx = 0; c.gridy = row;
            card.add(new JLabel(cat.getName() + " 한도(원)"), c);
            c.gridx = 1;
            card.add(field, c);
            row++;
        }

        JButton saveBtn = Theme.primaryButton("저장");
        saveBtn.addActionListener(e -> {
            long totalLimit = parseLongOrDefault(totalField.getText(), -1);
            Budget budget = new Budget(currentMonth, totalLimit);
            for (Map.Entry<String, JTextField> fe : categoryFields.entrySet()) {
                long limit = parseLongOrDefault(fe.getValue().getText(), -1);
                if (limit >= 0) {
                    budget.setCategoryLimit(fe.getKey(), limit);
                }
            }
            budgetManager.setBudget(budget);
            dialog.dispose();
            refresh();
        });
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        card.add(saveBtn, c);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private long parseLongOrDefault(String text, long def) {
        try {
            String trimmed = text.trim();
            if (trimmed.isEmpty()) return def;
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    /** 가로 진행률 바. 80% 이상이면 경고색으로 표시. */
    private static class ProgressBar extends JPanel {
        private final double ratio;

        ProgressBar(double ratio) {
            this.ratio = ratio;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(0xE5E9EF));
            g2.fillRoundRect(0, 0, w, h, h, h);
            int filled = (int) (Math.min(1.0, ratio) * w);
            g2.setColor(Theme.progressColor(ratio));
            if (filled > 0) g2.fillRoundRect(0, 0, filled, h, h, h);
            g2.dispose();
        }
    }

    /** 카테고리별 지출 비율 파이차트. */
    private static class PieChart extends JPanel {
        private final Map<String, Long> data;
        private static final Color[] PALETTE = {
                Theme.PRIMARY, Theme.PRIMARY_LIGHT, new Color(0xA9C6E8), new Color(0x7FA8D4),
                new Color(0x2F6690), new Color(0xC7DAEE)
        };

        PieChart(Map<String, Long> data) {
            this.data = data;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            long total = data.values().stream().mapToLong(Long::longValue).sum();
            if (total <= 0) {
                g.setColor(Theme.TEXT_MUTED);
                g.drawString("데이터 없음", getWidth() / 2 - 30, getHeight() / 2);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 20;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            double startAngle = 90;
            int colorIdx = 0;
            for (Map.Entry<String, Long> e : data.entrySet()) {
                double angle = 360.0 * e.getValue() / total;
                g2.setColor(PALETTE[colorIdx % PALETTE.length]);
                g2.fillArc(x, y, size, size, (int) Math.round(startAngle), -(int) Math.round(angle));
                startAngle -= angle;
                colorIdx++;
            }
            g2.dispose();
        }
    }

    /** 기간별 지출을 간단한 막대그래프로 그리는 패널. */
    private static class WeekChart extends JPanel {
        private List<StatEntry> data = List.of();

        void setData(List<StatEntry> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data.isEmpty()) return;

            int w = getWidth(), h = getHeight();
            int padding = 30;
            int chartH = h - padding * 2;
            int barWidth = Math.max(20, (w - padding * 2) / (data.size() * 2));

            long max = 1;
            for (StatEntry e : data) max = Math.max(max, e.getExpense());

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = padding;
            for (StatEntry e : data) {
                int barH = (int) ((double) e.getExpense() / max * chartH);
                g2.setColor(Theme.PRIMARY);
                g2.fillRoundRect(x, padding + chartH - barH, barWidth, Math.max(barH, 1), 4, 4);
                g2.setColor(Theme.TEXT_MUTED);
                g2.drawString(e.getLabel(), x, h - padding + 15);
                x += barWidth * 2;
            }
            g2.dispose();
        }
    }
}
