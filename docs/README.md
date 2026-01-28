# Diagramas — mTLS outbound

## Por onde começar

1) **Diagrama de Componentes (C4)**: visão rápida de responsabilidades e limites.
2) **Diagrama de Sequência**: fluxo completo do provisioning até o handshake e erros.
3) **Anti-patterns**: armadilhas comuns para revisão técnica.

## Como os diagramas se complementam

- **Componentes** responde “quem faz o quê” (aplicação, HTTP client e TLS).
- **Sequência** responde “quando acontece” (handshake antes do HTTP e modos de validação).
- **Anti-patterns** reforça boas práticas e riscos operacionais.

## Mermaid

Arquivos:
- `component-mtls-outbound.mmd`
- `sequence-mtls-outbound.mmd`
- `anti-patterns-mtls-outbound.mmd`

Renderize em:
- VS Code (extensão Mermaid)
- https://mermaid.live

## PlantUML

Arquivos:
- `component-mtls-outbound.puml`
- `sequence-mtls-outbound.puml`
- `anti-patterns-mtls-outbound.puml`

Renderize em:
- VS Code (extensão PlantUML)
- https://www.plantuml.com/plantuml/
