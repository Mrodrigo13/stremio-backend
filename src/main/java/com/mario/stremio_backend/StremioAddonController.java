package com.mario.stremio_backend;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/")
public class StremioAddonController {

    private final String MI_IP_LOCAL = "192.168.3.31"; // Tu IP del Huawei
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

        // Magia: Le decimos que vamos a usar IDs de IMDb
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

        Map<String, Object> movie1 = new HashMap<>();
        // USAMOS EL ID OFICIAL DE IMDb PARA ENGAÑAR A STREMIO
        movie1.put("id", "tt6263850");
        movie1.put("type", "movie");
        movie1.put("name", "Deadpool & Wolverine (4K Local)");
        movie1.put("poster", "https://image.tmdb.org/t/p/w500/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg");
        movie1.put("description", "Prueba de streaming 4K local");
        metas.add(movie1);

        Map<String, Object> response = new HashMap<>();
        response.put("metas", metas);
        return ResponseEntity.ok(response);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/stream/{type}/{id}.json")
    public ResponseEntity<Map<String, Object>> getStream(@PathVariable String type, @PathVariable String id) {
        List<Map<String, Object>> streams = new ArrayList<>();

        if (id.equals("tt6263850")) {
            Map<String, Object> stream = new HashMap<>();
            stream.put("name", "TorrServer");
            stream.put("title", "PC Local Directo (4K)");

            // Tu magnet original (que sacarías de tu base de datos)
            String magnet = "magnet:?xt=urn:btih:bf182eefd20fcbccd02fc3da2b50c899cd690378&dn=How.to.train.your.dragon.2025.2160p.x265.hdr.5.1-dual-lat-cinecalidad.rs.mkv&tr=udp...";

            // 1. Extraemos el hash del magnet automáticamente
            String hash = extraerHash(magnet);

            // 2. Construimos la URL directa usando tu IP y el hash extraído
            String urlDirecta = "http://" + MI_IP_LOCAL + ":" + MOTOR_TORRENT_PORT + "/stream/video.mkv?link=" + hash + "&index=1&play";

            stream.put("url", urlDirecta);
            streams.add(stream);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("streams", streams);
        return ResponseEntity.ok(response);
    }

    // Esta es la herramienta nueva que saca el texto clave del magnet
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
}