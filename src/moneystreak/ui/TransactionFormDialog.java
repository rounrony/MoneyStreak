package moneystreak.ui;

import moneystreak.manager.AutoClassifier;
import moneystreak.manager.TransactionManager;
import moneystreak.model.Transaction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 화면2: 거래 추가/수정 폼 (F1). CP3 GUI 와이어프레임의 "거래 입력 폼" 화면을 모달 다이얼로그로 구현.
 */
public class TransactionFormDialog extends JDialog {

    private final TransactionManager transactionManager;
    private final Transaction editing; // null이면 신규 추가
    private final Runnable onSaved;

    private final Theme.FlatToggleButton expenseToggle = new Theme.FlatToggleButton("지출", true);
    private final Theme.FlatToggleButton incomeToggle = new Theme.FlatToggleButton("수입", false);
    private final JTextField dateField = new JTextField();
    private final JTextField amountField = new JTextField();
    private final JComboBox<String> categoryBox = new JComboBox<>();
    private final JTextField memoField = new JTextField();

    public TransactionFormDialog(Frame owner, TransactionManager transactionManager,
                                  AutoClassifier classifier, Transaction editing, Runnable onSaved) {
        this(owner, transactionManager, classifier, editing, null, onSaved);
    }

    /**
     * defaultDate: 신규 추가(editing==null)일 때 날짜 칸에 미리 채워둘 날짜.
     * null이면 오늘 날짜를 쓴다 (캘린더에서 특정 날짜를 클릭해 들어온 경우 그 날짜를 넘겨준다).
     */
    public TransactionFormDialog(Frame owner, TransactionManager transactionManager,
                                  AutoClassifier classifier, Transaction editing, LocalDate defaultDate, Runnable onSaved) {
        super(owner, editing == null ? "거래 추가" : "거래 수정", true);
        this.transactionManager = transactionManager;
        this.editing = editing;
        this.onSaved = onSaved;

        getContentPane().setBackground(Theme.APP_BACKGROUND);
        setLayout(new BorderLayout());

        JPanel header = new JPanel();
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(14, 18, 14, 18));
        header.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel titleLabel = new JLabel("머니스트릭 (MoneyStreak) — 거래 입력 폼");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(Theme.FONT_TITLE);
        header.add(titleLabel);
        add(header, BorderLayout.NORTH);

        add(buildForm(), BorderLayout.CENTER);

        for (var c : classifier.getCategories()) {
            categoryBox.addItem(c.getName());
        }

        if (editing != null) {
            (editing.getType() == Transaction.Type.INCOME ? incomeToggle : expenseToggle).setSelected(true);
            dateField.setText(editing.getDate().toString());
            amountField.setText(String.valueOf(editing.getAmount()));
            categoryBox.setSelectedItem(editing.getCategory());
            memoField.setText(editing.getMemo());
        } else {
            dateField.setText((defaultDate != null ? defaultDate : LocalDate.now()).toString());
        }

        setSize(560, 520);
        setLocationRelativeTo(owner);
    }

    private JPanel buildForm() {
        Theme.Card card = new Theme.Card(new GridBagLayout());
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Theme.APP_BACKGROUND);
        outer.setBorder(new EmptyBorder(20, 20, 20, 20));
        outer.add(card, BorderLayout.CENTER);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        int y = 0;

        JLabel formTitle = Theme.sectionTitle("거래 추가 / 수정");
        c.gridx = 0; c.gridy = y; c.gridwidth = 2;
        card.add(formTitle, c);
        y++;

        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(expenseToggle);
        typeGroup.add(incomeToggle);
        JPanel togglePanel = new JPanel(new GridLayout(1, 2, 0, 0));
        togglePanel.add(expenseToggle);
        togglePanel.add(incomeToggle);
        togglePanel.setPreferredSize(new Dimension(300, 40));
        c.gridx = 0; c.gridy = y; c.gridwidth = 2;
        card.add(togglePanel, c);
        y++;

        c.gridwidth = 1;
        c.gridx = 0; c.gridy = y;
        card.add(labeled("날짜", dateField), c);
        c.gridx = 1;
        card.add(labeled("금액", amountField), c);
        y++;

        c.gridx = 0; c.gridy = y; c.gridwidth = 2;
        card.add(labeled("카테고리", categoryBox), c);
        y++;

        c.gridx = 0; c.gridy = y; c.gridwidth = 2;
        card.add(labeled("메모", memoField), c);
        y++;

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton cancelBtn = Theme.secondaryButton("취소");
        cancelBtn.addActionListener(e -> dispose());
        JButton saveBtn = Theme.primaryButton("저장");
        saveBtn.addActionListener(e -> onSave());
        buttonRow.add(cancelBtn);
        buttonRow.add(saveBtn);
        c.gridx = 0; c.gridy = y; c.gridwidth = 2;
        card.add(buttonRow, c);

        return outer;
    }

    private JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        JLabel l = new JLabel(label);
        l.setFont(Theme.FONT_BASE);
        l.setForeground(Theme.TEXT_MUTED);
        p.setOpaque(false);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 32));
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private void onSave() {
        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            Transaction.Type type = expenseToggle.isSelected() ? Transaction.Type.EXPENSE : Transaction.Type.INCOME;
            String category = (String) categoryBox.getSelectedItem();
            if (category == null) category = AutoClassifier.UNCATEGORIZED;
            long amount = Long.parseLong(amountField.getText().trim());
            String memo = memoField.getText().trim();

            if (editing == null) {
                transactionManager.add(date, type, category, amount, memo);
            } else {
                transactionManager.update(editing.getId(), date, type, category, amount, memo);
            }

            dispose();
            if (onSaved != null) onSaved.run();
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "날짜 형식이 올바르지 않습니다 (예: 2026-11-01)", "입력 오류", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "금액은 숫자로 입력해주세요", "입력 오류", JOptionPane.ERROR_MESSAGE);
        }
    }
}
