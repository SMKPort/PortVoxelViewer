# Port Arcane Codex 1.0 ✦

Grimório particular Android de Port Morningstar, com seis abas: **Assistente, Orações, Rituais, Grimório, Lore e Biblioteca**. Interface preto/vermelho/dourado, registros locais SQLite, PIN de abertura e IA opcional por servidor protegido.

## Como funciona

- **Orações / Rituais / Grimório / Lore**: criar, editar, excluir e classificar itens como `rascunho` ou `canon`.
- **Biblioteca**: busca geral; backup JSON copiado para a área de transferência; importação de backup por colagem.
- **Assistente**: conversa armazenada localmente; lê os registros canônicos nas requisições de IA e permite salvar respostas como rascunhos. A IA **nunca** aprova cânone automaticamente.
- **Acesso privado**: PIN de 6–12 dígitos salvo no SecureStore; o aplicativo bloqueia ao ir para segundo plano.
- **Primeira abertura**: inclui registros canônicos de Anael, Port, braço arcano e ritual O Preço da Verdade, editáveis.

## Executar Android

1. Instale Node.js 20+ e Expo Go no Android.
2. Na pasta do projeto, execute `npm install` e depois `npx expo start`.
3. Escaneie o QR code com Expo Go. Na primeira abertura, crie seu PIN.
4. Para um APK instalável, instale EAS CLI (`npm i -g eas-cli`), faça login (`eas login`) e execute `eas build:configure` e `npm run apk`. O perfil `preview` gera `.apk` para instalação direta, após vincular o projeto à sua conta Expo.

> O projeto inclui `eas.json`, mas **nenhum APK foi compilado nesta entrega**. O EAS Build depende de autenticação Expo e do serviço de compilação.

## Ativar IA (servidor próprio)

1. `cd server && npm install`.
2. Copie `server/.env.example` para `server/.env` e preencha `OPENAI_API_KEY`, `APP_ACCESS_TOKEN` (token aleatório com 32 caracteres ou mais), `OPENAI_MODEL` e opcionalmente `PORT`.
3. Execute `npm start`. Para uso no telefone, publique o servidor em HTTPS, com variáveis de ambiente configuradas no provedor de hospedagem e logs protegidos.
4. Em **Assistente > Configurações**, preencha a URL HTTPS do servidor e o mesmo `APP_ACCESS_TOKEN`.

A chave OpenAI fica **somente no backend**, nunca no APK. O servidor não armazena cânone ou mensagens, mas envia o pedido e o contexto canônico à API da OpenAI quando você usa o assistente. As conversas e a lore são salvas no SQLite **local** do telefone. O modelo pode gerar erros e deve ser revisado antes de aprovar novos fatos. O uso da API exige conta/créditos próprios, separados da assinatura ChatGPT.

## Privacidade e limitações da 1.0

O PIN protege o acesso casual à interface, mas **o SQLite local não tem criptografia de conteúdo**. Use bloqueio de tela e proteção do dispositivo. A perda do PIN pode exigir reinstalar e perder dados sem backup. Backups JSON copiados não são criptografados, então salve-os num local privado. Não há sincronização entre dispositivos nem integração automática com memórias do ChatGPT. A IA só conhece os registros canônicos enviados pelo próprio aplicativo.

O backend usa token Bearer privado, HTTPS recomendado e limite de 40 chamadas a cada 15 minutos. Configure acesso exclusivo e não exponha `.env` no GitHub. Para um produto com dados particularmente sensíveis, adicione criptografia de banco, autenticação mais forte e recuperação segura de dados antes de utilizar.

## Estrutura

- `App.tsx` telas Android, PIN, edição e integração de IA.
- `src/db.ts` SQLite, sementes canônicas e backup.
- `src/ai.ts` cliente HTTP com configuração privada.
- `server/` proxy autenticado à OpenAI Responses API.
- `eas.json` perfil APK Android.

## Testes

- `npm run check`: tipos TypeScript.
- `npm run test:server`: testes de validação e construção de contexto.

Projeto destinado a histórias de fantasia e RP. Rituais e orações no app são ficcionais.
