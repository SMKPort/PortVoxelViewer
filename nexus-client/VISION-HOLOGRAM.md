# Nexus Client 2.9.5: Visão Arcana e Hologramas 3D

Mod 100% client-side, Minecraft 1.21.1 / NeoForge.

## Controles

Abra a central com **F9**, em **F8 > GERAL > VISÃO ARCANA E HOLOGRAMA 3D**
ou com `/nexusclient vision menu`.

A Visão Arcana inicia **desligada**. Ative a opção no menu ou use
`/nexusclient vision on`. Personalize a pergunta e clique em
**ANALISAR TELA UMA VEZ**. Também é possível executar
`/nexusclient vision capture` para uma análise com pergunta padrão.

O jogo fecha a tela de configurações, espera alguns ticks e captura **apenas
um quadro**. A imagem é reduzida para no máximo 640×360, codificada em JPEG
na memória (sem salvar no disco) e enviada via HTTPS, com o token de
pareamento da Render. O servidor exige `consent: true`, limita a 5 imagens
por dia de sessão, com intervalo mínimo de 60 segundos, e não armazena
capturas. A resposta aparece **somente no chat local** do usuário.

Use `/nexusclient vision off` para bloquear novas capturas.

## Hologramas gratuitos

Ative pelo mesmo menu ou por `/nexusclient hologram on`. Os três modelos
são `orb` (esfera), `runes` (círculo de runas) e `schematic` (cubo de
arestas). O menu permite alternar modelo, escala (0,5 a 2) e distância
(1,5 a 5 blocos) à frente do jogador. As partículas são exclusivamente
locais, não colocam blocos nem usam API. Preferências persistem no arquivo
`config/nexus-hologram.properties`.

Comandos adicionais: `/nexusclient hologram status`,
`/nexusclient hologram model orb`,
`/nexusclient hologram model runes`,
`/nexusclient hologram model schematic`, `/nexusclient hologram off`.

`/nexusclient permissions off` encerra os hologramas e revoga capturas
futuras. O modo **ATIVAR TUDO** não ativa a Visão Arcana automaticamente.

## Chave Gemini e segurança de custo

Na Render, a rota da imagem usa `NEXUS_VISION_FREE_API_KEY`
(Gemini 2.5 Flash-Lite). Essa variável é **separada** de
`NEXUS_GEMINI_API_KEY`, porém você pode preencher as duas com
**o mesmo valor** se a chave for de um projeto Google AI Studio
**sem faturamento ativo**. O sistema não consegue verificar o status
de cobrança do projeto, portanto não existe garantia automática de
custo zero se a chave tiver cobrança habilitada.

Não há fallback para outro modelo ou chave quando a cota acaba.
Não inclua tokens ou chaves no código nem no GitHub.
