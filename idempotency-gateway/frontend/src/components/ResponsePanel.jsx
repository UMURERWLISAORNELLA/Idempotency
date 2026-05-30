export default function ResponsePanel({ results }) {
  if (results.length === 0) return null

  return (
    <div style={styles.container}>
      <h2 style={{ marginBottom: 16, color: '#a5b4fc' }}>Response Log</h2>
      {results.map((r, i) => (
        <div key={i} style={{ ...styles.card, borderColor: getBorderColor(r) }}>
          <div style={styles.header}>
            <span style={{ ...styles.badge, background: getBadgeBg(r) }}>
              {r.cacheHit ? '⚡ Cache Hit' : r.ok ? '✅ Processed' : '❌ Error'}
            </span>
            <span style={styles.meta}>HTTP {r.status} · {r.elapsed}s · key: {r.key?.slice(0, 8)}…</span>
          </div>
          <pre style={styles.pre}>{JSON.stringify(r.data, null, 2)}</pre>
        </div>
      ))}
    </div>
  )
}

function getBorderColor(r) {
  if (!r.ok) return '#ef4444'
  if (r.cacheHit) return '#f59e0b'
  return '#22c55e'
}

function getBadgeBg(r) {
  if (!r.ok) return '#7f1d1d'
  if (r.cacheHit) return '#78350f'
  return '#14532d'
}

const styles = {
  container: {
    background: '#1e293b',
    borderRadius: 12,
    padding: 28,
  },
  card: {
    border: '1px solid',
    borderRadius: 8,
    marginBottom: 12,
    overflow: 'hidden',
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    padding: '10px 14px',
    background: '#0f172a',
  },
  badge: {
    borderRadius: 6,
    padding: '3px 10px',
    fontSize: 12,
    fontWeight: 700,
  },
  meta: {
    fontSize: 12,
    color: '#64748b',
  },
  pre: {
    padding: 14,
    fontSize: 13,
    color: '#94a3b8',
    overflowX: 'auto',
    margin: 0,
  },
}
