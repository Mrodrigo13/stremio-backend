package com.mario.stremio_backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PeliculaRepository extends JpaRepository<Pelicula, String> {
    // Esto hereda automáticamente los métodos para buscar, guardar y borrar.
}