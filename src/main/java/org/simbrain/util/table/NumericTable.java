/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.util.table;

import org.simbrain.util.Utils;
import org.simbrain.util.table.SimbrainJTable.CellIndex;

import java.io.File;
import java.util.List;
import java.util.Random;

/**
 * Default implementation of a table of numerical data. The table is mutable,
 * and the data is saved as a list of lists of Doubles.
 *
 * @author jyoshimi
 */
public class NumericTable extends MutableTable<Double> implements IterableRowsTable {

    /**
     * Default initial number of rows.
     */
    private static final int DEFAULT_ROW_COUNT = 30;

    /**
     * Default initial number of columns.
     */
    private static final int DEFAULT_COLUMN_COUNT = 5;

    /**
     * Iteration mode.
     */
    private boolean iterationMode = false;

    // TODO: Temp until table refactors
    private boolean integerMode = false;

    /**
     * Current row.
     */
    private int currentRow = 0;

    /**
     * Randomization upper bound.
     */
    private int upperBound = 1;

    /**
     * Randomization lower bound.
     */
    private int lowerBound = -1;

    /**
     * Construct a table with a specified number of rows and columns.
     *
     * @param numRows    number of rows.
     * @param numColumns number of columns.
     */
    public NumericTable(final int numRows, final int numColumns) {
        init(numRows, numColumns);
    }

    /**
     * Construct a table from an 2-d array of doubles.
     *
     * @param data array of doubles
     */
    public NumericTable(final double[][] data) {
        setData(data);
    }

    /**
     * Display 1-d data.  For proper display suggest wrapping this in a
     * {@link SimbrainJTableScrollPanel}.
     */
    public NumericTable(final double[] data) {
        double[][] newData = new double[1][];
        newData[0] = data;
        setData(newData);
    }

    /**
     * Default constructor.
     */
    public NumericTable() {
        init(DEFAULT_ROW_COUNT, DEFAULT_COLUMN_COUNT);
    }

    /**
     * Initialize the table.
     *
     * @param rows num rows
     * @param cols num cols
     */
    protected void init(int rows, int cols) {
        rowData.clear();
        for (int i = 0; i < rows; i++) {
            rowData.add(createNewRow(0.0, cols));
        }
        fireTableStructureChanged();
    }

    @Override
    public Class<?> getDataType() {
        return Double.class;
    }

    @Override
    Double getDefaultValue() {
        return 0.0;
    }

