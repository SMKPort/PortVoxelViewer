# Nexus AI Gateway 3.0

Backend Node.js (sem dependências externas) para interpretar comandos livres, criar planos JSON com ações permitidas e executá-los **somente após confirmação no Android**. O servidor **não executa ações no celular**.

## Configuração no Render
- Root directory: raiz do repositório
- Branch: `port-nexus-launcher`
- Build command: `node --version`
- Start command: `node PortNexusLauncher/nexus-ai-server/server.mjs`
- Plan: free (se disponível)
- Environment: `OPENAI_API_KEY` (segredo, apenas servidor), `NEXUS_CLIENT_TOKEN` (token exclusivo do usuário), `OPENAI_MODEL` (opcional, padrão `gpt-4o-mini`).
- Endpoints: `GET /health` e `POST /v1/plan` com `Authorization: Bearer <NEXUS_CLIENT_TOKEN>` e `{"utterance":"..."}`.

O ChatGPT Plus não substitui a chave da API e chamadas à API são faturadas separadamente.

## Limites e segurança
- Não colocar `OPENAI_API_KEY` no APK ou no GitHub.
- Acesso é obrigatório por token, HTTPS e limite simples por IP.
- O modelo retorna somente ações do enum fixo. Servidor e cliente validam todas.
- A saída é uma **proposta**, não é executada automaticamente. O usuário revisa e confirma.
- Sem comandos root, shell, AccessibilityService irrestrito, envio de mensagens, compra ou pagamentos.
- Não faz log das transcrições nem armazena o corpo da solicitação neste código. A API e infraestrutura têm próprias políticas de tratamento de dados.
- Caso a IA não esteja disponível, Nexus Voice continua usando o motor local da versão 2.4.

Teste local: `cd PortNexusLauncher/nexus-ai-server && node --test`. Sem `OPENAI_API_KEY`, o endpoint informa que falta configurar o servidor.
