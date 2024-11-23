package com.stream.app.services.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.stream.app.entities.Video;
import com.stream.app.repositories.VideoRepository;
import com.stream.app.services.VideoService;

import jakarta.annotation.PostConstruct;

@Service
public class VideoServiceImpl implements VideoService {

    @Value("${files.video}")
    String DIR;

    @Value("${files.video.hls}")
    String HLS_DIR;

    @Autowired
    private VideoRepository mVideoRepository;

    @PostConstruct
    public void init() {

        File file = new File(DIR);
        if (!file.exists()) {
            file.mkdirs();
        }

        file = new File(HLS_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    @Override
    public Video save(Video video, MultipartFile file) {
        try {
            
            // folder path : create
            String filename = file.getOriginalFilename();
            String contentType = file.getContentType();
            InputStream inputStream = file.getInputStream();

            String clearFileName = StringUtils.cleanPath(filename);
            String cleanFolder = StringUtils.cleanPath(DIR);

            Path path = Paths.get(cleanFolder, clearFileName);

            // copy file to the folder
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);

            // video meta data
            video.setContentType(contentType);
            video.setFilePath(StringUtils.cleanPath(path.toString()));

            // metadata save
            Video saveVideo = mVideoRepository.save(video);

            //processVideo(saveVideo.getVideoId());

            return saveVideo;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        
    }

    @Override
    public Video get(String videoId) {
        
        Video video = mVideoRepository.findById(videoId)
            .orElseThrow(() -> new RuntimeException("Video not found"));

        return video;
    }

    @Override
    public Video getByTitle(String title) {
        return mVideoRepository.findByTitle(title)
            .orElseThrow(() -> new RuntimeException("Video not found"));
    }

    @Override
    public List<Video> getAll() {
        return mVideoRepository.findAll();
    }

    @Override
    public String processVideo(String videoId) {

        // get video from db
        Video video = this.get(videoId);
        String filePath = StringUtils.cleanPath(video.getFilePath());

        Path videoPath = Paths.get(filePath);

        String outputPath = StringUtils.cleanPath(HLS_DIR + videoId);
        // String output360p = StringUtils.cleanPath(HLS_DIR + videoId + "/360p/");
        // String output720p = StringUtils.cleanPath(HLS_DIR + videoId + "/720p/");
        // String output1080p = StringUtils.cleanPath(HLS_DIR + videoId + "/1080p/");
        
        try {
            Files.createDirectories(Paths.get(outputPath));
            // Files.createDirectories(Paths.get(output360p));
            // Files.createDirectories(Paths.get(output720p));
            // Files.createDirectories(Paths.get(output1080p));

            // Ffmpeg command
            
            String ffmpegCmd = String.format(
                    "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/segment_%%3d.ts\"  \"%s/master.m3u8\" ",
                    videoPath, outputPath, outputPath
            );

            // StringBuilder ffmpegCmd = new StringBuilder();
            // ffmpegCmd.append("ffmpeg  -i ")
            //         .append(videoPath.toString())
            //         .append(" -c:v libx264 -c:a aac")
            //         .append(" ")
            //         .append("-map 0:v -map 0:a -s:v:0 640x360 -b:v:0 800k ")
            //         .append("-map 0:v -map 0:a -s:v:1 1280x720 -b:v:1 2800k ")
            //         .append("-map 0:v -map 0:a -s:v:2 1920x1080 -b:v:2 5000k ")
            //         .append("-var_stream_map \"v:0,a:0 v:1,a:0 v:2,a:0\" ")
            //         .append("-master_pl_name ").append(HLS_DIR).append(videoId).append("/master.m3u8 ")
            //         .append("-f hls -hls_time 10 -hls_list_size 0 ")
            //         .append("-hls_segment_filename \"").append(HLS_DIR).append(videoId).append("/v%v/fileSequence%d.ts\" ")
            //         .append("\"").append(HLS_DIR).append(videoId).append("/v%v/prog_index.m3u8\"");


            System.out.println(ffmpegCmd);
            //file this command
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", ffmpegCmd);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("video processing failed!!");
            }

            return videoId;

        } catch (IOException e) {
            throw new RuntimeException("Video processing fail");
        } catch (InterruptedException e) {
            throw new RuntimeException();
        }

    }
    
}
