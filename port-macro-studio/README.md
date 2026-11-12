# Port Macro Studio 1.0 🎮

Mod **somente do cliente** para Minecraft Java **1.21.1 NeoForge**. Crie até quatro macros com 24 etapas cada, edite o fluxo na interface do jogo, atribua ações condicionais e execute por comandos ou atalhos. Não depende de permissões administrativas nem de plugin no servidor.

## Instalação

Baixe o artefato **Port-Macro-Studio-NeoForge-1.21.1** no GitHub Actions, extraia o arquivo `portmacro-1.0.0.jar` e coloque na pasta `mods` da instalação Minecraft Java 1.21.1 com NeoForge 21.1.x. Requer Java 21.

## Editor

Execute **`/portmacro`** ou pressione **Ctrl+Alt+M** dentro de um mundo para abrir o editor.

Escolha um dos quatro slots de macro. Adicione etapas pelos botões, selecione uma etapa no fluxo e edite seus parâmetros. Os textos de **Aviso** e **Chat** são editáveis no campo inferior. Para **Aguardar**, os botões Valor +/- ajustam a duração em segundos. Para **Hotbar**, modificam o slot de 1 a 9. Para **Vida menor**, modificam o limite de HP.

Etapas disponíveis:

- **Aguardar**: pausa a execução de 0,05 a 10 segundos.
- **Aviso**: envia uma notificação visível apenas no seu cliente, sem chat público.
- **Chat**: envia uma mensagem comum pelo chat do jogador, somente se você tiver autorizado. **Não executa comandos com /**.
- **Hotbar**: seleciona um item da barra rápida local.
- **Vida menor**: só continua se o jogador tiver menos HP que o limite configurado, caso contrário encerra a macro.
- **Agachado**: só continua se o jogador estiver agachado.
- **Encerrar**: interrompe o restante das etapas.

As macros são gravadas em `config/port-macro-studio.json`, de modo que persistam quando você reiniciar o Minecraft.

## Executar

- **Ctrl+Alt+1 a 4**: executa a macro correspondente.
- **Ctrl+Alt+M**: abre o editor.
- **`/portmacro play 1`** (ou 2, 3, 4): executa a macro escolhida.
- **`/portmacro stop`**: cancela a macro.
- **`/portmacro pause`** e **`/portmacro resume`**: pausa e retoma.
- **`/portmacro status`**: mostra o estado atual.
- **`/portmacro chat on`**: autoriza o envio de mensagens por macros apenas nesta sessão.
- **`/portmacro chat off`**: bloqueia mensagens enviadas por macros.

## Proteções

O envio de mensagens é bloqueado **por padrão em todo início de jogo**, sem autorização salva. Há intervalo mínimo de **5 segundos** entre mensagens enviadas por macros, limite de **3 mensagens por execução**, espera de **10 segundos** entre inícios e tempo máximo de execução de **45 segundos**. Não há laços infinitos nem comandos arbitrários. Desconectar o Minecraft encerra a execução.

O mod não faz simulação de cliques do mouse nem injeta comandos administrativos. Use automações somente onde o servidor permitir.

## Build e GitHub

Código na branch `port-macro-studio` do repositório `SMKPort/PortVoxelViewer`.

Workflow: https://github.com/SMKPort/PortVoxelViewer/actions/workflows/port-macro-studio-build.yml

Para compilar localmente com Java 21 e Gradle 9.2.1:

```bash
gradle -p port-macro-studio build --no-daemon
```

A versão inicial tem editor visual de etapas e atalhos fixos. Futuras versões podem receber construtor de condições com ramificações, integração com outros mods e perfis de atalhos. A compilação automatizada testa compatibilidade com as APIs do NeoForge, mas a integração com o modpack real ainda exige teste dentro do Minecraft.
