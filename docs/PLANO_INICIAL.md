# Plano inicial de desenvolvimento

**Projeto:** MMO Multiagente  
**Estado:** planejamento inicial  
**Objetivo desta fase:** provar que agentes autônomos conseguem completar um ciclo de atividade e comércio num mundo persistente simples.

Este plano é incremental. Cada etapa precisa demonstrar comportamento correto e ter testes antes de aumentar a complexidade ou a população. Os números de agentes são metas de validação, não prazos nem promessas de desempenho.

## Regras de execução

1. Código e testes são a fonte de verdade; a documentação descreve as decisões implementadas.
2. Não marcar uma etapa como concluída só porque os arquivos foram criados. Exigir implementação funcional, testes e evidência observável.
3. Separar regras do mundo, lógica dos agentes, persistência e visualização.
4. Usar valores determinísticos e sementes de aleatoriedade controladas nos testes sempre que possível.
5. Registrar eventos importantes para conseguir explicar por que um agente tomou uma decisão.
6. Corrigir falhas e atualizar o plano antes de avançar para a próxima escala.

## Fase 0 — Fundação do repositório

### Objetivos
- Definir a estrutura mínima do projeto.
- Escolher e registrar a versão estável do Godot adequada ao protótipo.
- Estabelecer convenções de código, testes e execução.
- Manter o simulador executável sem exigir IA generativa ou acesso a serviços externos.

### Entregáveis
- Projeto Godot mínimo que abre e executa.
- Estrutura separada para núcleo da simulação, agentes, mundo, economia, interface visual e testes.
- Instruções para executar localmente.
- Teste inicial automatizado ou verificações executáveis para validar a configuração.
- Registro das decisões arquiteturais iniciais.

### Critérios de aceite
- O projeto abre e executa sem erros.
- A lógica central não depende de nós visuais da cena.
- É possível executar pelo menos uma atualização de simulação sem renderizar o mapa.
- As instruções de execução foram verificadas.

## Fase 1 — Modelo mínimo do mundo

### Implementar
- Mapa 2D dividido em regiões ou células simples.
- Zona verde inicial.
- Recursos coletáveis com quantidade finita e localização.
- Monstros com atributos e comportamento básico.
- Inventário, dinheiro e estado do personagem.
- Relógio/ticks da simulação, desacoplado da velocidade de renderização.
- Registro de eventos: movimento, coleta, combate, fabricação, compra e venda.

### Critérios de aceite
- O estado do mundo pode ser consultado e validado por testes.
- Recursos não são coletados acima da quantidade disponível.
- O estado evolui de maneira reproduzível com a mesma configuração de teste.
- A simulação pode avançar sem desenhar os objetos.

## Fase 2 — Primeiro agente autônomo

### Implementar
- Identidade e estado persistente básico.
- Objetivos imediatos e avaliação de necessidades: saúde, ferramentas, inventário, dinheiro e distância.
- Escolha de objetivos alcançáveis.
- Ações disponíveis limitadas ao que o agente realmente pode executar.
- Reavaliação após sucesso, falha, mudança de recursos ou perigo.
- Registro explicável do objetivo escolhido e do resultado.

### Critérios de aceite
- O agente não escolhe indefinidamente alvos impossíveis.
- Um agente fraco prefere desafios compatíveis com sua capacidade.
- Se não tiver ferramenta, identifica a restrição e busca uma alternativa válida.
- Falhas não causam repetição infinita da mesma ação sem reavaliação.
- Testes demonstram o ciclo de decisão sem intervenção manual.

## Fase 3 — Ciclo de Tier 1

### Implementar
- Caçada a monstros fracos e obtenção válida de recompensas.
- XP geral de caçada e progressão específica por faixa, sem misturar automaticamente as habilidades.
- Coleta de materiais de Tier 1.
- Ferramentas e requisitos para coleta.
- Receitas e fabricação de itens de Tier 1.
- Custos de produção e consumo dos materiais usados.
- Tutorial inicial por quests, como forma de ensinar as ações fundamentais.

### Critérios de aceite
- Cada recompensa tem uma causa válida e é concedida uma única vez.
- Não é possível coletar recursos incompatíveis com a ferramenta ou o desbloqueio do agente.
- Fabricação consome os materiais exigidos e cria somente o resultado da receita.
- O agente consegue completar um ciclo de caçar/coletar, obter recursos e fabricar um item.

## Fase 4 — Mercado persistente e tutorial comercial

### Implementar
- Ofertas de compra e venda com item, quantidade, preço, vendedor/comprador e estado.
- Publicação de oferta sem venda imediata.
- Ofertas que continuam disponíveis entre ticks e sessões.
- Consulta de ofertas e comparação de preços.
- Compra válida com saldo suficiente e estoque disponível.
- Liquidação atômica: item e dinheiro são transferidos juntos ou a transação falha sem alterar o estado.
- Histórico de transações e cancelamento/expiração de ofertas conforme regras definidas.
- Black Market sistêmico inspirado no conceito do Albion, com ordens de compra limitadas e orçamento controlado.
- Quest de comércio que ensina a publicar oferta e comprar um item. Pode haver oferta Tier 2 para demonstrar compra, sem conceder automaticamente a habilidade de fabricar ou usar esse item.

