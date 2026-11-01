package moneystreak.ui;

import moneystreak.manager.*;
import moneystreak.model.StatEntry;
import moneystreak.model.StreakStatus;
import moneystreak.model.Tier;
import moneystreak.model.Transaction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * 화면1: 메인 대시보드 (F1 거래 CRUD + F3 캘린더 조회 + F5 예산 경고 요약 + F7 티어/스트릭 요약).
 * CP3 GUI 와이어프레임 "메인 화면"을 그대로 재구성: 예산 경고 배너 + 요약 카드 3개 +
 * 캘린더 카드 + 최근 거래 내역 카드.
 */
public class DashboardPanel extends JPanel {

    private final Frame owner;
    private final TransactionManager transactionManager;
    private final AutoClassifier classifier;
    private final BudgetManager budgetManager;
    private final StreakManager streakManager;
    private final StatisticsEngine statisticsEngine;
    private final Runnable onChanged;

    private final JPanel contentColumn = new JPanel();
    private final TxTableModel recentModel = new TxTableModel();
    private YearMonth calendarMonth = YearMonth.now();

    public DashboardPanel(Frame owner, TransactionManager transactionManager, AutoClassifier classifier,
                           BudgetManager budgetManager, StreakManager streakManager,
                           StatisticsEngine statisticsEngine, Runnable onChanged) {
        this.owner = owner;
        this.transactionManager = transactionManager;
        this.classifier = classifier;
        this.budgetManager = budgetManager;
        this.streakManager = streakManager;
        this.statisticsEngine = statisticsEngine;
        this.onChanged = onChanged;

        setLayout(new BorderLayout());
        setBackground(Theme.APP_BACKGROUND);

        contentColumn.setLayout(new BoxLayout(contentColumn, BoxLayout.Y_AXIS));
        contentColumn.setBackground(Theme.APP_BACKGROUND);
        contentColumn.setBorder(new EmptyBorder(16, 20, 16, 20));

        JScrollPane scroll = new JScrollPane(contentColumn);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        refresh();
    }

