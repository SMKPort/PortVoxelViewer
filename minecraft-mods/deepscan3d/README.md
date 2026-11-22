# DeepScan 3D (NeoForge 1.21.1)

**Independente, 100% client-side.** Aperte **J** e abra o mapa de cavernas em projeção 3D. Não precisa instalar no servidor.

## O que foi implementado

- Observações reais do cliente por raycast, sem atravessar blocos; mapeamento incremental e separado por servidor/dimensão.
- Visualização isométrica 3D rotacionável, camadas por altura, zoom, filtros para minérios visíveis e fluidos.
- Marcadores nomeados, trilhas históricas, rotas salvas, replay e indicação no HUD para retorno por percurso.
- Autosave JSON local em `minecraft/deepscan3d/worlds` a cada 300 ticks e na saída.
- Limites de memória por mundo, com amostragem de renderização para reduzir custo.

**Limitação:** o mapa não é um scanner de raios X. Apenas as linhas de visão do jogador, inclusive faces expostas de blocos, são observadas; blocos atrás de paredes não aparecem. Marcadores de minério exigem que o raio atinja diretamente o bloco. O retorno usa trilha registrada, não navegação A*.

## Build

Java 21, Gradle 8.14.3, NeoForge 21.1.267. Execute `gradle build`. O JAR ficará em `build/libs`. No GitHub, o workflow Actions incluído compila e disponibiliza o JAR como artifact. **O código ainda precisa de compilação e teste dentro do jogo**.
