package com.welf.estraigruppiutenti.util;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import javax.swing.SwingUtilities;
import java.util.function.Consumer;

/**
 * Appender Logback che inoltra i log verso l'interfaccia Swing.
 */
public class GuiLogAppender extends AppenderBase<ILoggingEvent> {
    private static volatile Consumer<String> logConsumer;
    private String pattern = "%d{HH:mm:ss} %-5level - %msg%n";
    private PatternLayout layout;

    /**
     * Imposta il consumer che riceve le righe di log da mostrare in GUI.
     *
     * @param consumer consumer (tipicamente un append su JTextArea).
     */
    public static void setLogConsumer(Consumer<String> consumer) {
        logConsumer = consumer;
    }

    /**
     * Imposta il pattern di formattazione.
     *
     * @param pattern pattern Logback.
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public void start() {
        PatternLayout pl = new PatternLayout();
        pl.setContext(getContext());
        pl.setPattern(pattern);
        pl.start();
        this.layout = pl;
        super.start();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        Consumer<String> consumer = logConsumer;
        PatternLayout currentLayout = layout;
        if (consumer == null || currentLayout == null) {
            return;
        }
        String msg = currentLayout.doLayout(eventObject);
        SwingUtilities.invokeLater(() -> consumer.accept(msg));
    }
}
