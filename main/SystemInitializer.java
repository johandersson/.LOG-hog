/*
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package main;

import gui.SystemTrayMenu;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;

public class SystemInitializer {
    private final JFrame frame;

    public SystemInitializer(JFrame frame) {
        this.frame = frame;
    }

    public void initializeSystemComponents() {
        initSystemTray();
        setupActivityListeners();
    }

    private void initSystemTray() {
        var systemTrayMenu = new SystemTrayMenu((LogTextEditor) frame);
        SystemTrayMenu.initSystemTray();
    }

    private void setupActivityListeners() {
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Activity detected, but no auto-lock
            }
        });
        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Activity detected, but no auto-lock
            }
        });
    }
}