    public void refresh() {
        contentColumn.removeAll();

        YearMonth thisMonth = YearMonth.now();
        List<Transaction> monthTx = transactionManager.getByMonth(thisMonth);

        JPanel alert = buildAlertBanner(thisMonth, monthTx);
        if (alert != null) {
            alert.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentColumn.add(alert);
            contentColumn.add(Box.createVerticalStrut(14));
        }

        JLabel overviewTitle = Theme.sectionTitle("이번 달 개요");
        overviewTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentColumn.add(overviewTitle);
        contentColumn.add(Box.createVerticalStrut(10));

        JPanel summaryRow = buildSummaryRow(thisMonth, monthTx);
        summaryRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentColumn.add(summaryRow);
        contentColumn.add(Box.createVerticalStrut(14));

        JPanel mainRow = new JPanel(new GridLayout(1, 2, 16, 0));
        mainRow.setOpaque(false);
        mainRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainRow.add(buildCalendarCard(calendarMonth));
        mainRow.add(buildRecentTransactionsCard());
        mainRow.setMinimumSize(new Dimension(500, 380));
        mainRow.setPreferredSize(new Dimension(880, 420));
        mainRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 420));
        contentColumn.add(mainRow);

        contentColumn.revalidate();
        contentColumn.repaint();
    }

    private JPanel buildAlertBanner(YearMonth month, List<Transaction> monthTx) {
        Map<String, Double> ratios = budgetManager.getCategoryUsageRatios(month, monthTx);
        String worstCategory = null;
        double worstRatio = 0;
        for (Map.Entry<String, Double> e : ratios.entrySet()) {
            if (e.getValue() > worstRatio) {
                worstRatio = e.getValue();
                worstCategory = e.getKey();
            }
        }
        if (worstCategory == null || worstRatio < 0.8) {
            return null;
        }
        Theme.Card card = new Theme.Card(new BorderLayout());
        card.setBackground(Theme.DANGER_BG);
        card.setOpaque(true);
        JLabel label = new JLabel(String.format("⚠ %s 카테고리 예산의 %.0f%%를 사용했습니다.", worstCategory, worstRatio * 100));
        label.setForeground(Theme.DANGER);
        label.setFont(Theme.FONT_BOLD);
        card.add(label, BorderLayout.WEST);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        return card;
    }

    private JPanel buildSummaryRow(YearMonth month, List<Transaction> monthTx) {
        long expense = 0, income = 0;
        for (Transaction t : monthTx) {
            if (t.getType() == Transaction.Type.EXPENSE) expense += t.getAmount();
            else income += t.getAmount();
        }

        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        row.add(statCard("이번 달 지출", String.format("-₩%,d", expense), Theme.DANGER));
        row.add(statCard("이번 달 수입", String.format("+₩%,d", income), Theme.SUCCESS));

        StreakStatus s = streakManager.getStatus();
        Tier tier = s.getCurrentTier();
        row.add(statCard("티어 · 스트릭", tier.getDisplayName() + " 🔥 " + s.getCurrentStreakDays() + "일 연속", Theme.PRIMARY));

        return row;
    }

    private Theme.Card statCard(String title, String value, Color valueColor) {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 6));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.FONT_BASE);
        titleLabel.setForeground(Theme.TEXT_MUTED);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        valueLabel.setForeground(valueColor);
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private Theme.Card buildCalendarCard(YearMonth month) {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 10));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel title = Theme.sectionTitle("캘린더로 내역 조회");
        JLabel sub = new JLabel(month.getYear() + "년 " + month.getMonthValue() + "월");
        sub.setFont(Theme.FONT_BASE);
        sub.setForeground(Theme.TEXT_MUTED);
        titleBlock.add(title);
        titleBlock.add(sub);
        headerPanel.add(titleBlock, BorderLayout.WEST);

        JPanel navRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        navRow.setOpaque(false);
        JButton prevBtn = Theme.secondaryButton("◀");
        JButton nextBtn = Theme.secondaryButton("▶");
        prevBtn.addActionListener(e -> { calendarMonth = calendarMonth.minusMonths(1); refresh(); });
        nextBtn.addActionListener(e -> { calendarMonth = calendarMonth.plusMonths(1); refresh(); });
        navRow.add(prevBtn);
        navRow.add(nextBtn);
        headerPanel.add(navRow, BorderLayout.EAST);

        card.add(headerPanel, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 7, 3, 3));
        grid.setOpaque(false);
        String[] dow = {"일", "월", "화", "수", "목", "금", "토"};
        for (String d : dow) {
            JLabel l = new JLabel(d, SwingConstants.CENTER);
            l.setFont(Theme.FONT_BASE);
            l.setForeground(Theme.TEXT_MUTED);
            grid.add(l);
        }
        LocalDate firstDay = month.atDay(1);
        int leading = firstDay.getDayOfWeek().getValue() % 7;
        for (int i = 0; i < leading; i++) grid.add(new JLabel(""));

        Map<LocalDate, StatEntry> calendar = statisticsEngine.getMonthlyCalendar(month);
        for (Map.Entry<LocalDate, StatEntry> e : calendar.entrySet()) {
            grid.add(dayCell(e.getKey(), e.getValue()));
        }

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel dayCell(LocalDate date, StatEntry entry) {
        boolean hasActivity = entry.getExpense() > 0 || entry.getIncome() > 0;
        JPanel cell = new JPanel();
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setBackground(hasActivity ? Theme.HIGHLIGHT_CELL : Theme.CARD_BACKGROUND);
        cell.setOpaque(true);
        cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                new EmptyBorder(4, 6, 4, 6)));
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cell.setToolTipText("클릭해서 이 날짜의 거래 내역 보기");

        JLabel dayLabel = new JLabel(String.valueOf(date.getDayOfMonth()));
        dayLabel.setFont(Theme.FONT_BASE);
        dayLabel.setForeground(Theme.TEXT_PRIMARY);
        cell.add(dayLabel);

        if (entry.getExpense() > 0) {
            JLabel amt = new JLabel(String.format("%,d", entry.getExpense()));
            amt.setFont(new Font("SansSerif", Font.BOLD, 11));
            amt.setForeground(Theme.TEXT_PRIMARY);
            cell.add(amt);
        }

        cell.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                new DayTransactionsDialog(owner, transactionManager, classifier, date, DashboardPanel.this::onFormSaved)
                        .setVisible(true);
            }
        });
        return cell;
    }

    private Theme.Card buildRecentTransactionsCard() {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 10));

        JLabel title = Theme.sectionTitle("최근 거래 내역");
        card.add(title, BorderLayout.NORTH);

        recentModel.setData(transactionManager.getAll());
        JTable table = new JTable(recentModel);
        table.setRowHeight(26);
        table.setShowGrid(false);
        table.setFont(Theme.FONT_BASE);
        table.setSelectionBackground(Theme.HIGHLIGHT_CELL);
        table.setSelectionForeground(Theme.TEXT_PRIMARY);

        JTableHeader header = table.getTableHeader();
        header.setBackground(Theme.PRIMARY);
        header.setForeground(Color.WHITE);
        header.setFont(Theme.FONT_BOLD);
        header.setPreferredSize(new Dimension(0, 30));

        DefaultTableCellRenderer stripedRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF5F8FB));
                }
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(stripedRenderer);
        }

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.setMinimumSize(new Dimension(200, 100));
        tableScroll.setPreferredSize(new Dimension(380, 220));
        card.add(tableScroll, BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new BorderLayout());
        buttonRow.setOpaque(false);

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        leftButtons.setOpaque(false);
        JButton editBtn = Theme.secondaryButton("선택 수정");
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            Transaction t = recentModel.getRowAt(row);
            new TransactionFormDialog(owner, transactionManager, classifier, t, this::onFormSaved).setVisible(true);
        });
        JButton deleteBtn = Theme.secondaryButton("선택 삭제");
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            Transaction t = recentModel.getRowAt(row);
            transactionManager.delete(t.getId());
            onFormSaved();
        });
        leftButtons.add(editBtn);
        leftButtons.add(deleteBtn);

        JButton addBtn = Theme.primaryButton("+ 거래 추가");
        addBtn.addActionListener(e ->
                new TransactionFormDialog(owner, transactionManager, classifier, null, this::onFormSaved).setVisible(true));
        JPanel rightButton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        rightButton.setOpaque(false);
        rightButton.add(addBtn);

        buttonRow.add(leftButtons, BorderLayout.WEST);
        buttonRow.add(rightButton, BorderLayout.EAST);
        card.add(buttonRow, BorderLayout.SOUTH);

        return card;
    }

    private void onFormSaved() {
        refresh();
        if (onChanged != null) onChanged.run();
    }

    /** 거래 목록 테이블 모델. */
    private static class TxTableModel extends AbstractTableModel {
        private final String[] columns = {"날짜", "카테고리", "금액", "메모"};
        private List<Transaction> data = List.of();

        void setData(List<Transaction> data) {
            this.data = data;
            fireTableDataChanged();
        }

        Transaction getRowAt(int row) {
            return data.get(data.size() - 1 - row);
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            // 최신 거래가 위로 오도록 역순 정렬
            Transaction t = data.get(data.size() - 1 - rowIndex);
            switch (columnIndex) {
                case 0: return t.getDate().toString();
                case 1: return t.getCategory();
                case 2: return (t.getType() == Transaction.Type.EXPENSE ? "-" : "+") + String.format("%,d", t.getAmount());
                case 3: return t.getMemo();
                default: return "";
            }
        }
    }
}
