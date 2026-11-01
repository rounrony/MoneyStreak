package moneystreak;

import moneystreak.ui.MainFrame;

import javax.swing.SwingUtilities;

/**
 * 머니스트릭(MoneyStreak) 실행 진입점.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
