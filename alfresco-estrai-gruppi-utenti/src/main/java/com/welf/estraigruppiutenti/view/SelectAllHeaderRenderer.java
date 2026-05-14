package com.welf.estraigruppiutenti.view;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

/**
 * Renderer dell'header della colonna checkbox con funzionalità "Seleziona tutti".
 */
public class SelectAllHeaderRenderer implements TableCellRenderer {
    private final JCheckBox checkBox;

    /**
     * Crea il renderer.
     */
    public SelectAllHeaderRenderer() {
        this.checkBox = new JCheckBox();
        this.checkBox.setHorizontalAlignment(SwingConstants.CENTER);
        this.checkBox.setOpaque(false);
    }

    /**
     * Imposta lo stato del checkbox nell'header.
     *
     * @param selected true se selezionato.
     */
    public void setSelected(boolean selected) {
        checkBox.setSelected(selected);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return checkBox;
    }
}
