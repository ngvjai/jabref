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
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.logic.importer.FulltextFetcher;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class for finding PDF URLs for entries on IEEE
 * Will first look for URLs of the type http://ieeexplore.ieee.org/stamp/stamp.jsp?[tp=&]arnumber=...
 * If not found, will resolve the DOI, if it starts with 10.1109, and try to find a similar link on the HTML page
 */
public class IEEE implements FulltextFetcher {

    private static final Log LOGGER = LogFactory.getLog(IEEE.class);
    private static final Pattern STAMP_PATTERN = Pattern.compile("(/stamp/stamp.jsp\\?t?p?=?&?arnumber=[0-9]+)");
    private static final Pattern PDF_PATTERN = Pattern
            .compile("\"(http://ieeexplore.ieee.org/ielx[0-9/]+\\.pdf[^\"]+)\"");
    private static final String IEEE_DOI = "10.1109";
    private static final String BASE_URL = "http://ieeexplore.ieee.org";


    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        String stampString = "";
        // Try URL first -- will primarily work for entries from the old IEEE search
        Optional<String> urlString = entry.getFieldOptional(FieldName.URL);
        if (urlString.isPresent()) {
            // Is the URL a direct link to IEEE?
            Matcher matcher = STAMP_PATTERN.matcher(urlString.get());
            if (matcher.find()) {
                // Found it
                stampString = matcher.group(1);
            }
        }

        // If not, try DOI
        if (stampString.isEmpty()) {
            Optional<DOI> doi = entry.getFieldOptional(FieldName.DOI).flatMap(DOI::build);
            if (doi.isPresent() && doi.get().getDOI().startsWith(IEEE_DOI) && doi.get().getURI().isPresent()) {
                // Download the HTML page from IEEE
                String resolvedDOIPage = new URLDownload(doi.get().getURI().get().toURL())
                        .downloadToString(StandardCharsets.UTF_8);
                // Try to find the link
                Matcher matcher = STAMP_PATTERN.matcher(resolvedDOIPage);
                if (matcher.find()) {
                    // Found it
                    stampString = matcher.group(1);
                }
            }
        }

        // Any success?
        if (stampString.isEmpty()) {
            return Optional.empty();
        }

        // Download the HTML page containing a frame with the PDF
        String framePage = new URLDownload(BASE_URL + stampString).downloadToString(StandardCharsets.UTF_8);
        // Try to find the direct PDF link
        Matcher matcher = PDF_PATTERN.matcher(framePage);
        if (matcher.find()) {
            // The PDF was found
            LOGGER.debug("Full text document found on IEEE Xplore");
            return Optional.of(new URL(matcher.group(1)));
        }
        return Optional.empty();
    }

}
