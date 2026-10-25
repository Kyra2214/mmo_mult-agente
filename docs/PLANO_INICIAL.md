# Plano inicial de desenvolvimento

**Projeto:** MMO Multiagente  
**Estado:** planejamento inicial  
**Objetivo:** provar que agentes autônomos conseguem aprender as mecânicas iniciais, agir num mundo persistente e participar de uma economia sem precisar de mapa gráfico.

Este plano é incremental. Cada etapa precisa demonstrar comportamento correto e ter testes antes de aumentar a complexidade ou a população. Os números de agentes são metas de validação, não prazos nem promessas de desempenho.

## Regras de execução

1. Código e testes são a fonte de verdade; a documentação descreve as decisões implementadas.
2. Não marcar uma etapa como concluída só porque os arquivos foram criados. Exigir implementação funcional, testes e evidência observável.
3. Separar núcleo do agente, regras do mundo, NPCs/quests, persistência, economia e apresentação textual.
4. O simulador inicial não deve depender de mapa gráfico, Godot ou IA generativa para executar a lógica central.
5. Usar valores determinísticos e sementes de aleatoriedade controladas nos testes sempre que possível.
6. Registrar eventos e razões relevantes para explicar as decisões dos agentes, sem transformar logs em conhecimento automático para todos.
7. Corrigir falhas e atualizar o plano antes de avançar para a próxima escala.

## Fase 0 — Fundação do simulador textual

### Objetivos
- Definir estrutura mínima e tecnologia de execução para um simulador headless.
- Manter lógica do agente e regras independentes de qualquer visualização.
- Definir como o tempo simulado avança por turnos ou em execução contínua.
- Preparar testes e persistência inicial.

### Entregáveis
- Aplicação mínima executável em modo texto.
- Estrutura separada para núcleo do agente, mundo, NPCs/quests, economia, persistência, console e testes.
- Instruções verificadas para executar localmente.
- Um tick de simulação que possa rodar sem renderização nem serviço externo.
- Registro das decisões arquiteturais iniciais.

### Critérios de aceite
- O programa inicia e executa sem interface gráfica.
- É possível avançar a simulação por turnos e observar o estado textual.
- O núcleo da lógica não depende de nós visuais.
- As instruções de execução foram verificadas.

## Fase 1 — Núcleo lógico do agente

### Implementar
- Identidade, estado e inventário básico.
- Objetivos, necessidades e preferências iniciais.
- Conhecimento limitado: o que viu, ouviu ou aprendeu de fontes permitidas.
- Avaliação de ações possíveis e escolha de um objetivo alcançável.
- Distinção entre conhecimento, capacidade, recursos possuídos e requisitos ainda não cumpridos.
- Reavaliação após sucesso, falha, falta de recursos ou mudança de condições.
- Registro explicável de decisão e resultado.

### Critérios de aceite
- O agente não escolhe indefinidamente alvos impossíveis.
- Não conhece automaticamente regiões, serviços ou fatos que nunca descobriu.
- Não confunde conhecer uma receita com ter capacidade de fabricá-la.
- Falhas não causam repetição infinita da mesma ação sem reavaliação.
- Testes exercitam decisões sem intervenção manual.

## Fase 2 — Regras do mundo abstrato

### Implementar
- Regiões identificadas por dados, sem mapa gráfico.
- Distâncias/rotas abstratas e custo de tempo para viajar.
- Zona inicial segura, recursos finitos e criaturas de Tier 1.
- Estado de personagem, saúde, ferramentas, inventário e dinheiro.
- Regras de deslocamento, coleta, combate, recompensas e fabricação.
- Relógio/ticks independente da velocidade de execução.
- Eventos estruturados para ações e consequências.

### Critérios de aceite
- A simulação pode avançar sem desenhar objetos.
- Recursos não são coletados acima da quantidade disponível.
- A viagem consome tempo e respeita rotas e restrições configuradas.
- Cada recompensa tem causa válida e é concedida uma única vez.
- O mesmo cenário de teste pode ser reproduzido com a mesma configuração.

## Fase 3 — NPCs, diálogos e conhecimento

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

### Implementar
- Cada NPC possui identidade, função, diálogos, serviços e informações que pode ensinar.
- NPCs de fabricação apresentam receitas de Tier 1, 2 e 3 desde o tutorial, incluindo materiais e requisitos.
- Receitas conhecidas não ignoram bloqueios de progressão, ferramenta, materiais ou capacidade.
- Conversar permite adquirir conhecimento, mas não obriga o agente a usar o serviço.
- Conhecimento adquirido é registrado na memória individual do agente.
- Diálogos e fatos aprendidos não são automaticamente compartilhados com todos os agentes.

