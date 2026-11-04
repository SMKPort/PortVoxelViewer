# Port Audio Booster 3.0 | Spotify / Android

Aplicativo Android Java, visual preto, vermelho e dourado, com controle experimental da saída do Android e foco no uso paralelo com Spotify.

## Recursos
- Presets Paredão, Grave Extremo, Voz Limpa, Funk, Grave Profundo, Voz Destacada, Rock, Eletrônica e Flat.
- Equalizador com 5, 10 ou 31 bandas **virtuais**, interpoladas para as bandas físicas disponibilizadas pelo dispositivo.
- Bass Boost PRO: foco no subgrave, grave e impacto por curva de EQ, com BassBoost do Android.
- Booster com ganho de até +6 dB e tentativa de limiter pela API DynamicsProcessing. Caso não exista limiter, opção de proteção reduz ganho ao máximo de +2 dB.
- A/B: A original (efeitos off) e B com processamento.
- Botão flutuante opcional com configurações de sobreposição concedidas manualmente.
- Automação Bluetooth (opt-in) via receiver e detecção de saídas ativas; perfil padrão e perfil por dispositivo.
- Visualizador de FFT REAL via Visualizer(0), exigindo RECORD_AUDIO e sujeito a restrições de privacidade.
- Exportar e importar presets em arquivo JSON pelo seletor nativo do Android.
- Diagnóstico de permissões, output de áudio, EQ do sistema, compatibilidade e acesso à reprodução do Spotify.
- Controle de reprodução do Spotify pela tela antiga integrada, com acesso opcional às notificações.

## Limitações reais
A aplicação em Android audio session 0 é LEGADA, experimental, não tem garantia de efeito em apps de terceiros, inclusive Spotify. O indicador OK confirma a disponibilização de um efeito, mas não que o áudio do Spotify tenha sido modificado. 5/10/31 controles gráficos não aumentam bandas físicas do telefone. DynamicsProcessing/limiter e Visualizer(0) podem ser indisponíveis. O receiver Bluetooth pode ser bloqueado pelo Android em segundo plano. Spotify Connect tocando em outro dispositivo não passa pelo processamento deste celular. Nenhum áudio do Spotify é capturado ou armazenado pelo app; o espectro é visualização transitória da API Android.

Graves e volumes excessivos podem danificar a audição e os alto-falantes. A/B permite confirmar auditivamente se os controles fazem alguma diferença no seu aparelho.

## APK
Executar o workflow [Port Audio Booster - Gerar APK](../../actions/workflows/port-audio-booster-apk.yml). Após sucesso, baixe o artifact **PortAudioBooster-v3.0-Spotify-APK** na execução ou o APK na pasta [download](download/).

Compilação: Gradle 8.13, JDK 17, SDK 35. APK debug para teste, sujeito a assinatura diferente entre versões; desinstalar o debug anterior se Android bloquear a atualização.