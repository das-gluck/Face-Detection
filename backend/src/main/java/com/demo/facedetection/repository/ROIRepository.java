package com.demo.facedetection.repository;

import com.demo.facedetection.entity.ROIData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ROIRepository extends JpaRepository<ROIData, Long> {

    /** All ROI rows for a given video, ordered by frame number */
    List<ROIData> findByVideoIdOrderByFrameNumberAsc(Long videoId);
}
