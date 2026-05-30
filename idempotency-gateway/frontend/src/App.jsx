import { useState } from 'react'
import PaymentForm from './components/PaymentForm'
import ResponsePanel from './components/ResponsePanel'
import KeysTable from './components/KeysTable'

export default function App() {
  const [results, setResults] = useState([])

  function handleResult(result) {
    setResults(prev => [result, ...prev])
  }

  return (
    <div style={styles.root}>
      <header style={styles.header}>
        <div style={styles.logo}>💳 FinSafe</div>
        <h1 style={styles.title}>Idempotency Gateway</h1>
        <span style={styles.subtitle}>Pay-Once Protocol</span>
      </header>

      <main style={styles.main}>
        <div style={styles.left}>
          <PaymentForm onResult={handleResult} />
          <div style={{ marginTop: 24 }}>
            <ResponsePanel results={results} />
          </div>
        </div>
        <div style={styles.right}>
          <KeysTable />
        </div>
      </main>
    </div>
  )
}

const styles = {
  root: { minHeight: '100vh', display: 'flex', flexDirection: 'column' },
  header: {
    background: '#1e293b',
    borderBottom: '1px solid #334155',
    padding: '16px 32px',
    display: 'flex',
    alignItems: 'center',
    gap: 16,
  },
  logo: { fontSize: 24 },
  title: { fontSize: 20, color: '#e2e8f0' },
  subtitle: { fontSize: 13, color: '#64748b', marginLeft: 'auto' },
  main: {
    display: 'grid',
    gridTemplateColumns: '420px 1fr',
    gap: 24,
    padding: 32,
    flex: 1,
    alignItems: 'start',
  },
  left: { display: 'flex', flexDirection: 'column' },
  right: {},
}
