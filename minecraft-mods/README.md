# Port Mods: 3 projetos independentes para Minecraft NeoForge 1.21.1

O pacote contém **três fontes independentes**, que podem ser compiladas, instaladas e atualizadas isoladamente:

- **DeepScan 3D**: mapa isométrico de cavernas observadas, filtros de blocos, trajetos, pontos, replay e HUD de retorno.
- **Phantom Studio**: criação de hologramas wireframe, cenas locais, edição de poses, equipamentos, efeitos, keyframes e reprodução.
- **Voxel Physics**: sobreposições de tecidos/acessórios físicos com vento simulado e presets.

## Como gerar os JARs

Cada pasta é um projeto Gradle separado. Abra uma pasta de cada vez no IntelliJ IDEA ou faça push para o próprio repositório GitHub. O workflow `.github/workflows/build.yml` compila via GitHub Actions e publica o JAR como artifact. Também é possível executar `gradle build` em cada pasta se Gradle e Java 21 estiverem instalados.

**Observação de qualidade:** este ambiente não oferece acesso ao Gradle/NeoForge para compilar o projeto e executar o Minecraft. Os arquivos são um **protótipo de código-fonte**, não JARs prontos, e precisam da etapa de build e teste in-game. Os três mods não necessitam ser instalados no servidor.

## Compatibilidade

O alvo é Minecraft 1.21.1, NeoForge 21.1.267, Java 21 e ModDevGradle 2.0.148. Foram usados os eventos de client tick, keymapping, renderização da GUI e renderização do mundo documentados para essa versão.

Não foi criada dependência entre os três projetos. Nenhum sistema depende do Nexus Aterna. Compatibilidade real com outros mods, principalmente sobreposições do Armourer's Workshop, deve ser testada no modpack Somake.

## Limites importantes

DeepScan não mostra blocos ocultos e seu recurso de retorno não é pathfinding. Phantom Studio renderiza **maniquins em linhas holográficas**, não modelos fiéis de jogadores nem skins completas. Voxel Physics adiciona **malhas de acessórios novas** e não deforma os vértices de uma skin ou de um equipamento do Armourer's Workshop. Essas integrações avançadas requerem uma etapa adicional e bibliotecas reais dos outros mods.
