package com.demo.facedetection.service;

import com.demo.facedetection.dto.ROIDataDTO;
import com.demo.facedetection.entity.ROIData;
import com.demo.facedetection.entity.Video;
import com.demo.facedetection.repository.ROIRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ROIService {

    private final ROIRepository roiRepository;

    @Transactional
    public ROIData save(Video video,
                        int frameNumber,
                        FaceDetectionService.FaceBox box) {

        ROIData roi = ROIData.builder()
                .video(video)
                .frameNumber(frameNumber)
                .boxX(box.x())
                .boxY(box.y())
                .boxWidth(box.width())
                .boxHeight(box.height())
                .confidence(box.confidence())
                .detectedAt(LocalDateTime.now())
                .build();

        return roiRepository.save(roi);
    }

    @Transactional(readOnly = true)
    public List<ROIDataDTO> getROIsForVideo(Long videoId) {
        return roiRepository
                .findByVideoIdOrderByFrameNumberAsc(videoId)
                .stream()
                .map(ROIDataDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ROIDataDTO> getAllROIs() {
        return roiRepository.findAll()
                .stream()
                .map(ROIDataDTO::from)
                .toList();
    }
}
