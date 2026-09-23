"""Demonstração da Lambda: implanta resumo_diario no LocalStack, invoca e mostra resultado.

Uso: python lambda/prova_lambda.py
Requer: boto3 (pip install boto3), LocalStack em localhost:4566.
"""
import io
import json
import zipfile

import boto3

ENDPOINT = "http://localhost:4566"
FUNC = "resumo-diario-pedidos"

CODE = open("lambda/resumo_diario.py", "rb").read()

buf = io.BytesIO()
with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as z:
    info = zipfile.ZipInfo("resumo_diario.py")
    info.external_attr = (0o644 << 16)
    info.create_system = 3
    z.writestr(info, CODE)
ZIP = buf.getvalue()

lam = boto3.client("lambda", endpoint_url=ENDPOINT, region_name="us-east-1",
                   aws_access_key_id="test", aws_secret_access_key="test")

try:
    lam.create_function(
        FunctionName=FUNC,
        Runtime="python3.11",
        Role="arn:aws:iam::000000000000:role/lambda-test",
        Handler="resumo_diario.handler",
        Code={"ZipFile": ZIP},
        Timeout=30,
    )
    print(f"[lambda] funcao {FUNC} criada")
except lam.exceptions.ResourceConflictException:
    lam.update_function_code(FunctionName=FUNC, ZipFile=ZIP)
    print(f"[lambda] funcao {FUNC} atualizada")

# Aguarda ativa
w = lam.get_waiter("function_active_v2")
w.wait(FunctionName=FUNC)
print("[lambda] funcao ativa")

payload = {"pedidos": [{"valor": "1500.00"}, {"valor": "250.00"}, {"valor": "100.00"}]}
resp = lam.invoke(FunctionName=FUNC, Payload=json.dumps(payload).encode())
resultado = json.loads(resp["Payload"].read())
print("[lambda] invoke direto ->", json.dumps(resultado))

# Tenta ligar a fila SQS pedidos-alto-valor como gatilho (prova SQS->Lambda)
sqs = boto3.client("sqs", endpoint_url=ENDPOINT, region_name="us-east-1",
                   aws_access_key_id="test", aws_secret_access_key="test")
try:
    try:
        url = sqs.get_queue_url(QueueName="pedidos-alto-valor")["QueueUrl"]
    except sqs.exceptions.QueueDoesNotExist:
        url = sqs.create_queue(QueueName="pedidos-alto-valor")["QueueUrl"]
        print(f"[lambda] fila pedidos-alto-valor criada: {url}")
    arn = sqs.get_queue_attributes(QueueUrl=url, AttributeNames=["QueueArn"])["Attributes"]["QueueArn"]
    try:
        lam.create_event_source_mapping(EventSourceArn=arn, FunctionName=FUNC, BatchSize=1)
        print(f"[lambda] event source mapping SQS {arn} -> {FUNC} criado")
    except Exception as e:
        print(f"[lambda] mapping ja existe ou indisponivel: {e}")
except Exception as e:
    print(f"[lambda] fila SQS ainda nao existe (rode o teste SQS antes): {e}")

assert resultado["totalPedidos"] == 3, resultado
assert resultado["valorTotal"] == 1850.0, resultado
print("[lambda] PROVA OK: 3 pedidos, total 1850.0")