    /**
     * Reset data using a 2-d array of doubles.
     *
     * @param data the new data
     */
    public void setData(double[][] data) {
        reset(data.length, data[0].length);
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                this.setLogicalValue(i, j, data[i][j], false);
            }
        }
        fireTableDataChanged();
    }

    public void setData(float[][] data) {
        reset(data.length, data[0].length);
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                this.setLogicalValue(i, j, (double) data[i][j], false);
            }
        }
        fireTableDataChanged();
    }

    /**
     * Set the current row using an array of doubles.
     *
     * @param data the values to use in setting the current row
     */
    public void setVectorCurrentRow(double[] data) {
        int i = getCurrentRow();
        for (int j = 0; j < data.length; j++) {
            if (j < this.getLogicalColumnCount()) {
                this.setLogicalValue(i, j, data[j], false);
            }
        }
        fireTableDataChanged();
    }

    /**
     * Return the current row of doubles as an array.
     *
     * @return the values of the current row
     */
    public double[] getVectorCurrentRow() {
        double[] retVec = new double[this.getColumnCount()-1];
        int currRow = getCurrentRow();
        for (int i = 0; i < this.getLogicalColumnCount(); i++) {
            retVec[i] = this.getLogicalValueAt(currRow, i);
        }
        return retVec;
    }

    /**
     * Return the data as a flattened vector
     *
     * @return the data vector
     */
    public double[] getFlattenedData() {
        int cols = this.getLogicalColumnCount();
        int rows = this.getRowCount();
        int totalCells = rows * cols;

        double[] retVec = new double[totalCells];

        for (int i = 0; i < totalCells; i++) {
            retVec[i] = this.getLogicalValueAt(i/cols, i%cols);
        }
        return retVec;
    }

    /**
     * Set the values of the specified column in the current row.
     *
     * @param column column index
     * @param value  value to set
     */
    public void setValueCurrentRow(final int column, final double value) {
        setLogicalValue(currentRow, column, value, true);
    }

    /**
     * Get the value of the specified column in the current row.
     *
     * @param column column index
     * @return value of this column in current row
     */
    public double getValueCurrentRow(final int column) {
        return getLogicalValueAt(currentRow, column);
    }

    @Override
    public int getCurrentRow() {
        return currentRow;
    }

    @Override
    public void setCurrentRow(int currentRow) {
        this.currentRow = currentRow;
    }

    @Override
    public void updateCurrentRow() {
        if (isIterationMode()) {
            if (getCurrentRow() >= (getRowCount() - 1)) {
                setCurrentRow(0);
            } else {
                setCurrentRow(getCurrentRow() + 1);
            }
        }
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    private Object readResolve() {
        return this;
    }

    /**
     * @return the iterationMode
     */
    public boolean isIterationMode() {
        return iterationMode;
    }

    /**
     * @param iterationMode the iterationMode to set
     */
    public void setIterationMode(boolean iterationMode) {
        this.iterationMode = iterationMode;
    }

    /**
     * Lower bound for (e.g.) randomization.
     *
     * @return The lower bound.
     */
    public int getLowerBound() {
        return lowerBound;
    }

    /**
     * Sets the lower bound .
     *
     * @param lowerBound value to set
     */
    public void setLowerBound(final int lowerBound) {
        this.lowerBound = lowerBound;
    }

    /**
     * Upper bound for (e.g.) randomization.
     *
     * @return The upper bound value.
     */
    public int getUpperBound() {
        return upperBound;
    }

    /**
     * Sets the upper bound value.
     *
     * @param upperBound Value to set
     */
    public void setUpperBound(final int upperBound) {
        this.upperBound = upperBound;
    }

    /**
     * Load a .csv file.
     *
     * @param file               the CSV file
     * @param allowRowChanges    whether to allow data with a different number of
     *                           rows
     * @param allowColumnChanges whether to allow data with a different number
     *                           of columns
     * @throws TableDataException
     */
    public void readData(final File file, final boolean allowRowChanges, final boolean allowColumnChanges) throws TableDataException {
        String[][] values = Utils.getStringMatrix(file);
        try {
            checkData(allowRowChanges, allowColumnChanges, values);
            reset(values.length, values[0].length);
            for (int i = 0; i < values.length; i++) {
                for (int j = 0; j < values[0].length; j++) {
                    if ((values[i][j]).length() > 0) {
                        setLogicalValue(i, j, Double.parseDouble(values[i][j]), false);
                    }
                }
            }
            fireTableStructureChanged();
        } catch (TableDataException tde) {
            throw tde;
        }
    }

    /**
     * Returns a double array representation of the table.
     *
     * @return representation of table as double array
     */
    public double[][] as2DDoubleArray() {
        double[][] returnList = new double[getRowCount()][getLogicalColumnCount()];
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = 0; j < getLogicalColumnCount(); j++) {
                returnList[i][j] = this.getLogicalValueAt(i, j);
            }
        }
        return returnList;
    }

    public float[][] as2DFloatArray() {
        float[][] returnList = new float[getRowCount()][getLogicalColumnCount()];
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = 0; j < getLogicalColumnCount(); j++) {
                returnList[i][j] = this.getLogicalValueAt(i, j).floatValue();
            }
        }
        return returnList;
    }

    /**
     * Fill the table at selected indices with the indicated value.
     *
     * @param cellIndices list of cell indices.
     * @param val         the value to fill
     */
    public void fill(List<CellIndex> cellIndices, double val) {
        for (CellIndex cellIndex : cellIndices) {
            int row = cellIndex.row;
            int col = cellIndex.col;
            this.setLogicalValue(row, col, val, false);
        }
        this.fireTableDataChanged();
    }

    /**
     * Randomize neurons within specified bounds.
     *
     * @param cellIndices list of cell indices.
     */
    public void randomize(List<CellIndex> cellIndices) {
        Random rand = new Random();
        int range = getUpperBound() - getLowerBound();
        for (CellIndex cellIndex : cellIndices) {
            int row = cellIndex.row;
            int col = cellIndex.col;
            double value;
            if (integerMode) {
                value = rand.nextInt(range) + lowerBound ;
            } else {
                value = (rand.nextDouble() * range) + getLowerBound();
            }
            setLogicalValue(row, col, value, false);
        }
        fireTableDataChanged();
    }

    /**
     * Normalize the whole table.
     */
    public void normalizeTable() {
        for (int i = 0; i < this.getLogicalColumnCount(); i++) {
            normalizeColumn(i);
        }
    }

    /**
     * Normalize data in selected column. TODO: Use bounds.
     *
     * @param columnIndex column to normalize.
     */
    public void normalizeColumn(final int columnIndex) {
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < this.getRowCount(); i++) {
            double val = getLogicalValueAt(i, columnIndex);
            if (val > max) {
                max = val;
            }
            if (val < min) {
                min = val;
            }
        }
        for (int i = 0; i < this.getRowCount(); i++) {
            setLogicalValue(i, columnIndex, (getLogicalValueAt(i, columnIndex) - min) / (max - min), false);
        }
        this.fireTableDataChanged();
    }

    public void setIntegerMode(boolean mode) {
        integerMode = mode;
    }
}
