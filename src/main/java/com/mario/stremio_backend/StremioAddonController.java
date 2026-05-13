package com.mario.stremio_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/")
public class StremioAddonController {

    @Autowired
    private PeliculaRepository repo; // Conexión a tu base de datos H2
    @Autowired
    private TMDBService tmdbService;
    private final String MI_IP_LOCAL = "192.168.3.31";
    private final String MOTOR_TORRENT_PORT = "8090";

    @CrossOrigin(origins = "*")
    @GetMapping("/manifest.json")
    public ResponseEntity<Map<String, Object>> getManifest() {
        Map<String, Object> manifest = new HashMap<>();
        manifest.put("id", "com.mario.stremio-privado");
        manifest.put("version", "1.0.0");
        manifest.put("name", "Mi Catálogo 4K Local");
        manifest.put("description", "Streaming local directo PC a TV");
        manifest.put("types", Arrays.asList("movie"));
        manifest.put("resources", Arrays.asList("catalog", "stream"));
        manifest.put("idPrefixes", Arrays.asList("tt"));

        Map<String, String> catalog = new HashMap<>();
        catalog.put("type", "movie");
        catalog.put("id", "peliculas_locales");
        catalog.put("name", "Mis Películazzz");
        manifest.put("catalogs", Arrays.asList(catalog));
        return ResponseEntity.ok(manifest);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/catalog/{type}/{id}.json")
    public ResponseEntity<Map<String, Object>> getCatalog(@PathVariable String type, @PathVariable String id) {
        List<Map<String, Object>> metas = new ArrayList<>();

        repo.findAll().forEach(p -> {
            // Magia: Pedimos la info real a TMDB usando el ID
            Map<String, Object> infoReal = tmdbService.getPeliculaInfo(p.getImdbId());

            Map<String, Object> movie = new HashMap<>();
            movie.put("id", p.getImdbId());
            movie.put("type", "movie");

            // Si TMDB nos dio datos, los usamos; si no, usamos los de nuestra BD
            movie.put("name", infoReal.getOrDefault("title", p.getTitulo()));
            String posterPath = (String) infoReal.get("poster_path");
            movie.put("poster", (posterPath != null) ? "https://image.tmdb.org/t/p/w500" + posterPath : "https://via.placeholder.com/500x750?text=Sin+Poster");
            movie.put("description", infoReal.getOrDefault("overview", "Sin descripción disponible localmente."));
            movie.put("releaseInfo", infoReal.getOrDefault("release_date", "").toString().split("-"));
            String fullDate = (String) infoReal.getOrDefault("release_date", "");
            movie.put("releaseInfo", fullDate.contains("-") ? fullDate.split("-")[0] : "");

            metas.add(movie);
        });

        return ResponseEntity.ok(Map.of("metas", metas));
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/stream/{type}/{id}.json")
    public ResponseEntity<Map<String, Object>> getStream(@PathVariable String type, @PathVariable String id) {
        List<Map<String, Object>> streams = new ArrayList<>();

        // Buscamos en la BD si existe una película con ese ID de IMDb
        repo.findById(id).ifPresent(p -> {
            Map<String, Object> stream = new HashMap<>();
            stream.put("name", "TorrServer");
            stream.put("title", p.getTitulo() + " (4K Local)");

            // Extraemos el hash del magnet que guardaste en la BD
            String hash = extraerHash(p.getMagnetLink());

            String urlDirecta = "http://" + MI_IP_LOCAL + ":" + MOTOR_TORRENT_PORT + "/stream/video.mkv?link=" + hash + "&index=1&play";

            stream.put("url", urlDirecta);
            streams.add(stream);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("streams", streams);
        return ResponseEntity.ok(response);
    }

    private String extraerHash(String magnet) {
        try {
            int inicio = magnet.indexOf("btih:") + 5;
            int fin = magnet.indexOf("&", inicio);
            if (fin == -1) {
                fin = magnet.length();
            }
            return magnet.substring(inicio, fin);
        } catch (Exception e) {
            return "";
        }
    }
    @PostMapping("/api/peliculas")
    public ResponseEntity<String> guardarPelicula(@RequestBody Pelicula nueva) {
        try {
            repo.save(nueva);
            return ResponseEntity.ok("Guardado");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error");
        }
    }
    // 1. Método para listar las películas en el panel de control
    @GetMapping("/api/peliculas")
    public ResponseEntity<List<Pelicula>> obtenerTodasLasPeliculas() {
        return ResponseEntity.ok(repo.findAll());
    }

    // 2. Método para eliminar una película cuando ya la viste
    @DeleteMapping("/api/peliculas/{id}")
    public ResponseEntity<String> eliminarPelicula(@PathVariable String id) {
        try {
            repo.deleteById(id);
            return ResponseEntity.ok("Película eliminada");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar");
        }
    }
}