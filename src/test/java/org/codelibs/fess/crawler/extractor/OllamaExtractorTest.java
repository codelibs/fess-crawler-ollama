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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.codelibs.curl.Curl;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.OllamaConstants;
import org.codelibs.fess.crawler.entity.ExtractData;
import org.codelibs.fess.crawler.exception.UnsupportedExtractException;
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
                if (OllamaConstants.OLLAMA_ENDPOINT.equals(key)) {
                    return ollama.getEndpoint();
                }
                if (key.equals(OllamaConstants.OLLAMA_EXTRACTOR_MODEL_PREFIX + "text_plain")) {
                    return modelName;
                }
                if (key.equals(OllamaConstants.OLLAMA_EXTRACTOR_PROMPT_PREFIX + "text_plain")) {
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
        assertNotNull(extractData.getContent());
        assertTrue(extractData.getContent().length() > 0);
    }

    public void test_getImageJpeg() throws Exception {
        ComponentUtil.setFessConfig(new FessConfig.SimpleImpl() {

            @Override
            public String getSystemProperty(final String key) {
                if (OllamaConstants.OLLAMA_ENDPOINT.equals(key)) {
                    return ollama.getEndpoint();
                }
                if (key.equals(OllamaConstants.OLLAMA_EXTRACTOR_MODEL_PREFIX + "image_jpeg")) {
                    return modelName;
                }
                if (key.equals(OllamaConstants.OLLAMA_EXTRACTOR_PROMPT_PREFIX + "image_jpeg")) {
                    return "Describe this image";
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
        extractor.register(List.of("image/jpeg"));

        // Create a simple test image
        final byte[] imageBytes = createTestImage("jpeg");

        final Map<String, String> params = new HashMap<>();
        params.put(ExtractData.RESOURCE_NAME_KEY, "test.jpg");
        params.put(ExtractData.CONTENT_TYPE, "image/jpeg");
        final ExtractData extractData = extractor.getText(new ByteArrayInputStream(imageBytes), params);
        logger.info("image output: " + extractData.getContent());
        assertNotNull(extractData);
        assertNotNull(extractData.getContent());
        assertTrue(extractData.getContent().length() > 0);
    }

    public void test_getImagePng() throws Exception {
        ComponentUtil.setFessConfig(new FessConfig.SimpleImpl() {

            @Override
            public String getSystemProperty(final String key) {
                if (OllamaConstants.OLLAMA_ENDPOINT.equals(key)) {
                    return ollama.getEndpoint();
                }
                if (key.equals(OllamaConstants.OLLAMA_EXTRACTOR_MODEL_PREFIX + "image_png")) {
                    return modelName;
                }
                if (key.equals(OllamaConstants.OLLAMA_EXTRACTOR_PROMPT_PREFIX + "image_png")) {
                    return "Describe this image";
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
        extractor.register(List.of("image/png"));

        // Create a simple test image
        final byte[] imageBytes = createTestImage("png");

        final Map<String, String> params = new HashMap<>();
        params.put(ExtractData.RESOURCE_NAME_KEY, "test.png");
        params.put(ExtractData.CONTENT_TYPE, "image/png");
        final ExtractData extractData = extractor.getText(new ByteArrayInputStream(imageBytes), params);
        logger.info("image output: " + extractData.getContent());
        assertNotNull(extractData);
        assertNotNull(extractData.getContent());
        assertTrue(extractData.getContent().length() > 0);
    }

    public void test_getMimeTypeKeyFromFilename() throws Exception {
        ComponentUtil.setFessConfig(new FessConfig.SimpleImpl() {

            @Override
            public String getSystemProperty(final String key) {
                if (OllamaConstants.OLLAMA_ENDPOINT.equals(key)) {
                    return ollama.getEndpoint();
                }
                if (key.equals(OllamaConstants.OLLAMA_EXTRACTOR_MODEL_PREFIX + "text_plain")) {
                    return modelName;
                }
                if (key.equals(OllamaConstants.OLLAMA_EXTRACTOR_PROMPT_PREFIX + "text_plain")) {
                    return "Summarize: [[INPUT_TEXT]]";
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

        // Test MIME type resolution from filename extension (no Content-Type provided)
        final Map<String, String> params = new HashMap<>();
        params.put(ExtractData.RESOURCE_NAME_KEY, "document.txt");
        // Note: no CONTENT_TYPE set, should resolve from filename

        final ExtractData extractData = extractor.getText(new ByteArrayInputStream("Test content".getBytes()), params);
        logger.info("output: " + extractData.getContent());
        assertNotNull(extractData);
        assertNotNull(extractData.getContent());
    }

    public void test_unsupportedMimeType() throws Exception {
        ComponentUtil.setFessConfig(new FessConfig.SimpleImpl() {

            @Override
            public String getSystemProperty(final String key) {
                if (OllamaConstants.OLLAMA_ENDPOINT.equals(key)) {
                    return ollama.getEndpoint();
                }
                if (key.equals(OllamaConstants.OLLAMA_EXTRACTOR_MODEL_PREFIX + "text_plain")) {
                    return modelName;
                }
                if (key.equals(OllamaConstants.OLLAMA_EXTRACTOR_PROMPT_PREFIX + "text_plain")) {
                    return "Summarize: [[INPUT_TEXT]]";
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

        // Test with unsupported MIME type - should throw UnsupportedExtractException
        final Map<String, String> params = new HashMap<>();
        params.put(ExtractData.RESOURCE_NAME_KEY, "test.pdf");
        params.put(ExtractData.CONTENT_TYPE, "application/pdf");

        try {
            extractor.getText(new ByteArrayInputStream("test".getBytes()), params);
            fail("Expected UnsupportedExtractException");
        } catch (final UnsupportedExtractException e) {
            logger.info("Expected exception: " + e.getMessage());
            assertTrue(e.getMessage().contains("No model found for key"));
        }
    }

    public void test_missingMimeTypeKey() throws Exception {
        ComponentUtil.setFessConfig(new FessConfig.SimpleImpl() {

            @Override
            public String getSystemProperty(final String key) {
                if (OllamaConstants.OLLAMA_ENDPOINT.equals(key)) {
                    return ollama.getEndpoint();
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

        // Test with no MIME type or filename that can be resolved
        final Map<String, String> params = new HashMap<>();
        params.put(ExtractData.RESOURCE_NAME_KEY, "test.unknown");

        try {
            extractor.getText(new ByteArrayInputStream("test".getBytes()), params);
            fail("Expected UnsupportedExtractException");
        } catch (final UnsupportedExtractException e) {
            logger.info("Expected exception: " + e.getMessage());
            assertTrue(e.getMessage().contains("MimeType key could not be resolved"));
        }
    }

    public void test_getWeight() throws Exception {
        ComponentUtil.setFessConfig(new FessConfig.SimpleImpl() {

            @Override
            public String getSystemProperty(final String key) {
                if (OllamaConstants.OLLAMA_ENDPOINT.equals(key)) {
                    return ollama.getEndpoint();
                }
                return null;
            }
        });

        final OllamaExtractor extractor = new OllamaExtractor();
        assertEquals(10, extractor.getWeight());
    }

    /**
     * Creates a simple test image with a colored rectangle
     */
    private byte[] createTestImage(final String format) throws IOException {
        final BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, 100, 100);
        g2d.setColor(Color.WHITE);
        g2d.drawString("Test", 40, 50);
        g2d.dispose();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

}
