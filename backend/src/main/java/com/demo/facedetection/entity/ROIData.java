package com.demo.facedetection.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * One row per frame where a face was detected.
 * Bounding box coords are in absolute pixels relative to the original frame size.
 */
@Entity
@Table(name = "roi_data", indexes = {
    @Index(name = "idx_roi_video", columnList = "video_id"),
    @Index(name = "idx_roi_frame", columnList = "video_id, frameNumber")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ROIData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(nullable = false)
    private Integer frameNumber;

    // Axis-aligned bounding box — top-left corner + dimensions (pixels)
    @Column(nullable = false)
    private Integer boxX;

    @Column(nullable = false)
    private Integer boxY;

    @Column(nullable = false)
    private Integer boxWidth;

    @Column(nullable = false)
    private Integer boxHeight;

    /** Confidence score from the face detector (0.0 – 1.0) */
    private Float confidence;

    /** Wall-clock timestamp when this frame was processed */
    @Column(nullable = false)
    private LocalDateTime detectedAt;
}
