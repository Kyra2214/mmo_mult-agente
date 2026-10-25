# Plano técnico — Economia emergente, habilidades e agentes autônomos

**Projeto:** MMO Multiagente  
**Status:** execução iniciada na Fase A; auditoria inicial registrada em `docs/FASE_A_AUDITORIA_INICIAL.md`. Economia compartilhada e fases B/C ainda não implementadas  
**Referência conceitual:** ciclos de progressão e produção de Albion Online, adaptados ao simulador. Não reproduzir literalmente sua árvore de nós, conteúdo protegido ou balanceamento.

## 1. Objetivo

Criar uma sociedade simulada capaz de sustentar pelo menos 1.000 agentes com estados e decisões individuais. Cada agente pode escolher entre coletar, comprar, transportar, refinar, cultivar, criar animais, pescar, caçar, fabricar, cozinhar, produzir poções, vender, combater ou combinar várias etapas.

O simulador não atribui profissão permanente. Atividades exercidas e modelos de negócio emergem das decisões dos agentes, das capacidades aprendidas, dos recursos disponíveis, dos preços, da concorrência, do capital e das necessidades pessoais.

Exemplos de estratégias possíveis — não classes pré-definidas:
- Coletar matéria-prima e vender.
- Comprar matéria-prima, refinar e vender.
- Comprar materiais refinados e fabricar.
- Controlar a cadeia inteira, da coleta à venda.
- Cultivar ingredientes e abastecer cozinheiros/alquimistas.
- Especializar-se em uma oportunidade temporária e depois mudar de atividade.

## 2. Princípios obrigatórios

1. **Agentes sem profissão fixa.** Profissão é uma descrição de atividade atual ou recorrente, não uma classe que restringe a vida do NPC.
2. **Autonomia algorítmica, sem LLM no loop.** A seleção de ações deve ser determinística quando configurada com a mesma semente, explicável e barata por tick.
3. **Capacidades limitam ações.** O agente só executa ações para as quais possui conhecimento, desbloqueios, ferramentas, ingredientes, capital, acesso e tempo necessários.
4. **Conhecimento, desbloqueio, especialização e posse são estados distintos.** Conhecer uma receita não significa poder fabricá-la; poder fabricá-la não significa ter os ingredientes.
5. **Aprender não é instantâneo.** Mudar de atividade é permitido, mas desenvolver uma nova habilidade consome ações, recursos e tempo.
6. **Mercado como sinal, não como ordem.** Preços e demanda influenciam a escolha, mas não obrigam todos os agentes a seguir a mesma atividade.
7. **Contabilidade fechada.** Itens e dinheiro só entram ou saem por eventos com causa válida e auditável.
8. **Recursos finitos e receitas explícitas.** Coleta, agricultura, pesca, caça, refino, culinária, alquimia e fabricação precisam de insumos e regras de produção.
9. **Sem onisciência.** Cada agente toma decisões com base nas informações que pode observar, consultar ou recordar.
10. **Escalar por medição.** Não assumir que 1.000 agentes funcionam bem sem testes de desempenho, persistência e coerência econômica.

## 3. Modelo de decisão do agente

Cada tick não deve executar uma IA generativa. O motor reúne ações viáveis e calcula uma pontuação de utilidade. A política escolhe uma ação ou mantém um plano curto se ele ainda for válido.

### 3.1 Entradas da decisão
- Necessidades: alimento, saúde, segurança, abrigo e urgência de dinheiro.
- Estado econômico: saldo, inventário, capacidade de carga, ferramentas e equipamentos.
- Habilidades: progresso por linha, especializações, receitas conhecidas e desbloqueios.
- Mundo: recursos disponíveis, rotas, perigos, oficinas, fazendas e serviços acessíveis.
- Mercado conhecido: preços recentes, ofertas, volume negociado, procura e concorrência.
- Preferências: aversão a risco, preferência por liquidez, investimento de longo prazo e atividades conhecidas.
- Memória individual: resultados anteriores, perdas, vendas frustradas e oportunidades observadas.
- Plano atual: etapas, custo previsto, validade e condição de reavaliação.

