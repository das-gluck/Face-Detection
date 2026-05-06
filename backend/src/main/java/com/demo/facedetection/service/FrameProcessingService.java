package com.demo.facedetection.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles the low-level frame pipeline:
 *   1. Uses FFmpeg CLI (via ProcessBuilder) to split a video into JPEG frames.
 *   2. Uses Java's own Graphics2D to draw bounding-box rectangles — no OpenCV.
 *   3. Uses FFmpeg CLI to reassemble processed frames back into an MP4.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FrameProcessingService {

    @Value("${app.frames-dir}")
    private String framesBaseDir;

    @Value("${app.processed-dir}")
    private String processedBaseDir;

    private static final int EXTRACT_FPS = 10; // extract 10 frames per second

    // ────────────────────────────────────────────────────────────
    // 1. Extract frames
    // ────────────────────────────────────────────────────────────

    /**
     * Runs ffmpeg to extract frames at EXTRACT_FPS from inputVideo.
     *
     * @return directory where raw JPEG frames were written (frame_0001.jpg …)
     */
    public Path extractFrames(String videoId, String inputVideoPath) throws IOException, InterruptedException {
        Path frameDir = Paths.get(framesBaseDir, videoId, "raw");
        Files.createDirectories(frameDir);

        // ffmpeg -i <input> -vf fps=10 <frameDir>/frame_%04d.jpg
        List<String> cmd = List.of(
                "ffmpeg", "-y",
                "-i", inputVideoPath,
                "-vf", "fps=" + EXTRACT_FPS,
                frameDir.resolve("frame_%04d.jpg").toString()
        );

        runProcess(cmd, "frame extraction");
        return frameDir;
    }

    /**
     * Returns sorted list of raw frame JPEG files from the given directory.
     */
    public List<File> listFrames(Path frameDir) {
        File[] files = frameDir.toFile().listFiles(
                (dir, name) -> name.startsWith("frame_") && name.endsWith(".jpg")
        );
        if (files == null) return List.of();
        List<File> sorted = new ArrayList<>(Arrays.asList(files));
        sorted.sort(java.util.Comparator.comparing(File::getName));
        return sorted;
    }

    // ────────────────────────────────────────────────────────────
    // 2. Draw bounding box using Java Graphics2D  (NOT OpenCV)
    // ────────────────────────────────────────────────────────────

    /**
     * Reads a JPEG frame, draws a green rectangle for the detected face,
     * and writes the result to the processed frames directory.
     *
     * Graphics2D is part of the standard JDK — zero third-party dependencies.
     *
     * @param frameFile  source JPEG
     * @param box        pixel-space bounding box (may be null → frame copied as-is)
     * @param processedDir target directory for annotated frames
     * @return the written output file
     */
    public File drawBoundingBox(File frameFile,
                                FaceDetectionService.FaceBox box,
                                Path processedDir) throws IOException {

        BufferedImage image = ImageIO.read(frameFile);

        if (box != null) {
            Graphics2D g2d = image.createGraphics();

            // Anti-aliasing for cleaner edges
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Outer glow effect: draw a slightly larger translucent rect first
            g2d.setColor(new Color(0, 255, 0, 60));
            g2d.setStroke(new BasicStroke(6));
            g2d.drawRect(box.x() - 2, box.y() - 2, box.width() + 4, box.height() + 4);

            // Main bounding box — bright green, 3 px
            g2d.setColor(new Color(0, 230, 0));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRect(box.x(), box.y(), box.width(), box.height());

            // Corner accent marks
            int mark = 12;
            g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // top-left
            g2d.drawLine(box.x(), box.y(), box.x() + mark, box.y());
            g2d.drawLine(box.x(), box.y(), box.x(), box.y() + mark);
            // top-right
            g2d.drawLine(box.x() + box.width() - mark, box.y(), box.x() + box.width(), box.y());
            g2d.drawLine(box.x() + box.width(), box.y(), box.x() + box.width(), box.y() + mark);
            // bottom-left
            g2d.drawLine(box.x(), box.y() + box.height() - mark, box.x(), box.y() + box.height());
            g2d.drawLine(box.x(), box.y() + box.height(), box.x() + mark, box.y() + box.height());
            // bottom-right
            g2d.drawLine(box.x() + box.width() - mark, box.y() + box.height(), box.x() + box.width(), box.y() + box.height());
            g2d.drawLine(box.x() + box.width(), box.y() + box.height() - mark, box.x() + box.width(), box.y() + box.height());

            // Confidence label
            String label = String.format("%.0f%%", box.confidence() * 100);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
            FontMetrics fm = g2d.getFontMetrics();
            int labelX = box.x();
            int labelY = box.y() - 6;
            // background pill
            g2d.setColor(new Color(0, 180, 0, 180));
            g2d.fillRoundRect(labelX - 2, labelY - fm.getAscent(), fm.stringWidth(label) + 6, fm.getHeight(), 4, 4);
            g2d.setColor(Color.WHITE);
            g2d.drawString(label, labelX + 1, labelY);

            g2d.dispose();
        }

        File outputFile = processedDir.resolve(frameFile.getName()).toFile();
        ImageIO.write(image, "jpg", outputFile);
        return outputFile;
    }

    // ────────────────────────────────────────────────────────────
    // 3. Reassemble processed frames → MP4
    // ────────────────────────────────────────────────────────────

    /**
     * Runs ffmpeg to encode all processed JPEG frames into an H.264 MP4.
     *
     * @param videoId         used to locate the frames directory
     * @param processedDir    directory containing annotated JPEGs
     * @return absolute path to the output MP4
     */
    public String reassembleVideo(String videoId, Path processedDir) throws IOException, InterruptedException {
        Path outputPath = Paths.get(processedBaseDir, videoId + "_processed.mp4");
        Files.createDirectories(outputPath.getParent());

        // ffmpeg -framerate 10 -i <processedDir>/frame_%04d.jpg -c:v libx264 -pix_fmt yuv420p output.mp4
        List<String> cmd = List.of(
                "ffmpeg", "-y",
                "-framerate", String.valueOf(EXTRACT_FPS),
                "-i", processedDir.resolve("frame_%04d.jpg").toString(),
                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",
                "-crf", "23",
                outputPath.toString()
        );

        runProcess(cmd, "video reassembly");
        return outputPath.toString();
    }

    // ────────────────────────────────────────────────────────────
    // Helper: run an external process and wait for it
    // ────────────────────────────────────────────────────────────

    private void runProcess(List<String> cmd, String stepName) throws IOException, InterruptedException {
        log.debug("Running {}: {}", stepName, String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Drain stdout+stderr so the process doesn't block on a full pipe buffer
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            log.error("FFmpeg {} failed (exit {}): {}", stepName, exitCode, output);
            throw new RuntimeException("FFmpeg " + stepName + " failed with exit code " + exitCode);
        }
        log.debug("FFmpeg {} completed successfully.", stepName);
    }
}
