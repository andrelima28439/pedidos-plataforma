import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { criarPedido, getClienteId } from '../api';

export default function CriarPedido({ carrinho, setCarrinho }) {
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState('');
  const [ok, setOk] = useState('');
  const navigate = useNavigate();

  function qtd(produtoId, delta) {
    setCarrinho((c) =>
      c.map((i) => (i.produtoId === produtoId ? { ...i, quantidade: Math.max(1, i.quantidade + delta) } : i)));
  }

  function remover(produtoId) {
    setCarrinho((c) => c.filter((i) => i.produtoId !== produtoId));
  }

  async function finalizar() {
    if (!getClienteId()) { setErro('Entre com um clienteId no topo antes de criar o pedido.'); return; }
    if (carrinho.length === 0) { setErro('Carrinho vazio.'); return; }
    setLoading(true); setErro(''); setOk('');
    try {
      const pedido = await criarPedido(
        carrinho.map((i) => ({ produtoId: i.produtoId, quantidade: i.quantidade, precoUnitario: i.precoUnitario })),
      );
      setOk(`Pedido ${pedido.id} criado: ${pedido.status}`);
      setCarrinho([]);
      setTimeout(() => navigate('/pedidos'), 800);
    } catch (e) {
      setErro(e.message);
    } finally {
      setLoading(false);
    }
  }

  const total = carrinho.reduce((s, i) => s + i.precoUnitario * i.quantidade, 0);

  return (
    <div>
      <h2>Criar pedido</h2>
      {loading && <p>Enviando pedido...</p>}
      {erro && <p className="erro">Erro: {erro}</p>}
      {ok && <p className="ok">{ok} — redirecionando para Meus Pedidos...</p>}
      {carrinho.length === 0 ? <p>Carrinho vazio — adicione produtos no Catálogo.</p> : (
        <>
          <table>
            <thead><tr><th>Produto</th><th>Preço</th><th>Qtd</th><th></th></tr></thead>
            <tbody>
              {carrinho.map((i) => (
                <tr key={i.produtoId}>
                  <td>{i.nome}</td>
                  <td>R$ {i.precoUnitario}</td>
                  <td>
                    <button onClick={() => qtd(i.produtoId, -1)}>-</button> {i.quantidade}{' '}
                    <button onClick={() => qtd(i.produtoId, 1)}>+</button>
                  </td>
                  <td><button onClick={() => remover(i.produtoId)}>remover</button></td>
                </tr>
              ))}
            </tbody>
          </table>
          <p><strong>Total: R$ {total.toFixed(2)}</strong></p>
          <button disabled={loading} onClick={finalizar}>Finalizar pedido</button>
        </>
      )}
    </div>
  );
}