### 3.2 Geração e avaliação de alternativas
1. Enumerar somente ações atualmente viáveis.
2. Estimar receita, custo de insumos, taxas, transporte, tempo, risco, capital imobilizado e probabilidade de venda.
3. Calcular retorno líquido estimado e retorno por unidade de tempo/capital.
4. Aplicar ajustes por necessidades urgentes, preferências, incerteza e objetivos de longo prazo.
5. Escolher a melhor alternativa elegível; desempates podem usar uma semente individual reproduzível.
6. Executar pelo motor de regras, que revalida todos os requisitos no momento da ação.
7. Registrar decisão, motivo, previsão, resultado e diferença entre previsão e resultado.
8. Reavaliar após mudança material: estoque esgotado, venda, preço alterado, ferramenta quebrada, perigo, falta de capital ou falha repetida.

A função de pontuação deve ser configurável e testada. Evitar uma fórmula que sempre maximize lucro imediato: isso pode causar comportamento oscilatório, concentração excessiva numa atividade ou abandono de necessidades básicas. Incluir histerese/custo de mudança e horizonte de planejamento para evitar trocar de estratégia a cada tick.

### 3.3 Estratégias curtas e longas
- **Curto prazo:** ações simples, como coletar uma quantidade, comprar ingredientes ou publicar uma oferta.
- **Plano composto:** sequência de ações para um objetivo, como comprar minério → refinar → fabricar → vender.
- **Estratégia de negócio:** investir em ferramentas, desbloquear especialização, adquirir insumos ou mudar de cidade.
- **Replanejamento:** preservar o plano enquanto for viável e competitivo; abandonar ou modificar quando as premissas mudarem.

O agente pode decidir integrar toda a cadeia, mas precisa comparar o lucro líquido da cadeia completa com a alternativa de vender insumos e considerar o valor do próprio tempo. A simulação não deve assumir que fabricar sempre gera mais lucro.

## 4. Progressão de habilidades inspirada no Destiny Board

Usar Albion Online como referência de organização e ciclos, não como especificação literal. Não é necessário replicar centenas de nós. O sistema será dirigido por dados e poderá crescer sem criar regras específicas no motor para cada receita.

### 4.1 Camadas
1. **Linha de habilidade:** coleta, pesca, agricultura, criação de animais, caça/combate, refino por família, fabricação por família, culinária, alquimia, comércio/transporte.
2. **Maestria e Tier:** definem os níveis de conteúdo que o agente pode acessar.
3. **Especialização:** melhora uma família específica, como espadas, armaduras, ervas, poções de cura ou refinamento de minério.
4. **Receita/atividade:** contém requisitos de conhecimento, tier, ferramentas, local, ingredientes e custo.
5. **Benefícios:** desbloqueios e melhorias graduais de rendimento, velocidade, qualidade ou custo, dentro de limites definidos.

### 4.2 Regras de progressão
- Cada linha progride separadamente; coletar não concede automaticamente nível de fabricação.
- Executar uma atividade válida concede progresso à linha correspondente, de acordo com a contribuição e as regras da ação.
- Tier superior exige pré-requisitos próprios; não basta possuir materiais ou comprar o produto.
- Conhecer uma receita, desbloquear a receita, ter habilidade suficiente e conseguir executá-la são verificações separadas.
- Especializações não obrigam o agente a continuar na atividade; elas preservam vantagens adquiridas se ele mudar.
- Desbloqueios de Tier liberam novas receitas, culturas, ingredientes, recursos, ferramentas ou poções, conforme catálogo.
- O custo e a velocidade de progressão precisam ser calibrados por testes, sem exigir grind artificial infinito.
- Os dados devem permitir novas linhas e receitas sem alterações no algoritmo de decisão.

## 5. Ciclos econômicos que precisam existir

### 5.1 Recursos naturais e coleta
Nós de recursos finitos por região, tier, tempo de reposição e ferramenta exigida. A coleta consome tempo e durabilidade, produz quantidade limitada e registra a origem. Recursos não aparecem no inventário apenas porque uma rotina decidiu que estão faltando.

Famílias iniciais a modelar conforme o mundo: madeira, minério, pedra, fibras, couro obtido por caça, ingredientes silvestres e peixes.

### 5.2 Agricultura e criação
Aquisição legítima de sementes/animais, preparação do terreno, plantio ou criação, tempo de crescimento, água/insumos se definidos, colheita e rendimento. A fazenda ocupa capacidade e exige investimento; o agente compara seu retorno com comprar ingredientes no mercado. Culturas e produtos animais podem desbloquear tiers e especializações próprias.

