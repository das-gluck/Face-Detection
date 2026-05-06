# Face-Detection
A full-stack web application that processes uploaded videos to detect faces in each frame, draws bounding boxes around detected faces, and presents the annotated frames in a visual dashboard.

Face Detection Demo
This project is a web application that processes videos to detect faces in each frame and draw boxes around them.

Tech Stack:
Backend: Java, Spring Boot
Database: PostgreSQL
Frontend: React
Video Processing: FFmpeg

How It Works:
1. User uploads a video.
2. FFmpeg breaks the video into frames (images).
3. Each frame is checked using a simple skin-color detection method.
4. If a face is detected, a box is drawn on the frame.
5. All frames are combined back into a video.
6. The final video is stored and shown in the frontend.

APIs:
POST /api/video/upload
Upload video

GET /api/video/{id}/status
Check processing status

GET /api/video/stream
View processed video

GET /api/roi?videoId={id}
Get detected face data

Database:
Video table stores video details.
ROI table stores face detection data.

Notes:
1. FFmpeg — A command-line video tool called from Java. It does two jobs: chop the video into individual JPEGs at 10 frames per second(this is what i am doing), and stitch them back together into an MP4 after annotation. Your Java code doesn't process video bytes directly — it just fires FFmpeg as a subprocess.
2. Face detection (skin-colour classifier) — No AI, no ML model. Just math on RGB pixel values. Each pixel is tested against 6 rules (e.g. red must dominate, there must be a noticeable red-green difference). If enough pixels in a frame pass the test, you have a face. Fast, zero dependencies, works entirely in Java.
3. Java Graphics2D — Comes built into the JDK, no extra library. After a face is found, it draws the green rectangle, corner marks, glow effect, and confidence label directly onto the JPEG image in memory before saving it to disk.

Pipeline at a glance -:
Upload MP4 -> FFmpeg split -> Skin detect -> Draw box -> FFmpeg join (frame) -> save Db

