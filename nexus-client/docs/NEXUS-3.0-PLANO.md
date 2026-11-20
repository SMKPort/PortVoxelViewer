# Nexus Aterna 3.0 | Plano de implementação (client-side)

> Estado: planejamento aprovado para desenvolvimento em branch isolada. **Este documento não representa recursos já implementados.**
> Base imutável de referência: `nexus-aterna-rp` no commit `1e33facae337ef34c8725c6d4218c56ec6bdf0a5` (2.9.4).
> Trabalho: `nexus-aterna-3.0`, sem alterar o comportamento da 2.9.4 antes da validação.

## Objetivo e contrato técnico

Criar um assistente tático **determinístico, local e auditável** para Minecraft 1.21.1 / NeoForge 21.1.265 / Java 21. A 3.0 reúne cinco frentes, nesta ordem de dependência: consciência situacional, núcleo de decisões, construtor de macros, protocolos de combate, memória contextual e VoiceCast inteligente. A UI será integrada ao F8, mantendo F6 (voz), F7 (HUD) e N (canal privado).

- **Client-side real:** ler somente dados recebidos/visíveis no cliente; nunca presumir dano confirmado, cooldown remoto, posição invisível de jogadores, comandos de operador ou poderes administrativos. Magias e itens usam somente as interações normais do jogo validadas pelo servidor.
- **Offline-first:** telemetria Render, Gemini, TTS remoto, Plasmo e canal público são opcionais e independentes. A lógica tática, as macros, a memória manual e o Vosk local devem funcionar sem esses serviços.
- **Consentimento:** novos perfis, macros e uso automático de consumíveis começam DESLIGADOS. `permissions all` não concede automaticamente execução de ações de combate, gravação contínua do microfone, registro de conversas alheias ou envio de memórias.
- **Integridade:** arquivos 2.9.4 são preservados; `permissions off` suspende decisões e voz, mas não apaga memórias. Versionar schemas e usar escrita atômica. Nunca gravar tokens ou transcrições de terceiros em logs.
- **Compatibilidade:** ausência/desatualização do Iron's Spells ou de magia equipada produz diagnóstico e bloqueio seguro, não crash.

## Base 2.9.4 a reutilizar

| Existente | Papel na 3.0 | Ajuste necessário |
|---|---|---|
| `NexusClientMod` (`ClientTickEvent.Post`, ataques, F6/F7/F8) | Entrada de eventos e integração da UI | Delegar orquestração, reduzir lógica central |
| `NexusArcaneSensor`, `NexusCombatAlerts`, `ClientLink` | Observações locais de magia/vida | Normalizar eventos e separar leitura de envio remoto |
| `NexusAutoHeal`, `NexusCombatMacro`, `NexusBuffManager` | Atuadores existentes de Heal, Divine Smite e hotbar | Passar por árbitro único para evitar disparos simultâneos |
| `NexusSpellAccess` | Adaptador da roda Iron's Spells 1.21.1 | Reuso da API `getAllSpells()/getSpellData()`, sem `getSpellCount()` |
| `NexusOwnerMemories`, `NexusMemoryRecall`, `NexusMind` | Memórias explícitas, busca textual, observações narrativas | Adicionar metadados/contexto sem perder os 200 registros existentes |
| `NexusVoiceCasting`, `NexusVoiceGrammar`, `NexusMicrophoneScreen` | Vosk offline, palavras de ativação, diagnósticos | Roteamento de intenções e calibração, sem execução arbitrária |
| `NexusPermissions`, `NexusPermissionsScreen` | Consentimento e interrupção global | Permissões granulares por ação e submenus compactos |
| `verification/*` e `nexus-client-build.yml` | Testes de políticas Java puras e JAR GitHub Actions | Estender com regressões 3.0 e CI da branch |

## Arquitetura proposta

```text
Observadores client-side (Minecraft + Iron's Spells)
    -> NexusSituationSampler (somente main client thread, amostra leve)
    -> NexusSituationSnapshot (imutável, atributos UNKNOWN explícitos)
    -> NexusSituationPolicy (classificação determinística, sem LLM)
    -> NexusTacticalEngine (prioridades e cooldowns)
        -> NexusActionGate (permissão por ação, perfil, tela aberta, mundo ativo)
        -> NexusActionCoordinator (uma ação ativa/vez; não disputa hotbar/QuickCast)
            -> Atuadores existentes: AutoHeal, CombatMacro, BuffManager
    -> NexusTacticalJournal (motivo/resultado, local, com retenção)
    -> F8/HUD + NexusContextMemory (opcional)
Vosk mic -> NexusVoiceIntentParser (gramática limitada)
         -> NexusActionGate -> mesmo Coordinator
```

**Estados e fronteiras:** snapshot e políticas são classes Java puras para testes sem Minecraft. Adapters de mundo, som e Iron's Spells ficam isolados. A UI nunca executa ações implicitamente ao abrir ou alternar abas.

