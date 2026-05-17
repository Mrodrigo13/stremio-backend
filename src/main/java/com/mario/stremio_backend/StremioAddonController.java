package com.mario.stremio_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
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

        repo.findById(id).ifPresent(p -> {
            Map<String, Object> stream = new HashMap<>();
            String enlaceGuardado = p.getMagnetLink();

            // INTERRUPTOR INTELIGENTE
            if (enlaceGuardado.startsWith("magnet:")) {
                // Opción A: Es un Torrent (Va por TorrServer en el puerto 8090)
                stream.put("name", "TorrServer");
                stream.put("title", p.getTitulo() + " (TorrServer)");

                String hash = extraerHash(enlaceGuardado);
                stream.put("url", "http://" + MI_IP_LOCAL + ":" + MOTOR_TORRENT_PORT + "/stream/video.mkv?link=" + hash + "&index=1&play");
            } else {
                // Opción B: Es un Archivo Local en tu PC
                stream.put("name", "Directo PC");
                stream.put("title", p.getTitulo() + " (MP4/MKV Local)");

                // ¡AQUÍ CAMBIA LA RUTA!
                stream.put("url", "http://" + MI_IP_LOCAL + ":8080/video/" + enlaceGuardado);
            }

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

    // --- SOLO SE MODIFICÓ ESTE MÉTODO PARA LA REPRODUCCIÓN LOCAL ---
    @CrossOrigin(origins = "*")
    @GetMapping("/video/{fileName:.+}")
    public ResponseEntity<Resource> streamLocalVideo(@PathVariable String fileName) {
        try {
            String baseDir = "C:/Users/PC/Downloads/Torrent/";
            File video = new File(baseDir + fileName);

            // 1. Autodetección de .mp4 y .mkv
            if (!video.exists()) {
                File mp4 = new File(baseDir + fileName + ".mp4");
                File mkv = new File(baseDir + fileName + ".mkv");

                if (mp4.exists()) {
                    video = mp4;
                } else if (mkv.exists()) {
                    video = mkv;
                }
            }

            if (!video.exists()) {
                System.out.println("❌ Archivo no encontrado: " + fileName);
                return ResponseEntity.notFound().build();
            }

            // 2. Motor nativo de Spring (evita el error de HttpMessageNotWritableException)
            Resource resource = new FileSystemResource(video);

            MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                    .orElse(video.getName().endsWith(".mkv") ? MediaType.parseMediaType("video/x-matroska") : MediaType.valueOf("video/mp4"));

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(resource);

        } catch (Exception e) {
            System.out.println("❌ Error en el streaming: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}