package gui;

import java.awt.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

public final class HiddenTabUI extends BasicTabbedPaneUI {
    @Override
    protected int calculateTabAreaHeight(int tabPlacement, int runCount, int maxTabHeight) {
        return 0; // reserve no space for tabs
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                      int x, int y, int w, int h, boolean isSelected) {
        // no-op: don't paint tab background
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                  int x, int y, int w, int h, boolean isSelected) {
        // no-op: don't paint tab border
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        // default content border painting is OK; if you want none, leave empty
        super.paintContentBorder(g, tabPlacement, selectedIndex);
    }
}