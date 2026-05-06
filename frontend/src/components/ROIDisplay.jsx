// import { useState, useEffect } from 'react';
// import { getROIs } from '../api/api';

// const styles = {
//   card: {
//     background: '#1e293b',
//     borderRadius: 12,
//     padding: '2rem',
//     border: '1px solid #334155',
//   },
//   header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' },
//   title: { margin: 0, fontSize: 20, fontWeight: 600, color: '#f1f5f9' },
//   count: { fontSize: 13, color: '#64748b' },
//   tableWrap: { overflowX: 'auto' },
//   table: { width: '100%', borderCollapse: 'collapse', fontSize: 13 },
//   th: {
//     padding: '8px 12px',
//     background: '#0f172a',
//     color: '#94a3b8',
//     fontWeight: 600,
//     textAlign: 'left',
//     borderBottom: '1px solid #334155',
//     whiteSpace: 'nowrap',
//   },
//   td: {
//     padding: '7px 12px',
//     borderBottom: '1px solid #1e293b',
//     color: '#cbd5e1',
//     fontVariantNumeric: 'tabular-nums',
//   },
//   tdHighlight: { color: '#38bdf8', fontWeight: 600 },
//   trEven: { background: '#162032' },
//   bar: (pct) => ({
//     display: 'inline-block',
//     height: 6,
//     width: `${Math.round(pct * 60)}px`,
//     background: `hsl(${120 * pct}, 80%, 45%)`,
//     borderRadius: 99,
//     marginRight: 6,
//     verticalAlign: 'middle',
//   }),
//   empty: {
//     padding: '3rem',
//     textAlign: 'center',
//     color: '#475569',
//   },
// };

// export default function ROIDisplay({ videoInfo }) {
//   const [rois, setRois] = useState([]);
//   const [loading, setLoading] = useState(false);
//   const [page, setPage] = useState(0);
//   const PAGE_SIZE = 20;

//   useEffect(() => {
//     if (!videoInfo?.videoId) return;
//     setLoading(true);
//     getROIs(videoInfo.videoId)
//       .then(setRois)
//       .catch(console.error)
//       .finally(() => setLoading(false));
//   }, [videoInfo]);

//   if (!videoInfo) {
//     return (
//       <div style={styles.card}>
//         <h2 style={{ ...styles.title, marginBottom: '1.5rem' }}>ROI Data</h2>
//         <div style={styles.empty}>Upload a video to see bounding box data</div>
//       </div>
//     );
//   }

//   const totalPages = Math.ceil(rois.length / PAGE_SIZE);
//   const slice = rois.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

//   return (
//     <div style={styles.card}>
//       <div style={styles.header}>
//         <h2 style={styles.title}>ROI Data</h2>
//         <span style={styles.count}>
//           {loading ? 'Loading…' : `${rois.length} detections`}
//         </span>
//       </div>

//       {rois.length === 0 && !loading ? (
//         <div style={styles.empty}>
//           No face detections yet — processing may still be running.
//         </div>
//       ) : (
//         <>
//           <div style={styles.tableWrap}>
//             <table style={styles.table}>
//               <thead>
//                 <tr>
//                   {['Frame', 'X', 'Y', 'Width', 'Height', 'Confidence', 'Detected at'].map(h => (
//                     <th key={h} style={styles.th}>{h}</th>
//                   ))}
//                 </tr>
//               </thead>
//               <tbody>
//                 {slice.map((roi, i) => (
//                   <tr key={roi.id} style={i % 2 === 1 ? styles.trEven : {}}>
//                     <td style={{ ...styles.td, ...styles.tdHighlight }}>{roi.frameNumber}</td>
//                     <td style={styles.td}>{roi.x}</td>
//                     <td style={styles.td}>{roi.y}</td>
//                     <td style={styles.td}>{roi.width}</td>
//                     <td style={styles.td}>{roi.height}</td>
//                     <td style={styles.td}>
//                       <span style={styles.bar(roi.confidence)} />
//                       {(roi.confidence * 100).toFixed(1)}%
//                     </td>
//                     <td style={styles.td}>
//                       {roi.detectedAt ? new Date(roi.detectedAt).toLocaleTimeString() : '—'}
//                     </td>
//                   </tr>
//                 ))}
//               </tbody>
//             </table>
//           </div>

//           {totalPages > 1 && (
//             <div style={{ marginTop: 12, display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
//               <button
//                 onClick={() => setPage(p => Math.max(0, p - 1))}
//                 disabled={page === 0}
//                 style={{ padding: '4px 12px', background: '#334155', color: '#e2e8f0',
//                          border: 'none', borderRadius: 6, cursor: 'pointer' }}
//               >
//                 ← Prev
//               </button>
//               <span style={{ color: '#64748b', fontSize: 13, alignSelf: 'center' }}>
//                 {page + 1} / {totalPages}
//               </span>
//               <button
//                 onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
//                 disabled={page === totalPages - 1}
//                 style={{ padding: '4px 12px', background: '#334155', color: '#e2e8f0',
//                          border: 'none', borderRadius: 6, cursor: 'pointer' }}
//               >
//                 Next →
//               </button>
//             </div>
//           )}
//         </>
//       )}
//     </div>
//   );
// }

