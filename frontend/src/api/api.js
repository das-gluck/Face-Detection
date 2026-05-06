import axios from 'axios';

const BASE = process.env.REACT_APP_API_URL || '/api';

const api = axios.create({ baseURL: BASE });

/** Upload a video file. Returns { videoId, status, message }. */
export async function uploadVideo(file, onProgress) {
  const form = new FormData();
  form.append('file', file);
  const { data } = await api.post('/video/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => onProgress && onProgress(Math.round((e.loaded / e.total) * 100)),
  });
  return data;
}

/** Poll processing status for a video. Returns { videoId, status, filename }. */
export async function getVideoStatus(videoId) {
  const { data } = await api.get(`/video/${videoId}/status`);
  return data;
}

/** Returns the URL to use as <video src="..."> */
export function getStreamUrl(videoId) {
  return `${BASE}/video/stream?videoId=${videoId}`;
}

/** Get ROI data for a video. Returns array of { frameNumber, x, y, width, height, confidence }. */
export async function getROIs(videoId) {
  const { data } = await api.get('/roi', { params: { videoId } });
  return data;
}
