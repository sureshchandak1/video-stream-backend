package com.stream.app.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.stream.app.services.VideoService;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

    private VideoService mVideoService;
    
    public VideoController(VideoService mVideoService) {
        this.mVideoService = mVideoService;
    }

    @PostMapping
    public ResponseEntity<CustomMessage> create(
        @RequestParam("file") MultipartFile file,
        @RequestParam("title") String title,
        @RequestParam("description") String description
    ) {

    }

}