import { useState, useEffect } from 'react';
import { getROIs } from '../api/api';

const BASE = process.env.REACT_APP_API_URL || '/api';

/** Returns the URL that serves a single processed frame JPEG from the backend. */
function getFrameImageUrl(videoId, frameNumber) {
  return `${BASE}/roi/frame-image?videoId=${videoId}&frameNumber=${frameNumber}`;
}

/** Converts frameNumber → approximate timestamp (assuming 10 fps extraction). */
function frameToTimestamp(frameNumber, fps = 10) {
  const totalSeconds = Math.floor((frameNumber - 1) / fps);
  const m = Math.floor(totalSeconds / 60).toString().padStart(2, '0');
  const s = (totalSeconds % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
}

// ── Styles ────────────────────────────────────────────────────────────────────

const S = {
  card: {
    background: '#1e293b',
    borderRadius: 12,
    padding: '2rem',
    border: '1px solid #334155',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '1.5rem',
  },
  title: { margin: 0, fontSize: 20, fontWeight: 600, color: '#f1f5f9' },
  count: { fontSize: 13, color: '#64748b' },

  // Photo grid
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))',
    gap: '1rem',
  },

  // Individual frame card
  frameCard: {
    background: '#0f172a',
    borderRadius: 10,
    overflow: 'hidden',
    border: '1px solid #334155',
    transition: 'transform 0.15s, box-shadow 0.15s',
    cursor: 'pointer',
  },
  frameCardHover: {
    transform: 'translateY(-3px)',
    boxShadow: '0 8px 24px rgba(0,0,0,0.4)',
    border: '1px solid #38bdf8',
  },

  // Image inside card
  frameImg: {
    width: '100%',
    height: 150,
    objectFit: 'cover',
    display: 'block',
    background: '#1e293b',
  },

  // Metadata strip at bottom of each card
  meta: {
    padding: '0.6rem 0.75rem',
    display: 'flex',
    flexDirection: 'column',
    gap: 3,
  },
  timestamp: {
    fontSize: 13,
    fontWeight: 700,
    color: '#38bdf8',
    letterSpacing: '0.02em',
  },
  frameBadge: {
    fontSize: 11,
    color: '#64748b',
  },
  confidenceRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
    marginTop: 2,
  },
  confBar: (pct) => ({
    flex: 1,
    height: 4,
    background: '#1e293b',
    borderRadius: 99,
    overflow: 'hidden',
  }),
  confFill: (pct) => ({
    height: '100%',
    width: `${Math.round(pct * 100)}%`,
    background: `hsl(${120 * pct}, 80%, 45%)`,
    borderRadius: 99,
    transition: 'width 0.4s',
  }),
  confLabel: {
    fontSize: 11,
    color: '#94a3b8',
    minWidth: 36,
    textAlign: 'right',
  },

  // Lightbox overlay
  overlay: {
    position: 'fixed',
    inset: 0,
    background: 'rgba(0,0,0,0.85)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
    padding: '2rem',
  },
  overlayInner: {
    position: 'relative',
    maxWidth: 900,
    width: '100%',
    background: '#1e293b',
    borderRadius: 12,
    overflow: 'hidden',
    boxShadow: '0 24px 80px rgba(0,0,0,0.7)',
  },
  overlayImg: {
    width: '100%',
    display: 'block',
    maxHeight: '75vh',
    objectFit: 'contain',
    background: '#000',
  },
  overlayMeta: {
    padding: '1rem 1.25rem',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  overlayClose: {
    position: 'absolute',
    top: 10,
    right: 12,
    background: 'rgba(0,0,0,0.5)',
    color: '#fff',
    border: 'none',
    borderRadius: 99,
    width: 32,
    height: 32,
    fontSize: 16,
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },

  // Pagination
  pagination: {
    display: 'flex',
    gap: 8,
    justifyContent: 'center',
    marginTop: '1.5rem',
    alignItems: 'center',
  },
  pageBtn: (disabled) => ({
    padding: '6px 16px',
    background: disabled ? '#1e293b' : '#334155',
    color: disabled ? '#475569' : '#e2e8f0',
    border: '1px solid #334155',
    borderRadius: 6,
    cursor: disabled ? 'not-allowed' : 'pointer',
    fontSize: 13,
  }),

  // Empty / loading states
  empty: {
    padding: '4rem 2rem',
    textAlign: 'center',
    color: '#475569',
    fontSize: 14,
  },
};

// ── Lightbox component ────────────────────────────────────────────────────────

