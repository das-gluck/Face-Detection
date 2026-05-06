import { useState, useEffect, useRef } from 'react';
import { getVideoStatus, getStreamUrl } from '../api/api';

const STATUS_COLORS = {
  UPLOADED:   '#94a3b8',
  PROCESSING: '#fbbf24',
  COMPLETED:  '#34d399',
  FAILED:     '#f87171',
};

const STATUS_LABELS = {
  UPLOADED:   'Queued',
  PROCESSING: 'Processing…',
  COMPLETED:  'Ready',
  FAILED:     'Failed',
};

const styles = {
  card: {
    background: '#1e293b',
    borderRadius: 12,
    padding: '2rem',
    border: '1px solid #334155',
  },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' },
  title: { margin: 0, fontSize: 20, fontWeight: 600, color: '#f1f5f9' },
  badge: (status) => ({
    padding: '3px 10px',
    borderRadius: 99,
    fontSize: 12,
    fontWeight: 600,
    color: '#0f172a',
    background: STATUS_COLORS[status] || '#94a3b8',
  }),
  video: {
    width: '100%',
    borderRadius: 8,
    background: '#000',
    maxHeight: 400,
  },
  placeholder: {
    height: 260,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    background: '#0f172a',
    borderRadius: 8,
    color: '#475569',
  },
  spinner: {
    width: 36,
    height: 36,
    border: '3px solid #334155',
    borderTopColor: '#fbbf24',
    borderRadius: '50%',
    animation: 'spin 0.8s linear infinite',
    marginBottom: 12,
  },
  meta: { marginTop: '0.75rem', fontSize: 12, color: '#64748b' },
};

export default function VideoPlayer({ videoInfo }) {
  const [status, setStatus] = useState(videoInfo?.status || 'UPLOADED');
  const [streamUrl, setStreamUrl] = useState(null);
  const pollRef = useRef(null);

  useEffect(() => {
    if (!videoInfo?.videoId) return;
    if (videoInfo.status === 'COMPLETED') {
      setStatus('COMPLETED');
      setStreamUrl(getStreamUrl(videoInfo.videoId));
      return;
    }

    // Poll every 3s until COMPLETED or FAILED
    pollRef.current = setInterval(async () => {
      try {
        const s = await getVideoStatus(videoInfo.videoId);
        setStatus(s.status);
        if (s.status === 'COMPLETED') {
          clearInterval(pollRef.current);
          setStreamUrl(getStreamUrl(videoInfo.videoId));
        } else if (s.status === 'FAILED') {
          clearInterval(pollRef.current);
        }
      } catch {
        // ignore transient network errors
      }
    }, 3000);

    return () => clearInterval(pollRef.current);
  }, [videoInfo]);

  if (!videoInfo) {
    return (
      <div style={styles.card}>
        <h2 style={{ ...styles.title, marginBottom: '1.5rem' }}>Processed Video</h2>
        <div style={styles.placeholder}>
          <span style={{ fontSize: 40, marginBottom: 10 }}>📹</span>
          <span>Upload a video to see it here</span>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.card}>
      <div style={styles.header}>
        <h2 style={styles.title}>Processed Video</h2>
        <span style={styles.badge(status)}>{STATUS_LABELS[status] || status}</span>
      </div>

      {streamUrl ? (
        <>
          <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
          <video
            key={streamUrl}
            style={styles.video}
            controls
            src={streamUrl}
          />
          <p style={styles.meta}>
            Video ID: {videoInfo.videoId} · {videoInfo.filename}
          </p>
        </>
      ) : (
        <div style={styles.placeholder}>
          <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
          {status === 'PROCESSING' || status === 'UPLOADED' ? (
            <>
              <div style={styles.spinner} />
              <span>Running face detection on frames…</span>
              <span style={{ fontSize: 12, marginTop: 6, color: '#475569' }}>
                Large videos may take a few minutes
              </span>
            </>
          ) : status === 'FAILED' ? (
            <>
              <span style={{ fontSize: 36, marginBottom: 8 }}>❌</span>
              <span style={{ color: '#f87171' }}>Processing failed</span>
            </>
          ) : null}
        </div>
      )}
    </div>
  );
}
