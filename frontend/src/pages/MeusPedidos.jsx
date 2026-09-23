import { useEffect, useState } from 'react';
import { getClienteId, listarPedidos, streamUrl } from '../api';

export default function MeusPedidos() {
  const [pedidos, setPedidos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState('');
  const [sse, setSse] = useState('desconectado');

  useEffect(() => {
    if (!getClienteId()) { setLoading(false); setErro('Entre com um clienteId no topo.'); return; }
    setLoading(true);
    listarPedidos()
      .then((p) => { setPedidos(p); setErro(''); })
      .catch((e) => setErro(e.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!getClienteId()) return;
    const es = new EventSource(streamUrl());
    es.onopen = () => setSse('conectado (tempo real)');
    es.onerror = () => setSse('desconectado — recarregue a página');
    es.addEventListener('pedido', (ev) => {
      try {
        const { pedidoId, status } = JSON.parse(ev.data);
        setPedidos((lista) => lista.map((p) => (p.id === pedidoId ? { ...p, status } : p)));
      } catch { /* ignora */ }
    });
    return () => es.close();
  }, []);

  if (loading) return <p>Carregando pedidos...</p>;
  if (erro) return <p className="erro">Erro: {erro}</p>;

  return (
    <div>
      <h2>Meus pedidos (SSE: {sse})</h2>
      {pedidos.length === 0 ? <p>Nenhum pedido ainda.</p> : (
        <div className="grid">
          {pedidos.map((p) => (
            <div key={p.id} className="card">
              <strong>{p.id.slice(0, 8)}…</strong>
              <span className={`status ${p.status}`}>{p.status}</span>
              <span>Total: R$ {p.valorTotal}</span>
              <details>
                <summary>Histórico ({p.historico.length})</summary>
                <ul>
                  {p.historico.map((h, i) => (
                    <li key={i}>{h.de || '—'} → {h.para} <small>{h.motivo}</small></li>
                  ))}
                </ul>
              </details>
            </div>
          ))}
        </div>
      )}
      <p><small>Abra duas abas com o mesmo cliente, crie um pedido em uma e veja o status mudar na outra sem F5.</small></p>
    </div>
  );
}
