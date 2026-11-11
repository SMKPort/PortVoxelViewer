# Agente Nexus Aterna RP 1.2 para Minecraft Java 1.21.1

O agente Python permite **conectar o Minecraft ao painel sem publicar a porta RCON na internet**. O processo roda na mesma máquina que hospeda o servidor de Minecraft e se conecta por HTTPS à Render. Funciona com servidor Vanilla/NeoForge que tenha RCON habilitado.

## Configuração na Render

Acesse seu serviço **nexus-aterna-rp** em https://dashboard.render.com/web/srv-dbq75mrncjis73bm9c1g, abra **Environment** e confira a variável privada `NEXUS_RP_AGENT_KEY`. Ela já foi criada na Render e deve ser copiada para o ambiente do agente, **não para uma mensagem pública ou repositório**.

O código já existente `NEXUS_RP_ACCESS_KEY` continua sendo usado para autenticar o painel web. É diferente da chave do agente.

## Configuração do Minecraft

Em `server.properties`:

```properties
enable-rcon=true
rcon.port=25575
rcon.password=COLOQUE_SUA_SENHA_FORTE_AQUI
```

Reinicie o Minecraft. **Mantenha a porta 25575 restrita ao localhost** e use firewall para bloquear acessos externos. O agente já faz a ponte HTTPS de saída. O agente usa `127.0.0.1` como host padrão.

## Executar no Linux

Requer Python 3.10+ e acesso à internet da máquina do servidor.

```bash
export NEXUS_RP_URL="https://nexus-aterna-rp.onrender.com"
export NEXUS_RP_AGENT_KEY="COLE_A_CHAVE_DA_RENDER"
export NEXUS_RP_PLAYER="SEU_NICK_MINECRAFT"
export RCON_HOST="127.0.0.1"
export RCON_PORT="25575"
export RCON_PASSWORD="SENHA_DE_SERVER_PROPERTIES"
python3 bridge_agent.py
```

## Executar no Windows PowerShell

```powershell
$env:NEXUS_RP_URL="https://nexus-aterna-rp.onrender.com"
$env:NEXUS_RP_AGENT_KEY="COLE_A_CHAVE_DA_RENDER"
$env:NEXUS_RP_PLAYER="SEU_NICK_MINECRAFT"
$env:RCON_HOST="127.0.0.1"
$env:RCON_PORT="25575"
$env:RCON_PASSWORD="SENHA_DE_SERVER_PROPERTIES"
py -3 bridge_agent.py
```

Evite salvar senhas em arquivos compartilhados ou publicar capturas do console.

## Ativar no Nexus Web

1. Abra https://nexus-aterna-rp.onrender.com/#config.
2. Selecione **AGENTE HTTPS LOCAL (RECOMENDADO)**.
3. Informe o **código de acesso da ponte HTTPS** (`NEXUS_RP_ACCESS_KEY` da Render, não a chave de agente).
4. Salve as configurações. Teste a conexão.
5. Em **Eventos do Jogo**, habilite a atualização automática.
6. Configure os perfis, sequências, emotes e o protocolo de emergência nos respectivos menus.

O agente recebe somente comandos RCON já validados pelo serviço (mensagens `tellraw`, `list` e `function`). O app não oferece console RCON arbitrário.

## Dados de eventos

O agente consulta a vida do jogador por `data get entity SEU_NICK Health` a cada 3 segundos (configurável por `NEXUS_RP_INTERVAL`). Calcula variações para inferir dano, cura, morte ou renascimento. Tenta ler também `active_effects`, quando essa informação está disponível.

As leituras são uma **aproximação por amostragem**. Dano e cura que ocorram no mesmo intervalo podem se anular, e alguns efeitos/mods não aparecem no NBT padrão. Para eventos exatos seria necessária uma integração nativa com o servidor NeoForge.

Os dados de eventos do último período ficam em memória no serviço Render, e serão perdidos quando reiniciar. O painel mantém seus próprios registros em `localStorage`.

## Atenção: emotes e armaduras 3D

O painel pode chamar funções Minecraft como `nexus:emotes/integrar` e `nexus:armadura/remodelar`, mas só terão efeito se seu datapack/mod as definir. Uma função vanilla pode mostrar partículas e sons. Animação verdadeira do Armourer's Workshop exige um adaptador para o mod ou sistema de emotes instalado.

## Diagnóstico

- Ponte HTTP online: `https://nexus-aterna-rp.onrender.com/api/health` (não expõe senhas).
- Painel: Configurações > Testar agente.
- Agent offline: confira `NEXUS_RP_AGENT_KEY`, RCON e o console do agente.
- Eventos não aparecem: confirme o nick do Minecraft e a opção Eventos do Jogo.
- Chat não envia: teste a conexão no painel e veja se `RCON_PASSWORD` é a senha correta.
