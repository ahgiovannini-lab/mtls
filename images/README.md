# 📘 README — Jornada Conceitual de mTLS Outbound em Microserviços Java

## Visão geral

Este material descreve, de forma **conceitual e progressiva**, a jornada completa de uma integração **outbound mTLS** entre um microserviço Java (Spring Boot) e a API de um parceiro externo exposta por um Gateway que exige autenticação mútua via TLS.

O objetivo não é apresentar código ou configurações, mas sim **construir o modelo mental correto** para desenvolvedores Java/Spring que já conhecem HTTP e APIs, mas não têm domínio sobre mTLS.

As imagens associadas a este README foram pensadas como apoio visual para:
- onboarding de desenvolvedores
- documentação de integrações B2B
- alinhamento arquitetural
- revisão de segurança e infraestrutura

---

## 1️⃣ Visão geral da jornada

A jornada começa com um cenário simples: um microserviço cliente precisa consumir uma API de um parceiro externo. Entre esses dois sistemas existe um Gateway que atua como ponto de controle de segurança e exige mTLS.

O aspecto central dessa arquitetura é que **não existe comunicação HTTP direta** entre cliente e API sem que, antes, exista um processo explícito de confiança mútua. O canal seguro é estabelecido antes de qualquer requisição HTTP ser enviada, e somente após essa etapa a comunicação da aplicação ocorre.

Essa visão geral estabelece o princípio fundamental do mTLS: **antes de trocar dados, os sistemas precisam provar quem são e validar quem está do outro lado**.

---

## 2️⃣ Identidade do cliente (Client Certificate)

No contexto de mTLS, o microserviço cliente possui uma identidade formal. Essa identidade não é representada por tokens, API keys ou senhas, mas por um **certificado digital associado a uma chave privada**.

O certificado público pode ser compartilhado com o parceiro, enquanto a chave privada permanece exclusivamente sob posse do serviço cliente. Durante o handshake TLS, o cliente prova que possui essa chave privada sem nunca transmiti-la.

Esse modelo elimina a dependência de segredos compartilhados e estabelece uma autenticação forte baseada em criptografia assimétrica. A identidade do serviço passa a ser verificável e auditável.

---

## 3️⃣ Confiança no servidor: Truststore ou Pinning

A autenticação mútua exige confiança nos dois sentidos. Assim como o parceiro precisa confiar no cliente, o cliente também precisa confiar no servidor do parceiro.

Essa confiança pode ser estabelecida de duas formas principais:

- **Truststore**, onde o cliente confia em uma CA ou cadeia de certificados que assina o certificado do servidor.
- **Pinning**, onde o cliente confia explicitamente em um certificado ou chave pública específica do servidor, usando fingerprints ou SPKI.

Cada abordagem possui trade-offs claros. Truststore oferece mais flexibilidade para rotação de certificados, enquanto pinning oferece maior restrição e controle, ao custo de maior esforço operacional. A escolha entre essas estratégias é arquitetural e depende do nível de governança e previsibilidade do parceiro.

---

## 4️⃣ Deploy seguro em Kubernetes

Os certificados e truststores utilizados no mTLS não fazem parte da imagem Docker da aplicação. Eles são fornecidos ao serviço **em tempo de execução**, por meio de mecanismos seguros da plataforma, como Kubernetes Secrets montados como volumes.

Esse modelo garante:
- separação entre código e segredo
- portabilidade entre diferentes provedores de infraestrutura
- possibilidade de rotação de certificados sem rebuild de imagem

A imagem da aplicação permanece genérica e reutilizável, enquanto os artefatos sensíveis são injetados dinamicamente no ambiente de execução.

---

## 5️⃣ Onde o mTLS realmente acontece

Um erro comum é associar mTLS diretamente ao servidor HTTP da aplicação ou ao framework de segurança. No caso de chamadas outbound, o mTLS **não ocorre no Jetty, nem no Spring Security**.

Ele acontece na camada do **cliente HTTP**, especificamente durante o handshake TLS realizado pelo SSLContext utilizado pelo RestTemplate e pelo HttpClient subjacente.

