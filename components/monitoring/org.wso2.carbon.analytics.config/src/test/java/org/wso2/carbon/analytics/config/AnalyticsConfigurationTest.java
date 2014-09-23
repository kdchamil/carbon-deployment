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


import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.Test;
import org.wso2.carbon.utils.CarbonUtils;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.lang.reflect.Field;
import java.util.List;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.verify;

@PrepareForTest({CarbonUtils.class, DocumentBuilderFactory.class})
public class AnalyticsConfigurationTest extends PowerMockTestCase {


    private AnalyticsConfiguration reader;

    @Test
    public void should_be_able_to_get_instance() throws AnalyticsConfigurationException {
        mockCarbonUtilEtcPath("/etc");
        reader = AnalyticsConfiguration.readAnalyticsConfiguration();
        verify(CarbonUtils.class);
        assertThat(reader, is(not(nullValue())));
    }

    @Test
    public void should_get_the_same_instance_every_time() throws AnalyticsConfigurationException {
        mockCarbonUtilEtcPath("/etc");
        reader = AnalyticsConfiguration.readAnalyticsConfiguration();
        AnalyticsConfiguration anotherReader = AnalyticsConfiguration.readAnalyticsConfiguration();
        assertThat(reader, is(sameInstance(anotherReader)));
    }

    @Test
    public void should_fetch_config_POJO_with_values_from_file()
            throws AnalyticsConfigurationException {
        mockCarbonUtilEtcPath("/etc");
        AnalyticsConfiguration config = AnalyticsConfiguration.readAnalyticsConfiguration();
        assertThat(config, is(notNullValue()));

        List<AnalyticsServer> servers = config.getServers();
        assertThat(servers, is(notNullValue()));
        assertThat(servers.size(), is(2));
        AnalyticsServer server1 = servers.get(0);
        assertThat(server1.isEnabled(), is(true));
        assertThat(server1.getUrl(), is("tcp://localhost:7614"));
        assertThat(server1.getUsername(), is("admin"));
        assertThat(server1.getPassword(), is("password"));
        assertThat(server1.getProtocol(), is("thrift"));
        assertThat(server1.getServerId(), is("server1"));

        List<Stream> streams = server1.getStreams();
        assertThat(streams.size(), is(1));
        Stream stream = streams.get(0);
        assertThat(stream.isEnabled(), is(true));
        assertThat(stream.getDescription(), is(notNullValue()));
        assertThat(stream.getName(), is("streamName1"));
        assertThat(stream.getVersion(), is("streamVersion1"));
    }


    @Test(expectedExceptions = AnalyticsConfigurationException.class)
    public void should_throw_exception_if_document_builder_creation_failed()
            throws AnalyticsConfigurationException, ParserConfigurationException,
                   NoSuchFieldException, IllegalAccessException {
        resetSingleton();
        mockCarbonUtilEtcPath("/etc");

        DocumentBuilderFactory factory = createMock(DocumentBuilderFactory.class);
        expect(factory.newDocumentBuilder()).andThrow(new ParserConfigurationException());
        replay(factory);

        mockStatic(DocumentBuilderFactory.class);
        expect(DocumentBuilderFactory.newInstance()).andReturn(factory);
        replay(DocumentBuilderFactory.class);

        AnalyticsConfiguration.readAnalyticsConfiguration();
    }


    @Test(expectedExceptions = AnalyticsConfigurationException.class)
    public void should_throw_exception_if_file_not_found()
            throws AnalyticsConfigurationException, ParserConfigurationException,
                   NoSuchFieldException, IllegalAccessException {

        resetSingleton();
        mockCarbonUtilEtcPath("/emptyEtc");
        AnalyticsConfiguration.readAnalyticsConfiguration();
    }

    @Test(expectedExceptions = AnalyticsConfigurationException.class)
    public void should_throw_exception_for_invalid_xml()
            throws NoSuchFieldException, IllegalAccessException, AnalyticsConfigurationException {
        resetSingleton();
        mockCarbonUtilEtcPath("/wrongEtc");
        AnalyticsConfiguration.readAnalyticsConfiguration();
    }

    @Test(expectedExceptions = AnalyticsConfigurationException.class)
    public void should_throw_exception_if_xml_root_is_not_analytics()
            throws NoSuchFieldException, IllegalAccessException, AnalyticsConfigurationException {
        resetSingleton();
        mockCarbonUtilEtcPath("/wrongRootEtc");
        AnalyticsConfiguration.readAnalyticsConfiguration();
    }

    /**
     * This will reset the static variable used for the singleton behavior
     * /The class construction will happen again after this method.
     */
    private void resetSingleton() throws NoSuchFieldException, IllegalAccessException {
        Field configuration = AnalyticsConfiguration.class.getDeclaredField("configuration");
        configuration.setAccessible(true);
        configuration.set(null, null);
    }

    private void mockCarbonUtilEtcPath(String path) {
        path = Test.class.getResource(path).getPath();
        mockStatic(CarbonUtils.class);
        expect(CarbonUtils.getEtcCarbonConfigDirPath()).andReturn(path);
        replay(CarbonUtils.class);
    }
}