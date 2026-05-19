import { useState, useEffect } from "react";
import { listen } from "@tauri-apps/api/event";
import "./App.css";

interface SavedFileEvent {
  fileName: string;
  path: string;
}

// Logo SVG inline para que funcione offline sin depender de assets externos
const FlorLogo = () => (
  <svg
    width="80"
    height="80"
    viewBox="0 0 200 200"
    xmlns="http://www.w3.org/2000/svg"
    aria-label="FlorSave logo"
    role="img"
  >
    {/* Pétalos traseros */}
    {[-15, 15, 45, 75, 105, 135, 165, 195, 225, 255, 285, 315].map((deg) => (
      <ellipse key={`back-${deg}`} cx="100" cy="100" rx="42" ry="19" fill="#FF4D8C" transform={`rotate(${deg} 100 100)`} opacity="0.55" />
    ))}
    {/* Pétalos delanteros */}
    {[-30, 0, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300].map((deg) => (
      <ellipse key={`front-${deg}`} cx="100" cy="100" rx="29" ry="13.5" fill="#FF4D8C" transform={`rotate(${deg} 100 100)`} />
    ))}
    {/* Centro */}
    <circle cx="100" cy="100" r="19" fill="#FF4D8C" />
    <circle cx="100" cy="100" r="11" fill="#ffffff" />
  </svg>
);

function App() {
  const [lastSaved, setLastSaved] = useState<SavedFileEvent | null>(null);

  useEffect(() => {
    const unlisten = listen<SavedFileEvent>("file-saved", (event) => {
      setLastSaved(event.payload);
    });

    return () => {
      unlisten.then((f) => f());
    };
  }, []);

  return (
    <main className="app-root dot-grid" id="app-main">
      {/* Header */}
      <header className="app-header" role="banner">
        <FlorLogo />
        <span className="app-wordmark">FlorSave</span>
        <span className="app-version">v0.1.0</span>
      </header>

      {lastSaved ? (
        /* ── SUCCESS STATE ── */
        <section className="success-state" id="success-view" aria-live="polite">
          <div className="success-status" role="status">
            Guardado correctamente
          </div>

          <div className="success-card">
            <p className="success-filename" id="saved-filename">
              {lastSaved.fileName}
            </p>

            <div className="success-divider" />

            <p className="success-meta-label">Ubicación</p>
            <p className="success-path" id="saved-path">
              {lastSaved.path}
            </p>
          </div>

          <button
            id="btn-dismiss"
            className="btn-ok"
            onClick={() => setLastSaved(null)}
            aria-label="Aceptar y volver al estado de espera"
          >
            OK
          </button>
        </section>
      ) : (
        /* ── IDLE STATE ── */
        <section className="idle-state" id="idle-view" aria-label="Esperando archivos">
          <div className="logo-wrap" aria-hidden="true">
            <FlorLogo />
          </div>

          <div style={{ textAlign: "center" }}>
            <h1 className="idle-heading">FlorSave</h1>
          </div>

          <p className="idle-label">Esperando archivos</p>

          <div className="idle-instruction">
            <p>
              Abre WhatsApp, mantén presionado un archivo y selecciona{" "}
              <code>FlorSave</code> en el menú de compartir.
            </p>
          </div>
        </section>
      )}
    </main>
  );
}

export default App;
