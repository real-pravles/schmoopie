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

import clojure.lang.Keyword;
import com.pravles.processengine.impl.ShariysDog;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;
import static javax.xml.xpath.XPathConstants.NODESET;
import static org.apache.commons.lang3.StringUtils.trim;

public class Fodp2Org implements BiFunction<String, PrintStream, String> {
   private static final Keyword ADD = Keyword.intern("add");
   private static final Keyword TODO = Keyword.intern("todo");

    private static final Keyword WITH_TITLE = Keyword.intern("with-title");

    private static final Keyword WITH_BODY = Keyword.intern("with-body");

    @Override
    public String apply(final String fodp,
                        final PrintStream err) {
        try {
            final DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final ByteArrayInputStream is = new ByteArrayInputStream(fodp.getBytes(StandardCharsets.UTF_8));

            final Document doc = builder.parse(is);

            doc.getDocumentElement().normalize();

            final List<Slide> slides = extractSlides(doc);

            final Map<String, Slide> slidesByName = slides.stream()
                    .collect(Collectors.toMap(Slide::getName, Function.identity()));

            final Slide rootSlide = slidesByName.get("/");

            if (rootSlide == null) {
                err.println("schmoopie: No root slide");
                return "";
            }

            final StringBuilder sb = new StringBuilder();

            final Queue<Slide> slidesToProcess =
                    new ArrayDeque<>(slides.size());
            slidesToProcess.offer(rootSlide);

            final Set<String> processedSlideNames = new HashSet<>();

            boolean slidesProcessingInProgress = true;

            while (slidesProcessingInProgress) {
                final Slide slide = slidesToProcess.poll();
                processedSlideNames.add(trim(slide.getName()));

                processDescriptiosn(sb, slide);

                sb.append("** TODO ~");
                sb.append(slide.getName());
                sb.append("~: ");
                sb.append("Evaluate the state of control process \"");
                sb.append(slide.getTitle());
                sb.append("\"");
                sb.append(lineSeparator());

                if ("/".equals(slide.getName())) {
                    sb.append("<<n>>");
                    sb.append(lineSeparator());
                }

                sb.append(lineSeparator());

                slide.getLinkedPageNames()
                        .stream()
                        .filter(name -> !processedSlideNames.contains(trim(name)))
                        .distinct()
                        .map(name -> slidesByName.get(name))
                        .filter(s -> s != null)
                        .forEach(s -> slidesToProcess.offer(s));

                slidesProcessingInProgress = !slidesToProcess.isEmpty();
            }
            return sb.toString();
        } catch (final ParserConfigurationException
                | IOException
                | SAXException
                | XPathExpressionException e) {
            e.printStackTrace(err);
            return "";
        }
    }

    private void processDescriptiosn(final StringBuilder sb,
                                     final Slide slide) {
        final String descBasedActionItems = slide.getDescriptions()
                .stream()
                .map(descTxt -> (Map) ShariysDog.woof("txt-to-map", descTxt))
                .map(map -> descriptionMapToTxt(map))
                .collect(Collectors.joining(lineSeparator()));
        sb.append(descBasedActionItems);
    }

    private String descriptionMapToTxt(final Map descMap) {
        final Object type = descMap.get(ADD);
        if (!TODO.equals(type)) {
            return "";
        }

        final String title = (String) descMap.get(WITH_TITLE);
        final List<String> body = (List<String>) descMap.getOrDefault(WITH_BODY, Collections.emptyList());
        final String bodyTxt = body.stream()
                .collect(Collectors.joining(lineSeparator()));

        return String.format("** TODO %s%s<<n>>%s%s%s%s%s",
                title,
                lineSeparator(),
                lineSeparator(),
                lineSeparator(),
                bodyTxt,
                lineSeparator(),
                lineSeparator());
    }

