/*
 * Copyright (c) 2021-present - Yupiik SAS - https://www.yupiik.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.yupiik.uship.documentation;

import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static jakarta.json.bind.config.PropertyOrderStrategy.LEXICOGRAPHICAL;
import static java.util.Comparator.comparing;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;
import static javax.xml.xpath.XPathConstants.NODESET;

public class TomcatConnectorAttributesExtractor implements Runnable {
    private final Logger logger = Logger.getLogger(getClass().getName());

    private final Map<String, String> configuration;

    public TomcatConnectorAttributesExtractor(final Map<String, String> configuration) {
        this.configuration = configuration;
    }

    private void doRun() {
        final var urls = Stream.of(getDocUrl().split(","))
                .map(String::strip)
                .filter(it -> !it.isBlank())
                .toArray(String[]::new);

        final var xPath = XPathFactory.newInstance().newXPath();
        try {
            final var sections = new Sections(
                    configuration.getOrDefault("tomcat.version", "1.0.21"),
                    fetchConnectorSections(urls[0], xPath),
                    fetchValves(urls[1], xPath));

            write(sections);
        } catch (final RuntimeException re) {
            throw re;
        } catch (final Exception re) {
            throw new IllegalStateException(re);
        }
    }

    @Override
    public void run() {
        if (Boolean.parseBoolean(configuration.getOrDefault("minisite.preActions.tomcat.ignore", "false"))) {
            logger.warning(() -> getClass().getName() + " ignored, skipping");
            return;
        }

        // todo: support multiple versions?
        doRun();
    }

    private void write(final Sections sections) throws Exception {
        final var output = configuration.get("output");
        final var log = output == null || output.isBlank();
        try (final var jsonb = JsonbBuilder.create(new JsonbConfig()
                .withFormatting(log)
                .withPropertyOrderStrategy(LEXICOGRAPHICAL))) {
            if (log) {
                final var writer = new StringWriter();
                try (final var out = writer) {
                    jsonb.toJson(sections, out);
                }
                logger.info("\n" + writer);
            } else {
                final var out = Path.of(output);
                if (out.getParent() != null) {
                    Files.createDirectories(out.getParent());
                }
                try (final var br = Files.newBufferedWriter(out)) {
                    jsonb.toJson(sections, br);
                }
            }
        }
    }

    private List<Section> fetchValves(final String url, final XPath xPath) throws XPathExpressionException, IOException, InterruptedException, ParserConfigurationException, SAXException {
        final var document = parseConnector(fetchDoc(url));
        document.normalize();

        final var attributesRoots = xPath.compile("/document/body/section/subsection");
        final var attributeSelector = xPath.compile("subsection[@name='Attributes']/attributes/attribute");
        final var descriptionSelector = xPath.compile("p");
        final var introductionSelector = xPath.compile("subsection[@name='Introduction']");

        final var valves = NodeList.class.cast(attributesRoots.evaluate(document, NODESET));

        final var sanitizer = Pattern.compile("\n +");
        return stream(valves)
                .map(e -> toSection(attributeSelector, descriptionSelector, introductionSelector, sanitizer, e))
                .filter(s -> !"Introduction".equals(s.name) && !"Attributes".equals(s.name)) // easier way to skip nested blocks
                .collect(toList());
    }

    private Section toSection(final XPathExpression attributeSelector,
                              final XPathExpression descriptionSelector,
                              final XPathExpression introductionSelector,
                              final Pattern sanitizer, final Node node) {
        try {
            final var evaluated = attributeSelector.evaluate(node, NODESET);
            final var configs = stream(NodeList.class.cast(evaluated))
                    .map(it -> {
                        final var nodeAttributes = it.getAttributes();
                        final var name = nodeAttributes
                                .getNamedItem("name")
                                .getNodeValue();
                        try {
                            var description = sanitizer.matcher(asText(descriptionSelector.evaluate(it, NODESET))).replaceAll(" ");
                            if (!description.endsWith(".") && !description.isBlank()) {
                                description += ".";
                            }
                            final var type = findType(name, description);
                            return new Attribute(
                                    name,
                                    description,
                                    Boolean.parseBoolean(nodeAttributes
                                            .getNamedItem("required")
                                            .getNodeValue()),
                                    type,
                                    findDefault(description)
                                            .orElseGet(() -> "boolean".equals(type) ? "false" : null),
                                    findAllowedValues(description));
                        } catch (final XPathExpressionException ex) {
                            throw new IllegalStateException(ex);
                        }
                    })
                    .sorted(comparing(a -> a.name.toLowerCase(ROOT)))
                    .collect(toList());
            return new Section(
                    node.getAttributes().getNamedItem("name").getNodeValue(),
                    asText(introductionSelector.evaluate(node, NODESET)),
                    configs);
        } catch (final XPathExpressionException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private List<Section> fetchConnectorSections(final String url, final XPath xPath) throws XPathExpressionException, IOException, InterruptedException, ParserConfigurationException, SAXException {
        final var document = parseConnector(fetchDoc(url));
        document.normalize();

        final var attributesRoots = xPath.compile("//section[@name='Attributes']");
        final var attributeSelector = xPath.compile("attributes/attribute");
        final var descriptionSelector = xPath.compile("p");

        final var attributes = NodeList.class.cast(attributesRoots
                .evaluate(document, NODESET /* theorically could be NODE but just in case */));

        final var sanitizer = Pattern.compile("\n *");
        return stream(attributes) // normally size == 1
                .flatMap(it -> stream(it.getChildNodes()))
                .filter(it -> "subsection".equals(it.getLocalName()))
                .map(e -> toSection(attributeSelector, descriptionSelector, descriptionSelector, sanitizer, e))
                .collect(toList());
    }

    private String findType(final String name, final String description) {
        if (name.startsWith("allow") ||
                name.startsWith("defer") ||
                name.startsWith("disable") ||
                name.startsWith("enable") ||
                name.startsWith("enforce") ||
                name.startsWith("bind") ||
                name.startsWith("use") ||
                name.startsWith("reject") ||
                name.startsWith("relaxed") ||
                name.startsWith("restricted") ||
                name.startsWith("throw") ||
                name.startsWith("xpoweredBy") ||
                name.endsWith("Enabled") ||
                name.endsWith("only") ||
                "secure".equals(name) ||
                "tcpNoDelay".equals(name) ||
                description.contains(" boolean ") ||
                description.contains("(bool)") ||
                description.contains("If set to true") ||
                description.startsWith("Flag to ")) {
            return "boolean";
        }
        if ("port".equals(name) ||
                name.startsWith("max") ||
                name.startsWith("min") ||
                name.endsWith("Count") ||
                name.endsWith("Port") ||
                name.endsWith("Priority") ||
                name.endsWith("Timeout") ||
                name.endsWith("Time") ||
                name.endsWith("Millis") ||
                name.endsWith("Size") ||
                "connectionLinger".equals(name) ||
                description.contains("(int)")) {
            return "integer";
        }
        return "string";
    }

    private List<String> findAllowedValues(final String description) {
        // When set to reject
        List<String> result = null;
        int from = 0;
        while (from < description.length()) {
            int start = description.indexOf("When set to ", from);
            if (start < 0) {
                return result;
            }
            start += "When set to ".length();

            final int end = description.indexOf(' ', start);
            if (end < 0) {
                return result;
            }

            from = end;

            if (result == null) {
                result = new ArrayList<>();
            }
            result.add(description.substring(start, end).strip());
        }
        return result;
    }

    private Optional<String> findDefault(final String description) {
        if (description.contains("The default value is an empty String") || description.contains("the default value is \"\"")) {
            return Optional.of("");
        }

        final int mustBeSetIndex = description.indexOf("This MUST be set to ");
        if (mustBeSetIndex > 0) {
            final int end = description.lastIndexOf('.');
            if (end > mustBeSetIndex) {
                final var value = description.substring(mustBeSetIndex + "This MUST be set to ".length(), end).strip();
                final int sep = value.indexOf(' ');
                if (sep > 0) {
                    return Optional.of(value.substring(0, sep).strip());
                }
                return Optional.of(value);
            }
        }

        // reference to another parameter
        return extract(description, "The default value is to use the value that has been set for the ", " attribute.", false)
                .or(() -> extract(description, "this attribute is set to the value of the ", " attribute.", false))
                .map(it -> "ref:" + it)
                // direct value
                .or(() -> extract(description, "The default value is .", ".", false))
                .or(() -> extract(description, " For Linux the default is ", ".", false))
                .or(() -> extract(description, "the default value is \"", "\"", false))
                .or(() -> extract(description, " default value of ", " ", true))
                .or(() -> extract(description, " default of ", " ", true))
                .or(() -> extract(description, /*t*/ "he default value is ", " ", true))
                .or(() -> extract(description, " default is ", " ", true))
                .or(() -> extract(description, "defaults to ", " ", true))
                .or(() -> extract(description, "If not specified, this attribute is set to ", " ", true))
                .or(() -> extract(description, "This is set to ", " by default.", false))
                .or(() -> extract(description, "By default, ", ".", false)
                        .filter(it -> it.endsWith("disabled"))
                        .map(it -> "false"))
                .or(() -> extract(description, "the default ", ".", false)
                        .flatMap(it -> extract(it, " value of ", " ", false))
                        .map(String::strip));
    }

    private Optional<String> extract(final String from, final String prefix, final String suffix, final boolean supportAlternativeEnds) {
        int start = from.indexOf(prefix);
        if (start < 0) {
            return Optional.empty();
        }
        start += prefix.length();
        while (from.charAt(start) == ' ') {
            start++;
            if (start >= from.length()) {
                return Optional.empty();
            }
        }

        int end = from.indexOf(suffix, start);
        if (supportAlternativeEnds) {
            final int end2 = from.indexOf('.', start);
            if ((end < 0 && end2 > 0) || (end2 > 0 && end2 < end)) {
                end = end2;
            }
            final int end3 = from.indexOf('(', start);
            if ((end < 0 && end3 > 0) || (end3 > 0 && end3 < end)) {
                end = end3;
            }
        }
        if (end > 0) {
            final var value = from.substring(start, end).strip();

            // some particular case where tested separators are not that great
            if (value.equals("HTTP/1") && from.length() > end + 3 && ".1".equals(from.substring(end, end + 2))) {
                return Optional.of("HTTP/1.1");
            }

            if (value.startsWith("\"") && value.endsWith("\"")) {
                return Optional.of(value.substring(1, value.length() - 1));
            }

            return Optional.of(value);
        }
        return Optional.empty();
    }

    private Document parseConnector(final byte[] xml) throws ParserConfigurationException, IOException, SAXException {
        final var builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setValidating(false);
        builderFactory.setNamespaceAware(true);
        builderFactory.setFeature(FEATURE_SECURE_PROCESSING, true);

        final var parser = builderFactory.newDocumentBuilder();
        parser.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));

        final var inputSource = new InputSource();
        inputSource.setByteStream(new ByteArrayInputStream(xml));
        inputSource.setEncoding("UTF-8");
        return parser.parse(inputSource);
    }

    private byte[] fetchDoc(final String url) throws IOException, InterruptedException {
        final var cache = Path.of(configuration.get("cache")).resolve(url.substring(url.lastIndexOf('/') + 1));
        if (Files.exists(cache)) {
            logger.info(() -> "Using cache '" + cache + "'");
            return Files.readAllBytes(cache);
        }

        final var xml = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(url))
                        .header("accept", "*/*")
                        .timeout(Duration.ofMinutes(Integer.getInteger(getClass().getName() + ".timeoutMn", 1)))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());
        if (xml.statusCode() != 200) {
            throw new IllegalStateException("Invalid documentation fetch: HTTP " + xml.statusCode());
        }
        logger.info(() -> "Got '" + url + "', will parse it now");
        if (!Files.exists(cache.getParent())) {
            Files.createDirectories(cache.getParent());
        }
        Files.write(cache, xml.body());
        return xml.body();
    }

    private String getDocUrl() {
        final var url = configuration.get("github");
        if (url == null) {
            throw new IllegalArgumentException("No 'github' url to fetch the documentation.");
        }
        if (!url.contains("-SNAPSHOT")) {
            return url;
        }
        // replace last digit + -SNAPSHOT by .x (branch 10.0.x for ex)
        return url.replaceFirst("\\.\\d+-SNAPSHOT/", ".x/");
    }

    private Stream<Node> stream(final NodeList attributes) {
        return IntStream.range(0, attributes.getLength())
                .mapToObj(attributes::item);
    }

    private String asText(final Object description) {
        return stream(NodeList.class.cast(description))
                .map(Node::getTextContent)
                .collect(joining("\n"))
                .strip();
    }

    public static final class Sections {
        public final String tomcatVersion;
        public final List<Section> connectors;
        public final List<Section> valves;

        private Sections(final String tomcatVersion,
                         final List<Section> connectors,
                         final List<Section> valves) {
            this.tomcatVersion = tomcatVersion;
            this.connectors = connectors;
            this.valves = valves;
        }
    }

    public static final class Section {
        public final String name;
        public final String description;
        public final List<Attribute> attributes;

        public Section(final String name, final String description, List<Attribute> attributes) {
            this.name = name;
            this.description = description;
            this.attributes = attributes;
        }
    }

    @JsonbPropertyOrder({"name", "type", "defaultValue", "allowedValue", "required", "description"})
    public static final class Attribute {
        public final String name;
        public final String description;
        public final boolean required;

        // kind of light JSON-Schema
        public final String type;
        public final String defaultValue;
        public final List<String> allowedvalues;

        public Attribute(final String name, final String description, final boolean required,
                         final String type, final String defaultValue, final List<String> allowedvalues) {
            this.name = name;
            this.description = description;
            this.required = required;
            this.type = type;
            this.defaultValue = defaultValue;
            this.allowedvalues = allowedvalues;
        }
    }
}
