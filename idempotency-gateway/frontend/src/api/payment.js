const BASE = import.meta.env.VITE_API_URL || '/api'

export async function processPayment(idempotencyKey, amount, currency) {
  const res = await fetch(`${BASE}/process-payment`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey,
    },
    body: JSON.stringify({ amount: parseFloat(amount), currency }),
  })

  const cacheHit = res.headers.get('X-Cache-Hit') === 'true'
  const data = await res.json()
  return { status: res.status, data, cacheHit, ok: res.ok }
}

export async function fetchKeys() {
  const res = await fetch(`${BASE}/keys`)
  return res.json()
}
