//package com.demo.facedetection.service;
//
//import ai.djl.ModelException;
//import ai.djl.inference.Predictor;
//import ai.djl.modality.cv.Image;
//import ai.djl.modality.cv.ImageFactory;
//import ai.djl.modality.cv.output.BoundingBox;
//import ai.djl.modality.cv.output.DetectedObjects;
//import ai.djl.repository.zoo.Criteria;
//import ai.djl.repository.zoo.ZooModel;
//import ai.djl.training.util.ProgressBar;
//import ai.djl.translate.TranslateException;
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.awt.image.BufferedImage;
//import java.io.IOException;
//import java.util.Optional;
//
///**
// * Face detection using DJL's UltraFace-320 model (PyTorch backend).
// *
// * FIX: Removed Application.CV.FACE_DETECTION (causes compile error in some DJL versions).
// * Instead, we load the model directly by groupId + artifactId from the DJL model zoo.
// * DJL resolves the correct translator automatically from the model's metadata.json.
// *
// * NO OpenCV — pure Java/JNI via DJL.
// */
//@Service
//@Slf4j
//public class FaceDetectionService {
//
//    private ZooModel<Image, DetectedObjects> model;
//
//    @PostConstruct
//    public void loadModel() {
//        try {
//            log.info("Loading UltraFace-320 model from DJL model zoo...");
//
//            Criteria<Image, DetectedObjects> criteria = Criteria.builder()
//                    .setTypes(Image.class, DetectedObjects.class)
//                    .optEngine("PyTorch")
//                    // Load directly by group + artifact — no Application.CV enum needed.
//                    // DJL model zoo picks the right UltraFace-320 translator automatically.
//                    .optGroupId("ai.djl.pytorch")
//                    .optArtifactId("ultraface-320")
//                    .optProgress(new ProgressBar())
//                    .build();
//
//            model = criteria.loadModel();
//            log.info("Face detection model loaded successfully.");
//
//        } catch (IOException | ModelException e) {
//            log.error("Failed to load model: {}", e.getMessage(), e);
//            model = null;
//            // App still starts — frames without a model return Optional.empty()
//        }
//    }
//
//    @PreDestroy
//    public void closeModel() {
//        if (model != null) {
//            model.close();
//        }
//    }
//
//    /**
//     * Detects the single highest-confidence face in one video frame.
//     *
//     * @param frame  standard Java BufferedImage — no OpenCV type needed
//     * @return pixel-space bounding box, or empty if no face found
//     */
//    public Optional<FaceBox> detectFace(BufferedImage frame) {
//        if (model == null) {
//            log.warn("Model not loaded — skipping frame.");
//            return Optional.empty();
//        }
//
//        try {
//            Image djlImage = ImageFactory.getInstance().fromImage(frame);
//
//            try (Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
//                DetectedObjects detections = predictor.predict(djlImage);
//
//                if (detections.getNumberOfObjects() == 0) {
//                    return Optional.empty();
//                }
//
//                // item(0) = highest confidence detection
//                DetectedObjects.DetectedObject best = detections.item(0);
//                BoundingBox bb = best.getBoundingBox();
//                ai.djl.modality.cv.output.Rectangle rect = bb.getBounds();
//
//                // UltraFace outputs normalised [0,1] coords — scale to pixel space
//                int W = frame.getWidth();
//                int H = frame.getHeight();
//
//                int x      = (int) Math.max(0,   rect.getX()      * W);
//                int y      = (int) Math.max(0,   rect.getY()      * H);
//                int width  = (int) Math.min(W - x, rect.getWidth()  * W);
//                int height = (int) Math.min(H - y, rect.getHeight() * H);
//
//                float confidence = (float) best.getProbability();
//                log.debug("Detected face at ({},{}) {}x{} conf={}", x, y, width, height, confidence);
//
//                return Optional.of(new FaceBox(x, y, width, height, confidence));
//            }
//
//        } catch (TranslateException e) {
//            log.warn("Inference error: {}", e.getMessage());
//            return Optional.empty();
//        }
//    }
//
//    /** Pixel-space bounding box + confidence from UltraFace. */
//    public record FaceBox(int x, int y, int width, int height, float confidence) {}
//}