### Critérios de aceite
- Publicar uma oferta não vende o item nem concede dinheiro.
- Um item listado continua pertencendo ao vendedor até a venda.
- Dois compradores não conseguem comprar a mesma unidade disponível.
- Uma venda concluída transfere item e dinheiro exatamente uma vez.
- Uma oferta sem comprador permanece listada segundo as regras definidas.
- O Black Market não cria dinheiro ilimitado nem compra qualquer item sem uma ordem elegível.
- Os testes cobrem saldo insuficiente, estoque insuficiente, concorrência e falhas de transação.

## Fase 5 — Desbloqueio de Tier 2

### Implementar
- Requisitos de progressão por atividade e Tier.
- Ferramentas, recursos, receitas e equipamentos de Tier 2.
- Quests como tutorial, sem transformar o comportamento posterior numa sequência roteirizada.
- Caminhos alternativos legítimos: fabricar uma ferramenta ou comprá-la, por exemplo.
- Regras para distinguir conhecimento, capacidade e posse de recursos.

### Critérios de aceite
- O agente não acessa uma receita ou coleta bloqueada sem cumprir os requisitos.
- Comprar um item não concede automaticamente domínio da atividade correspondente.
- O agente consegue avaliar se vale mais a pena fabricar, comprar ou adiar uma melhoria.
- A progressão não depende de uma venda imediata quando não há comprador; publicar uma oferta pode cumprir o objetivo de aprender a anunciar, enquanto a conclusão de transações é testada separadamente.

## Fase 6 — Cinco agentes e primeiras interações

### Implementar
- Múltiplos agentes com estados e objetivos separados.
- Preferências e recursos iniciais diferentes, mas dentro de regras justas.
- Compras e vendas entre agentes.
- Decisões de produzir para uso próprio ou para vender.
- Informações limitadas ao que cada agente consegue observar ou consultar.
- Persistência dos personagens e do mercado.

### Critérios de aceite
- Um agente não acessa a memória privada ou inventário de outro sem um mecanismo permitido.
- Agentes podem publicar ofertas que outros compram mais tarde.
- A população não gera itens ou dinheiro duplicados por concorrência.
- A simulação apresenta decisões diferentes sem precisar de roteiros individuais.
- Nenhum agente fica preso num ciclo de ações inúteis nos cenários de teste.

## Fase 7 — Dez a vinte e cinco agentes: economia emergente

### Implementar
- Oferta e demanda geradas pelos agentes.
- Custos de oportunidade, distância, tempo e risco.
- Variação de preços sem manipulação ilimitada.
- Especialização espontânea e mudança de atividade quando surgem oportunidades.
- Fontes e sumidouros de dinheiro explícitos.
- Métricas de estoque, preços, produção, consumo, riqueza e concentração econômica.

### Critérios de aceite
- Toda criação e remoção de item ou dinheiro tem uma causa registrada.
- O sistema detecta estoque encalhado, escassez e ausência de compradores.
- Os agentes podem escolher atividades diferentes com base nas condições.
- A economia é observável por métricas, não apenas pela animação visual.

## Fase 8 — Cinquenta agentes: grupos e risco

### Implementar
- Relações, reputação e formação experimental de grupos/guildas.
- Regiões com riscos e recompensas diferentes.
- Avaliação de risco baseada em capacidade, equipamento, carga, distância e possível perda.
- Combate em grupo e respostas a ameaças, com conhecimento obtido por observação, contatos ou rumores.
- Regras de morte, saque e transferência de itens, caso o modelo PvP seja ativado.

### Critérios de aceite
- Agentes cautelosos e ambiciosos podem tomar decisões diferentes.
- Informações sobre inimigos e oportunidades têm origem rastreável.
- Loot transfere itens existentes; não duplica inventários.
- A perda e a proteção de dinheiro seguem regras consistentes e testadas.

## Fase 9 — Cem agentes ou mais: desempenho e robustez

Só iniciar depois de validar a coerência das fases anteriores.

### Implementar
- Simulação acelerada sem renderização.
- Medição de tempo por tick, uso de memória e custo por agente.
- Redução de cálculos desnecessários e atualização por eventos quando fizer sentido.
- Testes prolongados de persistência, reinício e recuperação de estado.
- Cenários de estresse para mercado, combate e disputa por recursos.

### Critérios de aceite
- O desempenho é medido e documentado em ambiente conhecido.
- Reiniciar a simulação não corrompe personagens, inventários ou transações.
- Testes prolongados não revelam duplicação sistemática de recursos nem agentes permanentemente travados.
- O tamanho máximo da população é definido por medições, não por suposição.

## Fase futura — Adaptador para um MMORPG real

A integração com rAthena, outro servidor ou um MMORPG próprio fica fora do protótipo inicial. Quando o núcleo estiver validado, o adaptador deverá expor estado e ações permitidas, enquanto o servidor do jogo continua sendo a autoridade sobre movimento, combate, inventário, XP e demais regras reais.

Antes de qualquer integração ou distribuição, verificar a licença do servidor escolhido e os direitos de uso de código, marcas, dados e assets.

## Primeira meta concreta

A primeira entrega funcional deve demonstrar um agente no mapa 2D que:

1. avalia seu estado e escolhe um objetivo possível;
2. caça ou coleta recursos de Tier 1;
3. fabrica um item com materiais válidos;
4. publica o item no mercado;
5. mantém a oferta ativa enquanto não houver comprador;
6. consegue retomar suas atividades sem ficar bloqueado esperando uma venda.

Depois disso, adicionar um segundo agente para testar a compra real da oferta. Esse é o primeiro teste de uma economia que pode continuar existindo sem intervenção humana.
