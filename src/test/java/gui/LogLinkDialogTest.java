package gui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class LogLinkDialogTest {
    @Test
    void insertsCanonicalLinksFromAllSupportedDateFormats() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (String input : java.util.List.of("2022-12-19 13:23", "19/12/2022 13:23",
                    "12/19/2022 13:23", "19.12.2022 13:23", "19-12-2022 13:23")) {
                var target = new JTextArea();
                var closes = new AtomicInteger();
                var panel = new LogLinkDialog(target,
                        timestamp -> timestamp.equals("13:23 2022-12-19"), closes::incrementAndGet);
                panel.timestampField.setText(input);
                panel.insertButton.doClick();
                assertEquals("[13:23 2022-12-19]", target.getText(), input);
                assertEquals(1, closes.get(), input);
            }
        });
    }

    @Test
    void lookupFailureCanBeRetriedWithoutClosing() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var target = new JTextArea("text");
            target.setCaretPosition(4);
            var checks = new AtomicInteger();
            var closes = new AtomicInteger();
            var panel = new LogLinkDialog(target, timestamp -> {
                if (checks.getAndIncrement() == 0) throw new IllegalStateException("Unavailable");
                return true;
            }, closes::incrementAndGet);
            panel.timestampField.setText("13:23 2022-12-12");
            panel.insertButton.doClick();
            assertEquals(0, closes.get());
            assertEquals("text", target.getText());
            assertTrue(panel.errorLabel.getText().contains("Unable to check"));
            panel.insertButton.doClick();
            assertEquals("text[13:23 2022-12-12]", target.getText());
            assertEquals(1, closes.get());
        });
    }

    @Test
    void cannotInsertAfterEditorBecomesReadOnly() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var target = new JTextArea("text");
            var panel = new LogLinkDialog(target, timestamp -> true, () -> fail("Must not succeed"));
            panel.timestampField.setText("13:23 2022-12-12");
            target.setEditable(false);
            panel.insertButton.doClick();
            assertEquals("text", target.getText());
        });
    }

    @Test
    void modalDialogRemainsVisibleOnErrorsAndClosesOnSuccess() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless());
        var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        SwingUtilities.invokeAndWait(() -> {
            var parent = new javax.swing.JFrame();
            var target = new JTextArea("text");
            parent.add(target);
            parent.pack();
            SwingUtilities.invokeLater(() -> {
                javax.swing.JDialog dialog = null;
                try {
                    for (var window : java.awt.Window.getWindows()) {
                        if (window instanceof javax.swing.JDialog candidate
                                && candidate.getTitle().equals("Insert Log Link") && candidate.isVisible()) {
                            dialog = candidate;
                            break;
                        }
                    }
                    assertNotNull(dialog);
                    var panel = (LogLinkDialog) dialog.getContentPane();
                    panel.timestampField.setText("not a timestamp");
                    panel.insertButton.doClick();
                    assertTrue(dialog.isVisible());
                    panel.timestampField.setText("13:24 2022-12-12");
                    panel.insertButton.doClick();
                    assertTrue(dialog.isVisible());
                    panel.timestampField.setText("13:23 2022-12-12");
                    panel.insertButton.doClick();
                    assertFalse(dialog.isVisible());
                    assertTrue(target.getText().contains("[13:23 2022-12-12]"));
                } catch (Throwable ex) {
                    failure.set(ex);
                } finally {
                    if (dialog != null) dialog.dispose();
                }
            });
            try {
                LogLinkDialog.showInsertLogLinkDialog(target, timestamp -> timestamp.equals("13:23 2022-12-12"));
            } finally {
                parent.dispose();
            }
        });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    @Test
    void invalidAndMissingTargetsKeepInputOpenUntilSuccess() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var target = new JTextArea("before after");
            target.select(7, 12);
            var checks = new AtomicInteger();
            var closes = new AtomicInteger();
            var panel = new LogLinkDialog(target, timestamp -> {
                checks.incrementAndGet();
                return timestamp.equals("13:23 2022-12-12");
            }, closes::incrementAndGet);

            panel.timestampField.setText("13:23 2022-02-30");
            panel.insertButton.doClick();
            assertEquals(0, checks.get(), "Format must be checked before existence");
            assertEquals(0, closes.get());
            assertTrue(panel.errorLabel.getText().contains("HH:mm yyyy-MM-dd"));
            assertEquals("before after", target.getText());
            assertEquals("13:23 2022-02-30", panel.timestampField.getText());

            panel.timestampField.setText("13:24 2022-12-12");
            panel.insertButton.doClick();
            assertEquals(1, checks.get());
            assertEquals(0, closes.get());
            assertTrue(panel.errorLabel.getText().contains("No log entry"));

            panel.timestampField.setText("2022-12-12 13:23");
            panel.insertButton.doClick();
            assertEquals("before [13:23 2022-12-12]", target.getText());
            assertEquals(1, closes.get());
        });
    }

    @Test
    void cancelLeavesTextAndSelectionUntouched() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var target = new JTextArea("unchanged");
            target.selectAll();
            var closes = new AtomicInteger();
            var panel = new LogLinkDialog(target, timestamp -> true, closes::incrementAndGet);
            panel.cancelButton.doClick();
            assertEquals("unchanged", target.getText());
            assertEquals("unchanged", target.getSelectedText());
            assertEquals(1, closes.get());
        });
    }

    @Test
    void toolbarOffersLogLinkAlongsideExternalLink() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var panel = new FormattingPanel(new JTextArea(), timestamp -> true);
            assertTrue(java.util.Arrays.stream(panel.getComponents())
                    .anyMatch(c -> c instanceof JButton b && b.getText().equals("Log link")));
            assertTrue(java.util.Arrays.stream(panel.getComponents())
                    .anyMatch(c -> c instanceof JButton b && b.getText().equals("Link")));
        });
    }
}
