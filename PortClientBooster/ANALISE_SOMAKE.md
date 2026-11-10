# Auditoria inicial do Somake Network para o Port Client Booster

**Base examinada:** `manifest(1).json` e `modlist(1).html`, arquivos fornecidos pelo usuario.
**Modpack:** SSMP - Season 1 - Capitulo 2, v1.10, TeenLe.
**Ambiente declarado:** Minecraft 1.21.1, NeoForge 21.1.235.
**Inventario:** 214 entradas do export, 213 mods e 1 pacote de textura.

## 1. Otimizacoes que ja existem no pacote

| Categoria | Mods identificados | Consequencia para o booster |
|---|---|---|
| Pipeline de renderizacao | Sodium; Sodium Extra; Sodium/Embeddium Extras; ImmediatelyFast | Nao reimplementar renderizacao nem substituir o pipeline sem testes. |
| Ocultacao e alcance visual | Entity Culling; Better Fps - Render Distance; Farsight | Evitar um segundo culling agressivo em armaduras/entidades. |
| Memoria e inicializacao | FerriteCore; ModernFix; AllTheLeaks; FastBoot | Evitar intervencoes de JVM ou limpeza arbitraria de memoria. |
| Eficiencia complementar | Dynamic FPS; Immersive Optimization; Fast Item Frames | Nao disputar estados e opcoes graficas. |
| Shaders | Iris Shaders | Evitar injecoes nao verificadas na pipeline grafica. |

A presenca na lista **nao prova** que cada otimizacao esteja ativada ou configurada de maneira ideal.

## 2. Pontos de atencao especificos

- **Modelos e animacao:** Armourer's Workshop, Cosmetic Armor Reworked, Emotecraft, playerAnimator e GeckoLib. Reduzir a opcao global de distancia de entidades pode fazer armaduras de outros jogadores desaparecerem mais cedo. Nao desligar arbitrariamente bones/animacoes.
- **Magia:** Iron's Spells 'n Spellbooks, seus add-ons, Lodestone, Occultism, Malum e Toxony. A opcao global de particulas pode ocultar efeitos importantes; manter desativada por padrao.
- **Tecnologia:** Create, Mekanism, Applied Energistics 2, Oritech e Industrial Foregoing. Sao possiveis focos de renderizacao de blocos animados e interfaces, mas os arquivos nao medem custo real.
- **Multimidia:** MCEF, WATERFRAMES, WATERMeDIA e WATERViSION. Possivel carga de video/decodificacao: investigar apenas mediante profiling de cenas que realmente usem displays.
- **Exploracao:** JourneyMap, Farsight, Terralith e mods de biomas/estruturas.

## 3. Problema corrigido na versao de codigo 0.2.0

A versao 0.1.0 do Port Client Booster tinha `neo_version=21.1.265`, superior ao 21.1.235 especificado pelo modpack. A versao 0.2.0 configura alvo e dependencia minima em `21.1.235`.

## 4. Estrategia inicial do booster (cliente somente)

1. Coletar media de FPS, 1% low e p95 de frametime em cenas reproduziveis.
2. Evitar ajustes por padrao na distancia de entidades e particulas, preservando visuais de RP.
3. Habilitar ajustes graduais **somente com consentimento local do jogador**; a distancia minima proposta e 85% do ajuste original.
4. Nao alterar renderizadores de terceiros, ticks de mundo, pacotes de rede, servidor ou dados salvos.
5. Comparar antes/depois, e reverter em caso de glitches.

## 5. Cenas recomendadas para comparacao

- Spawn com varios jogadores usando Armourer's Workshop e emotes.
- Combate magico com efeitos simultaneos.
- Base tecnologica com maquinas Create/Mekanism e telas, se houver.

Medir entre 2 e 3 minutos por cena nas mesmas condicoes graficas, idealmente com tres repeticoes, e observar o `latest.log`.

## 6. Limitacoes

Auditoria baseada em metadados de exportacao, sem JARs, configuracoes completas, logs ou captura de performance. Nao prova a causa de travamentos nem ganho de FPS. O prototipo ainda exige compilacao completa e testes no jogo.
