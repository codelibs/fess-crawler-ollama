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

import java.io.InputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.io.InputStreamUtil;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.core.misc.Pair;
import org.codelibs.curl.Curl;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.Constants;
import org.codelibs.fess.OllamaConstants;
import org.codelibs.fess.crawler.entity.ExtractData;
import org.codelibs.fess.crawler.exception.ExtractException;
import org.codelibs.fess.crawler.exception.UnsupportedExtractException;
import org.codelibs.fess.crawler.extractor.impl.AbstractExtractor;
import org.codelibs.fess.crawler.ollama.OllamaConfig;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.opensearch.runner.net.OpenSearchCurl;

/**
 * Extract text from images using Ollama API.
 */
public class OllamaExtractor extends AbstractExtractor {

    private static final Logger logger = LogManager.getLogger(OllamaExtractor.class);

    protected String endpoint;

    protected Map<String, OllamaConfig> modelConfigMap = Collections.emptyMap();

    @Override
    public void register(final List<String> keyList) {
        super.register(keyList);

        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        endpoint = fessConfig.getSystemProperty(OllamaConstants.OLLAMA_ENDPOINT);
        if (StringUtil.isBlank(endpoint)) {
            if (logger.isDebugEnabled()) {
                logger.info("No Ollama endpoint is configured. Skipping registration.");
            }
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Ollama endpoint configured: {}", endpoint);
        }

        modelConfigMap = keyList.stream().map(s -> {
            final String key = s.replace('/', '_');
            final String model = fessConfig.getSystemProperty(OllamaConstants.OLLAMA_EXTRACTOR_MODEL_PREFIX + key);
            if (logger.isDebugEnabled()) {
                logger.debug(OllamaConstants.OLLAMA_EXTRACTOR_MODEL_PREFIX + key + " = " + model);
            }
            if (StringUtil.isBlank(model)) {
                return null;
            }
            final String prompt = fessConfig.getSystemProperty(OllamaConstants.OLLAMA_EXTRACTOR_PROMPT_PREFIX + key);
            if (logger.isDebugEnabled()) {
                logger.debug(OllamaConstants.OLLAMA_EXTRACTOR_PROMPT_PREFIX + key + " = " + prompt);
            }
            if (StringUtil.isBlank(prompt)) {
                return null;
            }
            logger.info("Registered OllamaExtractor for key: {}", key);
            return Pair.pair(key, new OllamaConfig(model, prompt, StringUtil.EMPTY));
        }).filter(x -> x != null).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    @Override
    public ExtractData getText(final InputStream in, final Map<String, String> params) {
        final String key = getMimeTypeKey(params);
        final OllamaConfig ollamaConfig = modelConfigMap.get(key);
        if (ollamaConfig == null) {
            throw new UnsupportedExtractException("No model found for key: " + key);
        }

        final StringBuilder buf = new StringBuilder();
        buf.append("{\"model\":\"").append(StringEscapeUtils.escapeJson(ollamaConfig.model())).append('"');
        buf.append(",\"stream\":false");
        if (key.startsWith("image_")) {
            buf.append(",\"prompt\":\"").append(StringEscapeUtils.escapeJson(ollamaConfig.prompt())).append('"');
            buf.append(",\"images\":[\"").append(Base64.getEncoder().encodeToString(InputStreamUtil.getBytes(in))).append("\"]");
        } else {
            final String prompt = ollamaConfig.prompt().replaceAll(OllamaConstants.TEXT_PLACEHOLDER,
                    new String(InputStreamUtil.getBytes(in), Constants.CHARSET_UTF_8));
            buf.append(",\"prompt\":\"").append(StringEscapeUtils.escapeJson(prompt)).append('"');
        }
        buf.append('}');

        try (final CurlResponse response =
                Curl.post(endpoint + "/api/generate").header("Content-Type", "application/json").body(buf.toString()).execute()) {
            final Map<String, Object> contentMap = response.getContent(OpenSearchCurl.jsonParser());
            if (logger.isDebugEnabled()) {
                logger.debug("response: ", contentMap);
            }
            if (contentMap.get("response") instanceof final String content) {
                return new ExtractData(content);
            }
            throw new ExtractException("No content received from Ollama. Response: " + response);
        } catch (final Exception e) {
            throw new ExtractException("Failed to process request to Ollama.", e);
        }
    }

    /**
     * Returns the key for the model to use based on the MIME type of the content.
     *
     * @param params The parameters of the content.
     * @return The key for the model.
     */
    protected String getMimeTypeKey(final Map<String, String> params) {
        final String mimeType = params.get(ExtractData.CONTENT_TYPE);
        if (StringUtil.isNotBlank(mimeType)) {
            final String key = mimeType.replace('/', '_');
            if (logger.isDebugEnabled()) {
                logger.debug("MimeType resolved to key: {}", key);
            }
            return key;
        }

        final String filename = params.get(ExtractData.RESOURCE_NAME_KEY);
        if (StringUtil.isNotBlank(filename)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Resolving MimeType key from filename: {}", filename);
            }
            final String key = filename.toLowerCase();
            if (key.endsWith(".txt")) {
                return "text_plain";
            }
            if (key.endsWith(".html")) {
                return "text_html";
            }
            if (key.endsWith(".xml")) {
                return "text_xml";
            }
            if (key.endsWith(".jpg") || key.endsWith(".jpeg")) {
                return "image_jpg";
            }
            if (key.endsWith(".png")) {
                return "image_png";
            }
        }

        throw new UnsupportedExtractException("MimeType key could not be resolved.");
    }

    @Override
    public int getWeight() {
        return 10;
    }

}
