# Face Detection — Frontend

React 18 · Axios · No UI framework (plain CSS-in-JS)

## Prerequisites

| Tool | Version |
|------|---------|
| Node.js | 18+ |
| npm | 9+ |

---

## 1. Install dependencies

```bash
cd frontend
npm install
```

---

## 2. Configure API URL

The `.env` file is already set to point at the local backend:

```
REACT_APP_API_URL=http://localhost:8080/api
```

Change this if your backend runs on a different port.

---

## 3. Start the dev server

```bash
npm start
```

Opens **http://localhost:3000** in your browser automatically.

> The backend must be running at `localhost:8080` before you upload a video.

---

## Project structure

```
frontend/
├── .env                          ← REACT_APP_API_URL=http://localhost:8080/api
├── package.json
└── src/
    ├── index.js                  ← React entry point
    ├── App.jsx                   ← root layout, holds videoInfo state
    ├── api/
    │   └── api.js                ← all Axios calls in one place
    └── components/
        ├── VideoUpload.jsx       ← drag-drop upload + progress bar
        ├── VideoPlayer.jsx       ← polls status, plays video when ready
        └── ROIDisplay.jsx        ← paginated table of bounding box data
```

---

## What each component does

**`VideoUpload.jsx`**
- Drag-and-drop or click-to-browse file picker
- Validates `video/*` MIME type
- Shows upload progress bar via Axios `onUploadProgress`
- On success calls `onUploadComplete(result)` → lifts state to `App`

**`VideoPlayer.jsx`**
- Receives `videoInfo` from App (has `videoId` + initial `status`)
- Polls `GET /api/video/{id}/status` every 3 seconds
- When status = `COMPLETED`, sets `<video src="...">` to the stream URL
- Shows spinner while processing, error state on failure

**`ROIDisplay.jsx`**
- Fetches `GET /api/roi?videoId=` once when videoInfo arrives
- Paginated table (20 rows/page): Frame · X · Y · Width · Height · Confidence · Time
- Confidence shown as a coloured bar (green → red by score)
