# Port Nexus AI Gateway 3.2 (Gemini grátis)

Backend Node.js sem dependências externas. A voz é transcrita no Android. O backend interpreta a frase e propõe um plano de até seis ações, com validação rígida. **O usuário confirma no Nexus antes de qualquer ação.** O servidor não controla diretamente o celular.

## Ativar Gemini no Render sem pagar pela API OpenAI

1. Entre em https://aistudio.google.com/api-keys e crie sua chave gratuita do Gemini.
2. Abra https://dashboard.render.com/web/srv-dblkroijnfac739kt2s0 e vá em Environment.
3. Adicione as variáveis:
   - `NEXUS_AI_PROVIDER=gemini` (define o provedor)
   - `GEMINI_MODEL=gemini-3.5-flash-lite` (modelo com nível gratuito, sujeito a cotas)
   - `GEMINI_API_KEY=<chave do Google AI Studio>` (valor secreto, apenas no Render)
   - `NEXUS_CLIENT_TOKEN` deve continuar o mesmo token pessoal já configurado.
4. Salve e aguarde o deployment ficar `live`.
5. Verifique https://port-nexus-ai-gateway.onrender.com/health: deve conter `"provider":"gemini"` e `"ai_configured":true`.
6. No Nexus Voice, **mantenha o endereço HTTPS e o token pessoal anteriores**. Não precisa de outro APK apenas para trocar o provedor.

O plano gratuito tem limites e condições próprias, depende da região e pode permitir uso das solicitações para melhoria de produtos do Google. Consulte a página de preços antes de ativar qualquer faturamento: https://ai.google.dev/gemini-api/docs/pricing. Não habilite uma modalidade paga se quiser permanecer sem custos da API.

A variável antiga `OPENAI_API_KEY` é ignorada quando `NEXUS_AI_PROVIDER=gemini`, então o servidor não faz solicitações à OpenAI. Para reduzir confusão, você pode removê-la manualmente do Render depois.

## Modo OpenAI opcional

Para voltar à integração original, defina `NEXUS_AI_PROVIDER=openai` e `OPENAI_API_KEY` (API paga separadamente do ChatGPT Plus). `OPENAI_MODEL` opcional, padrão `gpt-4o-mini`.

## Segurança e funcionamento

- `GET /health` fornece status, provedor e presença da chave (nunca seu valor).
- `POST /v1/plan` exige `Authorization: Bearer <NEXUS_CLIENT_TOKEN>`, payload `{"utterance":"..." }`.
- As chaves do Gemini/OpenAI ficam **somente no Render**, nunca embutidas no APK, links públicos ou repositório.
- Token do launcher é separado das chaves das IAs.
- A resposta do modelo é validada contra uma lista fixa de ações, não há shell, root, instalação, compras, nem envio autônomo de mensagens.
- O servidor não registra transcrições e usa `no-store` para OpenAI. Outros fornecedores têm políticas próprias.
- Se a IA falhar, o Nexus recorre aos comandos locais sem internet.

## Desenvolvimento

`node --test PortNexusLauncher/nexus-ai-server/server.test.mjs`

O script testa seleção de provedor, isolamento das chaves, limitação de ações, planos e erros de cotas usando respostas simuladas. Sem acesso a uma chave Gemini real, não se pode afirmar que uma chamada real foi aprovada.
