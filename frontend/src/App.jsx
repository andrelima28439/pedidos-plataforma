import { useState } from 'react';
import { Link, Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import { getClienteId, login, logout } from './api';
import Catalogo from './pages/Catalogo';
import CriarPedido from './pages/CriarPedido';
import MeusPedidos from './pages/MeusPedidos';
import Observabilidade from './pages/Observabilidade';
import './App.css';

export default function App() {
  const [cliente, setCliente] = useState(getClienteId());
  const [input, setInput] = useState(getClienteId());
  const [carrinho, setCarrinho] = useState([]);
  const [erroLogin, setErroLogin] = useState('');

  async function entrar() {
    if (!input.trim()) return;
    try {
      await login(input.trim());
      setCliente(input.trim());
      setErroLogin('');
    } catch (e) {
      setErroLogin(e.message);
    }
  }

  return (
    <Router>
      <header>
        <h1>Pedidos</h1>
        <nav>
          <Link to="/">Catálogo</Link>
          <Link to="/novo">Criar pedido ({carrinho.length})</Link>
          <Link to="/pedidos">Meus pedidos</Link>
          <Link to="/obs">Observabilidade</Link>
        </nav>
        <div className="login">
          <input value={input} onChange={(e) => setInput(e.target.value)} placeholder="clienteId" />
          {cliente ? (
            <><span>{cliente}</span><button onClick={() => { logout(); setCliente(''); }}>Sair</button></>
          ) : (
            <button onClick={entrar}>Entrar</button>
          )}
        </div>
        {erroLogin && <p className="erro">{erroLogin}</p>}
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Catalogo setCarrinho={setCarrinho} />} />
          <Route path="/novo" element={<CriarPedido carrinho={carrinho} setCarrinho={setCarrinho} />} />
          <Route path="/pedidos" element={<MeusPedidos />} />
          <Route path="/obs" element={<Observabilidade />} />
        </Routes>
      </main>
    </Router>
  );
}
