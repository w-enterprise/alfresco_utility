package com.welf.estraigruppiutenti.view;

import com.welf.estraigruppiutenti.model.GroupInfo;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TableModel per la visualizzazione dei gruppi con selezione tramite checkbox.
 */
public class GroupsTableModel extends AbstractTableModel {
    private static final int COL_SELECTED = 0;
    private static final int COL_GROUP = 1;
    private static final int COL_DESCRIPTION = 2;
    private static final int COL_USERS = 3;

    private final List<Row> rows = new ArrayList<>();

    /**
     * Imposta i gruppi da visualizzare.
     *
     * @param groups gruppi.
     */
    public void setGroups(List<GroupInfo> groups) {
        rows.clear();
        if (groups != null) {
            for (GroupInfo g : groups) {
                rows.add(new Row(false, g));
            }
        }
        fireTableDataChanged();
    }

    /**
     * Seleziona o deseleziona tutti i gruppi.
     *
     * @param selected true per selezionare.
     */
    public void setAllSelected(boolean selected) {
        for (Row row : rows) {
            row.selected = selected;
        }
        fireTableRowsUpdated(0, Math.max(0, rows.size() - 1));
    }

    /**
     * @return true se tutte le righe sono selezionate (e ce n'è almeno una).
     */
    public boolean areAllSelected() {
        if (rows.isEmpty()) {
            return false;
        }
        for (Row row : rows) {
            if (!row.selected) {
                return false;
            }
        }
        return true;
    }

    /**
     * Recupera i gruppi selezionati.
     *
     * @return lista gruppi selezionati.
     */
    public List<GroupInfo> getSelectedGroups() {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<GroupInfo> selected = new ArrayList<>();
        for (Row row : rows) {
            if (row.selected) {
                selected.add(row.group);
            }
        }
        return selected;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case COL_SELECTED:
                return "";
            case COL_GROUP:
                return "Nome gruppo";
            case COL_DESCRIPTION:
                return "Descrizione";
            case COL_USERS:
                return "Utenti";
            default:
                return super.getColumnName(column);
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == COL_SELECTED) {
            return Boolean.class;
        }
        if (columnIndex == COL_USERS) {
            return Integer.class;
        }
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == COL_SELECTED;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Row row = rows.get(rowIndex);
        switch (columnIndex) {
            case COL_SELECTED:
                return row.selected;
            case COL_GROUP:
                return row.group.getDisplayName();
            case COL_DESCRIPTION:
                return row.group.getDescription();
            case COL_USERS:
                return row.group.getDirectUsersCount();
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex != COL_SELECTED) {
            return;
        }
        Row row = rows.get(rowIndex);
        if (aValue instanceof Boolean) {
            row.selected = (Boolean) aValue;
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    private static final class Row {
        private boolean selected;
        private final GroupInfo group;

        private Row(boolean selected, GroupInfo group) {
            this.selected = selected;
            this.group = group;
        }
    }
}
