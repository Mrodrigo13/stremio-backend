package com.mario.stremio_backend;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Service
public class TMDBService {

    private final WebClient webClient;
    private final String API_TOKEN = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIyMWZhNDgzNDFkMzhiZjhjNTBjY2RhZDFmMDY4ZDY3NiIsIm5iZiI6MTc3ODQ0MzAwOS4wMDQ5OTk5LCJzdWIiOiI2YTAwZTMwMWEzZmY2NzI2YjEyNDEzZDkiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.XE1jhBOeD18EWLsG-LCnGLubCB9WlO77Fv2q7cLeaJY";

    public TMDBService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.themoviedb.org/3").build();
    }

    public Map<String, Object> getPeliculaInfo(String imdbId) {
        try {
            return (Map<String, Object>) webClient.get()
                    .uri("/find/{external_id}?external_source=imdb_id&language=es-MX", imdbId)
                    .header("Authorization", API_TOKEN)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(response -> {
                        var results = (java.util.List<Map<String, Object>>) response.get("movie_results");
                        return results.isEmpty() ? Map.of() : results.get(0);
                    })
                    .block();
        } catch (Exception e) {
            return Map.of();
        }
    }
}