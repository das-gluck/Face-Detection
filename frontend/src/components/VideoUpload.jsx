import { useState, useRef } from 'react';
import { uploadVideo } from '../api/api';

const styles = {
  card: {
    background: '#1e293b',
    borderRadius: 12,
    padding: '2rem',
    border: '1px solid #334155',
  },
  title: { margin: '0 0 1.5rem', fontSize: 20, fontWeight: 600, color: '#f1f5f9' },
  dropzone: {
    border: '2px dashed #475569',
    borderRadius: 10,
    padding: '3rem 2rem',
    textAlign: 'center',
    cursor: 'pointer',
    transition: 'all 0.2s',
    background: '#0f172a',
  },
  dropzoneActive: { borderColor: '#38bdf8', background: '#0c2540' },
  label: { display: 'block', fontSize: 15, color: '#94a3b8', marginTop: 8 },
  icon: { fontSize: 40, marginBottom: 12 },
  fileName: { margin: '1rem 0 0', color: '#38bdf8', fontSize: 14 },
  btn: {
    marginTop: '1.5rem',
    padding: '0.65rem 1.8rem',
    background: '#0ea5e9',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    fontSize: 15,
    fontWeight: 600,
    cursor: 'pointer',
    width: '100%',
    transition: 'background 0.15s',
  },
  btnDisabled: { background: '#475569', cursor: 'not-allowed' },
  progress: {
    marginTop: '1rem',
    height: 6,
    background: '#334155',
    borderRadius: 99,
    overflow: 'hidden',
  },
  progressBar: { height: '100%', background: '#0ea5e9', transition: 'width 0.3s' },
  error: { marginTop: 12, color: '#f87171', fontSize: 13 },
};

export default function VideoUpload({ onUploadComplete }) {
  const [file, setFile] = useState(null);
  const [progress, setProgress] = useState(0);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [dragging, setDragging] = useState(false);
  const inputRef = useRef();

  function handleFile(f) {
    if (!f) return;
    if (!f.type.startsWith('video/')) {
      setError('Please select a video file (mp4, avi, mov…)');
      return;
    }
    setError('');
    setFile(f);
  }

  function onDrop(e) {
    e.preventDefault();
    setDragging(false);
    handleFile(e.dataTransfer.files[0]);
  }

  async function handleUpload() {
    if (!file) return;
    setUploading(true);
    setError('');
    setProgress(0);
    try {
      const result = await uploadVideo(file, setProgress);
      onUploadComplete(result);
    } catch (err) {
      setError(err.response?.data?.message || 'Upload failed. Is the backend running?');
    } finally {
      setUploading(false);
    }
  }

  const dzStyle = {
    ...styles.dropzone,
    ...(dragging ? styles.dropzoneActive : {}),
  };

  return (
    <div style={styles.card}>
      <h2 style={styles.title}>Upload Video</h2>

      <div
        style={dzStyle}
        onClick={() => inputRef.current.click()}
        onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
      >
        <div style={styles.icon}>🎬</div>
        <span style={{ color: '#cbd5e1', fontSize: 15 }}>
          Drag & drop a video, or <u>click to browse</u>
        </span>
        <span style={styles.label}>MP4, AVI, MOV, MKV (up to 500 MB)</span>
        {file && <p style={styles.fileName}>{file.name} ({(file.size / 1e6).toFixed(1)} MB)</p>}
      </div>

      <input
        ref={inputRef}
        type="file"
        accept="video/*"
        style={{ display: 'none' }}
        onChange={(e) => handleFile(e.target.files[0])}
      />

      {uploading && (
        <div style={styles.progress}>
          <div style={{ ...styles.progressBar, width: `${progress}%` }} />
        </div>
      )}
      {error && <p style={styles.error}>{error}</p>}

      <button
        style={{ ...styles.btn, ...((!file || uploading) ? styles.btnDisabled : {}) }}
        onClick={handleUpload}
        disabled={!file || uploading}
      >
        {uploading ? `Uploading… ${progress}%` : 'Upload & Process'}
      </button>
    </div>
  );
}
