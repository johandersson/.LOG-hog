import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class NavItem extends JPanel {
    private final JLabel label;
    private final int tabIndex;
    private final JTabbedPane tabPane;

    public NavItem(String title, int tabIndex, JTabbedPane tabPane) {
        this.tabIndex = tabIndex;
        this.tabPane = tabPane;

        setLayout(new BorderLayout());
        setOpaque(false); // non-opaque by default to avoid stray rectangles
        setBackground(new Color(0xF7FAFC));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        label = new JLabel(title);
        label.setBorder(new EmptyBorder(8, 8, 8, 8));
        label.setOpaque(false);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 13f));
        label.setForeground(new Color(0x5E6A70));
        add(label, BorderLayout.WEST);

        // Mouse handling: click selects tab; hover shows subtle highlight
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (tabPane != null && tabIndex >= 0 && tabIndex < tabPane.getTabCount()) {
                    tabPane.setSelectedIndex(tabIndex);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (tabPane.getSelectedIndex() != tabIndex) {
                    setBackground(new Color(0xEEF6FF));
                    setOpaque(true);
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                updateBackgroundForActiveState();
            }
        });

        // keep visuals in sync when tabs change
        if (tabPane != null) {
            tabPane.addChangeListener(e -> updateBackgroundForActiveState());
        }

        // initial active state
        setActive(tabPane != null && tabPane.getSelectedIndex() == tabIndex);
    }

    public void setActive(boolean active) {
        label.setFont(label.getFont().deriveFont(active ? Font.BOLD : Font.PLAIN, 13f));
        label.setForeground(active ? new Color(0x2B3A42) : new Color(0x5E6A70));
        setOpaque(active);
        setBackground(active ? new Color(0xEAF3FF) : new Color(0xF7FAFC));
        repaint();
    }

    private void updateBackgroundForActiveState() {
        boolean active = tabPane != null && tabPane.getSelectedIndex() == tabIndex;
        setActive(active);
    }
}
