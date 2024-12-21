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
package org.codelibs.fess.crawler.extractor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.codelibs.curl.Curl;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.crawler.entity.ExtractData;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.utflute.core.PlainTestCase;
import org.testcontainers.containers.BindMode;
import org.testcontainers.ollama.OllamaContainer;

public class OllamaExtractorTest extends PlainTestCase {

    static final Logger logger = Logger.getLogger(OllamaExtractorTest.class.getName());

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

    public void test_getText() throws Exception {
        ComponentUtil.setFessConfig(new FessConfig.SimpleImpl() {

            @Override
            public String getSystemProperty(final String key) {
                if (OllamaExtractor.OLLAMA_ENDPOINT.equals(key)) {
                    return ollama.getEndpoint();
                }
                if (key.equals(OllamaExtractor.OLLAMA_MODEL_PREFIX + "text_plain")) {
                    return modelName;
                }
                if (key.equals(OllamaExtractor.OLLAMA_PROMPT_PREFIX + "text_plain")) {
                    return "What is [[INPUT_TEXT]]?";
                }
                return null;
            }
        });

        final OllamaExtractor extractor = new OllamaExtractor() {
            @Override
            protected ExtractorFactory getExtractorFactory() {
                return new ExtractorFactory();
            }
        };
        extractor.register(List.of("text/plain"));

        final Map<String, String> params = new HashMap<>();
        params.put(ExtractData.RESOURCE_NAME_KEY, "test.txt");
        params.put(ExtractData.CONTENT_TYPE, "text/plain");
        final ExtractData extractData = extractor.getText(new ByteArrayInputStream("Apple".getBytes()), params);
        logger.info("output: " + extractData.getContent());
        assertNotNull(extractData);
    }

}