Essa distinção é crucial para troubleshooting e desenho arquitetural: se o handshake TLS falhar, a requisição HTTP nunca chega a existir.

---

## 6️⃣ Handshake mTLS: autenticação mútua

Durante o handshake TLS com mTLS, ocorre uma sequência clara de validações:

1. O servidor apresenta seu certificado.
2. O cliente valida esse certificado com base na estratégia de confiança configurada.
3. O servidor solicita o certificado do cliente.
4. O cliente apresenta seu certificado e prova posse da chave privada.
5. O servidor valida a identidade do cliente.

Somente após essas etapas, a sessão TLS é estabelecida. Esse processo garante que ambos os lados saibam exatamente com quem estão se comunicando antes de qualquer troca de dados da aplicação.

---

## 7️⃣ HTTP como consequência do TLS

Com a sessão TLS estabelecida, o canal seguro passa a existir. A partir desse momento, requisições HTTP podem trafegar dentro desse túnel criptografado e autenticado.

Isso reforça um ponto essencial: **HTTP não é o mecanismo de segurança primário nessa arquitetura**. Ele é apenas o protocolo de aplicação que passa a operar depois que a segurança já foi garantida pelo TLS.

---

## 8️⃣ Falhas comuns e seus impactos

Grande parte dos problemas em integrações mTLS ocorre antes da camada HTTP. Erros como truststore incorreto, pin inválido, rejeição do certificado do cliente ou mismatch de hostname fazem com que o handshake TLS falhe.

Quando isso acontece:
- nenhuma requisição HTTP chega ao parceiro
- não há logs de controller ou camada de aplicação
- os erros aparecem como exceções de handshake TLS

Entender onde essas falhas acontecem evita longas sessões de debugging em camadas que nunca chegaram a ser executadas.

---

## 9️⃣ Modelo mental consolidado

Ao final da jornada, o modelo mental correto pode ser resumido em alguns princípios fundamentais:

- O microserviço possui uma identidade criptográfica própria.
- A confiança no servidor é explícita e configurada.
- O mTLS acontece antes de qualquer comunicação HTTP.
- Certificados e chaves não fazem parte da imagem da aplicação.
- Segurança é estabelecida no nível do transporte, não da aplicação.

Com esse entendimento, decisões técnicas e diagnósticos se tornam muito mais simples e previsíveis.

---

# 📚 Seção adicional — Descrição discursiva da jornada visual

As nove imagens associadas a este material representam uma narrativa contínua sobre como uma integração segura baseada em mTLS é construída e operada.

A jornada começa com uma visão ampla da comunicação entre sistemas, destacando a presença de um gateway como ponto de controle de segurança. Em seguida, o foco se desloca para a identidade do serviço cliente, evidenciando o papel do certificado digital e da chave privada como elementos centrais da autenticação.

A narrativa avança para a relação de confiança com o servidor, mostrando que essa confiança não é implícita, mas sim explicitamente configurada por meio de autoridades certificadoras ou mecanismos de pinning. O contexto operacional é então introduzido, demonstrando como plataformas como Kubernetes fornecem os meios adequados para injetar certificados e segredos de forma segura no ambiente de execução.

Com a base preparada, as imagens passam a detalhar o fluxo técnico real da comunicação, deixando claro que o mTLS ocorre antes do HTTP e que é responsabilidade do cliente HTTP, e não do servidor da aplicação. O handshake mTLS é representado como um processo de validação mútua, no qual ambas as partes se autenticam antes de permitir qualquer troca de dados.

A sequência visual reforça que o HTTP só passa a existir após o estabelecimento do canal seguro, e que falhas comuns interrompem o fluxo ainda na fase de handshake. Por fim, a jornada se encerra com a consolidação do modelo mental, sintetizando identidade, confiança e canal seguro como os três pilares do mTLS.

Essa abordagem visual e discursiva permite que desenvolvedores compreendam o mTLS não como um conjunto de configurações isoladas, mas como um fluxo lógico e coerente de decisões arquiteturais e técnicas.
