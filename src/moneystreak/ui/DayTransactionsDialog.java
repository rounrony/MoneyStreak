package moneystreak.ui;

import moneystreak.manager.AutoClassifier;
import moneystreak.manager.TransactionManager;
import moneystreak.model.Transaction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * 메인 화면 캘린더에서 날짜 칸을 클릭했을 때 뜨는 팝업.
 * 그 날짜의 거래 내역을 보여주고, 바로 수정/삭제/추가할 수 있다.
 */
public class DayTransactionsDialog extends JDialog {

    private final TransactionManager transactionManager;
    private final AutoClassifier classifier;
    private final LocalDate date;
    private final Runnable onChanged;

    private final DayTxTableModel tableModel = new DayTxTableModel();
    private JTable table;

    public DayTransactionsDialog(Frame owner, TransactionManager transactionManager, AutoClassifier classifier,
                                  LocalDate date, Runnable onChanged) {
        super(owner, "거래 내역 조회", true);
        this.transactionManager = transactionManager;
        this.classifier = classifier;
        this.date = date;
        this.onChanged = onChanged;

        getContentPane().setBackground(Theme.APP_BACKGROUND);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(14, 18, 14, 18));
        String dateText = date.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREAN));
        JLabel titleLabel = new JLabel(dateText + " 거래 내역");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(Theme.FONT_TITLE);
        header.add(titleLabel);
        add(header, BorderLayout.NORTH);

        add(buildBody(), BorderLayout.CENTER);

        reload();
        setSize(500, 420);
        setLocationRelativeTo(owner);
    }

    private JPanel buildBody() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Theme.APP_BACKGROUND);
        outer.setBorder(new EmptyBorder(20, 20, 20, 20));

        Theme.Card card = new Theme.Card(new BorderLayout(0, 10));

        table = new JTable(tableModel);
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

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setMinimumSize(new Dimension(200, 100));
        scroll.setPreferredSize(new Dimension(420, 220));
        card.add(scroll, BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new BorderLayout());
        buttonRow.setOpaque(false);

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        leftButtons.setOpaque(false);
        JButton editBtn = Theme.secondaryButton("선택 수정");
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            Transaction t = tableModel.getRowAt(row);
            new TransactionFormDialog((Frame) getOwner(), transactionManager, classifier, t, date, this::onFormSaved)
                    .setVisible(true);
        });
        JButton deleteBtn = Theme.secondaryButton("선택 삭제");
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            Transaction t = tableModel.getRowAt(row);
            transactionManager.delete(t.getId());
            onFormSaved();
        });
        leftButtons.add(editBtn);
        leftButtons.add(deleteBtn);

        JButton addBtn = Theme.primaryButton("+ 이 날짜에 거래 추가");
        addBtn.addActionListener(e ->
                new TransactionFormDialog((Frame) getOwner(), transactionManager, classifier, null, date, this::onFormSaved)
                        .setVisible(true));
        JPanel rightButton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        rightButton.setOpaque(false);
        rightButton.add(addBtn);

        buttonRow.add(leftButtons, BorderLayout.WEST);
        buttonRow.add(rightButton, BorderLayout.EAST);
        card.add(buttonRow, BorderLayout.SOUTH);

        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    private void onFormSaved() {
        reload();
        if (onChanged != null) onChanged.run();
    }

    private void reload() {
        tableModel.setData(transactionManager.getBetween(date, date));
    }

    /** 특정 날짜 거래 목록 테이블 모델 (이 다이얼로그 안에서는 최신순 정렬이 필요 없음). */
    private static class DayTxTableModel extends AbstractTableModel {
        private final String[] columns = {"구분", "카테고리", "금액", "메모"};
        private List<Transaction> data = List.of();

        void setData(List<Transaction> data) {
            this.data = data;
            fireTableDataChanged();
        }

        Transaction getRowAt(int row) {
            return data.get(row);
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
            Transaction t = data.get(rowIndex);
            switch (columnIndex) {
                case 0: return t.getType() == Transaction.Type.EXPENSE ? "지출" : "수입";
                case 1: return t.getCategory();
                case 2: return (t.getType() == Transaction.Type.EXPENSE ? "-" : "+") + String.format("%,d", t.getAmount());
                case 3: return t.getMemo();
                default: return "";
            }
        }
    }
}
