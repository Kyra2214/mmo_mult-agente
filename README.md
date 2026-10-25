# MMO Multiagente

**Um laboratório de sociedade artificial persistente para MMORPGs.**

O MMO Multiagente simula um mundo de fantasia habitado por agentes autônomos com objetivos, recursos, habilidades, memória e conhecimento limitado. O mundo deve continuar funcionando mesmo sem humanos conectados.

## Abordagem inicial: texto primeiro

A primeira versão é um **aplicativo Android com simulador textual, sem mapa gráfico**. O núcleo da simulação fica separado da interface e pode ser testado na JVM. A interface permite avançar um passo, executar dez passos, reiniciar e acompanhar o estado; o progresso é salvo no aparelho.

## APK para testar

O projeto já contém um protótipo Android Kotlin com identificador `com.kyra2214.mmomultiagente`, versão `0.1.0` e SDK mínimo 26 (Android 8.0 ou superior). O workflow do GitHub Actions executa os testes unitários e constrói um APK de debug.

1. Abra [GitHub Actions](https://github.com/Kyra2214/mmo_mult-agente/actions).
2. Entre na execução mais recente de **Android APK** e espere o teste/build terminar.
3. Quando a execução estiver concluída com sucesso, baixe o artefato `mmo-multiagente-debug-apk`, extraia o ZIP e instale `app-debug.apk` no Android.

O APK de debug é para testes, não uma versão de distribuição assinada para publicação. Se a execução falhar, o artefato não será publicado.

## Princípios

- **Autonomia:** nenhum agente tem profissão fixa; pode mudar de atividade conforme objetivos, capacidade e oportunidades.
- **Persistência:** personagens, inventários, relações, ofertas, transações, quests e acontecimentos sobrevivem entre sessões.
- **Conhecimento limitado:** cada agente sabe apenas o que observou, aprendeu ou recebeu por meios permitidos.
- **Economia rastreável:** itens e dinheiro não surgem sem uma regra explícita; transações registram origem e destino.
- **Mercado assíncrono:** uma oferta pode permanecer à venda sem comprador. Publicar não significa vender.
- **Progressão por Tier:** conhecer uma receita não significa ter habilidade, ferramenta ou materiais para executá-la.
- **Quests condicionais:** o tutorial ensina, verifica condições reais e não exige uma venda imediata para avançar.
- **Separação de sistemas:** inteligência e regras não dependem de mapa gráfico ou de um MMORPG específico.
- **IA generativa com parcimônia:** algoritmos convencionais cuidam das decisões operacionais; IA generativa pode apoiar diálogos e interações sociais complexas.
- **Testar antes de escalar:** ampliar a população somente após validar as etapas anteriores.

## Tutorial inicial — Dia 1 na vila

O agente começa com uma quest inicial guiada, não com uma avaliação livre da economia. Ele aprende conversando com NPCs e realizando ações.

### NPCs iniciais

1. Criador de armaduras.
2. Criador de capacetes.
3. Criador de luvas.
4. Criador de botas.
5. Criador de itens de mão secundária.
6. Criador de itens mágicos.
7. Criador de armas.
8. NPC do mercado.
9. Teleportador.
10. NPC do baú/armazenamento.
11. NPC da masmorra.

Cada NPC explica sua função, serviços, custos e requisitos. Todos os NPCs de fabricação apresentam receitas de **Tier 1, Tier 2 e Tier 3**, incluindo materiais e requisitos. Isso não libera fabricação automaticamente: o motor do mundo verifica progressão, capacidade, ferramentas e materiais.

### Sequência da quest

1. Conhecer a vila conversando com os NPCs.
2. Aprender a coletar recursos de Tier 1 e usar as ferramentas adequadas.
3. Aprender a caçar enfrentando uma criatura compatível.
4. Reunir os materiais que faltam por meios válidos.
5. Fabricar o primeiro item Tier 1 no NPC correto.
6. Aprender a usar o inventário e o baú.
7. Aprender a consultar ofertas, comprar e publicar uma oferta de venda.
8. Conhecer transporte, teleportador e masmorra.

A quest verifica o estado real: se o agente já tem um material, não precisa coletá-lo novamente; se perder um recurso necessário, precisa obtê-lo legitimamente. O tutorial ensina, mas não escolhe sua profissão definitiva.

### Decisões próprias

O teleportador explica destinos, custos em alzes e condições. O agente compara teleportar ou ir a pé considerando custo, tempo, distância, riscos, carga, objetivo e informações conhecidas. Conversar com um NPC não obriga a usar o serviço.

## Mundo abstrato, sem mapa

Regiões, distâncias, rotas, recursos e perigos serão representados por dados. Viajar consome tempo e pode envolver riscos, mas nenhuma coordenada ou desenho é necessário inicialmente. Um console textual exibirá diálogos, decisões, quests, inventário e eventos. Uma visualização gráfica poderá ser adicionada depois sem controlar a inteligência.

## Economia e mercado

- **Mercado entre agentes:** oferta e demanda surgem das decisões dos habitantes.
- **Black Market sistêmico:** inspirado conceitualmente no Black Market de Albion Online, com ordens elegíveis e orçamento limitado; não compra tudo infinitamente.

Um item listado continua pertencendo ao vendedor até uma transação válida. A compra transfere item e dinheiro de forma consistente e registra a operação. Uma oferta pode ficar esperando um comprador enquanto o agente segue com outras atividades. Ofertas Tier 2 podem ensinar compra sem conceder habilidade para fabricar ou usar o item.

## Progressão por Tier

A progressão de caça, coleta e fabricação é independente. Para coletar Tier 2, o agente pode precisar de desbloqueio e ferramenta adequada; pode fabricar ou comprar a ferramenta. Conhecimento, capacidade e posse de recursos são estados distintos.

## Arquitetura pretendida

- **Núcleo do agente:** objetivos, personalidade, memória, relações, conhecimento limitado e decisão.
- **Motor de regras:** valida ações e aplica consequências.
- **Mundo e economia:** regiões abstratas, recursos, inventário, receitas, mercado, riscos e persistência.
- **NPCs e quests:** diálogos, serviços, conhecimento e objetivos tutoriais condicionais.
- **Console textual:** observação e explicação das decisões.
- **Visualização futura:** camada opcional, independente da lógica.
- **Adaptador futuro:** possível conexão com servidor de MMORPG, que continuará autoridade sobre as regras reais do jogo.

## Escala de validação

- **1 agente:** tutorial, coleta/caça, fabricação e publicação de oferta.
- **2 agentes:** compra real de uma oferta persistente.
- **5 agentes:** validar interações e diversidade de decisões.
- **10–25 agentes:** observar escassez, preços e economia emergente.
- **50 agentes:** testar grupos, reputação e risco.
- **100+:** somente após validar coerência, persistência e desempenho.

## Fora do escopo inicial

- Construir um MMORPG completo de imediato.
- Criar mapa gráfico antes de validar o simulador textual.
- Implementar centenas de agentes antes do ciclo básico.
- Roteirizar a vida inteira de cada personagem.
- Integrar um servidor real nesta fase.
- Usar IA generativa para cada ação.
- Copiar código ou assets proprietários de outros jogos.

## Plano inicial

Consulte **[docs/PLANO_INICIAL.md](docs/PLANO_INICIAL.md)** para fases, critérios de aceite e testes.

## Estado atual

A interface Android agora funciona como observador: o tutorial avança sozinho, depois o agente executa ticks autônomos em primeiro plano e a simulação tenta recuperar tempo decorrido quando o app é reaberto (limitado a 3.600 ticks por retomada). O núcleo já inclui atividades iniciais de coleta, caça, pesca, refino e fabricação com consumo de materiais; isso ainda é uma base de protótipo, não a progressão completa até T4. O repositório contém o primeiro protótipo Android executável: interface textual, 11 NPCs, sequência de tutorial, coleta, caça, fabricação T1, baú, oferta persistente em espera, decisão de transporte, salvamento local e testes unitários. Nesta versão, a sequência do tutorial é determinística e, depois dela, uma política heurística escolhe ações com base no inventário, nas receitas disponíveis, na ferramenta de pesca e nas ofertas existentes. Isso já substitui o ciclo fixo de seis ações. Coleta, caça, pesca, refino e fabricação registram ações nas especializações correspondentes, e o progresso das especializações é salvo localmente. As receitas T1–T4 agora podem fabricar equipamentos reais a partir dos materiais do inventário, exigem o desbloqueio da especialização, consomem os materiais sem consumo parcial em caso de falha e atualizam o equipamento equipado. A coleta, a caça e os refinadores de minério, pedra, madeira, fibra, couro e fragmentos arcanos reconhecem os tiers liberados pela especialização; o ciclo autônomo tenta escolher receitas viáveis. A política mantém um tier-alvo compartilhado pelas sete categorias de equipamento, prioriza as especializações menos desenvolvidas e não avança a meta do conjunto até todas liberarem o tier seguinte. Há um teste de estabilidade de 500 ticks, mas ele não prova que uma personagem nova sempre consiga completar a progressão até T3. O simulador também possui um modelo inicial de masmorra T3 com bloqueio até equipar todos os sete espaços no mínimo em T3, combate separado contra mini-chefe e chefe, possibilidade de derrota com perda de recursos/alzes, baú liberado somente na vitória e raridade decidida por D4. Quando o agente estiver equipado em T3 completo, o ciclo tenta entrar autonomamente e salva o resultado. Ainda não há uma economia equilibrada que garanta progressão completa até T4, comprador autônomo ou economia multiagente. A política é heurística, não um planejador estratégico geral, e deverá evoluir com testes de simulação longa.

## Licenciamento e referências

Projeto experimental independente. Albion Online é apenas uma referência conceitual; não há afiliação nem intenção de copiar assets ou conteúdo protegido. A licença do projeto será definida antes de distribuir código ou conteúdo publicamente.
