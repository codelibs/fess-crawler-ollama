Fess Crawler with Ollama
[![Java CI with Maven](https://github.com/codelibs/fess-crawler-ollama/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/fess-crawler-ollama/actions/workflows/maven.yml)
============================

fess-crawler-ollama is a custom plugin for Fess that integrates with Ollama endpoints to process crawled data.
It supports extracting data from text and image inputs.

## Configuration

Add the following properties to the system.properties file:

```
ollama.endpoint=http://your-ollama-endpoint
ollama.extractor.model.image_jpg=image-classification-model
ollama.extractor.prompt.image_jpg=Describe this image.
```