### Critérios de aceite
- O agente consegue perguntar e descobrir a função de cada NPC.
- Depois de aprender uma informação, consegue recuperá-la da própria memória quando relevante.
- NPCs não concedem itens ou habilidades só por explicar um serviço.
- O agente pode comparar serviços e recusar utilizá-los.

## Fase 4 — Quest inicial do Dia 1

O tutorial começa com uma quest guiada, não com uma análise econômica livre. A quest apresenta as mecânicas e verifica o estado real do agente em cada etapa.

### Sequência inicial
1. **Conhecer a vila:** conversar com os 11 NPCs e descobrir as funções e serviços.
2. **Aprender a coletar:** receber instruções sobre recursos, ferramentas e coleta de Tier 1.
3. **Aprender a caçar:** enfrentar uma criatura compatível e aprender combate, riscos e recompensas.
4. **Reunir materiais:** verificar inventário e obter legitimamente os materiais que faltarem.
5. **Fabricar o primeiro item:** visitar o fabricante adequado e produzir um item Tier 1, consumindo os materiais da receita.
6. **Aprender inventário e baú:** guardar e retirar itens e entender a diferença entre carga e armazenamento.
7. **Aprender o mercado:** consultar ofertas, aprender a comprar e publicar uma oferta de venda.
8. **Conhecer deslocamento e masmorra:** entender destinos, custos, perigos e requisitos.

### Regras da quest
- A quest ensina; não escolhe a profissão definitiva do agente.
- Etapas verificam condições reais. Se o agente já tem um material, não precisa coletá-lo de novo.
- Se perder ou gastar um recurso necessário, deve obter outro por uma ação válida.
- Não conceder materiais, dinheiro, XP ou equipamentos sem uma causa definida.
- O tutorial pode orientar uma ação, mas o motor do mundo continua validando se ela é possível.
- Publicar uma oferta não exige que um comprador apareça para a quest continuar.
- O tutorial não pode deixar o agente preso indefinidamente se não houver comprador ou serviço disponível.

### Critérios de aceite
- O agente completa a sequência sem intervenção manual, respeitando pré-requisitos.
- A memória registra o que aprendeu e o estado da quest sobrevive ao reinício.
- Não há recompensa duplicada por repetir diálogo ou reprocessar uma etapa.
- O agente não recebe progresso por uma ação que não aconteceu.

## Fase 5 — Coleta, caça e fabricação de Tier 1

### Implementar
- Recursos de Tier 1 e ferramentas exigidas.
- Criaturas fracas e regras básicas de combate.
- XP de caça separado da progressão de coleta e fabricação.
- Receitas de Tier 1 e consumo dos materiais necessários.
- Custos de produção e resultados válidos.
- Alternativas permitidas quando faltam ferramentas ou materiais.

### Critérios de aceite
- O agente coleta apenas recursos acessíveis com os requisitos corretos.
- Fabricação consome os materiais exigidos e cria somente o resultado definido pela receita.
- A progressão de uma atividade não concede automaticamente domínio das outras.
- O primeiro agente consegue completar coleta/caça e fabricar um item sem intervenção manual.

## Fase 6 — Mercado persistente e transporte

### Implementar
- Ofertas de compra e venda com item, quantidade, preço, proprietário e estado.
- Publicação sem venda imediata; item permanece com o vendedor até a transação.
- Ofertas persistentes entre ticks e sessões.
- Consulta de ofertas e comparação de preços.
- Compra com saldo e estoque suficientes.
- Liquidação atômica: item e dinheiro transferidos juntos ou operação sem alterações.
- Histórico de transações, cancelamento e expiração segundo regras definidas.
- Black Market sistêmico com ordens elegíveis, orçamento limitado e sem compra infinita.
- Ofertas Tier 2 para ensinar compra, sem conceder automaticamente capacidade de fabricar ou usar o item.
- Teleportador com destinos e custos em alzes; comparação entre teleporte e viagem a pé.

### Decisão de transporte
O agente considera, conforme suas informações disponíveis, custo em alzes, tempo, distância, risco, carga transportada, objetivo e oportunidades. Conhecer o teleportador não obriga a usá-lo.

### Critérios de aceite
- Publicar oferta não vende item nem concede dinheiro.
- Dois compradores não conseguem comprar a mesma unidade disponível.
- Venda transfere item e dinheiro exatamente uma vez.
- Oferta sem comprador permanece conforme as regras e não bloqueia a vida do agente.
- Black Market não cria dinheiro ilimitado.
- Testes cobrem saldo insuficiente, estoque insuficiente, concorrência e falhas de transação.
- A decisão de transporte respeita orçamento, rotas e informações conhecidas.

## Fase 7 — Desbloqueio de Tiers superiores

