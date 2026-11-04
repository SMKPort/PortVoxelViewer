# PortCalc 1.0 | Cálculo I, II, III e IV

Projeto Android em Java com interface HTML/CSS/JS e motor simbólico Python (`SymPy`) através de Chaquopy. O processamento matemático funciona **sem servidor e sem conexão** após instalar o APK. Interface vermelho, dourado e preto. Histórico local e gráficos 2D.

## Conteúdo da versão 1.0

| Área | Operações implementadas |
| --- | --- |
| Cálculo I | limites (bilateral e laterais), derivadas de ordem 1–5, reta tangente, raízes reais |
| Cálculo II | integrais indefinidas e definidas, área entre curvas, volume de revolução (discos), Taylor, EDO linear de 1ª ordem |
| Cálculo III | derivadas parciais, gradiente, derivada direcional, integrais duplas/triplas, divergente, rotacional |
| Cálculo IV | Laplace/inversa, somas parciais de Fourier, EDO homogênea de 2ª ordem, integral de linha de campo vetorial |
| Extra | gráfico 2D de funções, histórico dos últimos 35 cálculos, copiar resposta e LaTeX |

**Limitações:** o app resolve os tipos acima, ainda não cobre todos os tópicos de todas as ementas de Cálculo I–IV. Os passos são marcos de resolução, não a demonstração de cada regra algébrica. Resultados simbólicos podem conter integrais não resolvidas ou depender de condições de domínio. Não há leitura de fotos ou OCR na v1. A Série de Fourier utiliza o intervalo [-π, π]. A área entre curvas usa o módulo. A operação de volume é o método de discos girando em torno do eixo x. Gráficos 3D não disponíveis.

## Compilar APK no GitHub (sem PC)

1. Crie um repositório **novo** no GitHub (ex.: `PortCalc`) e envie o conteúdo **da pasta PortCalc**, mantendo `.github/workflows/android-apk.yml`.
2. Vá para **Actions → Gerar APK Android** e clique em **Run workflow**. Também será executado um build a cada push em `main`.
3. Quando a execução terminar com sucesso, clique em **PortCalc-APK-v1** em **Artifacts**, extraia o ZIP e instale `app-debug.apk` no Android.
4. Na primeira instalação, o Android pode solicitar autorização para instalar de fontes desconhecidas. Verifique o arquivo e a procedência antes de conceder autorização.

O `app-debug.apk` é destinado a testes. Para distribuição pública, será preciso configurar assinatura release (keystore) e política de atualização. Não compartilhe credenciais, tokens ou keystores em commits.

## Desenvolvimento / testes

- Android Gradle Plugin: 8.8.2, Gradle 8.10.2, JDK 17.
- Chaquopy 17.2.0 e Python 3.11 com SymPy 1.13.3.
- ABI `arm64-v8a`, Android API 24+ (aparelhos Android 64 bits).
- Para testar o motor sem Android: `python -m pip install sympy==1.13.3` e `python -m unittest discover -s tests -v`.
- Para testar uma chamada: `PYTHONPATH=app/src/main/python python -c 'import engine; print(engine.solve({"op":"limit","args":{"f":"sin(x)/x","point":"0"}}))'`.

**Segurança de expressão:** o parser usa lista restrita de símbolos e funções e recusa caracteres fora da sintaxe matemática. Não exponha o motor como uma API pública sem proteção extra de tempo, memória e execução isolada.

## Próximas versões propostas

- OCR para reconhecer questões a partir da câmera/galeria, com revisão do texto antes de resolver.
- Explicações pedagógicas mais detalhadas por regra, gráficos 3D e campos vetoriais.
- Resolução de EDOs gerais com condições iniciais, problemas com séries avançadas e parametrizações 3D.
- Exportar resolução para PDF e cadernos de estudo.
