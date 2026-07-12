import { useEffect, useState } from 'react';
import { placeOrder, listOrders, type Order } from './api';

export default function App() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [email, setEmail] = useState('sam@example.com');
  const [amount, setAmount] = useState('79.99');
  const [error, setError] = useState('');

  async function refresh() {
    try { setOrders(await listOrders()); }
    catch (e) { setError(String(e)); }
  }

  // Poll so PENDING -> NOTIFIED transitions show up live.
  useEffect(() => {
    refresh();
    const t = setInterval(refresh, 2000);
    return () => clearInterval(t);
  }, []);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    try {
      await placeOrder(email, parseFloat(amount));
      await refresh();
    } catch (err) { setError(String(err)); }
  }

  return (
      <main style={{ maxWidth: 640, margin: '40px auto', fontFamily: 'system-ui' }}>
        <h1>Orders</h1>
        <form onSubmit={submit} style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
          <input value={email} onChange={e => setEmail(e.target.value)} placeholder="email" />
          <input value={amount} onChange={e => setAmount(e.target.value)} placeholder="amount" />
          <button type="submit">Place order</button>
        </form>
        {error && <p style={{ color: 'crimson' }}>{error}</p>}
        <table width="100%" cellPadding={6}>
          <thead>
          <tr><th align="left">#</th><th align="left">Email</th>
            <th align="left">Amount</th><th align="left">Status</th></tr>
          </thead>
          <tbody>
          {orders.map(o => (
              <tr key={o.id}>
                <td>{o.id}</td>
                <td>{o.customerEmail}</td>
                <td>${o.amount.toFixed(2)}</td>
                <td>
                <span style={{
                  color: o.status === 'NOTIFIED' ? 'green' : '#b58900',
                  fontWeight: 600
                }}>{o.status}</span>
                </td>
              </tr>
          ))}
          </tbody>
        </table>
      </main>
  );
}