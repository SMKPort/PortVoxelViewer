# Port Audio Booster 3.1 | Native MusicFX / Spotify

O **MusicFX (com.android.musicfx)** é o caminho principal desta versão porque apresentou alteração audível no áudio do Spotify no aparelho de teste. Os efeitos globais da versão 3.0 continuam acessíveis somente como **modo experimental**.

## Recursos 3.1

- Tela inicial MusicFX-first, visual preto/vermelho/dourado, atalho para Spotify e painel de áudio nativo.
- Presets de **referência manual** para 5 bandas (60 Hz, 230 Hz, 910 Hz, 4 kHz, 14 kHz): Paredão, Grave Extremo, Funk, Voz Limpa, Rock e Neutro, com sugestões de intensidade de grave e surround.
- Comparação A/B **manual**, orientando a alternar o MusicFX nativo e manter o mesmo volume.
- Perfis Bluetooth: escolha de preset padrão e associação por dispositivo; seleção de referência quando detectada conexão, sem tentar controlar parâmetros internos do MusicFX.
- Botão flutuante com atalho MusicFX, troca de referência e controle do volume multimídia.
- Diagnóstico: MusicFX e Spotify detectados, painel disponível, saída de áudio e status do processamento experimental.
- Modo experimental 3.0 preservado (EQ virtual até 31 bandas, Bass Boost, ganho, FFT, importação/exportação, controles de Spotify via MediaSession).

## Como usar com Spotify

1. Reproduza uma música no Spotify.
2. Abra **Spotify > Configurações e privacidade > Reprodução > Equalizador**. Esse é o caminho mais confiável para alterar a sessão de áudio do Spotify.
3. Selecione um preset de referência no Port Audio Booster 3.1.
4. Aplique manualmente os valores das cinco frequências na tela do MusicFX.
5. Faça o teste A/B alternando o botão superior do MusicFX, ouvindo o mesmo trecho da música e mantendo o mesmo volume.

O botão "Abrir MusicFX do Android" usa `AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL` (sem falsificar a sessão do Spotify). O painel pode armazenar preferências por aplicativo ou sessão, portanto abrir diretamente pelo nosso app **não garante que os ajustes afetem o Spotify**. Por isso, prefira abrir pelo Spotify quando a alteração for destinada a ele.

## Restrições e segurança

O app **não escreve automaticamente** nas bandas internas do MusicFX. O Android não oferece API pública universal para isso. Mudar preset Bluetooth ou pelo botão flutuante seleciona **somente uma referência visual**, não modifica o equalizador. O registro de conexões Bluetooth em background pode ser restrito pelo sistema; requer autorização "Dispositivos próximos" em Android 12+.

Com MusicFX ativo, mantenha o processamento global do Port Audio Booster desligado. O modo experimental da versão 3.0 pode não afetar áudio do Spotify, mesmo quando a API indica disponibilidade. Evite amplificação excessiva por risco de clipping e dano à audição.

## Gerar APK

O workflow [Port Audio Booster 3.1 - Gerar APK](../../actions/workflows/port-audio-booster-apk.yml) compila com Android SDK 35, Java 17 e Gradle 8.13. Depois de um build bem-sucedido na branch main, o APK fica em `download/PortAudioBooster-v3.1-MusicFX.apk`, além do artifact `PortAudioBooster-v3.1-MusicFX-APK`.

Pacote Android `com.port.audiobooster`, `versionCode 4`. A compilação é **debug**, com possível mudança de assinatura entre builds.
