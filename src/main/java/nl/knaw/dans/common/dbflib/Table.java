/*
 * Copyright 2009-2010 Data Archiving and Networked Services (DANS), Netherlands.
 *
 * This file is part of DANS DBF Library.
 *
 * DANS DBF Library is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * DANS DBF Library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with DANS DBF Library. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package nl.knaw.dans.common.dbflib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Represents a single table in a xBase database. A table is represented by a single
 * <code>.DBF</code> file. Some tables have an associated .DBT file to store memo field data.
 *
 * @author Jan van Mansum
 * @author Vesa Åkerman
 */
public class Table
{
    private static final int MARKER_RECORD_DELETED = 0x2A;
    private static final int MARKER_EOF = 0x1A;
    private static final int MARKER_RECORD_VALID = 0x20;

    private class RecordIterator
        implements Iterator<Record>
    {
        private int recordCounter = 0;
        private Record lastReadRecord = null;

        public boolean hasNext()
        {
            checkOpen();

            if (lastReadRecord == null && recordCounter < header.getRecordCount())
            {
                try
                {
                    lastReadRecord = readRecord(recordCounter++);
                }
                catch (final IOException ioException)
                {
                    throw new RuntimeException(ioException.getMessage(), ioException);
                }
                catch (final CorruptedTableException corruptedTableException)
                {
                    throw new RuntimeException(corruptedTableException.getMessage(), corruptedTableException);
                }
            }

            return lastReadRecord != null;
        }

        public Record next()
        {
            if (hasNext())
            {
                final Record next = lastReadRecord;
                lastReadRecord = null;

                return next;
            }

            throw new NoSuchElementException();
        }

        public void remove()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private final File tableFile;
    private final DbfHeader header = new DbfHeader();
    private final String charsetName;
    private Memo memo = null;
    private RandomAccessFile raFile = null;

    /**
     * Creates a new <code>Table</code> object. A {@link File} object representing the
     * <code>.DBF</code> file must be provided. To read from or write to the table it must first be
     * opened.
     *
     * @param tableFile a <code>File</code> object representing the <code>.DBF</code> file that
     *            stores this table's data.
     * @param charsetName the charset to use for reading and writing this file
     *
     * @see #open(IfNonExistent)
     *
     * @throws IllegalArgumentException if <code>tableFile </code> is <code>null</code>
     */
    public Table(final File tableFile)
          throws IllegalArgumentException
    {
        this(tableFile,
             Charset.defaultCharset().name());
    }

    /**
     * Creates a new Table object. A {@link File} object representing the <code>.DBF</code> file
     * must be provided. To read from or write to the table it must first be opened.
     *
     * @param tableFile a <code>File</code> object representing the <code>.DBF</code> file that
     *            stores this table's data.
     * @param charsetName the charset to use for reading and writing this file
     *
     * @see #open(IfNonExistent)
     *
     * @throws IllegalArgumentException if <code>tableFile </code> is <code>null</code>
     */
    public Table(final File tableFile, final String charsetName)
          throws IllegalArgumentException
    {
        if (tableFile == null)
        {
            throw new IllegalArgumentException("Table file must not be null");
        }

        this.tableFile = tableFile;
        this.charsetName = charsetName == null ? Charset.defaultCharset().name() : charsetName;

        Charset.forName(this.charsetName);
    }

    /**
     * Creates a new Table object. In order to read from or write to the table it must first be
     * opened.
     * <p>
     * <b>Note:</b> if the <code>.DBF</code> file already exists <code>aFields</code> will be
     * overwritten by the values in the existing file when opened. To replace an existing table,
     * first delete it and then create and open a new <code>Table</code> object.
     *
     * @param tableFile the <code>.DBF</code> file that contains the table data
     * @param version the dBase version to support
     * @param fields the fields to create if this is a new table
     * @param charsetName the charset to use for reading and writing this file
     *
     * @see #open(IfNonExistent)
     *
     * @throws IllegalArgumentException if <tt>aTableFiel</tt> is <tt>null</tt>
     */
    public Table(final File tableFile, final Version version, final List<Field> fields, final String charsetName)
          throws InvalidFieldTypeException, InvalidFieldLengthException
    {
        this(tableFile, charsetName);
        header.setVersion(version);
        header.setHasMemo(hasMemo(fields));
        header.setFields(fields);
    }

    public Table(final File tableFile, final Version version, final List<Field> fields)
          throws InvalidFieldTypeException, InvalidFieldLengthException
    {
        this(tableFile, version, fields,
             Charset.defaultCharset().name());
    }

    private static boolean hasMemo(final List<Field> fields)
    {
        for (final Field field : fields)
        {
            if (field.getType() == Type.MEMO)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Opens the table for reading and writing. Equivalent to {@link Table#open(IfNonExistent)
     * Table.open(IfNonExistent.ERROR)}
     *
     * @throws IOException if the table file does not exist or could not be opened
     * @throws CorruptedTableException if the header of the table file was corrupt
     */
    public void open()
              throws IOException, CorruptedTableException
    {
        open(IfNonExistent.ERROR);
    }

    /**
     * Opens the table for reading and writing.
     *
     * @param ifNonExistent what to do if the table file does not exist yet
     *
     * @throws IOException if the table does not exist or could be opened
     * @throws CorruptedTableException if the header of the table file was corrupt
     */
    public void open(final IfNonExistent ifNonExistent)
              throws IOException, CorruptedTableException
    {
        if (tableFile.exists())
        {
            raFile = new RandomAccessFile(tableFile, "rw");
            header.readAll(raFile);
        }
        else if (ifNonExistent.isCreate())
        {
            raFile = new RandomAccessFile(tableFile, "rw");
            header.writeAll(raFile);
        }
        else if (ifNonExistent.isError())
        {
            throw new FileNotFoundException("Input file " + tableFile + " not found");
        }
    }

    /**
     * Closes this table for reading and writing.
     *
     * @throws java.io.IOException if the table file or an associated file cannot be closed
     */
    public void close()
               throws IOException
    {
        try
        {
            if (raFile != null)
            {
                raFile.close();
            }
        }
        finally
        {
            raFile = null;
            ensureMemoClosed();
        }
    }

    /**
     * Closes and deletes the underlying table file and associated files.
     *
     * @throws IOException if the table file or an associated file cannot be closed or deleted
     */
    public void delete()
                throws IOException
    {
        close();
        tableFile.delete();

        if (memo != null)
        {
            memo.delete();
        }
    }

    /**
     * Returns the date on which this table was last modified. Note that the hours, minutes, seconds
     * and milliseconds fields are always set to zero. Also, the date time is not normalized to UTC.
     *
     * @return the last modified date of the table
     */
    public Date getLastModifiedDate()
    {
        checkOpen();

        return header.getLastModifiedDate();
    }

    /**
     * Returns the name of the table, including the extension.
     *
     * @return the name of the table
     */
    public String getName()
    {
        return tableFile.getName();
    }

    /**
     * Returns a {@link List} of {@link Field} objects, which provide a description of each field
     * (column) in the table. The order of the <code>Field</code> objects is guaranteed to be the
     * same as the order of the fields in each record returned. A new copy of the field list is
     * returned on each call.
     *
     * @return the list of field objects.
     */
    public List<Field> getFields()
    {
        checkOpen();

        return header.getFields();
    }

    /**
     * Returns a {@link Record} iterator. Note that, to use the iterator the table must be opened.
     *
     * @return a <code>Record</code> iterator
     *
     * @see Record
     */
    public Iterator<Record> recordIterator()
    {
        return new RecordIterator();
    }

    /**
     * Constructs and adds a record. The fields values for the record must be provided as parameters
     * in the same order that the fields are provided in the field list.
     *
     * @throws IOException if the record could not be written to the database file
     * @throws CorruptedTableException if the table was corrupt
     * @throws ValueTooLargeException if a field value exceeds the length of its corresponding field
     * @throws RecordTooLargeException if more field values are provided than there are field in
     *             this table
     */
    public void addRecord(final Object... fieldValues)
                   throws IOException, DbfLibException
    {
        if (fieldValues.length > header.getFields().size())
        {
            throw new RecordTooLargeException("Trying to add " + fieldValues.length + " fields while there are only "
                                              + header.getFields().size() + " defined in the table file");
        }

        final Map<String, Value> map = new HashMap<String, Value>();
        final Iterator<Field> fieldIterator = header.getFields().iterator();

        for (final Object fieldValue : fieldValues)
        {
            final Field field = fieldIterator.next();

            map.put(field.getName(),
                    createValueObject(fieldValue));
        }

        addRecord(new Record(map));
    }

    private Value createValueObject(final Object value)
    {
        if (value instanceof Number)
        {
            return new NumberValue((Number) value);
        }
        else if (value instanceof String)
        {
            return new StringValue((String) value, charsetName);
        }
        else if (value instanceof Boolean)
        {
            return new BooleanValue((Boolean) value);
        }
        else if (value instanceof Date)
        {
            return new DateValue((Date) value);
        }
        else if (value instanceof byte[])
        {
            return new ByteArrayValue((byte[]) value);
        }

        return null;
    }

    /**
     * Adds a record to this table.
     *
     * @param record the record to add.
     *
     * @throws IOException if the record could not be written to the database file
     * @throws CorruptedTableException if the table was corrupt
     * @throws ValueTooLargeException if a field value exceeds the length of its corresponding field
     *
     * @see Record
     */
    public void addRecord(final Record record)
                   throws IOException, DbfLibException
    {
        checkOpen();

        raFile.seek(header.getLength() + (header.getRecordCount() * header.getRecordLength()));
        raFile.writeByte(MARKER_RECORD_VALID);

        for (final Field field : header.getFields())
        {
            byte[] raw = record.getRawValue(field);

            if (raw == null)
            {
                raw = Util.repeat((byte) ' ',
                                  field.getLength());
            }
            else if (field.getType() == Type.MEMO || field.getType() == Type.BINARY || field.getType() == Type.GENERAL)
            {
                final int index = writeMemo(raw);

                if (header.getVersion() == Version.DBASE_4 || header.getVersion() == Version.DBASE_5)
                {
                    raw = String.format("%0" + field.getLength() + "d", index).getBytes();
                }
                else
                {
                    raw = String.format("%" + field.getLength() + "d", index).getBytes();
                }
            }

            raFile.write(raw);
        }

        raFile.writeByte(MARKER_EOF);
        writeRecordCount(header.getRecordCount() + 1);
    }

    private int writeMemo(final byte[] memoText)
                   throws IOException, CorruptedTableException
    {
        ensureMemoOpened(IfNonExistent.CREATE);

        return memo.writeMemo(memoText);
    }

    private void writeRecordCount(final int recordCount)
                           throws IOException
    {
        raFile.seek(DbfHeader.OFFSET_RECORD_COUNT);
        header.setRecordCount(recordCount);
        header.writeRecordCount(raFile);
    }

    private void checkOpen()
    {
        if (raFile == null)
        {
            throw new IllegalStateException("Table should be open for this operation");
        }
    }

    private byte[] readMemo(final String memoIndex)
                     throws IOException, CorruptedTableException
    {
        ensureMemoOpened(IfNonExistent.ERROR);

        if (memoIndex.trim().isEmpty())
        {
            return null;
        }

        return memo.readMemo(Integer.parseInt(memoIndex.trim()));
    }

    private void ensureMemoOpened(final IfNonExistent ifNonExistent)
                           throws IOException, CorruptedTableException
    {
        if (memo != null)
        {
            return;
        }

        openMemo(ifNonExistent);
    }

    private void ensureMemoClosed()
                           throws IOException
    {
        if (memo != null)
        {
            try
            {
                memo.close();
            }
            finally
            {
                memo = null;
            }
        }
    }

    /**
     * Opens the memo of this table.
     *
     * @param ifNonExistent what to do if the memo file does not exist. (Cannot be IGNORE.)
     * @throws IOException if the memo file could not be opened
     * @throws CorruptedTableException if the memo file could not be found or multiple matches
     *             exist, or if it is corrupt
     */
    private void openMemo(final IfNonExistent ifNonExistent)
                   throws IOException, CorruptedTableException
    {
        File memoFile = Util.getMemoFile(tableFile,
                                         header.getVersion());

        if (memoFile == null)
        {
            final String extension = (header.getVersion() == Version.FOXPRO_26 ? ".fpt" : ".dbt");

            if (ifNonExistent.isError())
            {
                throw new CorruptedTableException("Could not find file '" + Util.stripExtension(tableFile.getPath())
                                                  + extension + "' (or multiple matches for the file)");
            }
            else if (ifNonExistent.isCreate())
            {
                final String tableFilePath = tableFile.getPath();
                memoFile = new File(tableFilePath.substring(0, tableFilePath.length() - ".dbf".length()) + extension);
            }
            else
            {
                assert false : "Programming error: cannot ignore non existing memo.";
            }
        }

        memo =
            new Memo(memoFile,
                     header.getVersion());
        memo.open(ifNonExistent);
    }

    private Record readRecord(final int index)
                       throws IOException, CorruptedTableException
    {
        raFile.seek(header.getLength() + (index * header.getRecordLength()));

        final byte firstByteOfRecord = raFile.readByte();

        while (firstByteOfRecord == MARKER_RECORD_DELETED)
        {
            raFile.skipBytes(header.getRecordLength() - 1);
        }

        if (firstByteOfRecord == MARKER_EOF)
        {
            return null;
        }

        final Map<String, Value> recordValues = new HashMap<String, Value>();

        for (final Field field : header.getFields())
        {
            final byte[] rawData = Util.readStringBytes(raFile,
                                                        field.getLength());

            switch (field.getType())
            {
                case NUMBER:
                case FLOAT:
                    recordValues.put(field.getName(),
                                     new NumberValue(field, rawData));

                    break;

                case CHARACTER:
                    recordValues.put(field.getName(),
                                     new StringValue(field, rawData, charsetName));

                    break;

                case LOGICAL:
                    recordValues.put(field.getName(),
                                     new BooleanValue(field, rawData));

                    break;

                case DATE:
                    recordValues.put(field.getName(),
                                     new DateValue(field, rawData));

                    break;

                case MEMO:

                    final byte[] memoTextBytes = readMemo(new String(rawData));
                    recordValues.put(field.getName(),
                                     memoTextBytes == null ? null : new StringValue(field, memoTextBytes, charsetName));

                    break;

                case GENERAL:
                case BINARY:
                case PICTURE:
                    recordValues.put(field.getName(),
                                     new ByteArrayValue(readMemo(new String(rawData))));

                    break;

                default:
                    assert false : "Programming error: not all data types handled.";

                    return null;
            }
        }

        return new Record(recordValues);
    }

    /**
     * Returns the name of the character set used to read and write from/to this table file.
     *
     * @return a charset name
     */
    public String getCharsetName()
    {
        return charsetName;
    }

    /**
     * Returns the version of DBF use to write to the table file. For existing files, some detection
     * is attempted by the library, but it should not be relied on to heavily.
     *
     * @return the version of DBF
     */
    public Version getVersion()
    {
        return header.getVersion();
    }
}
