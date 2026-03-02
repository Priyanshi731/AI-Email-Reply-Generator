package com.email.email_writer_sb.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//import tools.jackson.databind.JsonNode;
//import tools.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class EmailGeneratorService {
    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReply(EmailRequest emailRequest) {
        //build the prompt
        String prompt = buildPrompt(emailRequest);

        //craft a request
//        Map<String,Object> requestBody= Map.of(
//                "contents",new Object[] {
//                       Map.of("parts",new Object[]{
//                               Map.of("text",prompt)
//
//                       })
//
//                }
//        );
        Map<String,Object> requestBody = Map.of(
                "contents", new Object[] {
                        Map.of(
                                "role","user",
                                "parts", new Object[] {
                                        Map.of("text", prompt)
                                }
                        )
                }
        );
        //do request and get response
        String response=webClient.post()
//               .uri(geminiApiUrl + geminiApiKey)
                .uri(geminiApiUrl + "?key=" + geminiApiKey)
                .header("Content-Type","application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        System.out.println("Gemini Raw Response: " + response);
//extract response and return response
        return extractResponseContent(response);

    }

//    private String extractResponseContent(String response) {
//        try{
//            ObjectMapper mapper=new ObjectMapper();
//            JsonNode rootNode=mapper.readTree(response);
//            return rootNode.path("candidates")
//                    .get(0)
//                    .path("content")
//                    .path("parts")
//                    .get(0)
//                    .path("text")
//                    .asText();
//        }
//        catch(Exception e)
//        {
//            return "Error processing request:" + e.getMessage();
//        }
//    }
private String extractResponseContent(String response) {
    try {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response);

        // If Gemini returns error
        if (rootNode.has("error")) {
            return "Gemini API Error: " + rootNode.get("error").get("message").asText();
        }

        JsonNode candidates = rootNode.path("candidates");

        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode content = candidates.get(0).path("content");
            JsonNode parts = content.path("parts");

            if (parts.isArray() && parts.size() > 0) {
                JsonNode textNode = parts.get(0).path("text");

                if (!textNode.isMissingNode()) {
                    return textNode.asText();
                }
            }
        }

        return "AI could not generate reply.";

    } catch (Exception e) {
        return "Error processing Gemini response: " + e.getMessage();
    }
}
    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt=new StringBuilder();
        prompt.append("Generate a professional email reply for the following email content.PLease don't generate a subject line ");
        if (emailRequest.getTone()!=null && !emailRequest.getTone().isEmpty())
        {
            prompt.append("Use a ").append(emailRequest.getTone()).append("tone.");
        }
        prompt.append("\nOriginal email : \n").append(emailRequest.getEmailContent());
        return prompt.toString();


    }

}
