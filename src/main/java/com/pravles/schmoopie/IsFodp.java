/*
 * Copyright 2026 Pravles Redneckoff
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pravles.schmoopie;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.XMLConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import java.util.function.Function;

public class IsFodp implements Function<String, Boolean> {
    private static final String OFFICE_NS =
            "urn:oasis:names:tc:opendocument:xmlns:office:1.0";
    private static final String IMPRESS_MIMETYPE =
            "application/vnd.oasis.opendocument.presentation";

    @Override
    public Boolean apply(final String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            // Harden against XXE / external entity attacks
            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(
                    XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(
                    new InputSource(new StringReader(data)));

            Element root = doc.getDocumentElement();
            if (root == null) {
                return false;
            }

            // Root must be office:document in the office namespace
            if (!"document".equals(root.getLocalName())) {
                return false;
            }
            if (!OFFICE_NS.equals(root.getNamespaceURI())) {
                return false;
            }

            String mimetype = root.getAttributeNS(OFFICE_NS, "mimetype");
            return IMPRESS_MIMETYPE.equals(mimetype);

        } catch (Exception e) {
            // Malformed XML, IO error, parser config issue, etc.
            return false;
        }
    }
}
