package com.mario.stremio_backend;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity // Esto le dice a la base de datos: "Crea una tabla con esta forma"
public class Pelicula {

    @Id
    private String imdbId; // Ej: tt6263850

    private String titulo; // Ej: Deadpool & Wolverine

    private String posterUrl;

    @Column(length = 2000) // Los magnets son textos muy largos, necesitamos darle espacio
    private String magnetLink;

    // Constructores vacíos por defecto (necesario para la base de datos)
    public Pelicula() {}

    public Pelicula(String imdbId, String titulo, String posterUrl, String magnetLink) {
        this.imdbId = imdbId;
        this.titulo = titulo;
        this.posterUrl = posterUrl;
        this.magnetLink = magnetLink;
    }

    // --- GETTERS Y SETTERS ---
    public String getImdbId() { return imdbId; }
    public void setImdbId(String imdbId) { this.imdbId = imdbId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public String getMagnetLink() { return magnetLink; }
    public void setMagnetLink(String magnetLink) { this.magnetLink = magnetLink; }
}