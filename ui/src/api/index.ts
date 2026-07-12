export interface Order {
    id: number;
    customerEmail: string;
    amount: number;
    status: 'PENDING' | 'NOTIFIED';
    createdAt: string;
}

// In the cluster, nginx proxies /api -> order-service. Locally, Vite proxies it.
const BASE = '/api';

export async function placeOrder(customerEmail: string, amount: number): Promise<Order> {
    const res = await fetch(`${BASE}/orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ customerEmail, amount }),
    });
    if (!res.ok) throw new Error(`Place order failed: ${res.status}`);
    return res.json();
}

export async function listOrders(): Promise<Order[]> {
    const res = await fetch(`${BASE}/orders`);
    if (!res.ok) throw new Error(`List failed: ${res.status}`);
    return res.json();
}