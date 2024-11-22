package com.stream.app.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stream.app.entities.Video;

@Repository
public interface VideoRepository extends JpaRepository<Video, String> {
    
    Optional<Video> findByTitle(String title);

}
