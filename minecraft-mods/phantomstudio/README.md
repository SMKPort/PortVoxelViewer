# Phantom Studio (NeoForge 1.21.1)

Mod **client-side** independente. Pressione **K** para abrir o editor de cenas.

## Recursos no codigo

- Multiples cenas salvas por mundo/servidor/dimensao em JSON em `minecraft/phantomstudio/scenes`.
- Hologramas humanoides locais de linhas coloridas, com varias poses, rotacao, posicao, nomes e selecao individual.
- Equipamento visual simplificado (espada, cajado, escudo e livro).
- Linha do tempo de 60 segundos, keyframes e interpolacao de posicao/rotacao, play/pause e passos de 5 ticks.
- Efeitos locais configuraveis: ouro, vermelho, azul e fumaca, com particulas existentes no cliente.
- Controles do editor: W/S deslocam Z, A/D deslocam X, Q/E altura, R/T rotacao.

## Restricoes

Os fantasmas usam **maniquins de contorno 3D**, nao skins reais e nem modelos importados do Armourer's Workshop. A geometria e uma implementacao inicial visual. Nenhum fantasma e criado no servidor, nenhum jogador ve esses efeitos sem uma implementacao propria.

Build: Java 21, Gradle 8.14.3 e NeoForge 21.1.267. `gradle build` gera o JAR em `build/libs`. Github Actions incluido para compilar. **Ainda necessita compilar e testar em jogo**.
