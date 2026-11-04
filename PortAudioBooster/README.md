# Port Audio Booster v2.0 | Spotify

Aplicativo Android com interface preto, vermelho e dourado, inspirado em equalizadores e sistemas de som automotivo.

## Recursos
- Presets Paredão, Grave Profundo, Voz Destacada, Rock, Eletrônica e Flat.
- Equalização por 5 controles e Bass Boost experimental.
- Virtualização, ganho adicional limitado e volume multimídia do Android.
- Abrir Spotify, ler dados da música e controlar reprodução (com permissão de acesso às notificações).
- Abrir equalizador nativo do Android, quando disponível.

## Compatibilidade e limitações
O processamento de áudio global por sessão 0 é legado e não garante mudanças no som do Spotify. Os controles de reprodução por MediaSession não modificam o áudio. Spotify Connect transmitindo para outro aparelho não pode ser equalizado pelo celular. Faça testes no aparelho real. O uso de ganho e volume altos pode causar distorção e prejudicar a audição.

## APK
Após execução bem-sucedida do workflow [Port Audio Booster Spotify - Gerar APK](../../actions/workflows/port-audio-booster-apk.yml), baixe o artifact **PortAudioBooster-v2.0-Spotify-APK** ou o arquivo em [download](download/).

O projeto Android fica na própria pasta `PortAudioBooster`, com Gradle 8.13 e JDK 17.
