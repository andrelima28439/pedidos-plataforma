export const ESTOQUE_URL = import.meta.env.VITE_ESTOQUE_URL || 'http://localhost:8081';
export const PEDIDO_URL = import.meta.env.VITE_PEDIDO_URL || 'http://localhost:8082';
export const PAGAMENTO_URL = import.meta.env.VITE_PAGAMENTO_URL || 'http://localhost:8083';

export function getToken() {
  return localStorage.getItem('token') || '';
}

export function getClienteId() {
  return localStorage.getItem('clienteId') || '';
}

export async function login(clienteId) {
  const resp = await fetch(`${PEDIDO_URL}/auth/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ clienteId }),
  });
  if (!resp.ok) throw new Error(`login falhou: ${resp.status}`);
  const { token } = await resp.json();
  localStorage.setItem('token', token);
  localStorage.setItem('clienteId', clienteId);
  return token;
}

export function logout() {
  localStorage.removeItem('token');
  localStorage.removeItem('clienteId');
}

function authHeaders() {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function lerErro(resp) {
  try {
    const j = await resp.json();
    return j.erro || `HTTP ${resp.status}`;
  } catch {
    return `HTTP ${resp.status}`;
  }
}

export async function listarProdutos() {
  const resp = await fetch(`${ESTOQUE_URL}/produtos`);
  if (!resp.ok) throw new Error(await lerErro(resp));
  return resp.json();
}

export async function criarPedido(itens) {
  const resp = await fetch(`${PEDIDO_URL}/pedidos`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ itens }),
  });
  if (!resp.ok) throw new Error(await lerErro(resp));
  return resp.json();
}

export async function listarPedidos() {
  const resp = await fetch(`${PEDIDO_URL}/pedidos`, { headers: { ...authHeaders() } });
  if (!resp.ok) throw new Error(await lerErro(resp));
  return resp.json();
}

export async function observabilidade() {
  const resp = await fetch(`${PAGAMENTO_URL}/pagamentos/observabilidade`);
  if (!resp.ok) throw new Error(await lerErro(resp));
  return resp.json();
}

export function streamUrl() {
  const token = getToken();
  return `${PEDIDO_URL}/pedidos/stream?token=${encodeURIComponent(token)}`;
}
