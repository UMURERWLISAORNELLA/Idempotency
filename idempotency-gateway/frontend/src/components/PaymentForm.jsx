import { useState } from 'react'
import { v4 as uuidv4 } from 'uuid'
import { processPayment } from '../api/payment'

const CURRENCIES = ['FRW', 'USD', 'EUR']

export default function PaymentForm({ onResult }) {
  const [key, setKey] = useState(uuidv4())
  const [amount, setAmount] = useState('100')
  const [currency, setCurrency] = useState('FRW')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    const start = Date.now()
    try {
      const result = await processPayment(key, amount, currency)
      result.elapsed = ((Date.now() - start) / 1000).toFixed(2)
      result.key = key
      onResult(result)
    } catch (err) {
      onResult({ ok: false, data: { error: err.message }, status: 0, cacheHit: false, key })
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} style={styles.form}>
      <h2 style={{ marginBottom: 20, color: '#a5b4fc' }}>Send Payment Request</h2>

      <label style={styles.label}>Idempotency Key</label>
      <div style={styles.keyRow}>
        <input value={key} onChange={e => setKey(e.target.value)} required />
        <button type="button" onClick={() => setKey(uuidv4())} style={styles.genBtn}>
          New Key
        </button>
      </div>

      <label style={styles.label}>Amount</label>
      <input
        type="number"
        min="1"
        value={amount}
        onChange={e => setAmount(e.target.value)}
        required
        style={{ marginBottom: 16 }}
      />

      <label style={styles.label}>Currency</label>
      <select value={currency} onChange={e => setCurrency(e.target.value)} style={{ marginBottom: 24 }}>
        {CURRENCIES.map(c => <option key={c}>{c}</option>)}
      </select>

      <button type="submit" disabled={loading} style={styles.submitBtn}>
        {loading ? 'Processing…' : 'Send Payment'}
      </button>
    </form>
  )
}

const styles = {
  form: {
    background: '#1e293b',
    borderRadius: 12,
    padding: 28,
    display: 'flex',
    flexDirection: 'column',
  },
  label: {
    fontSize: 12,
    color: '#94a3b8',
    marginBottom: 6,
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
  },
  keyRow: {
    display: 'flex',
    gap: 8,
    marginBottom: 16,
  },
  genBtn: {
    background: '#334155',
    color: '#e2e8f0',
    whiteSpace: 'nowrap',
    flexShrink: 0,
  },
  submitBtn: {
    background: '#6366f1',
    color: '#fff',
    fontSize: 15,
    padding: '12px 0',
  },
}
