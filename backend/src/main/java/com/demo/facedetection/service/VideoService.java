package com.demo.facedetection.service;

import com.demo.facedetection.dto.VideoUploadResponse;
import com.demo.facedetection.entity.Video;
import com.demo.facedetection.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the full video-processing workflow:
 *
 *  POST /upload  →  saveFile()  →  (async) processVideo()
 *                                     │
 *                                     ├─ extractFrames()
 *                                     ├─ for each frame: detectFace() → drawBoundingBox() → saveROI()
 *                                     └─ reassembleVideo()
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VideoService {

    @Value("${app.upload-dir}")
    private String uploadDir;

    private final VideoRepository videoRepository;
    private final FaceDetectionService faceDetectionService;
    private final FrameProcessingService frameProcessingService;
    private final ROIService roiService;

    // ──────────────────────────────────────────────
    // Upload + kick-off async processing
    // ──────────────────────────────────────────────

    @Transactional
    public VideoUploadResponse upload(MultipartFile file) throws IOException {
        // 1. Persist to disk
        Files.createDirectories(Paths.get(uploadDir));
        String storageName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path storagePath = Paths.get(uploadDir, storageName);
        file.transferTo(storagePath);
        log.info("Saved upload: {}", storagePath);

        // 2. Create DB record with UPLOADED status
        Video video = Video.builder()
                .originalFilename(file.getOriginalFilename())
                .storagePath(storagePath.toAbsolutePath().toString())
                .status(Video.VideoStatus.UPLOADED)
                .uploadedAt(LocalDateTime.now())
                .build();
        video = videoRepository.save(video);

        // 3. Kick off background processing (returns immediately to caller)
        processVideoAsync(video.getId());

        return VideoUploadResponse.builder()
                .videoId(video.getId())
                .filename(file.getOriginalFilename())
                .status(Video.VideoStatus.UPLOADED)
                .uploadedAt(video.getUploadedAt())
                .message("Upload successful. Processing started in background.")
                .build();
    }

    // ──────────────────────────────────────────────
    // Async processing pipeline
    // ──────────────────────────────────────────────

    @Async("videoProcessingExecutor")
    public void processVideoAsync(Long videoId) {
        log.info("Starting async processing for videoId={}", videoId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        video.setStatus(Video.VideoStatus.PROCESSING);
        videoRepository.save(video);

        try {
            String videoUid = String.valueOf(videoId);

            // Step 1 — extract frames via FFmpeg
            Path rawFrameDir = frameProcessingService.extractFrames(
                    videoUid, video.getStoragePath());

            List<File> frames = frameProcessingService.listFrames(rawFrameDir);
            video.setTotalFrames(frames.size());
            log.info("Extracted {} frames for videoId={}", frames.size(), videoId);

            // Step 2 — create output directory for processed frames
            Path processedFrameDir = rawFrameDir.getParent().resolve("processed");
            Files.createDirectories(processedFrameDir);

            // Step 3 — per-frame: detect face → draw box → save ROI
            int frameNumber = 1;
            for (File frame : frames) {
            	int countFrame = frameNumber;
                javax.imageio.ImageIO.scanForPlugins(); // ensure decoders loaded

                var bufferedImage = javax.imageio.ImageIO.read(frame);
                if (bufferedImage == null) {
                    log.warn("Could not decode frame: {}", frame.getName());
                    frameNumber++;
                    continue;
                }

                Optional<FaceDetectionService.FaceBox> detectedBox =
                        faceDetectionService.detectFace(bufferedImage);

                // Draw (or skip drawing) and write the processed frame
                frameProcessingService.drawBoundingBox(
                        frame,
                        detectedBox.orElse(null),
                        processedFrameDir);

                // Persist ROI only when a face was found
                detectedBox.ifPresent(box -> roiService.save(video, countFrame, box));

                if (frameNumber % 50 == 0) {
                    log.debug("Processed frame {}/{}", frameNumber, frames.size());
                }
                frameNumber++;
            }

            // Step 4 — reassemble processed frames into MP4
            String outputPath = frameProcessingService.reassembleVideo(videoUid, processedFrameDir);

            video.setProcessedPath(outputPath);
            video.setStatus(Video.VideoStatus.COMPLETED);
            video.setProcessedAt(LocalDateTime.now());
            videoRepository.save(video);
            log.info("Processing complete for videoId={}. Output: {}", videoId, outputPath);

        } catch (Exception e) {
            log.error("Processing failed for videoId={}: {}", videoId, e.getMessage(), e);
            video.setStatus(Video.VideoStatus.FAILED);
            videoRepository.save(video);
        }
    }

    // ──────────────────────────────────────────────
    // Serve processed video
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Resource getProcessedVideo(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        if (video.getStatus() != Video.VideoStatus.COMPLETED || video.getProcessedPath() == null) {
            throw new IllegalStateException("Video not yet processed. Status: " + video.getStatus());
        }

        File file = new File(video.getProcessedPath());
        if (!file.exists()) {
            throw new RuntimeException("Processed file missing on disk: " + video.getProcessedPath());
        }

        return new FileSystemResource(file);
    }

    @Transactional(readOnly = true)
    public Optional<Video> findById(Long id) {
        return videoRepository.findById(id);
    }
}
