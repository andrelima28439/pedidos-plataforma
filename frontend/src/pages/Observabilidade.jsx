import { useEffect, useState } from 'react';
import { observabilidade } from '../api';

export default function Observabilidade() {
  const [dados, setDados] = useState(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState('');

  async function carregar() {
    try {
      setDados(await observabilidade());
      setErro('');
    } catch (e) {
      setErro(e.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    carregar();
    const t = setInterval(carregar, 15000);
    return () => clearInterval(t);
  }, []);

  if (loading) return <p>Carregando observabilidade...</p>;
  if (erro) return (
    <div>
      <p className="erro">Erro: {erro} (suba o pagamento-service na porta 8083)</p>
      <button onClick={() => { setLoading(true); carregar(); }}>Tentar de novo</button>
    </div>
  );

  return (
    <div>
      <h2>Observabilidade (24h)</h2>
      <div className="grid">
        <div className="card"><strong>DLQ pagamento</strong><span>{dados.dlqPendente} pendente(s)</span></div>
        <div className="card"><strong>Pagamentos</strong><span>{dados.total24h} total</span></div>
        <div className="card"><strong>Com retry</strong><span>{dados.comRetry24h}</span></div>
        <div className="card"><strong>Taxa retry</strong><span>{(dados.taxaRetry * 100).toFixed(1)}%</span></div>
      </div>
      <button onClick={carregar}>Atualizar</button>
    </div>
  );
}
