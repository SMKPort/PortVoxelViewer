# Nexus Aterna 4.0.0-alpha.6 · Minecraft 1.21.1 NeoForge

A continuação da alpha.5 não inclui hologramas. O F9 abre a Visão Arcana.

## Avisos configuráveis
Em **Alertas do Nexus**, cada categoria (dano elevado, combo recebido, integridade crítica, risco, recuperação, vida crítica, consciência evolutiva e percepção arcana) pode ser **desligada**, exibida **somente no HUD**, publicada **somente no chat** ou enviada **aos dois**. O padrão seguro é HUD, sem publicar automaticamente. O intervalo entre alertas de combate continua configurável.

Avisos exclusivamente locais não precisam de acesso ao chat. Avisos públicos exigem `/nexusclient chat on`, `/nexusclient mind on`, `/nexusclient autonomy on`, acesso ao servidor e configuração do canal. Erros de conexão ou restrições do servidor não são confirmação de publicação.

## Canal personalizado único
Em Configurações, informe o comando do plugin de chat que seu servidor aceita (por exemplo, `/f chat`, `/rp` ou até cinco subcomandos fixos, limite de 64 caracteres). Não existe uma lista fechada de canais. Comandos administrativos, seletores, JSON, comandos dinâmicos, novas linhas e outros formatos não seguros são bloqueados. Depois de salvar, o site envia a configuração ao mod, que guarda em `config/nexus-chat-routing.json`. As mensagens manuais, as respostas da Voz do Nexus e as falas automáticas autorizadas usam o canal configurado. Caso o site seja fechado, o mod mantém seu último canal sincronizado.

## Visão Arcana
Histórico textual de até seis resultados. **Guardar relatórios** fica desligado inicialmente. O usuário pode ativar persistência local opcional ou apagar relatórios pelo F9. As imagens nunca são salvas em disco por esse recurso.

## Correções
- Comandos do site apontam para os nomes realmente registrados no mod.
- Reações do sensor arcano são agora acionáveis quando o módulo de reações está ativado, sob controle do destino escolhido.
- Proteção de vida, macros, VoiceCast, leitor do Iron's Spells e diário no Minecraft foram preservados.
- Não alteramos chaves privadas da Render nem podemos garantir acesso ao Gemini quando a API estiver indisponível.
