import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class NavItem extends JPanel {
    private final JLabel label;
    private final int tabIndex;
    private final JTabbedPane tabPane;
    private final Runnable onClick;
    private boolean hovered = false;

    public NavItem(String title, int tabIndex, JTabbedPane tabPane, Runnable onClick) {
        this.tabIndex = tabIndex;
        this.tabPane = tabPane;
        this.onClick = onClick;

        setLayout(new BorderLayout());
        setOpaque(false); // never fill with an opaque rectangular background by default
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        setPreferredSize(new Dimension(160, 36));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(0, 0, 0, 0));

        label = new JLabel(title);
        label.setBorder(new EmptyBorder(8, 8, 8, 8));
        label.setOpaque(false);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 13f));
        label.setForeground(new Color(0x5E6A70));
        add(label, BorderLayout.WEST);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onClick != null) {
                    onClick.run();
                } else if (tabPane != null && tabIndex >= 0 && tabIndex < tabPane.getTabCount()) {
                    tabPane.setSelectedIndex(tabIndex);
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                // only request repaint; do not call setOpaque(true) — painting is done in paintComponent
                repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });

        if (tabPane != null) {
            tabPane.addChangeListener(e -> updateVisualState());
        }

        updateVisualState();
    }

    private void updateVisualState() {
        boolean active = tabPane != null && tabPane.getSelectedIndex() == tabIndex;
        label.setFont(label.getFont().deriveFont(active ? Font.BOLD : Font.PLAIN, 13f));
        label.setForeground(active ? new Color(0x2B3A42) : new Color(0x5E6A70));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Paint nothing when not active/hovered to avoid any rectangular artifacts
        boolean active = tabPane != null && tabPane.getSelectedIndex() == tabIndex;
        if (active || hovered) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = active ? new Color(0xEAF3FF) : new Color(0xEEF6FF);
                g2.setColor(fill);
                int arc = 10;
                int pad = 2;
                g2.fillRoundRect(pad, pad, getWidth() - pad * 2, getHeight() - pad * 2, arc, arc);
            } finally {
                g2.dispose();
            }
        }
        super.paintComponent(g); // allow children (label) to paint on top
    }
}
