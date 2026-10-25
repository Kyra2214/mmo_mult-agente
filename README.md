# MMO Multiagente

**Um laboratório de sociedade artificial persistente para MMORPGs.**

O MMO Multiagente é um projeto experimental para simular um mundo de fantasia habitado por agentes autônomos. Cada habitante possui objetivos, recursos, habilidades, memória e informações limitadas sobre o mundo. Os agentes tomam decisões, interagem e produzem consequências econômicas e sociais sem depender da presença constante de jogadores humanos.

A proposta não é começar construindo um MMORPG completo. Primeiro vamos construir um **simulador multiagente 2D**, capaz de demonstrar que uma pequena sociedade pode funcionar a partir de regras coerentes. A visualização inicial poderá usar círculos, quadrados, pontos, regiões e rotas; o sistema de decisões deve continuar funcionando mesmo sem renderização.

## Visão

Queremos observar uma sociedade emergir de regras e decisões, em vez de escrever antecipadamente uma história para cada personagem.

Um agente pode começar caçando e depois descobrir que coletar, fabricar ou comerciar é mais vantajoso. Pode trabalhar sozinho, cooperar com outros, entrar numa guilda, proteger comerciantes ou assumir riscos em regiões de PvP. Essas escolhas devem resultar de seus objetivos, capacidades, recursos, personalidade, conhecimento e avaliação de riscos.

O mundo deve continuar avançando mesmo quando nenhum humano estiver conectado.

## Princípios do projeto

- **Agentes autônomos:** não atribuir a cada personagem uma profissão fixa. As atividades são escolhas que podem mudar ao longo do tempo.
- **Mundo persistente:** personagens, inventários, relações, ofertas, transações e acontecimentos devem sobreviver entre ciclos da simulação.
- **Conhecimento limitado:** um agente só deve saber o que observou, descobriu ou recebeu de outros sistemas de informação disponíveis para ele. Não deve agir com onisciência.
- **Economia rastreável:** recursos precisam ter origem; itens e dinheiro não devem aparecer sem uma regra explícita. Transações devem registrar origem, destino e valor.
- **Mercado assíncrono:** uma oferta pode permanecer à venda mesmo sem compradores. Outro agente poderá encontrá-la e comprá-la mais tarde.
- **Progressão por Tier:** ferramentas, materiais, receitas, equipamentos e desafios possuem requisitos. Ter experiência suficiente não substitui uma ferramenta necessária.
- **Consequências reais:** decisões de produção, compra, venda, deslocamento, combate e risco alteram o estado do mundo.
- **Sistemas separados:** a lógica da sociedade não deve depender dos objetos visuais do Godot nem de um MMORPG específico.
- **IA generativa com parcimônia:** usar regras e algoritmos convencionais para decisões operacionais frequentes. IA generativa poderá apoiar conversas e interações sociais complexas, sem ser chamada a cada movimento.
- **Testes antes de escalar:** aumentar a população apenas quando o ciclo anterior estiver coerente e verificável.

## Primeiro cenário

O protótipo começa numa zona verde de baixo risco, com recursos e monstros de Tier 1. O primeiro agente aprende as operações básicas por meio de tarefas tutoriais:

1. movimentar-se e avaliar o próprio estado;
2. caçar monstros compatíveis com sua capacidade;
3. coletar recursos com as ferramentas corretas;
4. fabricar itens a partir de materiais disponíveis;
5. publicar itens à venda e consultar ofertas;
6. comprar itens quando houver uma oferta compatível e recursos suficientes;
7. evoluir para atividades de Tier superior quando cumprir os requisitos.

O tutorial ensina o funcionamento do mundo, mas não deve determinar para sempre o comportamento do agente. Depois de aprender o básico, ele escolhe suas próprias atividades.

## Economia e mercado

O mercado mantém ofertas persistentes. Publicar um item não significa que ele foi vendido: o item continua pertencendo ao vendedor até uma transação válida. Quando a compra acontece, o item e o dinheiro são transferidos de forma consistente e a operação é registrada.

O projeto prevê dois mecanismos diferentes:

- **Mercado entre agentes:** oferta e demanda surgem das decisões dos próprios habitantes.
- **Black Market sistêmico:** mecanismo inspirado no Black Market de Albion Online, com ordens de compra limitadas que podem absorver itens elegíveis. Não deve ser um comprador infinito a preço garantido.

O tutorial poderá exibir ofertas de Tier 2 desde o início para ensinar a comprar, mesmo que o agente ainda não saiba fabricar ou usar esses itens. O acesso à compra não concede automaticamente conhecimento, habilidade ou capacidade de uso.

## Progressão por Tier

Os Tiers representam faixas de materiais, ferramentas, receitas, equipamentos e desafios. A progressão de cada atividade deve ser separada: praticar caçada não concede automaticamente domínio de coleta ou fabricação.

Exemplo: para coletar um recurso de Tier 2, o agente pode precisar ter desbloqueado essa faixa e possuir a ferramenta adequada. Ele pode fabricar a ferramenta, comprá-la de outro agente ou encontrar outra forma legítima de obtê-la.

## Arquitetura pretendida

- **Núcleo da sociedade:** objetivos, personalidade, memória, relações, tomada de decisão e avaliação de oportunidades.
- **Modelo de mundo e economia:** recursos, inventários, receitas, mercado, transações, regiões, risco e persistência.
- **Regras de simulação:** validação e aplicação das consequências das ações.
- **Adaptadores de ambiente:** interface entre o núcleo e um simulador ou, futuramente, um servidor de MMORPG.
- **Visualização Godot:** mapa 2D e ferramentas de observação, sem ser dona da lógica dos agentes.
- **Camada de IA generativa:** componente opcional para diálogo e interações complexas, desacoplado das regras críticas.

O laboratório inicial usará Godot para visualização e experimentação. A lógica deverá poder ser executada sem renderização para facilitar testes, simulações aceleradas e aumento de escala.

## Fora do escopo inicial

- Construir imediatamente um MMORPG completo.
- Implementar centenas de agentes antes de validar o ciclo básico.
- Criar uma história roteirizada para cada personagem.
- Integrar já um servidor de jogo real.
- Usar IA generativa para cada ação de movimento, coleta ou combate.
- Copiar código, marcas ou assets proprietários de Albion Online ou de outros jogos.

## Plano inicial

O plano de implementação, critérios de aceite e etapas de validação estão em **[docs/PLANO_INICIAL.md](docs/PLANO_INICIAL.md)**.

## Estado atual

O repositório está no início da fase de definição. As decisões deste README são a direção inicial do projeto; detalhes de balanceamento, fórmulas econômicas, persistência e implementação ainda precisam ser definidos e validados por testes.

## Licenciamento e referências

Este projeto é experimental e independente. Albion Online serve apenas como referência conceitual para algumas mecânicas de economia e progressão; o projeto não é afiliado ao jogo e não pretende copiar seus assets ou conteúdo protegido. A licença do próprio projeto deverá ser definida antes de distribuir código ou conteúdo em uma versão pública.
