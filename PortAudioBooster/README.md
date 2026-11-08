# Port Audio Booster 3.3 | Car Audio + Xiaomi Buds

Aplicativo Android com equalizador próprio e presets distintos para **som automotivo** e **fones Xiaomi/Redmi Buds**, sem dependência do MusicFX.

## Novidades 3.3

- **Modo Xiaomi Buds** com seis presets: Xiaomi Buds Equilibrado, Graves Suaves, Voz Cristalina, Funk nos Fones, Podcast e Neutro.
- **Proteção de fones (ativada por padrão)**: enquanto ligada, EQ aplicado ao Android no máximo +2 dB, Bass Boost até 10%, ganho extra forçado para 0 dB e virtualização até 20%. Isso é uma limitação de reforço digital e **não garante níveis de escuta seguros**.
- Controle do volume multimídia e botão opcional para reduzi-lo a aproximadamente 35% do intervalo do Android antes de testar (não equivale a um limite seguro em decibéis).
- Comparador A/B destacado, para identificar claramente áudio original versus processamento.
- **Bluetooth opt-in**: ao conectar fones cujo nome contenha Xiaomi/Redmi/Mi e Buds/Earbuds/AirDots, aplica o perfil de fones do Port Audio Booster, sem usar o perfil agressivo do som automotivo. Se o nome foi alterado ou o Android restringir broadcasts de conexão, a seleção manual continua disponível.
- O botão flutuante identifica se o perfil ativo é do carro ou dos fones e altera somente os presets daquele modo.
- **Modo Car Audio Edition preservado**, incluindo presets, diagnóstico do paredão, limites de ganho e ferramentas avançadas.

## Como usar

1. Abra **Port Audio Booster**, toque em **Modo Xiaomi Buds**.
2. Conecte os fones pelo Bluetooth e selecione **Xiaomi Buds Equilibrado** para começar.
3. Com o volume baixo, ative os efeitos e confirme que está em **B COM EFEITOS**.
4. Compare com **A ORIGINAL** no mesmo volume. Se houver distorção ou desconforto, reduza o volume e o reforço de graves.
5. Para automatizar o perfil na próxima conexão, ative a opção **Selecionar perfil automaticamente ao conectar Xiaomi/Redmi Buds** e autorize Dispositivos próximos, quando solicitado.
6. Para voltar à configuração de som automotivo, use **Voltar ao modo Paredão**.

O app **não controla ANC, transparência, gestos ou firmware** proprietários dos Xiaomi Buds. Esses recursos dependem do modelo e da aplicação oficial Xiaomi Earbuds. O modelo exato de Xiaomi Buds não foi informado, portanto os presets são genéricos e conservadores.

## Limitações

O Android depreca o uso de efeitos na sessão global 0. A criação de EQ/BassBoost do aplicativo não garante a modificação do áudio do Spotify em todos os celulares. O resultado depende do firmware, saída de áudio e dispositivos; o usuário precisa validar ouvindo.

O volume percentual do Android não representa uma medição de decibéis na orelha. Proteções digitais **não detectam clipping ou falhas do amplificador do carro**, nem medem dose sonora dos fones. Evite volume elevado e interrompa o uso se houver distorção.

## Compilar e instalar

- GitHub Actions: [Port Audio Booster 3.3 - Car Audio + Xiaomi Buds APK](../../actions/workflows/port-audio-booster-apk.yml)
- APK, após build bem-sucedido na branch main: [download/PortAudioBooster-v3.3-XiaomiBuds.apk](download/PortAudioBooster-v3.3-XiaomiBuds.apk)
- Artifact: `PortAudioBooster-v3.3-XiaomiBuds-APK`
- Android `com.port.audiobooster`, `versionCode 6`, `versionName 3.3.0-xiaomi-buds`, minSdk 26.
- Build debug (Java 17, Gradle 8.13, SDK 35). Dependendo da assinatura, a instalação sobre uma versão anterior pode exigir desinstalá-la.

O repositório preserva os outros aplicativos. Versões antigas do APK ficam no histórico e nos arquivos do GitHub, mas **não fazem parte da experiência principal da 3.3**.
