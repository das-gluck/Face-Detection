package com.demo.facedetection.controller;

import com.demo.facedetection.dto.VideoUploadResponse;
import com.demo.facedetection.entity.Video;
import com.demo.facedetection.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

    private final VideoService videoService;

    /**
     * POST /api/video/upload
     *
     * Accepts a video file, saves it, kicks off async processing,
     * and immediately returns a 202 Accepted with the video ID.
     * The client can poll GET /api/video/{id}/status to check progress.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoUploadResponse> upload(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Received upload: {} ({} bytes)",
                file.getOriginalFilename(), file.getSize());

        try {
            VideoUploadResponse response = videoService.upload(file);
            return ResponseEntity.accepted().body(response);
        } catch (IOException e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/video/stream?videoId={id}
     *
     * Streams the processed MP4 file. The client can use this URL as
     * a <video src="..."> once processing is complete.
     */
    @GetMapping("/stream")
    public ResponseEntity<Resource> stream(@RequestParam Long videoId) {
        try {
            Resource resource = videoService.getProcessedVideo(videoId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"processed_" + videoId + ".mp4\"")
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .body(resource);
        } catch (IllegalStateException e) {
            // Not ready yet
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(null);
        } catch (RuntimeException e) {
            log.error("Stream error: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/video/{id}/status
     *
     * Returns the current processing status so the frontend can poll.
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> status(@PathVariable Long id) {
        return videoService.findById(id)
                .map(v -> ResponseEntity.ok(Map.of(
                        "videoId", String.valueOf(v.getId()),
                        "status", v.getStatus().name(),
                        "filename", v.getOriginalFilename()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