### Eventos e limitações de observação

`HEALTH_LOW`, `DAMAGE_OBSERVED`, `MANA_LOW`, `EFFECT_EXPIRING`, `SAME_TARGET_HIT_CONFIRMED_CLIENT`, `NEARBY_CAST_OBSERVED`, `DEATH`, `RESPAWN` e `WORLD_CHANGED`. O rótulo "confirmado no cliente" significa a animação/estado recebido, não dano confirmado pelo servidor. Valores indisponíveis devem ser `UNKNOWN`, nunca inventados. Distância/ameaça só considera entidades que o cliente conhece.

**Ritmo inicial:** snapshot leve a cada 5 ticks, varredura mais custosa a cada 20 ticks, eventos de ataque quando disponíveis. Ajustar após profiling. Nunca bloquear a thread do Minecraft em áudio, rede, busca ou gravação em disco.

### Ordem de prioridades e conflitos

1. `permissions off` / pausa / desconexão / troca de mundo cancelam fila pendente.
2. Cancelamento por voz autorizado tem preferência.
3. Cura emergencial já autorizada.
4. Comando manual de voz autorizado e válido.
5. Macro configurada; buff de combate por último.
6. Alertas e sugestões (não ocupam o slot de ação).

**Regra:** no máximo uma tentativa de `QuickCast` ou uma troca/uso da hotbar por janela de ação. Nunca interromper uma conjuração/uso em andamento sem comando explícito. Cooldowns de segurança independentes do estado remoto. Falha em `QuickCast` é relatada como "solicitação enviada", não como magia garantida.

## Entregas incrementais

### Fase 0 | Isolamento, testes e compatibilidade

- [ ] Fixar commit-base 2.9.4 e preparar branch/PR draft da 3.0.
- [ ] Incluir branch de desenvolvimento e `pull_request` no CI do Nexus; manter Java 21 e build NeoForge.
- [ ] Adicionar testes de regressão para arquivos de configuração 2.9.4, `permissions off`, mundo desconectado, Iron's Spells ausente, GUI aberta e múltiplos atuadores.
- [ ] Definir incremento de versão `3.0.0-beta.1` **somente quando houver código funcional**; não declarar release na fase documental.

**Aceite:** CI verde e 2.9.4 intacta, sem alteração não autorizada de flags/configs.

### Fase 1 | Consciência situacional

- [ ] `NexusSituationSnapshot`, `NexusSituationSampler`, `NexusSituationPolicy`, `NexusTacticalJournal`.
- [ ] Classificação `NORMAL`, `ATENCAO`, `PERIGO`, `CRITICO` com limiares configuráveis, histerese e debounce.
- [ ] Avisos locais sem spam, com causa verificável (HP, mana, magia observada, efeitos); não publicar no chat automaticamente.
- [ ] Testes de dados ausentes, mudança rápida de HP, desconexão, morte/respawn, grandes grupos e orçamento por tick.

**Aceite:** HUD/diagnóstico mostra estado e motivos sem ações automáticas; sem Render/Gemini/Plasmo.

### Fase 2 | Motor de ações e construtor de macros

- [ ] `NexusTacticalEngine`, `NexusActionGate`, `NexusActionCoordinator`, `NexusMacroDefinition`, `NexusMacroValidator`, `NexusMacroEngine`.
- [ ] Editor F8: nome, ativação manual/automática, gatilho, condições, ação permitida, cooldown e botão `SIMULAR`.
- [ ] Gatilhos: hits no mesmo alvo, HP abaixo de %, mana acima de %, efeito ausente/prestes a expirar e comando de voz exato. Combinações `AND` limitadas; máximo de 3 etapas e sem loops.
- [ ] Ações aprovadas: sugerir/avisar, solicitar QuickCast de magia **equipada**, usar item da hotbar cadastrado; sem execução de scripts, comandos arbitrários de chat, RCON ou envio de rede.
- [ ] Migrar a macro "3 golpes -> Divine Smite" mantendo JSON legado e comportamento até o usuário optar pela nova configuração.
- [ ] Testar prioridades, anti-spam, tela aberta, efeito desconhecido, combo em alvos diferentes, cooldown e colisão Heal x Smite x buff.

**Aceite:** simulador relata a decisão e o motivo antes de permitir habilitação real; nenhum disparo duplicado.

### Fase 3 | Protocolos de combate

- [ ] `NexusProtocolPolicy` + seleção F8: `OBSERVADOR`, `DEFENSIVO`, `OFENSIVO`, `EMERGENCIA`.
- [ ] Cada perfil tem limiares, limites de frequência e permissões por ação. Trocar perfil **não habilita** magias/buffs por si só.
- [ ] Em emergência, Heal equipado/autorizado pode ser priorizado; sem magia ou mana, só alerta.
- [ ] Salvar perfil e permitir pausa geral instantânea. Nenhum item de último estoque será usado automaticamente sem permissão individual já existente.

