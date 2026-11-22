# Voxel Physics (NeoForge 1.21.1)

Mod independente **100% client-side**. Pressione **V** para configurar.

## Implementado no codigo

- Simulador visual de molas amortecidas ligado ao movimento e direcao dos jogadores.
- Fisica secundaria para capa, cabelo (3 mechas), cachecol, faixa, mangas, correntes, franjas e pontas de asas.
- Suporte a corrida, agachamento, voo, deslocamento horizontal e vento oscilante simulado.
- Renderizacao de faixas pixeladas texturizadas sem novas entidades no servidor.
- Presets CLASSICO, ANGELICAL, NINJA e ARCADIO, com controles individuais de ativacao/largura.
- Parametros editaveis de vento, mola, amortecimento, opacidade, cores por item em `minecraft/config/voxelphysics.json`.
- Opcao para mostrar overlays de outros jogadores (inicialmente desligada para poupar FPS).

## Restricao tecnica relevante

Este prototipo **sobrepoe faixas e pecas fisicas**, mas **nao altera a malha real do cabelo/capa/roupa de skins ou do Armourer's Workshop**. O AW precisaria de um adaptador especifico para os ossos e API do mod, apos estudo e validacao das suas classes. As pontas de asas sao overlays, nao substituem asas existentes.

Build: Java 21, Gradle 8.14.3, NeoForge 21.1.267. `gradle build` gera o JAR em `build/libs`. Github Actions ja preparado para compilar. **Ainda necessita compilar e testar em jogo**.
