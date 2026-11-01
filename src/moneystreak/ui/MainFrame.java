package moneystreak.ui;

import moneystreak.manager.*;
import moneystreak.store.DataStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;

/**
 * 머니스트릭(MoneyStreak) 메인 윈도우.
 * CP3 GUI 와이어프레임의 구조(타이틀바 + 좌측 네비 + 카드형 콘텐츠)를 그대로 따른다.
 * 네비게이션: 메인(대시보드) / 통계·예산 / 그룹 경쟁.
 */
public class MainFrame extends JFrame {

    private final TransactionManager transactionManager;
    private final AutoClassifier classifier;
    private final BudgetManager budgetManager;
    private final StreakManager streakManager;
    private final StatisticsEngine statisticsEngine;
    private final ShareManager shareManager;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final JLabel titleLabel = new JLabel();

    private DashboardPanel dashboardPanel;
    private StatisticsPanel statisticsPanel;
    private GroupPanel groupPanel;

    private final String[] navNames = {"메인", "통계·예산", "그룹 경쟁"};
    private final JPanel[] navItems = new JPanel[3];
    private int selectedNav = 0;

    public MainFrame() {
        super("머니스트릭 (MoneyStreak)");

        DataStore store = new DataStore("data");
        this.transactionManager = new TransactionManager(store);
        this.classifier = new AutoClassifier(store);
        this.budgetManager = new BudgetManager(store);
        this.streakManager = new StreakManager(store, budgetManager);
        this.statisticsEngine = new StatisticsEngine(transactionManager);
        this.shareManager = new ShareManager(store, streakManager, budgetManager, transactionManager);

        streakManager.catchUpTo(LocalDate.now(), transactionManager);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 760);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        add(buildTitleBar(), BorderLayout.NORTH);
        add(buildNavPanel(), BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        buildPanels();
        setScreenTitle(0);
    }

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.PRIMARY);
        bar.setBorder(new EmptyBorder(14, 20, 14, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(Theme.FONT_TITLE);
        bar.add(titleLabel, BorderLayout.WEST);
        return bar;
    }

    private JPanel buildNavPanel() {
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBackground(Theme.NAV_BACKGROUND);
        nav.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.CARD_BORDER));
        nav.setPreferredSize(new Dimension(160, 0));
        nav.add(Box.createVerticalStrut(10));

        String[] cardNames = {"dashboard", "statistics", "group"};
        for (int i = 0; i < navNames.length; i++) {
            int idx = i;
            JPanel item = buildNavItem(navNames[i]);
            item.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    selectNav(idx, cardNames[idx]);
                }
            });
            navItems[i] = item;
            nav.add(item);
            nav.add(Box.createVerticalStrut(4));
        }
        return nav;
    }

    private JPanel buildNavItem(String label) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBorder(new EmptyBorder(12, 16, 12, 16));
        item.setMaximumSize(new Dimension(160, 44));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel label1 = new JLabel(label);
        label1.setFont(Theme.FONT_BOLD);
        item.add(label1, BorderLayout.WEST);
        return item;
    }

    private void selectNav(int idx, String cardName) {
        selectedNav = idx;
        cardLayout.show(contentPanel, cardName);
        setScreenTitle(idx);
        refreshStyles();
        refreshCurrentPanel(cardName);
    }

    private void setScreenTitle(int idx) {
        titleLabel.setText("머니스트릭 (MoneyStreak) — " + navNames[idx]);
        refreshStyles();
    }

    private void refreshStyles() {
        for (int i = 0; i < navItems.length; i++) {
            JPanel item = navItems[i];
            JLabel label = (JLabel) item.getComponent(0);
            if (i == selectedNav) {
                item.setBackground(Theme.PRIMARY);
                item.setOpaque(true);
                label.setForeground(Color.WHITE);
            } else {
                item.setOpaque(false);
                label.setForeground(Theme.TEXT_PRIMARY);
            }
            item.repaint();
        }
    }

    private void buildPanels() {
        dashboardPanel = new DashboardPanel(this, transactionManager, classifier, budgetManager,
                streakManager, statisticsEngine, this::onTransactionsChanged);
        statisticsPanel = new StatisticsPanel(statisticsEngine, budgetManager, classifier, transactionManager);
        groupPanel = new GroupPanel(shareManager);

        contentPanel.add(dashboardPanel, "dashboard");
        contentPanel.add(statisticsPanel, "statistics");
        contentPanel.add(groupPanel, "group");
    }

    private void refreshCurrentPanel(String cardName) {
        switch (cardName) {
            case "dashboard": dashboardPanel.refresh(); break;
            case "statistics": statisticsPanel.refresh(); break;
            case "group": groupPanel.refresh(); break;
            default: break;
        }
    }

    /** 거래가 추가/수정/삭제되면 그날의 스트릭 처리를 다시 돌린다. */
    private void onTransactionsChanged() {
        streakManager.catchUpTo(LocalDate.now(), transactionManager);
    }
}
