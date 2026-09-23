package com.pedidos.pagamento.aws;

import com.pedidos.pagamento.domain.Pagamento;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Comprovante de pagamento aprovado em JSON no S3 (LocalStack).
 * Bucket padrao: comprovantes-pagamentos. Chave: comprovantes/{eventId}.json.
 */
@Service
public class S3ComprovanteService {

    private static final Logger log = LoggerFactory.getLogger(S3ComprovanteService.class);

    private final S3Client s3;
    private final String bucket;

    public S3ComprovanteService(S3Client s3, @Value("${aws.s3.bucket:comprovantes-pagamentos}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    public void garantirBucket() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("[s3] bucket criado: {}", bucket);
        }
    }

    public String salvarComprovante(Pagamento pagamento) {
        garantirBucket();
        String chave = "comprovantes/" + pagamento.getEventId() + ".json";
        String json =
                """
                {"eventId":"%s","pedidoId":"%s","valor":"%s","status":"%s","tentativas":%d,"em":"%s","correlationId":"%s"}"""
                        .formatted(
                                pagamento.getEventId(),
                                pagamento.getPedidoId(),
                                pagamento.getValor().toPlainString(),
                                pagamento.getStatus().name(),
                                pagamento.getTentativas(),
                                Instant.now().toString(),
                                pagamento.getCorrelationId());
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(chave)
                        .contentType("application/json")
                        .build(),
                RequestBody.fromString(json, StandardCharsets.UTF_8));
        log.info("[s3] comprovante salvo s3://{}/{}", bucket, chave);
        return chave;
    }

    public List<String> listarChaves() {
        garantirBucket();
        return s3.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).build()).contents().stream()
                .map(c -> c.key())
                .toList();
    }

    public String getBucket() {
        return bucket;
    }
}
