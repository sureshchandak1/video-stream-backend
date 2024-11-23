package com.stream.app.controllers;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.stream.app.AppConstants;
import com.stream.app.entities.Video;
import com.stream.app.playload.CustomMessage;
import com.stream.app.services.VideoService;

@RestController
@RequestMapping("/api/v1/videos")
@CrossOrigin("*")
public class VideoController {

    private VideoService mVideoService;
    
    public VideoController(VideoService mVideoService) {
        this.mVideoService = mVideoService;
    }

    // video upload
    @PostMapping
    public ResponseEntity<?> create(
        @RequestParam("file") MultipartFile file,
        @RequestParam("title") String title,
        @RequestParam("description") String description
    ) {

        Video video = new Video();
        video.setVideoId(UUID.randomUUID().toString());
        video.setTitle(title);
        video.setDescription(description);

        Video savedVideo = mVideoService.save(video, file);

        if (savedVideo != null) {
            return ResponseEntity.status(HttpStatus.OK).body(video);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    CustomMessage.builder().message("Video not uploaded").success(false).build()
                );
        }

    }

    // get all videos
    @GetMapping
    public List<Video> getAll() {
        return mVideoService.getAll();
    }

    // stream video
    @GetMapping("/stream/{videoId}")
    public ResponseEntity<Resource> stream(@PathVariable String videoId) {

        System.out.println("Video ID: " + videoId);

        // find video from db
        Video video = mVideoService.get(videoId);

        String contentType = video.getContentType();
        String filePath = StringUtils.cleanPath(video.getFilePath());

        System.out.println("File path: " + filePath);

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        Resource resource = new FileSystemResource(filePath);

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    // stream video in chunks
    @GetMapping("/stream/range/{videoId}")
    public ResponseEntity<Resource> streamVideoRange(
        @PathVariable String videoId,
        @RequestHeader(value = "Range", required = false) String range
     ) {

        System.out.println("Range Header: " + range);

        // fetch video from db
        Video video = mVideoService.get(videoId);

        String contentType = video.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        Path path = Paths.get(StringUtils.cleanPath(video.getFilePath()));

        Resource resource = new FileSystemResource(path);

        if (range == null) {
            return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
        }

        long fileLength = path.toFile().length();

        String[] ranges = range.replace("bytes=", "").split("-");

        long rangeStart = Long.parseLong(ranges[0]);
        long rangeEnd;
        // if (ranges.length > 1) {
        //     rangeEnd = Long.parseLong(ranges[1]);
        // } else {
        //     rangeEnd = fileLength - 1;
        // }

        // if (rangeEnd > fileLength - 1) {
        //     rangeEnd = fileLength - 1;
        // }

        rangeEnd = rangeStart + AppConstants.CHUNK_SIZE - 1;
        if (rangeEnd >= fileLength) {
            rangeEnd = fileLength - 1;
        }

        System.out.println("Range Start: " + rangeStart);
        System.out.println("Range End: " + rangeEnd);

        try (InputStream inputStream = Files.newInputStream(path)) {

            inputStream.skip(rangeStart);
            long contentLength = rangeEnd - rangeStart + 1;

            byte[] data = new byte[(int) contentLength];
            int read = inputStream.read(data, 0, data.length);
            System.out.println("Read(number of bytes): " + read);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            headers.add("X-Content-Type-Options", "nosniff");
            headers.setContentLength(contentLength);

            // return ResponseEntity
            //         .status(HttpStatus.PARTIAL_CONTENT)
            //         .headers(headers)
            //         .contentType(MediaType.parseMediaType(contentType))
            //         .body(new InputStreamResource(inputStream));

            return ResponseEntity
                    .status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new ByteArrayResource(data));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

}
