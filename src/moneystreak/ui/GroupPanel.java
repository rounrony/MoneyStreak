package moneystreak.ui;

import moneystreak.manager.ShareManager;
import moneystreak.model.GroupRanking;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;

/**
 * 화면4: 그룹 경쟁 (F9). CP3 GUI 와이어프레임 "그룹 경쟁" 화면 레이아웃을 재구성:
 * 랭킹 표 카드 + 공유 카드 미리보기 카드 + 내보내기/가져오기 버튼.
 */
public class GroupPanel extends JPanel {

    private final ShareManager shareManager;
    private final JTextField nicknameField = new JTextField("홍길동", 10);
    private final JPanel rankingRows = new JPanel();
    private final JLabel cardPreviewTitle = new JLabel();
    private final JLabel cardPreviewBody = new JLabel();

    public GroupPanel(ShareManager shareManager) {
        this.shareManager = shareManager;

        setLayout(new BorderLayout());
        setBackground(Theme.APP_BACKGROUND);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.APP_BACKGROUND);
        header.setBorder(new EmptyBorder(16, 20, 14, 20));
        JLabel title = Theme.sectionTitle("그룹 경쟁 및 통계 카드 공유");
        title.setFont(Theme.FONT_TITLE);
        header.add(title, BorderLayout.WEST);

        JPanel nickRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        nickRow.setOpaque(false);
        nickRow.add(new JLabel("닉네임:"));
        nickRow.add(nicknameField);
        header.add(nickRow, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridLayout(1, 2, 16, 0));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(0, 20, 20, 20));
        content.add(buildRankingCard());
        content.add(buildSharePreviewCard());
        add(content, BorderLayout.CENTER);

        refresh();
    }

    private Theme.Card buildRankingCard() {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 10));
        card.add(Theme.sectionTitle("그룹 랭킹"), BorderLayout.NORTH);

        JPanel table = new JPanel();
        table.setLayout(new BoxLayout(table, BoxLayout.Y_AXIS));
        table.setOpaque(false);

        JPanel headerRow = new JPanel(new GridLayout(1, 5));
        headerRow.setBackground(Theme.PRIMARY);
        headerRow.setBorder(new EmptyBorder(6, 10, 6, 10));
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String col : new String[]{"순위", "닉네임", "티어", "진행점수", "연속일수"}) {
            JLabel l = new JLabel(col);
            l.setForeground(Color.WHITE);
            l.setFont(Theme.FONT_BOLD);
            headerRow.add(l);
        }
        table.add(headerRow);

        rankingRows.setLayout(new BoxLayout(rankingRows, BoxLayout.Y_AXIS));
        rankingRows.setOpaque(false);
        table.add(rankingRows);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private Theme.Card buildSharePreviewCard() {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 14));
        card.add(Theme.sectionTitle("공유 카드 미리보기"), BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JPanel cardPreview = new JPanel();
        cardPreview.setLayout(new BoxLayout(cardPreview, BoxLayout.Y_AXIS));
        cardPreview.setBackground(Theme.PRIMARY);
        cardPreview.setBorder(new EmptyBorder(18, 18, 18, 18));
        cardPreview.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPreview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        cardPreviewTitle.setForeground(Color.WHITE);
        cardPreviewTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        cardPreviewBody.setForeground(new Color(0xDCE6F2));
        cardPreviewBody.setFont(Theme.FONT_BASE);
        cardPreview.add(cardPreviewTitle);
        cardPreview.add(Box.createVerticalGlue());
        cardPreview.add(cardPreviewBody);

        center.add(cardPreview);
        center.add(Box.createVerticalStrut(14));

        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JButton exportBtn = Theme.primaryButton("내보내기");
        exportBtn.addActionListener(e -> onExport());
        JButton importBtn = Theme.secondaryButton("가져오기");
        importBtn.addActionListener(e -> onImport());
        buttons.add(exportBtn);
        buttons.add(importBtn);
        center.add(buttons);

        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private void onExport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(nicknameField.getText() + "_moneystreak.json"));
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            Path path = chooser.getSelectedFile().toPath();
            shareManager.exportTo(path, nicknameField.getText().trim());
            JOptionPane.showMessageDialog(this, "내보내기 완료: " + path);
        }
    }

    private void onImport() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            Path path = chooser.getSelectedFile().toPath();
            try {
                shareManager.importFrom(path);
                refresh();
                JOptionPane.showMessageDialog(this, "가져오기 완료");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "파일을 읽을 수 없습니다: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refresh() {
        rankingRows.removeAll();
        int rank = 1;
        String myNickname = nicknameField.getText().trim();
        for (GroupRanking.Entry e : shareManager.getRanking().getEntries()) {
            boolean isMe = e.getNickname().equals(myNickname);
            JPanel row = new JPanel(new GridLayout(1, 5));
            row.setBorder(new EmptyBorder(6, 10, 6, 10));
            row.setBackground(rank % 2 == 1 ? Color.WHITE : new Color(0xF5F8FB));
            row.setOpaque(true);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);

            row.add(cell(String.valueOf(rank)));
            row.add(cell(e.getNickname() + (isMe ? " (나)" : "")));
            row.add(cell(e.getTier().getDisplayName()));
            row.add(cell(String.valueOf(e.getTierProgressScore())));
            row.add(cell(e.getStreakDays() + "일"));
            rankingRows.add(row);
            rank++;
        }

        String month = java.time.YearMonth.now().getMonthValue() + "월";
        cardPreviewTitle.setText(myNickname + "의 " + month + " 절약 카드");
        cardPreviewBody.setText(shareManager.generateShareCardFooter());

        rankingRows.revalidate();
        rankingRows.repaint();
    }

    private JLabel cell(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_BASE);
        return l;
    }
}
