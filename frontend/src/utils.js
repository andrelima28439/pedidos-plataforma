/** Utilidades puras (testadas com vitest). */

export function formatBRL(valor) {
  const n = Number(valor);
  if (Number.isNaN(n)) return 'R$ 0,00';
  return n.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

export function statusCor(status) {
  switch (status) {
    case 'PAGO': return 'green';
    case 'RECUSADO': return 'crimson';
    case 'AGUARDANDO_PAGAMENTO': return 'darkorange';
    default: return 'inherit';
  }
}

export function paginaDe(lista, pagina, tamanho) {
  const p = Math.max(0, pagina);
  return lista.slice(p * tamanho, p * tamanho + tamanho);
}
