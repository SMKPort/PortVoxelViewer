# 🖥️ Port HUD Studio 1.0

Mod client-side para **Minecraft Java 1.21.1 NeoForge 21.1.x**. Editor de HUD independente, sem acesso ao servidor.

## Utilização

Digite `/porthud` ou `/porthud edit` para abrir o editor.

- Clique num indicador para selecioná-lo.
- Arraste o indicador para mudar sua posição.
- Use os botões para alterar escala, opacidade ou visibilidade.
- Alterne os perfis **RPG**, **Minimal** e **Performance**.
- Se desejar, ative/desative **Modo cinema** para ocultar o HUD ao reproduzir tomadas do Port Cinematic Studio.
- Use **Salvar HUD** ou pressione ESC para salvar.

## Indicadores

Vida, armadura, fome, posição, FPS e ping. A posição é lida somente do cliente local. O ping pode não estar disponível em singleplayer.

## Comandos

- `/porthud`: editor visual.
- `/porthud next`: alterna perfil.
- `/porthud on` / `/porthud off`: ativa ou oculta todos os indicadores.
- `/porthud status`: perfil e preferências.

Todas as alterações ficam em `config/port-hud-studio.json`, sem transmissão por internet. O mod **não modifica os elementos vanilla** do HUD, ele sobrepõe seus próprios widgets. Portanto, algumas informações podem aparecer duplicadas até que você escolha ocultar widgets ou o HUD vanilla com outro recurso compatível.

## Compatibilidade

Não depende do Cinematic Studio. Se ambos estiverem instalados, o HUD detecta opcionalmente a reprodução cinematográfica e oculta seus widgets, sem qualquer comunicação de rede.

Para compilar: `gradle -p port-hud-studio build --no-daemon`. O artefato é `porthud-1.0.0.jar`.
