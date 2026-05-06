package com.demo.facedetection.controller;

import com.demo.facedetection.dto.ROIDataDTO;
import com.demo.facedetection.service.ROIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roi")
@RequiredArgsConstructor
@Slf4j
public class ROIController {

    private final ROIService roiService;

    /**
     * Injects the frames base directory from application.yml (app.frames-dir).
     * On your Ubuntu machine this is: /home/cdac/face-detection/frames
     */
    @Value("${app.frames-dir}")
    private String framesBaseDir;

    // ── 1. ROI list (unchanged) ──────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<ROIDataDTO>> getRois(
            @RequestParam(required = false) Long videoId) {

        List<ROIDataDTO> data = (videoId != null)
                ? roiService.getROIsForVideo(videoId)
                : roiService.getAllROIs();

        return ResponseEntity.ok(data);
    }

    // ── 2. NEW: serve a single processed frame as image/jpeg ─────────────────

    /**
     * GET /api/roi/frame-image?videoId=3&frameNumber=42
     *
     * Reads the already-annotated JPEG from:
     *   {app.frames-dir}/{videoId}/processed/frame_0042.jpg
     * and returns it as image/jpeg — no extra processing.
     *
     * The frontend ROI photo grid calls this URL for every detection card.
     */
    @GetMapping("/frame-image")
    public ResponseEntity<byte[]> getFrameImage(
            @RequestParam Long videoId,
            @RequestParam int frameNumber) {

        // Build path: e.g. /home/cdac/face-detection/frames/3/processed/frame_0042.jpg
        String filename = String.format("frame_%04d.jpg", frameNumber);
        Path framePath = Paths.get(framesBaseDir,
                                   String.valueOf(videoId),
                                   "processed",
                                   filename);

        File frameFile = framePath.toFile();

        if (!frameFile.exists()) {
            log.warn("Frame not found: {}", framePath);
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] imageBytes = Files.readAllBytes(framePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .body(imageBytes);
        } catch (IOException e) {
            log.error("Failed to read frame {}: {}", framePath, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/frame-debug")
    public ResponseEntity<Map<String, Object>> debugFramePath(
            @RequestParam Long videoId,
            @RequestParam int frameNumber) {
 
        String filename = String.format("frame_%04d.jpg", frameNumber);
        Path framePath = Paths.get(framesBaseDir,
                                   String.valueOf(videoId),
                                   "processed",
                                   filename);
 
        Map<String, Object> info = new java.util.LinkedHashMap<>();
        info.put("framesBaseDir",  framesBaseDir);
        info.put("builtPath",      framePath.toAbsolutePath().toString());
        info.put("fileExists",     framePath.toFile().exists());
        info.put("filename",       filename);
 
        return ResponseEntity.ok(info);
    }
}