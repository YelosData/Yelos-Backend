package com.yelos.backend.ai.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.yelos.backend.ai.dto.AiPipelineResponse;

import tools.jackson.databind.ObjectMapper;

import com.yelos.backend.ai.dto.EmbeddingResponse;


@Service
public class AiService {

    private final String aiServiceUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AiService(@Value("${ai.service.url}") String aiServiceUrl) {
        this.aiServiceUrl = aiServiceUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public AiPipelineResponse runPipeline(MultipartFile file) {
        try {
            String filename = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                    : "uploaded-file";
            String contentType = file.getContentType() != null
                    ? file.getContentType()
                    : "application/octet-stream";
            byte[] fileBytes = file.getBytes();

            String boundary = "----FormBoundary" + System.currentTimeMillis();
            String CRLF = "\r\n";

            String partHeader = "--" + boundary + CRLF
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"" + CRLF
                    + "Content-Type: " + contentType + CRLF
                    + CRLF;
            String partFooter = CRLF + "--" + boundary + "--" + CRLF;

            byte[] headerBytes = partHeader.getBytes(StandardCharsets.UTF_8);
            byte[] footerBytes = partFooter.getBytes(StandardCharsets.UTF_8);
            byte[] body = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
            System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
            System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
            System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiServiceUrl + "/upload_features/run"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI service returned an error: " + response.body()
                );
            }

            return objectMapper.readValue(response.body(), AiPipelineResponse.class);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not connect to AI service: " + e.getMessage());
        }
    }

    public float[] embedQuery(String text) {
    try {
        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of("text", text)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiServiceUrl + "/Embedding/"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI service returned an error: " + response.body()
            );
        }

        EmbeddingResponse result = objectMapper.readValue(response.body(), EmbeddingResponse.class);
        return result.embedding();

    } catch (ResponseStatusException e) {
        throw e;
    } catch (Exception e) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not connect to AI service: " + e.getMessage());
    }
}


    public byte[] getPiiCsvBytes(AiPipelineResponse response) {
        if (response == null || response.piiFile() == null || response.piiFile().contentBase64() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI service did not return PII file content"
            );
        }
        return Base64.getDecoder().decode(response.piiFile().contentBase64());
    }
}
