import { describe, expect, it } from 'vitest';
import { formatBRL, paginaDe, statusCor } from './utils';

describe('utils', () => {
  it('formata BRL', () => {
    expect(formatBRL(10)).toContain('10,00');
    expect(formatBRL('xx')).toBe('R$ 0,00');
  });

  it('cor por status', () => {
    expect(statusCor('PAGO')).toBe('green');
    expect(statusCor('RECUSADO')).toBe('crimson');
    expect(statusCor('OUTRO')).toBe('inherit');
  });

  it('paginacao simples', () => {
    expect(paginaDe([1, 2, 3, 4, 5], 0, 2)).toEqual([1, 2]);
    expect(paginaDe([1, 2, 3, 4, 5], 2, 2)).toEqual([5]);
    expect(paginaDe([], 0, 6)).toEqual([]);
  });
});
