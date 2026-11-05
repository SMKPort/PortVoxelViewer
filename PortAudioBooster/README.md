# Port Audio Booster 3.2 | Car Audio Edition

Aplicativo Android com equalizador próprio, reforço de graves, comparador A/B, presets e ferramentas voltadas ao som automotivo. **Sem integração com MusicFX** ou outras interfaces externas de equalização.

## Funcionalidades

- **A/B no topo da tela:** A = áudio original (efeitos do Port Audio desligados), B = efeitos ativos, com sinalização grande; requer ativar o processamento antes.
- **Car Audio Edition:** presets Paredão Controlado, Pancada Limpa, Subgrave Suave, Voz Presente e Flat Seguro.
- **Proteção preventiva:** quando ativada, limita o EQ digital a no máximo +2 dB, Bass Boost do Android a 15% e força ganho extra de 0 dB; tenta o limiter do Android quando disponível. Não analisa o sinal de saída do amplificador e não garante eliminação de clipping.
- **Equalizador próprio:** controles de 5/10/31 bandas virtuais, EQ do sistema conforme disponibilizado ao aplicativo, grave, espacialização e ganho opcional (pela tela avançada).
- **Spotify:** botão para abrir Spotify, além de controles opcionais de reprodução por MediaSession; áudio é processado somente se o Android aceitar efeitos na sessão global 0.
- **Bluetooth:** ativação opt-in, seleção automática de preset do Car Audio Edition e vínculo de perfil ao dispositivo. A inicialização do serviço em segundo plano pode ser bloqueada pelo Android.
- **Botão flutuante:** comparação A/B, escolha de preset, proteção e volume.
- **Meu Paredão:** formulário para dois módulos, quatro falantes, ligação série/paralelo e tensões inseridas manualmente; cálculo estimado de impedância e registro de dados. Não há medição automática de clipping ou de tensão elétrica.
- **Ferramentas avançadas:** analisador FFT pela API Visualizer quando autorizado, importação/exportação de presets, diagnóstico de recursos e permissões.

## Como usar com o som do carro

1. Ligue o Spotify e abra o Port Audio Booster 3.2.
2. Ative o processamento do aplicativo e selecione **B COM EFEITOS** no comparador.
3. Comece com **Flat Seguro** ou **Paredão Controlado** e **proteção preventiva ligada**.
4. Aumente o volume gradualmente, interrompendo imediatamente se os graves distorcerem.
5. Cadastre as informações reais dos módulos e alto-falantes em **Meu Paredão** para estimar ligações. Confira sempre a potência por impedância, a fiação e os cortes com profissional.

### Limitações técnicas

O Android considera obsoleto conectar efeitos de inserção à sessão global 0. Por isso, o aplicativo **não pode garantir processamento do Spotify ou outros aplicativos** em todos os celulares. A indicação de API disponível também não confirma efeito audível. O app não consegue medir a distorção do módulo nem corrente, tensão ou a temperatura por software comum. Um limiter digital é diferente de proteção de clipping física do amplificador.

Os valores de potência e impedância dependem do modelo exato e da ligação das bobinas. Os cálculos simplificados supõem **dois falantes idênticos por caixa**, com impedância efetiva já definida em cada falante, e uma das duas opções (paralelo/série). Não servem para dimensionar um amplificador sem inspeção da instalação.

## APK Android

Workflow: [Port Audio Booster 3.2 - Car Audio APK](../../actions/workflows/port-audio-booster-apk.yml).

Após build de `main` com sucesso, APK de teste publicado em [download/PortAudioBooster-v3.2-CarAudio.apk](download/PortAudioBooster-v3.2-CarAudio.apk). GitHub Actions também publica o artifact `PortAudioBooster-v3.2-CarAudio-APK`.

- Android SDK 35, JDK 17, Gradle 8.13
- Package: `com.port.audiobooster`
- Version: `3.2.0-car-audio` (`versionCode 5`)
- APK debug; sua assinatura pode diferir de outros APKs.

Código-fonte e APK de versões anteriores podem permanecer no histórico do GitHub, mas a versão 3.2 não tem controles ou dependências do MusicFX.
