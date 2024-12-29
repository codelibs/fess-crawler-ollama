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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.core.misc.Pair;
import org.codelibs.curl.Curl;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.OllamaConstants;
import org.codelibs.fess.crawler.entity.ResponseData;
import org.codelibs.fess.crawler.entity.ResultData;
import org.codelibs.fess.crawler.exception.CrawlerSystemException;
import org.codelibs.fess.crawler.ollama.OllamaConfig;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.opensearch.runner.net.OpenSearchCurl;

/**
 * Ingests data into OpenSearch using Ollama.
 */
public class OllamaIngester extends Ingester {
    private static final Logger logger = LogManager.getLogger(OllamaIngester.class);

    /** The endpoint of the Ollama service. */
    protected String endpoint;

    /** The map of model configurations. */
    protected Map<String, OllamaConfig> modelConfigMap = Collections.emptyMap();

    @Override
    public void register() {
        super.register();

        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        endpoint = fessConfig.getSystemProperty(OllamaConstants.OLLAMA_ENDPOINT);
        if (StringUtil.isBlank(endpoint)) {
            if (logger.isDebugEnabled()) {
                logger.info("No Ollama endpoint is configured. Skipping registration.");
            }
            return;
        }
        logger.info("Ollama endpoint configured: {}", endpoint);

        modelConfigMap = Arrays.stream(getMimeTypeKeysFromSystemProperties()).map(key -> {
            final String model = fessConfig.getSystemProperty(OllamaConstants.OLLAMA_INGESTER_MODEL_PREFIX + key);
            if (StringUtil.isBlank(model)) {
                return null;
            }
            final String outputField = fessConfig.getSystemProperty(OllamaConstants.OLLAMA_INGESTER_FIELD_PREFIX + key);
            if (StringUtil.isBlank(outputField)) {
                return null;
            }
            final String prompt = fessConfig.getSystemProperty(OllamaConstants.OLLAMA_INGESTER_PROMPT_PREFIX + key);
            if (StringUtil.isBlank(prompt)) {
                return null;
            }
            logger.info("Registered Ollama model for key: {}", key);
            return Pair.pair(key, new OllamaConfig(model, prompt, outputField));
        }).filter(x -> x != null).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    protected String[] getMimeTypeKeysFromSystemProperties() {
        return ComponentUtil.getSystemProperties().keySet().stream()//
                .map(Object::toString)//
                .filter(s -> s.startsWith(OllamaConstants.OLLAMA_INGESTER_PROMPT_PREFIX))//
                .map(s -> s.toString().substring(OllamaConstants.OLLAMA_INGESTER_PROMPT_PREFIX.length()))//
                .toArray(n -> new String[n]);
    }

    @Override
    public Map<String, Object> process(final Map<String, Object> target, final DataStoreParams params) {
        return process(target);
    }

    @Override
    public ResultData process(final ResultData target, final ResponseData responseData) {
        if (target.getRawData() instanceof final Map<?, ?> rawData) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) rawData;
            target.setRawData(process(map));
        }
        return target;
    }

    @Override
    protected Map<String, Object> process(final Map<String, Object> target) {
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        if (target.get(fessConfig.getIndexFieldMimetype()) instanceof final String mimeType) {
            final String key = mimeType.replace('/', '_');
            if (logger.isDebugEnabled()) {
                logger.debug("MimeType resolved to key: {}", key);
            }
            final OllamaConfig ollamaConfig = modelConfigMap.get(key);
            if (ollamaConfig != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Ollama model found for key: {}", key);
                }

                final StringBuilder buf = new StringBuilder();
                buf.append("{\"model\":\"").append(StringEscapeUtils.escapeJson(ollamaConfig.model())).append('"');
                buf.append(",\"stream\":false");
                final String prompt = replacePlaceholders(ollamaConfig.prompt(), target);
                buf.append(",\"prompt\":\"").append(StringEscapeUtils.escapeJson(prompt)).append('"');
                buf.append('}');

                try (final CurlResponse response =
                        Curl.post(endpoint + "/api/generate").header("Content-Type", "application/json").body(buf.toString()).execute()) {
                    final Map<String, Object> contentMap = response.getContent(OpenSearchCurl.jsonParser());
                    if (logger.isDebugEnabled()) {
                        logger.debug("response: ", contentMap);
                    }
                    if (contentMap.get("response") instanceof final String content) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Content received from Ollama: {}", content);
                        }
                        target.put(ollamaConfig.outputField(), content);
                    }
                    return target;
                } catch (final Exception e) {
                    throw new CrawlerSystemException("Failed to process request to Ollama.", e);
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("No model found for key: {}", key);
            }
        }
        return target;
    }

    /**
     * Replaces placeholders in the prompt with values from the target map.
     *
     * @param prompt The prompt with placeholders.
     * @param map The map with values to replace.
     * @return The prompt with placeholders replaced.
     */
    protected String replacePlaceholders(final String prompt, final Map<String, Object> map) {
        String result = prompt;
        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            final String placeholder = "\\[\\[" + entry.getKey() + "\\]\\]";
            result = result.replaceAll(placeholder, entry.getValue().toString());
        }
        return result.replaceAll("\\[\\[.*?\\]\\]", "");
    }
}
