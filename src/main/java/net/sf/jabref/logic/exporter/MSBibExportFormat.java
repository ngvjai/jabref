/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.exporter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.logic.msbib.MSBibDatabase;
import net.sf.jabref.model.entry.BibEntry;

/**
 * ExportFormat for exporting in MSBIB XML format.
 */
class MSBibExportFormat extends ExportFormat {

    public MSBibExportFormat() {
        super("MS Office 2007", "MSBib", null, null, ".xml");
    }

    @Override
    public void performExport(final BibDatabaseContext databaseContext, final String file,
            final Charset encoding, List<BibEntry> entries) throws SaveException {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(entries);

        if (entries.isEmpty()) {
            return;
        }
        // forcing to use UTF8 output format for some problems with xml export in other encodings
        SaveSession session = new FileSaveSession(StandardCharsets.UTF_8, false);
        MSBibDatabase msBibDatabase = new MSBibDatabase(databaseContext.getDatabase(), entries);

        try (VerifyingWriter ps = session.getWriter()) {
            try {
                DOMSource source = new DOMSource(msBibDatabase.getDOM());
                StreamResult result = new StreamResult(ps);
                Transformer trans = TransformerFactory.newInstance().newTransformer();
                trans.setOutputProperty(OutputKeys.INDENT, "yes");
                trans.transform(source, result);
            } catch (TransformerException | IllegalArgumentException | TransformerFactoryConfigurationError e) {
                throw new Error(e);
            }
            finalizeSaveSession(session, Paths.get(file));
        } catch (IOException ex) {
            throw new SaveException(ex);
        }
    }
}
