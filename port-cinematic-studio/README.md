# 🎬 Port Cinematic Studio 1.0

Minecraft **Java 1.21.1 / NeoForge 21.1.x / Java 21**.

Mod independente, instalado apenas no cliente. Não altera a posição ou o movimento real do jogador nem exige instalar nada no servidor. A câmera virtual usa uma entidade somente local, não adicionada ao mundo.

## Controles

- `/portcinema` ou `/portcinema open`: abre o editor.
- **Câmera ON**: ativa câmera virtual. Use **WASD** para mover, **Q/E** para descer/subir e **Shift** para acelerar. Arraste o mouse para girar.
- **Salvar ponto**: registra posição, orientação e duração do próximo trecho.
- **Duração +/-**: define a duração (1 a 120 segundos) até o próximo keyframe.
- **▶ Reproduzir**: toca uma sequência de pelo menos dois pontos com aceleração e desaceleração suaves.
- **Timeline**: clique ou arraste para pré-visualizar posições ao longo do trajeto.
- **Câmera OFF** ou **ESC**: retorna à câmera normal, caso não esteja reproduzindo.
- `/portcinema frame`: salva keyframe de 3 segundos.
- `/portcinema play`: reproduz a sequência.
- `/portcinema stop`: interrompe e restaura a câmera.
- `/portcinema status`: mostra estado atual.

A cena é salva em `config/port-cinematic-studio.json` automaticamente. Apagar a cena remove seus keyframes dessa cópia local.

## Integração com o HUD Studio

Durante a reprodução, o Minecraft oculta o HUD e restaura a preferência anterior no encerramento. O Port HUD Studio também verifica opcionalmente o método público `com.port.cinematic.CinematicState.isPlaying()` para respeitar o modo cinema sem precisar importar o mod.

## Limitações

Este é o primeiro editor funcional. A navegação livre é controlada pela **tela do editor**. Não há gravação de vídeo ou MP4, replay de entidades/jogadores ou importação de cenas de outras máquinas nesta versão. Keyframes são posições da câmera na dimensão em que foram capturados. A câmera não carrega chunks remotos e fica limitada ao que o Minecraft já tiver carregado no cliente. O jogador real permanece sujeito a dano, movimentação e física. Use em servidores que permitam câmeras livres.

Para compilar: `gradle -p port-cinematic-studio build --no-daemon`. O artefato é `portcinema-1.0.0.jar`.