**Aceite:** mesma situação gera decisões distintas conforme perfil, e o kill switch funciona imediatamente.

### Fase 4 | Memória contextual

- [ ] Expandir `NexusOwnerMemories` por um repositório contextual separado, sem modificar `nexus-owner-memories.json` existente; copiar/importar somente com confirmação.
- [ ] Salvar nota manual via F8/comando, com assunto, mundo/servidor (quando disponível), dimensão, coordenadas opcionais, data, fonte e tags. Eventos automáticos somente com opt-in explícito.
- [ ] Consultar por assunto e contexto (`Nexus, onde estava a oficina?`) com evidência/registro e resposta "não encontrei" se não houver resultado.
- [ ] Busca determinística com limites de armazenamento, exportar/importar JSON e exclusão por registro. Não gravar microfone, Plasmo, conversa de terceiros ou coordenadas privadas sem consentimento granular.
- [ ] Não misturar mundo single-player e servidores automaticamente.

**Aceite:** memória sobrevive à reinicialização, não vaza para Render e não é apagada por `permissions off`.

### Fase 5 | VoiceCast inteligente

- [ ] Preservar Vosk local com ativação de sessão por F6 (não ligar o microfone ao iniciar o jogo).
- [ ] Assistente de teste: dispositivo, nível, silêncio/ruído, frase reconhecida, confiança, magia equipada, motivo de rejeição; opção push-to-talk e wake word.
- [ ] `NexusVoiceIntentParser`: frases exatas e aliases pt-BR para comandos de consultar estado, perguntar memória, escolher protocolo e solicitar macro **autorizada**.
- [ ] Para ações arriscadas (consumível/alteração de perfil), confirmação explícita opcional/obrigatória conforme política. "Nexus, cancelar" sempre prioritário.
- [ ] Testar voz simultânea com combate, chat aberto, foco perdido, dispositivo desconectado, modelo ausente e frases parecidas. Captura/transcrição ficam locais e efêmeras.

**Aceite:** comandos incorretos não executam ações, diagnóstico explica motivo e nenhuma transcrição é enviada ao servidor.

### Fase 6 | Integração e candidata 3.0

- [ ] F8 com submenus compactos, navegáveis em escalas GUI comuns, e F7/HUD somente visualização.
- [ ] Testes Java puros + Gradle mod, checagem de desempenho, troca de mundos, queda de FPS, comandos e migração.
- [ ] Smoke test manual no Somake (itens reais e feitiços equipados, respeitando regras do servidor).
- [ ] Publicar JAR `3.0.0-rc.1` como artifact após aprovação; release 3.0 só após validação no jogo.

## Arquivos novos previstos

```text
nexus-client/src/main/java/com/port/nexusclient/
  NexusSituationSnapshot.java
  NexusSituationSampler.java
  NexusSituationPolicy.java
  NexusTacticalEngine.java
  NexusActionGate.java
  NexusActionCoordinator.java
  NexusMacroDefinition.java
  NexusMacroValidator.java
  NexusMacroEngine.java
  NexusProtocolPolicy.java
  NexusContextMemory.java
  NexusVoiceIntentParser.java
  NexusTacticalScreen.java
nexus-client/verification/
  NexusSituationPolicyTest.java
  NexusActionCoordinatorTest.java
  NexusMacroValidatorTest.java
  NexusProtocolPolicyTest.java
  NexusContextMemoryTest.java
  NexusVoiceIntentParserTest.java
```

Nomes são contratos de planejamento; mudanças de nomes/agrupamento são aceitáveis se os limites de responsabilidade forem preservados.

## Critérios de conclusão 3.0

1. Build GitHub Actions aprovado para NeoForge 1.21.1 / Java 21.
2. Regressões 2.9.4 preservadas, inclusive VoiceCast 2.9.4, três golpes -> Divine Smite, auto Heal, buffs autorizados e memórias.
3. Mundo sem Iron's Spells, sem Render, sem microfone ou sem internet não causa crash.
4. Nenhum fluxo ativa ações automáticas ou escuta sem opt-in; `permissions off` interrompe ações e preserva memórias.
5. Um único coordenador arbitra todas as tentativas de usar hotbar/QuickCast.
6. Logs locais mostram observação, regra, ação pretendida, decisão e resultado sem expor dados de terceiros.
7. Testes reais no modpack confirmam que os pacotes normais foram aceitos pelo servidor. Nenhum teste unitário substitui essa confirmação.

## Ordem sugerida para o primeiro ciclo

Implementar Fase 0 + Fase 1 primeiro. A consciência situacional deve observar e justificar decisões **sem disparar magias**. Só depois integrar motor, macros, protocolos, memória e voz, liberando uma função de cada vez atrás de permissão específica.
