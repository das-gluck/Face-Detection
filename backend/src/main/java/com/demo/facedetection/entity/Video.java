package com.demo.facedetection.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "videos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String storagePath;       // absolute path to original uploaded file

    private String processedPath;     // absolute path to processed output video

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoStatus status;

    private Integer totalFrames;      // populated after extraction

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    private LocalDateTime processedAt;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ROIData> roiData;

    public enum VideoStatus {
        UPLOADED, PROCESSING, COMPLETED, FAILED
    }
}