package com.demo.facedetection.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * Pure-Java face detection using skin-color analysis.
 *
 * No DJL, no OpenCV, no native dependencies — only java.awt.image.BufferedImage.
 *
 * Algorithm:
 *   1. Scan every other pixel (speed optimisation)
 *   2. Classify each pixel as "skin" using an RGB rule-based filter
 *      (covers a wide range of human skin tones in reasonable lighting)
 *   3. Track the bounding box of all skin pixels combined
 *   4. Apply sanity checks (minimum size, minimum skin-pixel ratio)
 *   5. Return the bounding box as a FaceBox
 *
 * Accuracy: good enough for a demo / interview project.
 * For production replace this class body with a real model (DJL, TF, ONNX, etc.)
 * — the public API (detectFace / FaceBox) stays identical, nothing else changes.
 */
@Service
@Slf4j
public class FaceDetectionService {

    // Minimum fraction of sampled pixels that must be skin for us to report a face
    private static final float MIN_SKIN_RATIO = 0.008f;

    // Minimum bounding box side length in pixels
    private static final int MIN_BOX_SIZE = 30;

    /**
     * Detects the primary skin region in a video frame and returns its bounding box.
     *
     * @param frame  standard Java BufferedImage — no third-party types
     * @return bounding box of detected face region, or empty if none found
     */
    public Optional<FaceBox> detectFace(BufferedImage frame) {
        if (frame == null) return Optional.empty();

        int W = frame.getWidth();
        int H = frame.getHeight();

        int minX = W, minY = H, maxX = 0, maxY = 0;
        int skinCount = 0;
        int sampledCount = 0;

        // Sample every 2nd pixel in both axes — 4× speedup, negligible accuracy loss
        for (int y = 0; y < H; y += 2) {
            for (int x = 0; x < W; x += 2) {
                sampledCount++;
                int rgb = frame.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >>  8) & 0xFF;
                int b =  rgb        & 0xFF;

                if (isSkin(r, g, b)) {
                    skinCount++;
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        // Not enough skin pixels → no face
        if (sampledCount == 0 || (float) skinCount / sampledCount < MIN_SKIN_RATIO) {
            log.debug("No face: skin ratio {}/{} too low", skinCount, sampledCount);
            return Optional.empty();
        }

        int boxW = maxX - minX;
        int boxH = maxY - minY;

        // Box too small → noise, not a face
        if (boxW < MIN_BOX_SIZE || boxH < MIN_BOX_SIZE) {
            log.debug("No face: bounding box {}x{} too small", boxW, boxH);
            return Optional.empty();
        }

        // Confidence = capped ratio of skin pixels vs sampled pixels
        float confidence = Math.min(0.99f, (float) skinCount / sampledCount * 12f);

        log.debug("Face detected at ({},{}) {}x{} conf={}", minX, minY, boxW, boxH, confidence);
        return Optional.of(new FaceBox(minX, minY, boxW, boxH, confidence));
    }

    /**
     * Rule-based skin-color classifier in RGB space.
     *
     * Conditions (from Kovac et al. + Peer et al. combined):
     *   - R, G, B all above minimum thresholds
     *   - Red channel dominates (skin is redder than green or blue)
     *   - Red-green difference above threshold (avoids grey/white regions)
     *   - Red substantially above the darker of G/B
     *
     * Works for a wide range of skin tones under normal indoor/outdoor lighting.
     */
    private boolean isSkin(int r, int g, int b) {
        return r > 95
            && g > 40
            && b > 20
            && r > g
            && r > b
            && Math.abs(r - g) > 15
            && (r - Math.min(g, b)) > 20;
    }

    /** Pixel-space bounding box + confidence (0–1). */
    public record FaceBox(int x, int y, int width, int height, float confidence) {}
}
