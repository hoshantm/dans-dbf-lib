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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Does roundtrip tests of the library, i.e. touches all the functionality, but does not
 * go very deep.
 *
 * @author Jan van Mansum
 * @author Vesa Akerman
 */
public class TestRoundTrip
{
    /**
     * A short roundtrip of the library.  Covers:
     * <ul>
     * <li>Opening a database</li>
     * <li>Retrieving the table names</li>
     * <li>Checking last modified date</li>
     * <li>Retrieving fields of one table</li>
     * <li>Retrieving records of one table (TODO)</li>
     * <li>Adding a record to one table (TODO)</li>
     * </ul>
     */
    @Test
    public void shortRoundTrip()
                        throws FileNotFoundException, IOException, CorruptedTableException
    {
        final Database database = new Database(new File("src/test/resources/dbase3plus/rndtrip"));
        final Set<String> tableNames = database.getTableNames();

        assertEquals(2,
                     tableNames.size());
        assertTrue("TABLE1 not found in 'short roundtrip' database",
                   tableNames.contains("TABLE1.DBF"));
        assertTrue("TABLE2 not found in 'short roundtrip' database",
                   tableNames.contains("TABLE2.DBF"));

        final Table t1 = database.getTable("TABLE1.DBF");

        try
        {
            t1.open(false);

            assertEquals("Table name incorrect",
                         "TABLE1.DBF",
                         t1.getName());
            assertEquals("Last modified date incorrect",
                         UnitTestUtil.createDate(2009, Calendar.APRIL, 1),
                         t1.getLastUpdated());

            final List<Field> fields = t1.getFields();

            Iterator<Field> fieldIterator = fields.iterator();
            Map<String, Field> nameFieldMap = new HashMap<String, Field>();

            while (fieldIterator.hasNext())
            {
                Field f = fieldIterator.next();
                nameFieldMap.put(f.getName(),
                                 f);
            }

            final Field idField = nameFieldMap.get("ID");
            assertEquals(Type.NUMBER,
                         idField.getType());
            assertEquals(idField.getLength(),
                         3);

            final Field stringField = nameFieldMap.get("STRFIELD");
            assertEquals(Type.CHARACTER,
                         stringField.getType());
            assertEquals(stringField.getLength(),
                         50);

            final Field logicField = nameFieldMap.get("LOGICFIELD");
            assertEquals(Type.LOGICAL,
                         logicField.getType());
            assertEquals(logicField.getLength(),
                         1);

            final Field dateField = nameFieldMap.get("DATEFIELD");
            assertEquals(Type.DATE,
                         dateField.getType());
            assertEquals(8,
                         dateField.getLength());

            final Field floatField = nameFieldMap.get("FLOATFIELD");
            assertEquals(Type.NUMBER,
                         floatField.getType());
            assertEquals(10,
                         floatField.getLength());

            final List<Record> records = UnitTestUtil.createSortedRecordList(t1.recordIterator(),
                                                                             "ID");
            final Record r0 = records.get(0);

            assertEquals(1.0,
                         r0.getValue("ID"));
            assertEquals("String data 01",
                         r0.getValue("STRFIELD").toString().trim());
            assertEquals(true,
                         r0.getValue("LOGICFIELD"));
            assertEquals(UnitTestUtil.createDate(1909, Calendar.MARCH, 18),
                         r0.getValue("DATEFIELD"));
            assertEquals(1234.56,
                         r0.getValue("FLOATFIELD"));

            final Record r1 = records.get(1);

            assertEquals(2.0,
                         r1.getValue("ID"));
            assertEquals("String data 02",
                         r1.getValue("STRFIELD").toString().trim());
            assertEquals(false,
                         r1.getValue("LOGICFIELD"));
            assertEquals(UnitTestUtil.createDate(1909, Calendar.MARCH, 20),
                         r1.getValue("DATEFIELD"));
            assertEquals(-23.45,
                         r1.getValue("FLOATFIELD"));

            final Record r2 = records.get(2);

            assertEquals(3.0,
                         r2.getValue("ID"));
            assertEquals("",
                         r2.getValue("STRFIELD").toString().trim());
            assertEquals(null,
                         r2.getValue("LOGICFIELD"));
            assertEquals(null,
                         r2.getValue("DATEFIELD"));
            assertEquals(null,
                         r2.getValue("FLOATFIELD"));

            final Record r3 = records.get(3);

            assertEquals(4.0,
                         r3.getValue("ID"));
            assertEquals("Full5678901234567890123456789012345678901234567890",
                         r3.getValue("STRFIELD").toString().trim());
            assertEquals(false,
                         r3.getValue("LOGICFIELD"));
            assertEquals(UnitTestUtil.createDate(1909, Calendar.MARCH, 20),
                         r3.getValue("DATEFIELD"));
            assertEquals(-0.30,
                         r3.getValue("FLOATFIELD"));
        }
        finally
        {
            t1.close();
        }

        final Table t2 = database.getTable("TABLE2.DBF");

        try
        {
            t2.open(false);

            final List<Field> fields = t2.getFields();

            Iterator<Field> fieldIterator = fields.iterator();
            Map<String, Field> nameFieldMap = new HashMap<String, Field>();

            while (fieldIterator.hasNext())
            {
                Field f = fieldIterator.next();
                nameFieldMap.put(f.getName(),
                                 f);
            }

            final Field idField = nameFieldMap.get("ID2");
            assertEquals(Type.NUMBER,
                         idField.getType());
            assertEquals(idField.getLength(),
                         4);

            final Field stringField = nameFieldMap.get("MEMOFIELD");
            assertEquals(Type.MEMO,
                         stringField.getType());
            assertEquals(10,
                         stringField.getLength());

            final Iterator<Record> recordIterator = t2.recordIterator();
            final Record r = recordIterator.next();

            String declarationOfIndependence = "";

            declarationOfIndependence += "When in the Course of human events it becomes necessary for one people ";
            declarationOfIndependence += "to dissolve the political bands which have connected them with another and ";
            declarationOfIndependence += "to assume among the powers of the earth, the separate and equal station to ";
            declarationOfIndependence += "which the Laws of Nature and of Nature's God entitle them, a decent respect ";
            declarationOfIndependence += "to the opinions of mankind requires that they should declare the causes which ";
            declarationOfIndependence += "impel them to the separation.";
            declarationOfIndependence += "\r\n\r\n";
            declarationOfIndependence += "We hold these truths to be self-evident, that all men are created equal, ";
            declarationOfIndependence += "that they are endowed by their Creator with certain unalienable Rights, ";
            declarationOfIndependence += "that among these are Life, Liberty and the persuit of Happiness.";

            assertEquals(1.0,
                         r.getValue("ID2"));
            assertEquals(declarationOfIndependence,
                         r.getValue("MEMOFIELD"));
        }
        finally
        {
            t2.close();
        }
    }
}