### 5.3 Refino
Receitas explícitas de múltiplos ingredientes, ferramentas/estação e custo de operação. Exemplo conceitual: minério + combustível → lingotes. O refinador pode comprar matéria-prima de coletores e vender produtos refinados, ou refinar recursos que coletou. Custos e rendimento precisam estar definidos nos dados.

### 5.4 Fabricação
Receitas de múltiplos materiais, estação, ferramenta, tier e especialização. Produzir consome todos os insumos atomicamente: ou todos são debitados e o produto é criado, ou nada muda. Incluir desgaste, taxas e falha somente quando forem regras explícitas.

### 5.5 Culinária
Cadeia: sementes/culturas, caça, pesca e outros ingredientes → receitas de comida → consumo ou venda. Alimentos aplicam efeitos temporários, como regeneração, defesa, ataque, velocidade ou rendimento de coleta. Para a primeira versão, definir categorias de efeito, duração, regras de substituição e limites; não permitir empilhamento infinito de buffs equivalentes. O catálogo de receitas deve ser orientado a dados.

### 5.6 Alquimia
Cadeia: ervas, culturas e outros ingredientes → processamento, se necessário → poções → consumo ou venda. Poções têm efeitos, duração, requisitos e custos configurados. Cultivo de ervas e alquimia devem ser linhas distintas, para que agentes diferentes possam abastecer uns aos outros.

### 5.7 Combate, caça e exploração
Combate exige alvos, risco, equipamento e regras válidas. Loot tem origem em criaturas, recompensas ou eventos definidos; não é concedido por uma função sem causa. Materiais de couro e outros produtos de caça precisam ter origem rastreável. O agente compara caça com alternativas econômicas.

### 5.8 Mercado, transporte e armazenamento
Mercado deve aceitar múltiplas ofertas simultâneas, de diferentes itens e quantidades, com proprietário, preço, estado, expiração e histórico. Reservar mercadoria ao listar; liquidar item e dinheiro atomicamente; impedir venda duplicada; permitir cancelar/expirar segundo regras. Agentes comparam comprar pronto, comprar insumos ou produzir. Transporte tem custo de tempo, dinheiro, carga e risco.

### 5.9 Reparos e consumo
Equipamentos desgastam e precisam de reparo ou substituição. Alimentos, poções, combustível e ferramentas são consumidos de acordo com regras claras. Isso cria demanda contínua, mas não deve ser calibrado para forçar artificialmente toda economia a consumir sem motivo.

## 6. Modelos de negócio emergentes

O motor não atribui esses papéis. Eles são resultados possíveis das decisões:
- **Coletor:** vende recursos naturais.
- **Comprador/refinador:** compra recursos e vende material processado.
- **Fabricante:** compra insumos e vende produtos finais.
- **Integrador:** coleta, refina, fabrica e vende quando a margem justifica.
- **Agricultor/criador:** vende ingredientes a cozinheiros e alquimistas.
- **Cozinheiro/alquimista:** transforma ingredientes em consumíveis.
- **Comerciante:** compra barato e vende em outro momento/local, assumindo risco e custos.
- **Aventureiro:** obtém recursos e recompensas por combate e vende excedentes.
- **Híbrido/temporário:** combina atividades ou muda de estratégia conforme preços e necessidades.

Esses nomes servem para análise e telemetria, não para restringir ações. A distribuição de estratégias deve surgir da população, de seus estados iniciais e das condições do mundo.

## 7. Mercado e decisões econômicas

### Regras de mercado
- Cada oferta representa item, quantidade, preço, vendedor/comprador, estado e prazo.
- Reservar o estoque listado; impedir que o mesmo item seja vendido ou consumido duas vezes.
- Liquidação transacional: validar saldo e estoque, transferir ambos e registrar evento indivisível.
- Compradores têm orçamento e objetivos; não há comprador sistêmico com dinheiro infinito.
- A demanda vem de necessidades, receitas, reparos, consumo e objetivos dos agentes, mais eventuais ordens sistêmicas limitadas e explicitamente configuradas.
- Preços devem resultar de ofertas e transações observáveis, não de uma garantia de lucro.
- Ofertas não vendidas continuam pendentes, expiram ou são canceladas conforme regras explícitas.
- Cada agente vê apenas os dados de mercado permitidos pela sua localização, consultas e conhecimento.

### Comparação de alternativas
Ao decidir entre vender minério agora ou refinar, o agente estima o valor esperado de ambas as opções. A estimativa considera preço de venda observado, custo de insumos, taxa, tempo de produção, chance de venda, estoque concorrente, risco e capital necessário. Não usar apenas o preço de anúncio como se fosse venda garantida.

