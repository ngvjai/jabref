/*
 * Copyright (C) 2003-2016 JabRef contributors.
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
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.support.DevEnvironment;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class ScienceDirectTest {

    private ScienceDirect finder;
    private BibEntry entry;

    @Before
    public void setUp() {
        finder = new ScienceDirect();
        entry = new BibEntry();
    }

    @Test(expected = NullPointerException.class)
    public void rejectNullParameter() throws IOException {
        finder.findFullText(null);
        Assert.fail();
    }

    @Test
    public void doiNotPresent() throws IOException {
        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    public void findByDOI() throws IOException {
        // CI server is blocked
        Assume.assumeFalse(DevEnvironment.isCIServer());

        entry.setField("doi", "10.1016/j.jrmge.2015.08.004");

        Assert.assertEquals(
                Optional.of(new URL("http://www.sciencedirect.com/science/article/pii/S1674775515001079/pdfft?md5=2b19b19a387cffbae237ca6a987279df&pid=1-s2.0-S1674775515001079-main.pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    public void notFoundByDOI() throws IOException {
        // CI server is blocked
        Assume.assumeFalse(DevEnvironment.isCIServer());

        entry.setField("doi", "10.1016/j.aasri.2014.0559.002");

        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }
}
