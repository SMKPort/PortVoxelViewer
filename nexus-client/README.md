# Nexus Aterna Client · NeoForge 1.21.1

**Mod Client-Side de Port Morningstar para o Nexus Aterna RP.** Pode ser instalado apenas no Minecraft do jogador, inclusive quando ele não tem acesso aos arquivos nem às permissões administrativas do servidor.

O código-fonte fica em `nexus-client/`, dentro da branch `nexus-aterna-rp`. O painel web é https://nexus-aterna-rp.onrender.com.

## Instalação

1. Use **Minecraft Java Edition 1.21.1** com **NeoForge 21.1.x**.
2. Baixe o JAR a partir do artefato do workflow **Nexus Aterna Client NeoForge 1.21.1** do GitHub Actions.
3. Coloque `nexusclient-1.1.0.jar` na pasta `.minecraft/mods/` do perfil 1.21.1.
4. Inicie o Minecraft e entre em um mundo ou servidor.
5. No painel Nexus RP (HTTPS), entre em **Configurações** e selecione **NEXUS CLIENT (MOD APENAS NO JOGADOR)**. Informe o código de acesso normal do painel e salve.
6. Abra **Nexus Client** no menu lateral. Clique em **Gerar código de pareamento**.
7. No chat do Minecraft digite o comando **local** `/nexusclient pair CODIGO` (substitua pelo código de 10 caracteres).
8. Para permitir que mensagens escritas no painel sejam realmente enviadas pelo seu jogador, digite `/nexusclient chat on`.
9. Para acompanhar vida e efeitos no Nexus, digite `/nexusclient telemetry on` e habilite Eventos do Jogo no painel.
10. Use **Testar conexão** no painel.

O servidor não precisa do mod, RCON, whitelist alterada ou conta de administrador.

## Comandos locais

| Comando | Efeito |
| --- | --- |
| `/nexusclient status` | Mostra status do vínculo e permissões |
| `/nexusclient pair CODIGO` | Pareia com o código temporário do painel |
| `/nexusclient chat on` | Autoriza o envio de mensagens pelo chat do seu jogador |
| `/nexusclient chat off` | Bloqueia novas mensagens remotas |
| `/nexusclient telemetry on` | Autoriza envio de vida, dano e IDs de efeitos ao Nexus |
| `/nexusclient telemetry off` | Desativa telemetria |
| `/nexusclient unpair` | Limpa credencial e revoga permissões locais |

Esses comandos são registrados no cliente NeoForge, **não** são executados pelo servidor Minecraft.

## Limitações honestas

- As mensagens aparecem como mensagens normais **da sua conta**, sujeitas a filtros de chat, cooldown e regras do servidor. O Nexus não se passa pelo console.
- O mod não executa comandos de administrador, não altera entidades de outros jogadores e não modifica o mundo do servidor.
- Os indicadores de energia arcana, integridade do Nexus, bônus Holy Spellpower, rituais e Protocolo TS continuam narrativos.
- Integrar/desintegrar/reparar/remodelar executa **avisos visuais e partículas locais**. Não altera, por si só, o modelo 3D do Armourer's Workshop. Para animações fiéis é necessário um adaptador para o sistema de armaduras/emotes instalado.
- A leitura de vida e efeitos é feita periodicamente no cliente (a cada três segundos), portanto impactos e curas muito rápidos podem se anular entre leituras.
- Quando o Render reiniciar, o vínculo pode precisar de novo pareamento porque a sessão do servidor é mantida em memória.
- Permissões de chat e telemetria começam desligadas no primeiro pareamento.

## Segurança e privacidade

O mod só faz requisições HTTPS ao domínio do Nexus RP. A credencial forte criada no pareamento é gravada localmente em `.minecraft/config/nexus-client.properties` e **não deve ser enviada para outras pessoas**. O servidor da Render guarda apenas o hash de validação do token. Se houver suspeita de comprometimento, use `/nexusclient unpair` e gere um novo código.

A ponte do Nexus aceita apenas mensagens curtas sem comandos `/` e ações locais permitidas. O mod também verifica uma autorização local explícita antes de enviar cada mensagem. O intervalo mínimo entre mensagens remotas é de 4,5 segundos.

## Compilação

Projeto Gradle ModDevGradle 2.x, Java 21, NeoForge 21.1.265. Use Gradle 9.2.1:

```bash
gradle -p nexus-client build --no-daemon
```

O resultado fica em `nexus-client/build/libs/nexusclient-1.1.0.jar`.

O workflow `.github/workflows/nexus-client-build.yml` compila automaticamente o mod na branch `nexus-aterna-rp` e disponibiliza o artefato ZIP com JAR após finalizar.

O JAR foi compilado no GitHub Actions. Ainda é recomendado testar no seu modpack específico para identificar conflitos de outros mods ou configurações do servidor.

## Canais de chat (Nexus Client 1.1.0)

No painel, vá em **Configurações > Comunicador do Jogador > Canal das Mensagens** e escolha **Local (/l)**, que é o padrão. Ao escrever `As placas estão se integrando.`, o mod chama a API do Minecraft para enviar `/l As placas estão se integrando.` usando `sendCommand`. **Não abre o chat, não pressiona T e não simula digitação**.

Também há **Global** (mensagem direta pelo `sendChat`) e **Personalizado**, que aceita apenas comandos de chat seguros listados na interface, como `/rp` e `/me`. Escreva apenas a mensagem no painel, sem barra inicial. O prefixo `[NEXUS ATERNA]` é opcional e, no modo cliente, fica desmarcado inicialmente.

O comando `/l` deve existir no servidor e respeita as regras do plugin de chat local. O resultado no jogo deve ser testado no seu servidor. Para aplicar a novidade, **substitua o JAR antigo pelo Nexus Client 1.1.0** e reinicie o Minecraft.
