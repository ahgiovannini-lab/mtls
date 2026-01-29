# mTLS outbound (client-side) com Spring Boot 3 + RestTemplate

Este projeto demonstra **mTLS outbound (client-side)** para chamadas HTTP usando `RestTemplate` com **Apache HttpClient 5** e **Java 21**. Não há configuração de mTLS inbound (server-side) para Jetty/Tomcat neste exemplo.

## Visão geral

- O microserviço apresenta um **certificado de cliente** durante o handshake TLS.
- A validação do certificado do servidor do parceiro possui **dois modos mutuamente exclusivos**:
  - **TRUSTSTORE**: valida a cadeia via `truststore.p12`.
  - **PINNING**: valida o certificado via pin.
    - **PIN_FINGERPRINT**: SHA-256 hex do certificado leaf.
    - **PIN_SPKI**: Base64 do SHA-256 da SubjectPublicKeyInfo.

## Configuração (YAML)

As propriedades são definidas em `application.yml`, com secrets via variáveis de ambiente:

```yaml
partner:
  mtls:
    enabled: true
    client-keystore-path: /etc/mtls/client.p12
    client-keystore-password: ${CLIENT_KEYSTORE_PASSWORD}
    client-keystore-type: PKCS12
    server-validation:
      mode: TRUSTSTORE
      truststore-path: /etc/mtls/truststore.p12
      truststore-password: ${TRUSTSTORE_PASSWORD}
      truststore-type: PKCS12
      pinned-cert-sha256-hex: ${PARTNER_PIN_FINGERPRINT:}
      pinned-spki-sha256-base64: ${PARTNER_PIN_SPKI:}
```

### Escolha do modo

- Se o parceiro fornecer **CA/cadeia**, use `TRUSTSTORE` (recomendado).
- Se o parceiro fornecer **certificado leaf**:
  - você pode usar pinning (fingerprint ou SPKI), ou
  - importar o leaf no truststore.
- Se o parceiro fornecer apenas **fingerprint**, use `PIN_FINGERPRINT`.
- Se o parceiro fornecer **SPKI pin**, use `PIN_SPKI`.

## Como gerar/atualizar truststore (recomendado)

### Importar CA/cadeia no truststore

```bash
keytool -importcert -noprompt \
  -alias partner-ca \
  -file partner-ca.pem \
  -keystore truststore.p12 \
  -storetype PKCS12 \
  -storepass "$TRUSTSTORE_PASSWORD"
```

### Importar certificado leaf no truststore

```bash
keytool -importcert -noprompt \
  -alias partner-leaf \
  -file partner-leaf.pem \
  -keystore truststore.p12 \
  -storetype PKCS12 \
  -storepass "$TRUSTSTORE_PASSWORD"
```

Depois, empacote o `truststore.p12` no `Secret` do Kubernetes.

## Abordagem alternativa (initContainer/Job)

Em ambientes onde o truststore é criado **dentro do cluster**, você pode usar um `initContainer` para gerar o `truststore.p12` a partir de um certificado CA fornecido via `ConfigMap`. Veja o exemplo em `k8s/truststore-initcontainer.yml`. Esta abordagem é útil quando a cadeia muda frequentemente, mas adiciona tempo de start do pod e necessidade de distribuir a CA de forma segura.

## Como obter pins

### Fingerprint SHA-256 (hex)

```bash
openssl x509 -in partner-leaf.pem -noout -fingerprint -sha256
```

### SPKI pin (Base64 do SHA-256 da public key)

```bash
openssl x509 -in partner-leaf.pem -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl base64
```

## Setup fake + smoke test (local)

Este passo a passo cria um **ambiente fake** de mTLS local (client + "partner server") e um **smoke test**
que usa o `RestTemplate` mTLS do app para validar o handshake.

### Pré-requisitos

- Java 21
- Maven
- `openssl` e `keytool` (o `keytool` vem com o JDK)

### 1) Criar pasta local de mTLS e senhas

```bash
mkdir -p .mtls

# gerar senhas aleatorias
CLIENT_PWD="$(openssl rand -base64 24)"
TRUSTSTORE_PWD="$(openssl rand -base64 24)"

cat <<EOF > .env
CLIENT_KEYSTORE_PASSWORD=$CLIENT_PWD
TRUSTSTORE_PASSWORD=$TRUSTSTORE_PWD
CLIENT_KEYSTORE_PATH=$(pwd)/.mtls/client.p12
TRUSTSTORE_PATH=$(pwd)/.mtls/truststore.p12
EOF
```

> A pasta `.mtls/` e o arquivo `.env` ja estao no `.gitignore` e nao devem ir para o git.

### 2) Gerar certificado/keystore do cliente (fake)

```bash
openssl req -x509 -newkey rsa:2048 \
  -keyout .mtls/client.key -out .mtls/client.crt \
  -days 3650 -nodes -subj "/CN=mtls-client"

openssl pkcs12 -export \
  -out .mtls/client.p12 \
  -inkey .mtls/client.key \
  -in .mtls/client.crt \
  -passout pass:"$CLIENT_PWD"
```

### 3) Gerar certificado do "partner" (fake) com SAN=localhost

```bash
openssl req -x509 -newkey rsa:2048 \
  -keyout .mtls/partner.key -out .mtls/partner.crt \
  -days 3650 -nodes -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost"
```

### 4) Criar truststore com o cert do partner

```bash
keytool -importcert -noprompt \
  -alias partner-leaf \
  -file .mtls/partner.crt \
  -keystore .mtls/truststore.p12 \
  -storetype PKCS12 \
  -storepass "$TRUSTSTORE_PWD"
```

### 5) Subir servidor fake exigindo certificado de cliente

Em um terminal:

```bash
openssl s_server -accept 9443 \
  -cert .mtls/partner.crt -key .mtls/partner.key \
  -Verify 1 -CAfile .mtls/client.crt -www
```

### 6) Build do app

```bash
mvn clean package
```

### 7) Rodar o app com smoke test mTLS (usa o RestTemplate mTLS)

Em outro terminal:

```bash
set -a; source .env; set +a
java -jar target/mtls-0.0.1-SNAPSHOT.jar \
  --server.port=0 \
  --partner.mtls.enabled=true \
  --partner.mtls.smoke.enabled=true \
  --partner.mtls.smoke.url=https://localhost:9443/
```

Log esperado:

- `Running mTLS smoke check against https://localhost:9443/`
- `mTLS smoke check OK: status 200 OK`

### Observacoes

- A porta **9443** evita conflito com processos que ja usam **8443**.
- Este setup e **apenas para validacao local**. Para producao use certificados reais do parceiro.

## Troubleshooting

- `PKIX path building failed`: truststore não possui CA/chain correta.
- `bad_certificate`: o certificado de cliente não corresponde à chave privada ou o servidor rejeitou.
- `handshake_failure`: mismatch de cipher/protocol ou pinning incorreto.
- `hostname mismatch`: o CN/SAN do certificado do parceiro não corresponde ao host usado na URL.

## Implicações operacionais

- **Pinning** exige atualização sempre que o parceiro rotaciona o certificado.
- **Truststore** é mais estável quando a cadeia é assinada por uma CA controlada.

## Build e execução local

```bash
mvn clean package
java -jar target/mtls-0.0.1-SNAPSHOT.jar
```

## Kubernetes

Os manifestos estão em `k8s/` e incluem `Secret`, `ConfigMap`, `Deployment`, `Service`, `HPA` e `NetworkPolicy`.
