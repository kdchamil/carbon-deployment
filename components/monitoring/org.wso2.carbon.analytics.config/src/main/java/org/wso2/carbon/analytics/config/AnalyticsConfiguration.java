/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.analytics.config;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.carbon.utils.CarbonUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * AnalyticsConfiguration represents the information in analytics.xml file pragmatically.
 */
public class AnalyticsConfiguration {


    private static final String ANALYTICS_XML = "analytics.xml";
    private static final String ANALYTICS = "analytics";
    private static final String SERVER = "server";

    private static volatile AnalyticsConfiguration configuration;

    private final List<AnalyticsServer> servers = new ArrayList<AnalyticsServer>();

    // Block object initialization using private constructor
    private AnalyticsConfiguration() throws AnalyticsConfigurationException {

        //construct the analytics.xml file path
        String filePath = CarbonUtils.getEtcCarbonConfigDirPath() + File.separator + ANALYTICS_XML;

        Document document;
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = builder.parse(filePath);
        } catch (ParserConfigurationException e) {
            // if a DocumentBuilder cannot be created which satisfies the configuration requested.
            throw new AnalyticsConfigurationException("Error occurred while reading the " + filePath, e);
        } catch (SAXException e) {
            // When xml parsing fails
            throw new AnalyticsConfigurationException("Error occurred while parsing the " + filePath, e);
        } catch (IOException e) {
            // file not found etc...
            throw new AnalyticsConfigurationException("Error occurred while reading the " + filePath, e);
        }

        extractServers(document);
    }

    /**
     * Reads the analytics.xml file to AnalyticsConfiguration object
     *
     * @return The AnalyticsConfiguration
     * @throws AnalyticsConfigurationException when error occurred while reading/parsing analytics.xml
     */
    public static AnalyticsConfiguration readAnalyticsConfiguration()
            throws AnalyticsConfigurationException {
        if (configuration == null) {
            synchronized (AnalyticsConfiguration.class) {
                if (configuration == null) {
                    configuration = new AnalyticsConfiguration();
                }
            }
        }

        return configuration;
    }


    /**
     * List of Servers defined in the analytics.xml
     *
     * @return The list of AnalyticsServer objects that are configured in analytics.xml
     */
    public List<AnalyticsServer> getServers() {
        return servers;
    }


    //extracts the server tags.
    private void extractServers(Document document) throws AnalyticsConfigurationException {
        Node parentNode = document.getDocumentElement();
        if (parentNode == null || !ANALYTICS.equals(parentNode.getNodeName())) {
            throw new AnalyticsConfigurationException("The root element of the '" + ANALYTICS_XML + "' should be <analytics>");
        }

        NodeList serverNodes = document.getElementsByTagName(SERVER);
        for (int index = 0; index < serverNodes.getLength(); index++) {
            Node serverNode = serverNodes.item(index);
            extractServer(serverNode);
        }


    }

    // Extract the details in the <server> tag
    private void extractServer(Node serverNode) throws AnalyticsConfigurationException {
        if (serverNode.getNodeType() == Node.ELEMENT_NODE) {
            Element serverElement = (Element) serverNode;
            AnalyticsServer server = new AnalyticsServer();
            server.setEnabled(readBoolean(serverElement, "enabled"));
            server.setUrl(readString(serverElement, "url"));
            server.setUsername(readString(serverElement, "username"));
            server.setPassword(readString(serverElement, "password"));
            server.setProtocol(readString(serverElement, "protocol"));
            server.setServerId(readAttribute(serverElement, "id"));

            readStreamsTags(serverElement, server);
            this.servers.add(server);
        }
    }

    private void readStreamsTags(Element serverElement, AnalyticsServer server)
            throws AnalyticsConfigurationException {
        NodeList streamsTags = serverElement.getElementsByTagName("streams");

        // streams are optional we extract them only if available
        if (streamsTags != null && streamsTags.getLength() > 0) {
            Node streamsTag = streamsTags.item(0);
            extractStreams(streamsTag, server);
        }
    }

    private void extractStreams(Node streamsTag, AnalyticsServer server)
            throws AnalyticsConfigurationException {
        if (streamsTag.getNodeType() == Node.ELEMENT_NODE) {
            Element streamsElement = (Element) streamsTag;
            NodeList streamNodes = streamsElement.getElementsByTagName("stream");

            for (int index = 0; index < streamNodes.getLength(); index++) {
                Node streamNode = streamNodes.item(index);
                extractStream(streamNode, server);
            }
        }
    }

    private void extractStream(Node streamNode, AnalyticsServer server)
            throws AnalyticsConfigurationException {
        if (streamNode.getNodeType() == Node.ELEMENT_NODE) {
            Stream stream = new Stream();
            Element streamElement = (Element) streamNode;
            stream.setEnabled(readBoolean(streamElement, "enabled"));
            stream.setDescription(readString(streamElement, "description"));
            stream.setName(readString(streamElement, "name"));
            stream.setVersion(readString(streamElement, "version"));
            server.getStreams().add(stream);
        }

    }

    private String readAttribute(Element element, String id) {
        return element.getAttribute(id);
    }

    private boolean readBoolean(Element serverElement, String tagName)
            throws AnalyticsConfigurationException {
        String value = readString(serverElement, tagName);
        return Boolean.valueOf(value);
    }

    private String readString(Element serverElement, String tagName)
            throws AnalyticsConfigurationException {
        Node node = getNode(serverElement, tagName);
        return node.getTextContent();
    }

    private Node getNode(Element serverElement, String tagName)
            throws AnalyticsConfigurationException {
        NodeList elements = serverElement.getElementsByTagName(tagName);
        if (elements.getLength() == 0) {
            throw new AnalyticsConfigurationException(tagName + " not found in the " + serverElement.getNodeName());
        }

        return elements.item(0);
    }
}
