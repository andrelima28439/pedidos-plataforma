"""Resumo diario de pedidos (Lambda via LocalStack).

Disparo suportado: evento do SQS (fila pedidos-alto-valor) ou invocacao
manual/agendada. Em AWS real seria disparada por EventBridge Scheduler
diario ou por SQS event source mapping; aqui demonstramos os dois formatos.
"""

def _extrair_pedidos(event):
    # Formato SQS: {"Records": [{"body": "{...}"}, ...]}
    if isinstance(event, dict) and "Records" in event:
        import json
        pedidos = []
        for r in event["Records"]:
            body = r.get("body", "{}")
            try:
                pedidos.append(json.loads(body) if isinstance(body, str) else body)
            except Exception:
                pass
        return pedidos
    # Formato direto: {"pedidos": [{"valor": "1500.00"}, ...]}
    if isinstance(event, dict) and "pedidos" in event:
        return event["pedidos"]
    return []


def handler(event, context):
    pedidos = _extrair_pedidos(event)
    total = len(pedidos)
    soma = 0.0
    for p in pedidos:
        try:
            soma += float(p.get("valor", 0))
        except Exception:
            pass
    media = round(soma / total, 2) if total else 0.0
    return {
        "totalPedidos": total,
        "valorTotal": round(soma, 2),
        "ticketMedio": media,
        "origem": "sqs" if isinstance(event, dict) and "Records" in event else "direta",
    }
