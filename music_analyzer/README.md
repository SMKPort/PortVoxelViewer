# Port Music Analyzer 1.0 (Windows)

Estúdio de análise musical para PC, com interface preta, vermelha e dourada.

## Recursos disponíveis

- Importação de áudio local WAV, MP3, FLAC e OGG, conforme codecs disponíveis.
- Análise de BPM, duração, tom provável e visualização de forma de onda.
- Reprodução do áudio original.
- Separação Demucs em 4 faixas (voz, bateria, baixo, outros) ou 6 (inclui guitarra e piano).
- Importação de stems WAV produzidos anteriormente.
- Mixer com volume, mudo, solo, reprodução sincronizada e navegação.
- Exportação de mixagem WAV ou FLAC.
- Instalação isolada da IA e workflow para gerar executável Windows.

## Executar o código-fonte no Windows

Instale Python 3.11 de 64 bits e abra o PowerShell na pasta music_analyzer.
Execute os comandos:

    py -3.11 -m venv .venv
    .\.venv\Scripts\Activate.ps1
    python -m pip install -r requirements.txt
    python main.py

Na aba Configurações, clique em Instalar / reparar IA. O programa prepara Demucs e PyTorch em uma pasta separada no LocalAppData. A instalação e o primeiro uso do modelo requerem internet. O processamento das músicas é local.

## Baixar executável pela Action

Abra Actions > Port Music Analyzer - Windows e consulte a execução mais recente bem-sucedida. Baixe o artefato PortMusicAnalyzer-Windows, extraia os arquivos e execute PortMusicAnalyzer.exe dentro da pasta de distribuição, mantendo a pasta completa.

O executável portátil abre a interface e realiza a análise sem Python do sistema. A separação por IA exige Python 3.11 instalado separadamente: PyTorch e Demucs são baixados no primeiro uso. O pacote não inclui os pesos dos modelos nem o runtime de IA.

## Passos de utilização

1. Abra uma música local e consulte BPM e tonalidade.
2. Instale o motor de IA em Configurações.
3. Escolha 4 ou 6 faixas em Separação IA e execute.
4. Ouça cada stem no Mixer, altere volumes, mute ou solo e exporte WAV/FLAC.

Os stems WAV ficam na pasta de saída, organizados por modelo e faixa.

## Limitações

- BPM pode ser estimado na metade ou no dobro do andamento real.
- Tonalidade é estimada por correlação cromática, sem detecção confiável de modulações.
- O Demucs não individualiza cada cantor nem todo instrumento. Guitarra e piano no modelo de 6 stems têm resultados experimentais.
- O programa não processa áudio diretamente do Spotify.
- WAV e FLAC são os formatos mais confiáveis; MP3/OGG dependem dos codecs.
- O pacote é um executável portátil, não um instalador de sistema, e não possui assinatura.
- A compilação e a reprodução real no Windows dependem de validação na execução do workflow e em um PC.

## Estrutura

    music_analyzer/main.py
    music_analyzer/requirements.txt
    music_analyzer/port_music_analyzer/app.py
    music_analyzer/port_music_analyzer/theme.py
    music_analyzer/port_music_analyzer/audio_core.py
    music_analyzer/port_music_analyzer/ai_runtime.py
    music_analyzer/tests/test_core.py
    .github/workflows/port-music-analyzer.yml

O projeto está isolado na branch port-music-analyzer do repositório PortVoxelViewer para não alterar a versão principal do aplicativo Android. Recomenda-se migrá-lo depois para um repositório próprio.
