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
package org.codelibs.fess;

public class OllamaConstants {

    /** The key of the Ollama endpoint. */
    public static final String OLLAMA_ENDPOINT = "ollama.endpoint";

    /** The key prefix for the Ollama prompt of the extractor */
    public static final String OLLAMA_EXTRACTOR_PROMPT_PREFIX = "ollama.extractor.prompt.";

    /** The key prefix for the Ollama model of the extractor */
    public static final String OLLAMA_EXTRACTOR_MODEL_PREFIX = "ollama.extractor.model.";

    /** The key prefix for the Ollama prompt of the ingester */
    public static final String OLLAMA_INGESTER_PROMPT_PREFIX = "ollama.ingester.prompt.";

    /** The key prefix for the Ollama model of the ingester */
    public static final String OLLAMA_INGESTER_MODEL_PREFIX = "ollama.ingester.model.";

    /** The key prefix for the Ollama output field of the ingester */
    public static final String OLLAMA_INGESTER_FIELD_PREFIX = "ollama.ingester.field.";

    /** The placeholder for the input text in the Ollama prompt */
    public static final String TEXT_PLACEHOLDER = "[[INPUT_TEXT]]";

    private OllamaConstants() {
        // nothing
    }
}