## 8. Dados e arquitetura sugeridos

Adaptar aos modelos existentes após inspeção; nomes abaixo são conceitos, não compromisso de API final.
- **SkillLineDefinition:** linha, categorias, progressão e requisitos.
- **AgentSkillProgress:** progresso individual por linha/especialização.
- **RecipeDefinition:** entradas, quantidades, saída, estação, ferramenta, requisitos e duração.
- **ResourceNode:** região, recurso, tier, estoque atual/máximo, reposição e requisitos.
- **FarmPlot / CropDefinition:** estado do cultivo, duração, insumos, rendimento e requisitos.
- **MarketOffer:** proprietário, lado compra/venda, item, quantidade, preço, estado, expiração e reserva.
- **AgentPlan:** objetivo, sequência de etapas, orçamento, premissas e condição de reavaliação.
- **DecisionRecord:** alternativas consideradas, pontuações, escolha, razão e resultado.
- **EconomicEvent:** origem/destino de itens e dinheiro, causa, quantidades e identificador idempotente.

Preferir catálogos imutáveis carregados uma vez e estado mutável por agente/recurso. Evitar copiar catálogos completos para cada NPC. Persistir estado necessário para reinício; decisões podem ser resumidas em histórico limitado, com agregados para análise.

## 9. Fases de implementação e critérios de aceite

### Fase A — Auditoria e contratos
- Mapear o código real de decisão, progressão, receitas, inventário, persistência e mercado.
- Identificar pontos que criam recursos/dinheiro sem causa e o mercado de oferta única.
- Definir contratos de dados e eventos antes de mudar o motor.
- Registrar decisões de arquitetura e migração do estado salvo.
**Aceite:** mapa dos modelos e fluxos existentes; riscos documentados; testes atuais identificados; nenhuma funcionalidade declarada pronta sem evidência.

### Fase B — Economia rastreável
- Eliminar geração arbitrária de materiais.
- Implementar fontes finitas e eventos de coleta, caça e pesca.
- Definir receitas de múltiplos ingredientes.
- Tornar criação/consumo de itens e transferências de dinheiro auditáveis e idempotentes.
**Aceite:** nenhum item sem causa; coleta limitada pelo estoque; receita falha sem alterar estado se faltar qualquer insumo; testes de duplicação e persistência.

### Fase C — Mercado multi-oferta transacional
- Substituir o estado escalar de oferta por uma coleção persistente.
- Permitir vários itens/tipos e quantidades em paralelo.
- Implementar reserva, compra, venda, cancelamento, expiração e liquidação atômica.
**Aceite:** várias ofertas coexistem; não há venda duplicada, criação de dinheiro ou perda parcial de itens em falhas; ofertas sobrevivem ao reinício.

### Fase D — Catálogo de habilidades e receitas
- Formalizar linhas, maestrias, tiers, especializações e desbloqueios.
- Converter receitas para dados, com múltiplos ingredientes e requisitos.
- Separar conhecimento, desbloqueio, ferramenta, habilidade e posse de insumos.
**Aceite:** novos dados de receita/especialização podem ser adicionados sem reescrever a política de decisão; requisitos inválidos sempre bloqueiam a execução.

### Fase E — Decisão econômica orientada a utilidade
- Enumerar ações viáveis e estimar retorno líquido ajustado a tempo, risco e capital.
- Adicionar planos compostos e reavaliação com histerese.
- Permitir atividades híbridas e mudança de estratégia.
- Registrar razões para decisões e resultados reais.
**Aceite:** cenários controlados produzem escolhas coerentes; agentes com estados diferentes podem escolher estratégias diferentes; não há troca frenética a cada tick nem ciclo infinito de ação inviável.

### Fase F — Agricultura, culinária e alquimia
- Implementar sementes, plantio, crescimento, colheita e rendimento.
- Implementar cadeias de comida e poções com receitas e efeitos temporários orientados a dados.
- Integrar compra/venda e consumo à decisão dos agentes.
**Aceite:** ingredientes têm origem e estoque; cultivo consome tempo/capacidade; consumíveis são usados uma vez e aplicam efeitos conforme regras; receitas não geram ingredientes de graça.

