import { useEffect, useState } from 'react';
import { listarProdutos } from '../api';

const PAGE = 6;

export default function Catalogo({ setCarrinho }) {
  const [produtos, setProdutos] = useState([]);
  const [pagina, setPagina] = useState(0);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState('');

  useEffect(() => {
    setLoading(true);
    listarProdutos()
      .then((p) => { setProdutos(p); setErro(''); })
      .catch((e) => setErro(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p>Carregando catálogo...</p>;
  if (erro) return <p className="erro">Erro ao carregar catálogo: {erro}</p>;

  const totalPag = Math.max(1, Math.ceil(produtos.length / PAGE));
  const visiveis = produtos.slice(pagina * PAGE, pagina * PAGE + PAGE);

  function adicionar(produto) {
    setCarrinho((c) => {
      const achou = c.find((i) => i.produtoId === produto.id);
      if (achou) {
        return c.map((i) => (i.produtoId === produto.id ? { ...i, quantidade: i.quantidade + 1 } : i));
      }
      return [...c, { produtoId: produto.id, nome: produto.nome, precoUnitario: produto.preco, quantidade: 1 }];
    });
  }

  return (
    <div>
      <h2>Catálogo ({produtos.length})</h2>
      <div className="grid">
        {visiveis.map((p) => (
          <div key={p.id} className="card">
            <strong>{p.nome}</strong>
            <span>R$ {p.preco}</span>
            <span>Livre: {p.quantidadeDisponivel - p.quantidadeReservada}</span>
            <button onClick={() => adicionar(p)}>Adicionar</button>
          </div>
        ))}
      </div>
      <div className="paginacao">
        <button disabled={pagina === 0} onClick={() => setPagina((x) => x - 1)}>◀</button>
        <span>{pagina + 1}/{totalPag}</span>
        <button disabled={pagina + 1 >= totalPag} onClick={() => setPagina((x) => x + 1)}>▶</button>
      </div>
    </div>
  );
}
