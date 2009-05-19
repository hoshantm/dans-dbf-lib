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

import org.junit.runner.RunWith;

import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * Tests reading FLOAT fields.
 *
 * @author Vesa Åkerman
 */
@RunWith(Parameterized.class)
public class TestFloat
    extends BaseTestcase
{
    /**
     * Creates a new TestFloat object.
     *
     * @param aVersion test parameter
     * @param aVersionDirectory test parameter
     */
    public TestFloat(final Version aVersion, final String aVersionDirectory)
    {
        super(aVersion, aVersionDirectory);
    }

    /**
     * {@inheritDoc}
     *
     * No dBase III+, because it has no FLOAT type.
     */
    @Parameters
    public static Collection<Object[]> data()
    {
        final Object[][] testParameters =
            new Object[][]
            {
                { Version.DBASE_4, "dbase4" },
                { Version.DBASE_5, "dbase5" },
                { Version.CLIPPER_5, "clipper5" }
            };

        return Arrays.asList(testParameters);
    }

    /**
     * Tests reading of DATE fields.
     *
     * @throws IOException not expected
     * @throws CorruptedTableException not expected
     */
    @Test
    public void readFloat()
                   throws IOException, CorruptedTableException
    {
        final Table t1 = new Table(new File("src/test/resources/" + versionDirectory + "/types/FLOAT.DBF"));

        try
        {
            t1.open(IfNonExistent.ERROR);

            final Iterator<Record> recordIterator = t1.recordIterator();

            Record r = recordIterator.next();

            assertEquals(1,
                         r.getNumberValue("FLOAT1").intValue());
            assertEquals(1234567890123450000L,
                         r.getNumberValue("FLOAT2").longValue());
            assertEquals(1.111111111111110000,
                         r.getNumberValue("FLOAT3").doubleValue(),
                         0.0);
            assertEquals(4.44444444,
                         r.getNumberValue("FLOAT4").doubleValue(),
                         0.0);

            r = recordIterator.next();
            assertEquals(9,
                         r.getNumberValue("FLOAT1").intValue());
            assertEquals(999999999999990000L,
                         r.getNumberValue("FLOAT2").longValue());
            assertEquals(9.999999999990000000,
                         r.getNumberValue("FLOAT3").doubleValue(),
                         0.0);
            assertEquals(9.99999999,
                         r.getNumberValue("FLOAT4").doubleValue(),
                         0.0);

            r = recordIterator.next();
            assertNull(r.getNumberValue("FLOAT1"));
            assertNull(r.getNumberValue("FLOAT2"));
            assertNull(r.getNumberValue("FLOAT3"));
            assertNull(r.getNumberValue("FLOAT4"));

            r = recordIterator.next();
            assertEquals(0,
                         r.getNumberValue("FLOAT1").intValue());

            // In Clipper5 maximum length 19 digits
            if (version == Version.CLIPPER_5)
            {
                assertEquals(1000000000000000000L,
                             r.getNumberValue("FLOAT2").longValue());
            }
            else
            {
                assertEquals(new BigInteger("10000000000000000000"),
                             (BigInteger) (r.getNumberValue("FLOAT2")));
            }

            assertEquals(5.555555555555560000,
                         r.getNumberValue("FLOAT3").doubleValue(),
                         0.0);
            assertEquals(0.00000000,
                         r.getNumberValue("FLOAT4").doubleValue(),
                         0.0);
        }
        finally
        {
            t1.close();
        }
    }

    /**
    * Tests writing float fields that are first read from a .dbf file.
    */
    @Test
    public void writeFloat()
                    throws IOException,
                           CorruptedTableException,
                           ValueTooLargeException,
                           InvalidFieldTypeException,
                           InvalidFieldLengthException
    {
        final Ranges ignoredRanges = new Ranges();
        ignoredRanges.addRange(0x01, 0x03); // modified
        ignoredRanges.addRange(0x1d, 0x1d); // language driver
        ignoredRanges.addRange(0x1e, 0x1f); // reserved
        ignoredRanges.addRange(0x2c, 0x2f); // field description "address in memory"
        ignoredRanges.addRange(0x4c, 0x4f); // field description "address in memory"
        ignoredRanges.addRange(0x6c, 0x6f); // field description "address in memory"
        ignoredRanges.addRange(0x8c, 0x8f); // field description "address in memory"
        ignoredRanges.addRange(0xac, 0xaf); // field description "address in memory"
        ignoredRanges.addRange(0x25, 0x2a); // garbage at the end of Field Name field
        ignoredRanges.addRange(0x47, 0x4a); // garbage at the end of Field Name field

        /*
         * Garbage in Clipper 5, in other versions not meaningful.
         */
        ignoredRanges.addRange(0x32, 0x3f); // garbage
        ignoredRanges.addRange(0x52, 0x5f); // garbage
        ignoredRanges.addRange(0x67, 0x6a); // garbage
        ignoredRanges.addRange(0x72, 0x7f); // garbage
        ignoredRanges.addRange(0x87, 0x8a); // garbage
        ignoredRanges.addRange(0x92, 0x9f); // garbage
        ignoredRanges.addRange(0xa7, 0xaa); // garbage
        ignoredRanges.addRange(0xb2, 0xbf); // garbage

        UnitTestUtil.doCopyAndCompareTest(versionDirectory + "/types", "FLOAT", version, ignoredRanges, null);
    }
}
