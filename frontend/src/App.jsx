import { useState } from 'react';
import VideoUpload from './components/VideoUpload';
import VideoPlayer from './components/VideoPlayer';
import ROIDisplay from './components/ROIDisplay';

const styles = {
  app: { minHeight: '100vh', padding: '2rem', maxWidth: 1100, margin: '0 auto' },
  header: {
    display: 'flex',
    alignItems: 'center',
    gap: 14,
    marginBottom: '2.5rem',
  },
  logo: {
    width: 42,
    height: 42,
    background: 'linear-gradient(135deg, #0ea5e9, #6366f1)',
    borderRadius: 10,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: 22,
  },
  h1: { margin: 0, fontSize: 26, fontWeight: 700, color: '#f1f5f9' },
  subtitle: { margin: '4px 0 0', fontSize: 13, color: '#64748b' },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(440px, 1fr))',
    gap: '1.5rem',
  },
  fullWidth: { gridColumn: '1 / -1' },
};

export default function App() {
  const [videoInfo, setVideoInfo] = useState(null);

  return (
    <div style={styles.app}>
      <header style={styles.header}>
        <div style={styles.logo}>👁</div>
        <div>
          <h1 style={styles.h1}>Face Detection Demo</h1>
          <p style={styles.subtitle}>
            Upload a video · detect faces with DJL UltraFace · stream annotated output
          </p>
        </div>
      </header>

      <div style={styles.grid}>
        {/* Upload */}
        <VideoUpload onUploadComplete={setVideoInfo} />

        {/* Video player */}
        <VideoPlayer videoInfo={videoInfo} />

        {/* ROI table — full width */}
        <div style={styles.fullWidth}>
          <ROIDisplay videoInfo={videoInfo} />
        </div>
      </div>
    </div>
  );
}
