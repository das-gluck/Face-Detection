package com.demo.facedetection.dto;

import com.demo.facedetection.entity.ROIData;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Lightweight view of a single ROI row — sent to the frontend. */
@Data @Builder
public class ROIDataDTO {

    private Long id;
    private Long videoId;
    private Integer frameNumber;
    private Integer x;
    private Integer y;
    private Integer width;
    private Integer height;
    private Float confidence;
    private LocalDateTime detectedAt;

    public static ROIDataDTO from(ROIData roi) {
        return ROIDataDTO.builder()
                .id(roi.getId())
                .videoId(roi.getVideo().getId())
                .frameNumber(roi.getFrameNumber())
                .x(roi.getBoxX())
                .y(roi.getBoxY())
                .width(roi.getBoxWidth())
                .height(roi.getBoxHeight())
                .confidence(roi.getConfidence())
                .detectedAt(roi.getDetectedAt())
                .build();
    }
}