    private List<Slide> extractSlides(final Document doc) throws XPathExpressionException {
        final XPathFactory xPathFactory = XPathFactory.newInstance();
        final XPath xPath = xPathFactory.newXPath();
        final XPathExpression pagesExpr = xPath.compile("//*[local-name()='page']");
        final XPathExpression titleExpr = xPath.compile(
                ".//*[local-name()='frame' and @*[local-name()='class']='title']" +
                        "//*[local-name()='p']");
        final XPathExpression descExpr = xPath.compile(".//*[local-name()='desc']");

        final NodeList nodeList = (NodeList) pagesExpr.evaluate(doc,
                NODESET);
        final ArrayList<Slide> slides =
                new ArrayList<>(nodeList.getLength());

        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            final NamedNodeMap nnm = node.getAttributes();
            final String name = nnm.getNamedItem("draw:name").getNodeValue();

            slides.add(Slide.builder()
                            .name(name)
                            .title(extractTitle(titleExpr, node))
                            .linkedPageNames(extractLinkedPages(node))
                            .descriptions(extractDescriptions(descExpr, node))
                    .build());
        }
        return slides;
    }

    private List<String> extractDescriptions(XPathExpression descExpr, Node node)
            throws XPathExpressionException {
        final NodeList descs = (NodeList) descExpr.evaluate(node, XPathConstants.NODESET);
        final List<String> result = new ArrayList<>(descs.getLength());
        for (int i = 0; i < descs.getLength(); i++) {
            result.add(descs.item(i).getTextContent());
        }
        return result;
    }

    private static List<String> extractLinkedPages(Node node) throws XPathExpressionException {
        final XPath xp = XPathFactory.newInstance().newXPath();
        // Find all <draw:g> groups under this page that contain an event-listener with xlink:href
        final XPathExpression linkedGroupsExpr = xp.compile(
                ".//*[local-name()='g'][.//*[local-name()='event-listener']" +
                        "/@*[local-name()='href']]");
        // Within a group, find the href value
        final XPathExpression hrefExpr = xp.compile(
                ".//*[local-name()='event-listener']/@*[local-name()='href']");
        // Within a group, find all svg:x attribute values on descendant shapes
        final XPathExpression xAttrsExpr = xp.compile(
                ".//@*[local-name()='x']");

        final NodeList groups = (NodeList) linkedGroupsExpr.evaluate(node, NODESET);

        // Pair (leftmostX, href) so we can sort by X
        final List<double[]> indices = new ArrayList<>(); // [leftmostXcm, originalIndex]
        final List<String> hrefs = new ArrayList<>();

        for (int g = 0; g < groups.getLength(); g++) {
            final Node group = groups.item(g);

            final String href = (String) hrefExpr.evaluate(group, javax.xml.xpath.XPathConstants.STRING);
            final String name1 = href.startsWith("#") ? href.substring(1) : href;

            final NodeList xs = (NodeList) xAttrsExpr.evaluate(group, NODESET);
            double leftmost = Double.POSITIVE_INFINITY;
            for (int k = 0; k < xs.getLength(); k++) {
                final String raw = xs.item(k).getNodeValue();            // e.g. "15.076cm"
                final String numeric = raw.replaceAll("[^0-9.\\-]", ""); // strip "cm"
                if (!numeric.isEmpty()) {
                    final double v = Double.parseDouble(numeric);
                    if (v < leftmost) leftmost = v;
                }
            }

            indices.add(new double[]{leftmost, g});
            hrefs.add(name1);
        }

        // Sort by leftmost x ascending
        indices.sort((a, b) -> Double.compare(a[0], b[0]));

        final List<String> linkedPageNames = new ArrayList<>(indices.size());
        for (double[] pair : indices) {
            linkedPageNames.add(hrefs.get((int) pair[1]));
        }
        return linkedPageNames;
    }

    private static String extractTitle(XPathExpression titleExpr, Node node) throws XPathExpressionException {
        final NodeList titleParas = (NodeList) titleExpr.evaluate(node, NODESET);
        final StringBuilder titleBuf = new StringBuilder();
        for (int j = 0; j < titleParas.getLength(); j++) {
            if (titleBuf.length() > 0) titleBuf.append('\n');
            titleBuf.append(titleParas.item(j).getTextContent());
        }
        final String title = titleBuf.toString();
        return title;
    }
}
