import { useEffect, useState } from 'react'
import { fetchKeys } from '../api/payment'

export default function KeysTable() {
  const [keys, setKeys] = useState([])

  useEffect(() => {
    const load = () => fetchKeys().then(setKeys).catch(() => {})
    load()
    const id = setInterval(load, 3000)
    return () => clearInterval(id)
  }, [])

  if (keys.length === 0) return (
    <div style={styles.empty}>No idempotency keys stored yet.</div>
  )

  return (
    <div style={styles.container}>
      <h2 style={{ marginBottom: 16, color: '#a5b4fc' }}>Stored Keys ({keys.length})</h2>
      <div style={{ overflowX: 'auto' }}>
        <table style={styles.table}>
          <thead>
            <tr>
              {['Key', 'State', 'Amount', 'Currency', 'Transaction ID', 'Created At'].map(h => (
                <th key={h} style={styles.th}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {keys.map((k, i) => (
              <tr key={i} style={i % 2 === 0 ? styles.rowEven : styles.rowOdd}>
                <td style={styles.td}><code style={styles.code}>{k.key.slice(0, 12)}…</code></td>
                <td style={styles.td}>
                  <span style={{ ...styles.stateBadge, background: k.state === 'COMPLETED' ? '#14532d' : '#78350f' }}>
                    {k.state}
                  </span>
                </td>
                <td style={styles.td}>{k.amount}</td>
                <td style={styles.td}>{k.currency}</td>
                <td style={styles.td}><code style={styles.code}>{k.transactionId}</code></td>
                <td style={styles.td}>{new Date(k.createdAt).toLocaleTimeString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

const styles = {
  container: { background: '#1e293b', borderRadius: 12, padding: 28 },
  empty: { color: '#64748b', textAlign: 'center', padding: 20 },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: 13 },
  th: { textAlign: 'left', padding: '8px 12px', color: '#64748b', borderBottom: '1px solid #334155', fontSize: 11, textTransform: 'uppercase' },
  td: { padding: '10px 12px', color: '#cbd5e1' },
  rowEven: { background: '#0f172a' },
  rowOdd: { background: 'transparent' },
  code: { fontFamily: 'monospace', color: '#a5b4fc' },
  stateBadge: { borderRadius: 4, padding: '2px 8px', fontSize: 11, fontWeight: 700 },
}