### Implementar
- Requisitos de progressão por atividade e Tier.
- Receitas Tier 1, 2 e 3 apresentadas pelos NPCs de fabricação desde o tutorial.
- Regras de desbloqueio, ferramentas, materiais e capacidade real de fabricação.
- Caminhos alternativos: fabricar uma ferramenta, comprá-la ou adiar a melhoria.
- Diferença explícita entre conhecer, conseguir executar e possuir recursos.

### Critérios de aceite
- O agente pode conhecer receitas T2/T3 sem executá-las antes de cumprir os requisitos.
- Comprar um item não concede domínio da atividade correspondente.
- O agente consegue avaliar se vale mais a pena fabricar, comprar ou adiar uma melhoria.

## Fase 8 — Dois a cinco agentes

### Implementar
- Agentes com estados, memórias, preferências e objetivos separados.
- Compras e vendas reais entre agentes.
- Ofertas publicadas que podem ser compradas mais tarde.
- Informações limitadas ao que cada agente observa, aprende ou consulta.
- Persistência dos personagens, quest, inventários e mercado.

### Critérios de aceite
- Um agente não acessa a memória privada ou o inventário de outro sem mecanismo permitido.
- Um segundo agente consegue comprar uma oferta persistente com transferência correta de item e dinheiro.
- Não há duplicação de itens ou dinheiro por concorrência.
- Agentes podem tomar decisões diferentes sem roteiros individuais.
- Nenhum agente fica preso num ciclo de ações inúteis nos cenários de teste.

## Fase 9 — Dez a vinte e cinco agentes: economia emergente

### Implementar
- Oferta e demanda geradas pelos agentes.
- Custos de oportunidade, distância, tempo e risco.
- Variação de preços sem manipulação ilimitada.
- Especialização espontânea e mudança de atividade.
- Fontes e sumidouros de dinheiro explícitos.
- Métricas de estoque, preços, produção, consumo, riqueza e concentração econômica.

### Critérios de aceite
- Toda criação e remoção de item ou dinheiro tem causa registrada.
- O sistema detecta estoque encalhado, escassez e ausência de compradores.
- A economia é observável por métricas e logs, não por animação visual.

## Fase 10 — Cinquenta agentes: grupos e risco

### Implementar
- Relações, reputação e formação experimental de grupos/guildas.
- Regiões com riscos e recompensas diferentes.
- Avaliação de risco baseada em capacidade, equipamento, carga, distância e possível perda.
- Combate em grupo e respostas a ameaças, com conhecimento obtido por observação, contatos ou rumores.
- Regras de morte, saque e transferência de itens, se o modelo PvP for ativado.

### Critérios de aceite
- Agentes cautelosos e ambiciosos podem tomar decisões diferentes.
- Informações sobre inimigos e oportunidades têm origem rastreável.
- Loot transfere itens existentes; não duplica inventários.

## Fase 11 — Cem agentes ou mais: desempenho e robustez

Só iniciar depois de validar a coerência das fases anteriores.

### Implementar
- Simulação acelerada sem renderização.
- Medição de tempo por tick, uso de memória e custo por agente.
- Redução de cálculos desnecessários e atualização por eventos quando fizer sentido.
- Testes prolongados de persistência, reinício e recuperação de estado.
- Cenários de estresse para mercado, combate e disputa por recursos.

### Critérios de aceite
- Desempenho medido e documentado em ambiente conhecido.
- Reiniciar a simulação não corrompe personagens, inventários, quests ou transações.
- Testes prolongados não revelam duplicação sistemática de recursos nem agentes permanentemente travados.
- O tamanho máximo da população é definido por medições, não por suposição.

## Fase futura — Visualização gráfica ou adaptador para MMORPG

Um mapa gráfico é opcional e só deve ser considerado depois que o simulador textual e o núcleo dos agentes forem validados. Se for adicionado, será uma camada de visualização, não a dona da lógica.

A integração com rAthena, outro servidor ou um MMORPG próprio também fica fora do protótipo inicial. Quando o núcleo estiver validado, o adaptador deverá expor estado e ações permitidas, enquanto o servidor do jogo continua sendo a autoridade sobre movimento, combate, inventário, XP e demais regras reais.

Antes de qualquer integração ou distribuição, verificar licenças e direitos de uso de código, marcas, dados e assets.

## Primeira meta concreta

A primeira entrega funcional deve demonstrar, em modo textual e sem mapa:
1. Um agente inicia a quest do Dia 1.
2. Conversa com NPCs e aprende suas funções.
3. Aprende coleta e caça e obtém recursos por ações válidas.
4. Fabrica um item Tier 1 com materiais corretos.
5. Aprende o mercado e publica uma oferta persistente.
6. Continua sua vida sem ficar bloqueado enquanto não houver comprador.
7. Salva e recupera o estado sem duplicar progresso, itens ou dinheiro.

Depois, adicionar um segundo agente para comprar a oferta. Só após validar esse ciclo ampliar a população e considerar uma visualização gráfica.
