# TimeFrame v0.1.0 | Minecraft 1.21.1 NeoForge | 100% client-side

Mod independente do Nexus Aterna, DeepScan 3D, Phantom Studio e Voxel Physics.

## O que esta versao faz
- Grava clipes locais da AREA DE TRABALHO via FFmpeg externo (Windows e Linux/X11), usando segmentos de 10 segundos.
- Buffer circular no disco, duracao configuravel (30 a 600 s); salvar ultimo trecho por F9.
- Microfone e dispositivo de captura de audio (incluindo fontes loopback, se disponiveis no sistema) selecionaveis na interface.
- Gravacao de um ou dois fluxos de audio (faixas separadas, quando as duas fontes estao ativas).
- Opcoes de volume, filtro de ruido do microfone, teste gravado de 3s, e extracao MP4.
- F8 abre painel; F10 abre replay 3D de trajetorias observadas. Movimento e dano local gravados em historico de 5 minutos.
- Salvar snapshots JSON compactados (.json.gz), marcador manual, salvamento automatico apos combates e morte e abrir ultimo replay salvo.
- Player temporal 3D esquematico, com orbita da camera, scrub e controle de velocidade de visualizacao.

## Instalacao
1. Use Java 21 e NeoForge 1.21.1. Coloque timeframe-0.1.0.jar na pasta mods.
2. Instale FFmpeg e configure o executavel no painel (F8). Precisa estar no PATH ou fornecer caminho completo.
3. Rode o jogo em janela ou borda sem exclusividade; o FFmpeg grava a AREA DE TRABALHO, nao uma janela exclusiva.
4. Detecte as fontes de audio e selecione microfone e, se houver, uma fonte de mixagem/loopback.
5. Ative a gravacao com o botao no painel. A captacao de microfone vem desligada por padrao.
6. Videos salvos ficam em .minecraft/timeframe/clips. Replays em .minecraft/timeframe/replays.
7. O buffer temporario fica em .minecraft/timeframe/buffer.

## Limites reais
- A gravacao do desktop via FFmpeg e a captura dos dispositivos depende do sistema operacional e dos drivers. Linux Wayland e macOS nao sao suportados pelo backend atual.
- No Windows, para gravar audio do jogo, selecione uma fonte loopback disponivel como Stereo Mix ou cabo virtual. Selecionar o fone de saida normal NAO garante loopback. No Linux escolha a fonte PulseAudio com sufixo .monitor.
- Alguns dispositivos e codecs nao sao apresentados em todas as builds do FFmpeg. O botao de detectar usa FFmpeg e/ou pactl.
- O replay 3D v0.1 nao reconstroi blocos, texturas, inventario ou animacoes completas. Registra somente trajetorias, posicoes e eventos disponiveis ao cliente, exibidos esquematicamente.
- Os replays 3D nao interrompem, atrasam nem revertem o servidor. Nao revelam jogadores ocultos ou areas nao recebidas.
- O buffer e os clipes gravam imagem e audio localmente. O mod NAO envia nenhum conteudo para a internet.
- Teste de microfone cria WAV de 3 segundos; pode ser ouvido pela funcao de reproduzir gravacao do sistema (via Desktop).
- Os clipes automaticos de mortes e combates so salvam trechos SE o gravador de video estiver ativo e houver segmentos completos. Combates sao detectados por dano recebido ou tentativa de ataque em entidade sob a mira; o clipe e salvo apos 15s sem novas interacoes.
- A gravacao por segmentos implica precisao de recorte de cerca de 10 segundos. A copia ocorre apenas dos segmentos finalizados.
- Reducao de ruido e filtro de passa-altas sao filtros FFmpeg. Nao sao cancelamento de ruido por IA.

## Build
Na pasta minecraft-mods/timeframe: gradle build (Gradle 8.14.3 e Java 21)
Workflow: .github/workflows/build-timeframe.yml.

## Teclas
F8: configurar / iniciar gravacao. F9: salvar os ultimos minutos. F10: abrir visualizador de replay 3D.