function Lightbox({ roi, videoId, onClose }) {
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [onClose]);

  return (
    <div style={S.overlay} onClick={onClose}>
      <div style={S.overlayInner} onClick={(e) => e.stopPropagation()}>
        <button style={S.overlayClose} onClick={onClose}>✕</button>
        <img
          src={getFrameImageUrl(videoId, roi.frameNumber)}
          alt={`Frame ${roi.frameNumber}`}
          style={S.overlayImg}
        />
        <div style={S.overlayMeta}>
          <div>
            <div style={{ ...S.timestamp, fontSize: 16 }}>
              🕐 {frameToTimestamp(roi.frameNumber)}
            </div>
            <div style={{ fontSize: 12, color: '#64748b', marginTop: 2 }}>
              Frame #{roi.frameNumber} · Box: ({roi.x}, {roi.y}) {roi.width}×{roi.height}px
            </div>
          </div>
          <div style={{
            background: `hsl(${120 * roi.confidence}, 80%, 35%)`,
            color: '#fff',
            padding: '4px 12px',
            borderRadius: 99,
            fontSize: 14,
            fontWeight: 700,
          }}>
            {(roi.confidence * 100).toFixed(1)}% confidence
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Frame Card ────────────────────────────────────────────────────────────────

function FrameCard({ roi, videoId, onClick }) {
  const [hovered, setHovered] = useState(false);
  const [imgError, setImgError] = useState(false);

  return (
    <div
      style={{ ...S.frameCard, ...(hovered ? S.frameCardHover : {}) }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onClick={onClick}
    >
      {imgError ? (
        <div style={{ ...S.frameImg, display: 'flex', alignItems: 'center',
                      justifyContent: 'center', color: '#475569', fontSize: 12 }}>
          Frame unavailable
        </div>
      ) : (
        <img
          src={getFrameImageUrl(videoId, roi.frameNumber)}
          alt={`Detected face at ${frameToTimestamp(roi.frameNumber)}`}
          style={S.frameImg}
          loading="lazy"
          onError={() => setImgError(true)}
        />
      )}

      <div style={S.meta}>
        <span style={S.timestamp}>🕐 {frameToTimestamp(roi.frameNumber)}</span>
        <span style={S.frameBadge}>Frame #{roi.frameNumber}</span>
        <div style={S.confidenceRow}>
          <div style={S.confBar(roi.confidence)}>
            <div style={S.confFill(roi.confidence)} />
          </div>
          <span style={S.confLabel}>{(roi.confidence * 100).toFixed(0)}%</span>
        </div>
      </div>
    </div>
  );
}

// ── Main Component ────────────────────────────────────────────────────────────

const PAGE_SIZE = 12; // 12 cards per page (3 columns × 4 rows typically)

export default function ROIDisplay({ videoInfo }) {
  const [rois, setRois]           = useState([]);
  const [loading, setLoading]     = useState(false);
  const [page, setPage]           = useState(0);
  const [selected, setSelected]   = useState(null); // for lightbox

  useEffect(() => {
    if (!videoInfo?.videoId) return;
    setLoading(true);
    setPage(0);
    getROIs(videoInfo.videoId)
      .then(setRois)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [videoInfo]);

  if (!videoInfo) {
    return (
      <div style={S.card}>
        <h2 style={{ ...S.title, marginBottom: '1.5rem' }}>Detected Frames</h2>
        <div style={S.empty}>
          <div style={{ fontSize: 36, marginBottom: 10 }}>🎞️</div>
          Upload a video to see detected face frames here
        </div>
      </div>
    );
  }

  const totalPages = Math.ceil(rois.length / PAGE_SIZE);
  const slice = rois.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  return (
    <div style={S.card}>
      {/* Header */}
      <div style={S.header}>
        <h2 style={S.title}>Detected Frames</h2>
        <span style={S.count}>
          {loading ? 'Loading…' : `${rois.length} face detection${rois.length !== 1 ? 's' : ''}`}
        </span>
      </div>

      {/* Empty state */}
      {!loading && rois.length === 0 && (
        <div style={S.empty}>
          <div style={{ fontSize: 36, marginBottom: 10 }}>😶</div>
          No faces detected in this video
        </div>
      )}

      {/* Photo grid */}
      {slice.length > 0 && (
        <>
          <div style={S.grid}>
            {slice.map((roi) => (
              <FrameCard
                key={roi.id ?? roi.frameNumber}
                roi={roi}
                videoId={videoInfo.videoId}
                onClick={() => setSelected(roi)}
              />
            ))}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div style={S.pagination}>
              <button
                style={S.pageBtn(page === 0)}
                disabled={page === 0}
                onClick={() => setPage(p => p - 1)}
              >
                ← Prev
              </button>
              <span style={{ color: '#64748b', fontSize: 13 }}>
                Page {page + 1} of {totalPages}
              </span>
              <button
                style={S.pageBtn(page === totalPages - 1)}
                disabled={page === totalPages - 1}
                onClick={() => setPage(p => p + 1)}
              >
                Next →
              </button>
            </div>
          )}
        </>
      )}

      {/* Lightbox */}
      {selected && (
        <Lightbox
          roi={selected}
          videoId={videoInfo.videoId}
          onClose={() => setSelected(null)}
        />
      )}
    </div>
  );
}