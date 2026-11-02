# Ativar servidor e IA no Port Arcane Codex 1.0

O backend já está em `PortArcaneCodex/server`; o manifesto para hospedagem Render está na raiz do repositório privado, em `render.yaml`.

## 1. Credenciais necessárias

A chave `OPENAI_API_KEY` é criada pelo proprietário do projeto em https://platform.openai.com/api-keys . **Nunca cole essa chave em conversas ou no repositório.** Verifique o faturamento da API em https://platform.openai.com/settings/organization/billing/overview. O ChatGPT Plus não inclui por padrão os créditos da API.

A credencial `APP_ACCESS_TOKEN` é outra chave privada, de pelo menos 32 caracteres, usada pelo aplicativo Android no cabeçalho HTTP Authorization. Gere uma sequência criptograficamente aleatória em um local seguro, guarde-a no seu gerenciador de senhas e insira **exatamente o mesmo valor** no Render e em Assistente > Configurações no aplicativo. Nunca registre este token no GitHub.

## 2. Publicar servidor usando Blueprint Render

1. Acesse https://dashboard.render.com/ e conecte sua conta GitHub.
2. Autorize o repositório **privado** `SMKPort/PortVoxelViewer`. Não torne o repositório público.
3. Escolha **New > Blueprint** e selecione este repositório no branch `main`.
4. Use `render.yaml` da raiz. O serviço criado é `port-arcane-codex-api`, tipo Web Service Node.js, diretório `PortArcaneCodex/server` e plano `free`.
5. Durante a criação, preencha os campos secretos `OPENAI_API_KEY` e `APP_ACCESS_TOKEN` (ambos definidos como `sync: false` no Blueprint). No caso de um serviço já existente, configure as variáveis manualmente no painel do serviço.
6. Confira que o deploy ficou **Live** e copie a URL HTTPS **real** que aparece no painel Render.
7. Abra a URL seguida de `/health`; a resposta esperada é `{"ok":true,"name":"arcane-codex-api"}`. Isso apenas testa a disponibilidade. Para testar a IA, o aplicativo deve fazer uma pergunta autenticada.

A URL é atribuída pelo Render. Não deduza o endereço somente pelo nome do serviço. No plano gratuito, a primeira requisição depois de inatividade pode demorar, pois o servidor dorme.

## 3. Conectar o aplicativo

Na aba **Assistente**, abra o ícone de engrenagem e configure:

- **URL do servidor:** HTTPS real exibida no Render, sem /api/chat nem /health;
- **Token privado:** valor de `APP_ACCESS_TOKEN`.

Salve e envie uma pergunta, por exemplo: `O que você sabe sobre Anael?`. Um HTTP 401 significa token divergente; HTTP 502 significa falha de chave, créditos, modelo ou conectividade da API; erro de conexão aponta para URL incorreta, servidor inativo ou rede.

## 4. Notas de segurança

A chave OpenAI existe somente no servidor Render; o aplicativo recebe apenas o token de acesso. Não inclua segredos em screenshots, commits, arquivos de backup, mensagens ou logs. Troque as duas credenciais se elas vazarem. A `/health` é pública, mas o endpoint `/api/chat` exige token de acesso.
