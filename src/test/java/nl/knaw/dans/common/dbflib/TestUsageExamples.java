/*
 *  Copyright 2009
 *  Data Archiving and Networked Services (DANS), Netherlands.
 *
 *  This file is part of DANS DBF Library.
 *
 *  DANS DBF Library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  DANS DBF Library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with DANS DBF Library.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.knaw.dans.common.dbflib;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Tests that the code used in the Usage page runs without throwing exceptions.
 *
 * @author Jan van Mansum
 */
public class TestUsageExamples
{
    private class DummyOutputStream
        extends OutputStream
    {
        @Override
        public void write(int b)
                   throws IOException
        {
        }
    }

    private static final File srcDir = new File("src/test/resources/dbase3plus/usage");
    private static final File outDir = new File("target/test-output/dbase3plus/usage");
    private static final String fileNameDbf = "MYTABLE.DBF";
    private static final String fileNameDbt = "MYTABLE.DBT";

    /**
     * DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     * @throws CorruptedTableException DOCUMENT ME!
     * @throws ValueTooLargeException DOCUMENT ME!
     */
    @Test
    public void test()
              throws IOException, CorruptedTableException, ValueTooLargeException, RecordTooLargeException
    {
        PrintStream out = System.out;

        try
        {
            /*
             * Sending the standard output to a black hole, because we do want the code to be easily
             * copy-pastable to the Usage page, but we don't want to see the output in this test.
             */
            System.setOut(new PrintStream(new DummyOutputStream()));

            UnitTestUtil.copyFile(new File(srcDir, fileNameDbf),
                                  outDir,
                                  fileNameDbf);
            UnitTestUtil.copyFile(new File(srcDir, fileNameDbt),
                                  outDir,
                                  fileNameDbt);

            Table table = new Table(new File(outDir, fileNameDbf));

            try
            {
                table.open(IfNonExistent.ERROR);

                List<Field> fields = table.getFields();

                for (final Field field : fields)
                {
                    System.out.println("Name:         " + field.getName());
                    System.out.println("Type:         " + field.getType());
                    System.out.println("Length:       " + field.getLength());
                    System.out.println("DecimalCount: " + field.getDecimalCount());
                    System.out.println();
                }

                Iterator<Record> iterator = table.recordIterator();

                while (iterator.hasNext())
                {
                    Record record = iterator.next();

                    Number nv = record.getNumberValue("NUMFLD");

                    // Convert to a primitive before comparing
                    if (nv != null && nv.intValue() == 2)
                    {
                        System.out.println("Well, what do you know? numfld was 2!");
                    }

                    // Get all the bytes from the memo field including
                    // "soft returns" (0x8d 0x0a)
                    byte[] memoData = record.getRawValue(new Field("MEMOFLD", Type.MEMO));

                    if (memoData == null)
                    {
                        System.out.println("No memo data");
                    }
                    else
                    {
                        System.out.println("Raw memo data: " + new String(memoData));
                    }

                    // Get the memo data as a String, with no soft returns.
                    String memoString = record.getStringValue("MEMOFLD");

                    System.out.println("Memo date as string: " + memoString);
                }

                // The hard way ...
                Map map = new HashMap<String, Value>();
                map.put("NUMFLD",
                        new NumberValue(17));
                map.put("LOGICFLD",
                        new BooleanValue(true));
                map.put("CHARFLD",
                        new StringValue("This is a new string"));
                map.put("MEMOFLD",
                        new StringValue("This could be a very long string"));
                map.put("DATEFLD",
                        new DateValue(Calendar.getInstance().getTime()));

                Record record = new Record(map);
                table.addRecord(record);

                // The easy way ...
                // The values have to be of the appropriate type and in the same order as the corresponding fields
                // in the list returned by Table.getFields();
                table.addRecord(18,
                                false,
                                "Another new string",
                                "Another long string",
                                Calendar.getInstance().getTime());

                // ... do your stuff
            }
            finally
            {
                table.close(); // don't forget to close it!
            }

            assertTrue(true);
        }
        finally
        {
            System.setOut(out);
        }
    }
}