### Fase G — Cadeia produtiva e população
- Conectar coleta, agricultura, pesca, caça, refino, fabricação, consumíveis, comércio, combate e reparos.
- Inicializar agentes com variações de capital, habilidades, preferências, inventário e conhecimento, sem profissão fixa.
- Validar mercado com produtores, intermediários, fabricantes e integradores surgindo de escolhas, não de atribuições manuais.
**Aceite:** em simulações repetidas, há mais de uma estratégia viável; a distribuição é explicável pelos estados e incentivos; cadeias conseguem operar sem criação artificial de insumos.

### Fase H — Testes de escala e equilíbrio
- Executar cenários determinísticos e simulações prolongadas.
- Medir tempo por tick, memória, eventos, tamanho de persistência e crescimento de ofertas.
- Testar população de 1, 5, 25, 100, 500 e 1.000 agentes.
- Medir inflação/deflação, escassez, itens encalhados, concentração de riqueza, falências, gargalos e atividades abandonadas.
- Introduzir limites de orçamento computacional por tick sem tornar agentes permanentemente inertes.
**Aceite:** a população de 1.000 agentes cumpre orçamento de desempenho acordado em aparelho/ambiente de referência; não há corrupção de estado ou duplicação econômica; métricas de equilíbrio são registradas. O orçamento de tempo/memória deve ser medido antes de fixar metas numéricas.

## 10. Testes obrigatórios
1. Coleta nunca excede estoque disponível.
2. Toda criação de item tem evento causal válido.
3. Receita com ingrediente ausente não consome parcialmente o inventário.
4. Progresso numa linha não desbloqueia indevidamente outra.
5. Conhecer uma receita não ignora maestria, tier, ferramenta ou estação.
6. Várias ofertas de itens distintos coexistem e persistem.
7. Dois compradores concorrentes não compram a mesma unidade.
8. A liquidação transfere item e dinheiro uma única vez.
9. Agente sem capital não executa compra ou investimento que não pode pagar.
10. Agente consegue escolher vender matéria-prima mesmo quando sabe fabricar, se essa for a melhor opção.
11. Agente consegue escolher a cadeia completa quando o retorno líquido esperado justificar o investimento.
12. Queda de preço, falta de insumos ou falha de venda provoca reavaliação, sem loop infinito.
13. Cultivo respeita duração, insumos, capacidade e rendimento.
14. Alimentos e poções respeitam receita, consumo, duração e regras de substituição de efeitos.
15. Resultados são reproduzíveis com semente/configuração iguais.
16. Teste de longa duração não produz criação ou destruição inexplicável de itens/dinheiro.
17. Teste de escala mede 1.000 agentes, não apenas assume que a arquitetura é eficiente.

## 11. Fora de escopo nesta etapa
- Usar IA generativa para decidir cada ação ou controlar cada NPC.
- Atribuir uma profissão permanente a cada agente.
- Copiar literalmente todos os nós, textos, receitas, artes ou números de Albion Online.
- Adicionar centenas de receitas antes de existir o motor genérico de receitas.
- Construir mapa gráfico antes de validar a economia textual.
- Declarar suporte a 1.000 agentes antes de medir desempenho.
- Balancear preços para garantir lucro a todas as profissões; perdas e mudança de estratégia fazem parte da simulação.

## 12. Situação atual conhecida e próximo passo

O protótipo já tem política heurística, progressão por especialização, receitas T1–T4, atividades iniciais, equipamento, masmorra e teste prolongado de 20.000 ticks. Entretanto, a política ainda não é um planejador estratégico geral, o mercado atual usa uma única oferta e há caminhos de geração de recursos que precisam de validação rigorosa.

**Execução atual:** a auditoria inicial foi registrada e a interface de observabilidade foi ampliada para cinco perfis com logs individuais. Isso não conclui a Fase A nem implementa comércio compartilhado. Próximas ações: terminar a auditoria dos caminhos de criação/consumo de recursos, validar a persistência por perfil e então iniciar a Fase B (economia rastreável), seguida da Fase C (mercado multi-oferta transacional). Não expandir ainda agricultura, culinária ou alquimia. Este documento é plano; cada fase exige testes e evidência funcional.

## Referência conceitual

Albion Online — Destiny Board, gathering, refining, crafting, farming, cooking e alchemy. Consultar documentação e referências comunitárias atuais durante a auditoria; usar apenas como referência funcional para identificar ciclos e tipos de progressão. O projeto MMO Multiagente permanece independente.