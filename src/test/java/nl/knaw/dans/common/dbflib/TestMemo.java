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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

/**
 * Tests reading and writing memo fields.
 *
 * @author Vesa Åkerman
 */
@RunWith(Parameterized.class)
public class TestMemo
    extends BaseTestcase
{
    /**
     * Creates a new TestMemo object.
     *
     * @param aVersion test parameter
     * @param aVersionDirectory test parameter
     */
    public TestMemo(final Version aVersion, final String aVersionDirectory)
    {
        super(aVersion, aVersionDirectory);
    }

    /**
    * Tests reading memo fields.
    */
    @Test
    public void readMemo()
                  throws FileNotFoundException, IOException, CorruptedTableException
    {
        final Table t1 = new Table(new File("src/test/resources/" + versionDirectory + "/types/MEMOTEST.DBF"));

        try
        {
            t1.open(IfNonExistent.ERROR);

            final Iterator<Record> recordIterator = t1.recordIterator();

            Record r = recordIterator.next();
            assertEquals("m",
                         r.getStringValue("MEMO"));

            r = recordIterator.next();
            assertEquals(null,
                         r.getStringValue("MEMO"));

            r = recordIterator.next();
            assertEquals("This is a very long memo",
                         r.getStringValue("MEMO").substring(0, 24));

            r = recordIterator.next();

            assertEquals("1234567890",
                         r.getStringValue("MEMO").substring(0, 10));
        }
        finally
        {
            t1.close();
        }
    }

    /**
    * Tests writing memo fields.
    */
    @Test
    public void writeMemo()
                   throws IOException, CorruptedTableException, ValueTooLargeException
    {
        final Ranges ignoredRangesDbf = new Ranges();
        ignoredRangesDbf.addRange(0x01, 0x03); // modified
        ignoredRangesDbf.addRange(0x1d, 0x1d); // language driver
        ignoredRangesDbf.addRange(0x1e, 0x1f); // ???
        ignoredRangesDbf.addRange(0x2c, 0x2f); // field description "address in memory"
        ignoredRangesDbf.addRange(0x34, 0x34); // work area id
        ignoredRangesDbf.addRange(0x4c, 0x4f); // field description "address in memory"
        ignoredRangesDbf.addRange(0x54, 0x54); // work area id

        final Ranges ignoredRangesDbt = new Ranges();

        if (version == Version.DBASE_3)
        {
            ignoredRangesDbt.addRange(0x04, 0x1ff); // reserved/garbage
        }
        else if (version == Version.DBASE_4)
        {
            ignoredRangesDbt.addRange(0xcbf, 0xdff); // end of the block garbage
            ignoredRangesDbt.addRange(0x1005, 0x11ff); // end of the block garbage
        }
        else if (version == Version.DBASE_5)
        {
            ignoredRangesDbt.addRange(0x16, 0x1ff); // reserved/garbage
        }

        UnitTestUtil.doCopyAndCompareTest(versionDirectory + "/types", "MEMOTEST", version, ignoredRangesDbf,
                                          ignoredRangesDbt);
    }
}
