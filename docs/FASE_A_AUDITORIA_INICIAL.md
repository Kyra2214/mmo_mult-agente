# Fase A — Auditoria inicial do código e contratos

**Branch:** `feature/emergent-economy-multi-agent-ui`  
**Estado:** auditoria inicial registrada; Fase A ainda não concluída.  
**Base consultada:** `main` no início da implementação.

## 1. Fluxo atual observado

- `MainActivity` era responsável por criar/carregar um único `SimulatorEngine`, avançar um tick a cada dois segundos e mostrar o estado e o log desse agente.
- `SimulatorEngine.tick()` executa o tutorial até `TOTAL_STAGES`; depois chama a política heurística `AutonomousDecisionPolicy.choose(...)` e executa uma ação operacional.
- A progressão está separada por `SkillId`, com requisitos de tier e receitas em `TierRecipes`.
- O estado do agente é serializado em `SharedPreferences`; o log tem limite de 120 entradas.
- O mercado legado é escalar: `marketListing`, `marketPrice` e `marketListingAgeTicks` representam uma única oferta por motor.
- `resolveMarketBuyer()` credita o saldo após cinco ticks elegíveis, sem consultar um comprador agente com orçamento, preferência ou decisão própria. Isso é um comprador sistêmico simplificado, não comércio emergente entre personagens.
- As rotinas de coleta/caça/pesca/refino/fabricação precisam ser auditadas individualmente para rastrear cada entrada e saída de item. A simples presença de uma mensagem de log não substitui um evento econômico estruturado.

## 2. Riscos e lacunas confirmados

1. **Mercado não compartilhado:** uma oferta pertence ao motor individual e não pode ser encontrada/aceita por outro agente.
2. **Liquidação simplificada:** o comprador periódico não representa saldo e decisão de um agente independente.
3. **Ausência de livro-caixa estruturado:** mensagens textuais são o histórico principal; falta um contrato de evento idempotente para entradas/saídas de itens e dinheiro.
4. **Recursos por validar:** as rotinas operacionais devem ser revisadas para garantir estoque finito por região e causalidade em toda criação de item.
5. **Decisão heurística:** a política atual seleciona ações operacionais, mas ainda não compara planos compostos como coletar → refinar → fabricar → vender.
6. **Persistência por agente:** o protótipo novo cria preferências separadas para cada perfil; a migração e a versão do estado ainda precisam ser testadas explicitamente.

## 3. Contratos a adotar nas fases seguintes

- `EconomicEvent`: identificador único/idempotente, tick, agente, causa, item/moeda, quantidade, origem e destino.
- `MarketOffer`: identificador, proprietário, item, quantidade, preço unitário, estado, expiração e reserva de estoque.
- `AgentPlan`: objetivo, etapas, orçamento, premissas e condição de reavaliação.
- `DecisionRecord`: alternativas viáveis, pontuação estimada, escolha, razão e resultado observado.
- A liquidação de mercado deve validar saldo e estoque e transferir item e dinheiro atomicamente.
- Estado econômico compartilhado deve pertencer ao mundo/servidor; inventário, habilidades, preferências e memória permanecem individuais.
- A interface deve apenas observar os modelos do servidor; não deve ser a fonte de verdade da economia.

## 4. Interface de observabilidade iniciada

A branch adiciona a primeira camada de observação com cinco perfis persistidos separadamente (Aldren, Mira, Borin, Kael e Sera) e páginas de Servidor, Eventos, Agentes e Detalhe do agente. O detalhe expõe estado, saldo, inventário, equipamento, oferta e log individual.

**Limite atual:** os cinco motores ainda são instâncias isoladas do simulador legado. A UI não deve afirmar que há comércio emergente entre eles. O próximo passo de domínio é implementar economia rastreável e depois mercado compartilhado multi-oferta.

## 5. Testes existentes identificados

- `AutonomousDecisionPolicyTest`
- `AutonomousDungeonIntegrationTest`
- `MarketSettlementTest`
- `ProgressionRulesTest`
- `SimulatorEngineTest`
- `T3DungeonSimulatorTest`

Esses testes são a linha de base. A implementação multiagente precisa adicionar testes para isolamento de inventários, persistência por perfil, eventos causais, ofertas concorrentes e liquidação atômica.

## 6. Critério para concluir a Fase A

A Fase A só estará concluída depois de validar este mapa contra todas as rotinas que alteram inventário/saldo, definir contratos de migração e acrescentar testes de base que reproduzam os riscos descritos. Este documento registra a auditoria inicial, não declara o aceite completo.
