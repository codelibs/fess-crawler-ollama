Fess Crawler with Ollama
[![Java CI with Maven](https://github.com/codelibs/fess-crawler-ollama/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/fess-crawler-ollama/actions/workflows/maven.yml)
============================

fess-crawler-ollama is a custom plugin for Fess that integrates with Ollama endpoints to process crawled data.
It supports extracting data from text and image inputs.

## Configuration

Add the following properties to the system.properties file:

```
ollama.endpoint=http://localhost:11434

ollama.extractor.model.image_jpeg=llama3.2-vision
ollama.extractor.prompt.image_jpeg=Describe this image in detail. Include key elements such as objects, people, text, colors, and the relationships between them. Make sure the description is comprehensive enough to be used for search purposes, highlighting all distinctive features of the image.

ollama.ingester.model.text_plain=llama3.2
ollama.ingester.field.text_plain=important_content
ollama.ingester.prompt.text_plain=<|start_header_id|>system<|end_header_id|>You are an expert to extract a list of keywords from the given text that are relevant for search purposes. Focus on nouns, proper nouns, and key phrases. Do not include stop words or irrelevant terms. Provide the keywords as a comma-separated list.<|eot_id|><|start_header_id|>user<|end_header_id|>[[content]]<|eot_id|><|start_header_id|>assistant<|end_header_id|>
```

