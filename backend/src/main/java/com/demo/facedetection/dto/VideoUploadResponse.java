package com.demo.facedetection.dto;

import com.demo.facedetection.entity.Video;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Returned immediately after upload so the frontend can poll status. */
@Data @Builder
public class VideoUploadResponse {
    private Long videoId;
    private String filename;
    private Video.VideoStatus status;
    private LocalDateTime uploadedAt;
    private String message;
}
