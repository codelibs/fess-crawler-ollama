/*
 * Copyright 2012-2024 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.ingest;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.codelibs.curl.Curl;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.OllamaConstants;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.utflute.core.PlainTestCase;
import org.testcontainers.containers.BindMode;
import org.testcontainers.ollama.OllamaContainer;

public class OllamaIngesterTest extends PlainTestCase {

    static final Logger logger = Logger.getLogger(OllamaIngesterTest.class.getName());

    static final String imageTag = "ollama/ollama:0.5.4";

    static final String modelName = "smollm:135m";

    OllamaContainer ollama;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        final File modelDir = new File(".ollama");
        modelDir.mkdirs();
        logger.info("model dir: " + modelDir.getAbsolutePath());

        ollama = new OllamaContainer(imageTag);
        ollama.withExposedPorts(11434);
        ollama.withFileSystemBind(modelDir.getAbsolutePath(), "/root/.ollama", BindMode.READ_WRITE);
        ollama.start();

        try (final CurlResponse response = Curl.post(ollama.getEndpoint() + "/api/pull").header("Content-Type", "application/json")
                .body("{\"model\":\"" + modelName + "\"}").execute()) {
            logger.info("Model Pull Response: " + response.getContentAsString());
        }
    }

    @Override
    protected void tearDown() throws Exception {
        ollama.stop();
        ComponentUtil.setFessConfig(null);
        super.tearDown();
    }

    public void test_process() throws Exception {
        ComponentUtil.setFessConfig(new FessConfig.SimpleImpl() {

            @Override
            public String getSystemProperty(final String key) {
                if (OllamaConstants.OLLAMA_ENDPOINT.equals(key)) {
                    return ollama.getEndpoint();
                }
                if (key.equals(OllamaConstants.OLLAMA_INGESTER_MODEL_PREFIX + "text_plain")) {
                    return modelName;
                }
                if (key.equals(OllamaConstants.OLLAMA_INGESTER_PROMPT_PREFIX + "text_plain")) {
                    return "What is [[content]]?";
                }
                if (key.equals(OllamaConstants.OLLAMA_INGESTER_FIELD_PREFIX + "text_plain")) {
                    return "ollama_content";
                }
                return null;
            }

            @Override
            public String getIndexFieldMimetype() {
                return "mimetype";
            }
        });

        final OllamaIngester ingester = new OllamaIngester() {
            @Override
            protected String[] getMimeTypeKeysFromSystemProperties() {
                return new String[] { "text_plain" };
            }

            @Override
            protected IngestFactory getIngestFactory() {
                return new IngestFactory();
            }
        };
        ingester.register();

        {
            final Map<String, Object> map = new HashMap<>();
            map.put("mimetype", "text/plain");
            map.put("content", "Apple");
            final Map<String, Object> output = ingester.process(map);
            assertTrue(output.get("ollama_content").toString().contains("Apple"));
        }
        {
            final Map<String, Object> map = new HashMap<>();
            map.put("mimetype", "text/html");
            map.put("content", "Apple");
            final Map<String, Object> output = ingester.process(map);
            assertFalse(output.containsKey("ollama_content"));
        }

    }

    public void test_replacePlaceholders() {
        final OllamaIngester ingester = new OllamaIngester();

        final Map<String, Object> map = new HashMap<>();
        map.put("v1", "111");
        map.put("v2", "222");
        map.put("v3", "333");
        assertEquals("111 aaa 222 bbb ", ingester.replacePlaceholders("[[v1]] aaa [[v2]] bbb [[v4]]", map));
    }

}
