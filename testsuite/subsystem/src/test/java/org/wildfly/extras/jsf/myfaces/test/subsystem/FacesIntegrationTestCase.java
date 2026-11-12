/*
 * Copyright 2026 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extras.jsf.myfaces.test.subsystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.primefaces.PrimeFaces;
import org.wildfly.core.testrunner.ServerController;
import org.wildfly.core.testrunner.WildFlyRunner;

/**
 * Exercises the provisioned MyFaces integration through EAR deployments and real Faces postbacks.
 */
@RunWith(WildFlyRunner.class)
public class FacesIntegrationTestCase {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final List<String> DEPLOYMENTS = new ArrayList<>();
    private static URI webUri;

    @Inject
    private static ServerController controller;

    /**
     * Deploys identical EAR fixtures with both discovery modes to verify CDI and Faces integration.
     */
    @BeforeClass
    public static void deployApplications() throws Exception {
        ModelNode operation = new ModelNode();
        operation.get("operation").set("read-resource");
        operation.get("address").add("socket-binding-group", "standard-sockets").add("socket-binding", "http");
        operation.get("include-runtime").set(true);
        ModelNode binding = controller.getClient().executeForResult(operation);
        webUri = new URI("http", null, binding.get("bound-address").asString(),
                binding.get("bound-port").asInt(), "/", null, null);
        for (String mode : List.of("all", "annotated")) {
            String name = "myfaces-" + mode;
            WebArchive war = ShrinkWrap.create(WebArchive.class, name + ".war")
                    .addClasses(ConversationBean.class, ConversationBean.AnnotationConverter.class)
                    .addAsWebResource("conversation.xhtml", "test.xhtml")
                    .addAsLibraries(Path.of(PrimeFaces.class.getProtectionDomain().getCodeSource()
                            .getLocation().toURI()).toFile())
                    .addAsWebInfResource(new StringAsset("""
                            <beans xmlns="https://jakarta.ee/xml/ns/jakartaee" version="4.0"
                                   bean-discovery-mode="%s"/>
                            """.formatted(mode)), "beans.xml")
                    .setWebXML(new StringAsset("""
                            <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee" version="6.0">
                                <servlet>
                                    <servlet-name>Faces Servlet</servlet-name>
                                    <servlet-class>jakarta.faces.webapp.FacesServlet</servlet-class>
                                    <load-on-startup>1</load-on-startup>
                                </servlet>
                                <servlet-mapping>
                                    <servlet-name>Faces Servlet</servlet-name>
                                    <url-pattern>*.xhtml</url-pattern>
                                </servlet-mapping>
                            </web-app>
                            """));
            EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, name + ".ear")
                    .addAsModule(war);
            controller.deploy(ear, ear.getName());
            DEPLOYMENTS.add(ear.getName());
        }
    }

    /**
     * Removes every successfully deployed fixture, preserving any cleanup failures.
     */
    @AfterClass
    public static void undeployApplications() throws Exception {
        Exception failure = null;
        for (String name : DEPLOYMENTS) {
            try {
                controller.undeploy(name);
            } catch (Exception e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        DEPLOYMENTS.clear();
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Catches a test execution silently provisioning the wrong WildFly product family.
     */
    @Test
    public void serverUsesExpectedProduct() throws Exception {
        String expected = System.getProperty("test.server.product");
        assertNotNull("The test execution must declare its expected server product", expected);
        ModelNode operation = new ModelNode();
        operation.get("operation").set("read-attribute");
        operation.get("address").setEmptyList();
        operation.get("name").set("product-name");
        assertEquals(expected, controller.getClient().executeForResult(operation).asString());
    }

    /**
     * Executes the rendered PrimeFaces JavaScript to verify row expansion from an actual click.
     */
    @Test
    public void browserRowExpansionRetainsConversation() throws Exception {
        try (WebClient browser = new WebClient(BrowserVersion.CHROME)) {
            browser.getOptions().setCssEnabled(false);
            browser.getOptions().setTimeout((int) TIMEOUT.toMillis());
            browser.setJavaScriptTimeout(TIMEOUT.toMillis());
            HtmlPage page = browser.getPage(webUri.resolve("myfaces-all/test.xhtml").toURL());
            browser.waitForBackgroundJavaScript(TIMEOUT.toMillis());
            String token = page.getHtmlElementById("form:token").getTextContent();
            HtmlElement toggler = page.getFirstByXPath(
                    "//*[contains(concat(' ', normalize-space(@class), ' '), ' ui-row-toggler ')]");
            assertNotNull("PrimeFaces row toggler is missing", toggler);
            toggler.click();
            browser.waitForBackgroundJavaScript(TIMEOUT.toMillis());
            assertTrue("The JavaScript click did not expand the row",
                    page.asNormalizedText().contains("Details for Row 0"));
            assertEquals(token, page.getHtmlElementById("form:token").getTextContent());
            assertEquals("1", page.getHtmlElementById("form:toggles").getTextContent());
        }
    }

    /**
     * Catches missing conversation propagation during the reporter's PrimeFaces row-toggle postback.
     */
    @Test
    public void rowExpansionRetainsConversation() throws Exception {
        Page page = open("all");
        Document expanded = expand(page);
        assertTrue("Expanded row content is missing", expanded.text().contains("Details for Row 0"));
        assertEquals(text(page.document(), "form:token"), text(expanded, "form:token"));
        assertEquals("1", text(expanded, "form:toggles"));
    }

    /**
     * Catches lost conversations on ordinary form submissions, independently of AJAX decoding.
     */
    @Test
    public void fullPostbackRetainsConversation() throws Exception {
        Page page = open("all");
        Document submitted = Jsoup.parse(post(page, Map.of("form:submit", "Submit"), false));
        assertEquals(text(page.document(), "form:token"), text(submitted, "form:token"));
        assertEquals("1", text(submitted, "form:submissions"));
        assertTrue("Conversation rows were lost", text(submitted, "repeat").contains("Repeat 0"));
    }

    /**
     * Catches lost Faces annotations in an archive using annotated CDI bean discovery.
     */
    @Test
    public void annotatedDiscoveryFindsFacesConverter() throws Exception {
        Page page = open("annotated");
        assertEquals("ANNOTATED-discovered", text(page.document(), "form:annotation"));
        Document expanded = expand(page);
        assertTrue(expanded.text().contains("Details for Row 0"));
        assertEquals(text(page.document(), "form:token"), text(expanded, "form:token"));
    }

    /**
     * Exercises MyFaces UIRepeat's model creation, which previously lacked the java.sql dependency.
     */
    @Test
    public void repeatRendersRows() throws Exception {
        Document document = open("all").document();
        assertTrue(text(document, "repeat").contains("Repeat 0"));
        assertTrue(text(document, "repeat").contains("Repeat 1"));
    }

    /**
     * Catches conversation state shared across independent HTTP sessions.
     */
    @Test
    public void sessionsKeepSeparateConversations() throws Exception {
        Page first = open("all");
        Page second = open("all");
        String firstToken = text(first.document(), "form:token");
        String secondToken = text(second.document(), "form:token");
        assertNotEquals("Independent sessions shared a backing bean", firstToken, secondToken);
        assertEquals(firstToken, text(expand(first), "form:token"));
        assertEquals(secondToken, text(expand(second), "form:token"));
    }

    /**
     * Creates a fresh browser session and captures the actual rendered form, including its action URL.
     */
    private static Page open(String mode) throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(TIMEOUT).build();
        URI uri = webUri.resolve("myfaces-" + mode + "/test.xhtml");
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals("GET " + uri + " failed; see server log", 200, response.statusCode());
        Document document = Jsoup.parse(response.body(), response.uri().toString());
        assertTrue("The deployed application did not use MyFaces",
                text(document, "form:implementation").startsWith("org.apache.myfaces."));
        Element form = document.getElementById("form");
        assertNotNull("Faces form is missing", form);
        Map<String, String> fields = new LinkedHashMap<>();
        for (Element input : form.select("input[type=hidden][name]")) {
            fields.put(input.attr("name"), input.attr("value"));
        }
        assertTrue("Faces view state is missing", fields.containsKey("jakarta.faces.ViewState"));
        return new Page(client, URI.create(form.absUrl("action")), document, fields);
    }

    /**
     * Replays PrimeFaces 15's row-expansion request and extracts the rendered partial-response updates.
     */
    private static Document expand(Page page) throws Exception {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("jakarta.faces.partial.ajax", "true");
        parameters.put("jakarta.faces.source", "form:table");
        parameters.put("jakarta.faces.partial.execute", "form:table");
        parameters.put("jakarta.faces.partial.render", "form:table form:token form:toggles");
        parameters.put("jakarta.faces.behavior.event", "rowToggle");
        parameters.put("jakarta.faces.partial.event", "rowToggle");
        parameters.put("form:table_rowExpansion", "true");
        parameters.put("form:table_expandedRowIndex", "0");
        parameters.put("form:table_expandedRowKey", "0");
        parameters.put("form:table_encodeFeature", "true");
        parameters.put("form:table_skipChildren", "true");
        parameters.put("form:table_rowExpansionState", "0");
        String response = post(page, parameters, true);
        Document partial = Jsoup.parse(response, "", Parser.xmlParser());
        assertFalse(response, partial.select("partial-response").isEmpty());
        assertTrue(response, partial.select("error").isEmpty());
        StringBuilder markup = new StringBuilder();
        for (Element update : partial.select("update")) {
            markup.append(update.text());
        }
        return Jsoup.parseBodyFragment(markup.toString());
    }

    /**
     * Posts the browser's hidden state to its rendered action without injecting a conversation ID.
     */
    private static String post(Page page, Map<String, String> parameters, boolean ajax) throws Exception {
        Map<String, String> fields = new LinkedHashMap<>(page.fields());
        fields.putAll(parameters);
        String body = fields.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpRequest.Builder request = HttpRequest.newBuilder(page.action()).timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (ajax) {
            request.header("Faces-Request", "partial/ajax");
        }
        HttpResponse<String> response = page.client().send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals("POST " + page.action() + " failed; see server log", 200, response.statusCode());
        return response.body();
    }

    /**
     * Reads a visible fixture value and makes missing output an explicit assertion failure.
     */
    private static String text(Document document, String id) {
        Element element = document.getElementById(id);
        assertNotNull("Missing rendered element " + id, element);
        return element.text();
    }

    /**
     * Keeps a rendered form and its HTTP session together for a subsequent Faces postback.
     */
    private record Page(HttpClient client, URI action, Document document, Map<String, String> fields) {
    }
}
