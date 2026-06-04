/*
 * LockOverlay: reusable glass-pane component that blocks the UI when the file is locked.
 */
package gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import main.LogTextEditor;
import javax.swing.SwingUtilities;

public class LockOverlay extends JPanel {
    private static final java.awt.Color OVERLAY_COLOR = new java.awt.Color(0, 0, 0, 140);
    private final LogTextEditor owner;

    @Override
    protected void paintComponent(Graphics g) {
        // Paint semi-transparent overlay manually — do NOT call super so we don't
        // paint an opaque background that hides the content underneath.
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(OVERLAY_COLOR);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
        // Let Swing paint child components (the message label) on top.
        super.paintChildren(g);
    }

    public LockOverlay(LogTextEditor owner) {
        this.owner = owner;
        try {
            setLayout(new GridBagLayout());
            // Must be false so Swing still paints the content pane behind this glass pane.
            setOpaque(false);

            JLabel msg = new JLabel("<html><div style='text-align:center;font-size:14px;color:#FFFFFF;'>\uD83D\uDD12 File locked.&nbsp;&nbsp;<u>Unlock file</u></div></html>", SwingConstants.CENTER);
            msg.setForeground(java.awt.Color.WHITE);
            msg.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            msg.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            owner.getTabPane().setSelectedIndex(2);
                            owner.manualUnlock();
                        } catch (Exception ignored) {}
                    });
                }
            });
            add(msg);
            // Consume all mouse/key events so they don't bleed through to components below.
            java.awt.event.MouseAdapter blocker = new java.awt.event.MouseAdapter() {};
            addMouseListener(blocker);
            addMouseMotionListener(blocker);
            setVisible(false);
            // Register as glass pane on owner
            try { owner.setGlassPane(this); } catch (Exception ignore) {}
        } catch (Exception ignore) {}
    }

    public void showOverlay(boolean visible) {
        try {
            setVisible(visible);
            // Ensure the glass pane itself is visible/invisible; repaint parent so components behind re-render.
            if (getParent() != null) getParent().repaint();
        } catch (Exception ignore) {}
    }

    /**
     * Run a Callable while temporarily hiding the overlay, restoring visibility afterwards.
     */
    public <T> T withOverlayHidden(java.util.concurrent.Callable<T> callable) throws Exception {
        boolean wasVisible = isVisible();
        try {
            showOverlay(false);
            return callable.call();
        } finally {
            showOverlay(wasVisible);
        }
    }

    public void runWithOverlayHidden(Runnable r) {
        try {
            withOverlayHidden(() -> { r.run(); return null; });
        } catch (Exception ignore) {}
    }

}
