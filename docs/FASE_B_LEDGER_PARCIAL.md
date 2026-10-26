# Fase B — Ledger econômico parcial

## Implementado

- Modelo de evento tipado em `EconomyLedger.kt`.
- Cada evento exige agente, tick não negativo, item/identificador, quantidade positiva e causa.
- Eventos distintos para moeda inicial de referência (`CURRENCY_INITIALIZED`), emissão (`CURRENCY_MINTED`) e remoção (`CURRENCY_BURNED`), para não confundir saldo de abertura com dinheiro criado durante a simulação.
- `ITEM_TRANSFERRED` registra movimentações internas para baú/equipamento e a reserva/liberação de itens no escrow do mercado sem contá-las como produção ou consumo de recurso.
- `CURRENCY_TRANSFERRED` registra o pagamento entre comprador e vendedor sem classificá-lo como emissão ou queima; a liquidação do mercado registra pagamento, transferência do item e os dois lados da transação.
- Reparos, teleporte, fabricação da vara de pesca, loot da masmorra e reserva/liquidação de itens de mercado agora emitem eventos econômicos.
- O coordenador de mercado registra a transação no ledger para comprador e vendedor.
- A tela Eventos mostra a quantidade de liquidações e informa o escopo limitado do ledger.
- Testes cobrem validação de eventos e registro de liquidações.

## Limitações conhecidas

- O ledger agora é serializado em `SharedPreferences` do servidor e recuperado na abertura do app. Linhas inválidas são ignoradas durante a recuperação; ainda não existe reconciliação/replay com os saldos e inventários dos agentes.
- A instrumentação ainda precisa de auditoria por mutação para garantir que todo caminho esteja coberto, incluindo mudanças de equipamento e estados salvos legados.
- Não existe um estoque natural compartilhado finito; por isso ainda há caminhos em que a produção pode gerar itens diretamente.
- Os saldos iniciais de perfis existentes recebem um evento de referência ao iniciar o ledger; isso não é emissão monetária. Ainda falta uma reconciliação integral entre todos os saldos/inventários persistidos e o histórico.
- A UI não deve apresentar o ledger atual como balanço econômico total.

## Próximos critérios de aceite

1. Cada mutação de inventário ou saldo deve emitir evento causal.
2. Transferências de comércio devem gerar eventos pareados sem contar transferência como criação de moeda.
3. Estoques do mundo devem ser finitos, compartilhados e atualizados de forma determinística.
4. **Parcialmente atendido:** o histórico sobrevive a reinício; ainda falta reconciliar o histórico com saldos/quantidades dos agentes e testar reinicialização do app no dispositivo.
5. Testes devem verificar conservação de itens e moeda em compra, fabricação, refino, recompensa, reparo e falha de ação.


## Additional economic mutation coverage

- Teleporte and equipment repair record currency removal.
- Finite gathering/hunting/fishing nodes now emit separate stock-consumption events before inventory creation; the legacy periodic market buyer also records its currency and escrow settlement, and equipment destroyed after three unrepaired defeats is recorded as consumed.
- Fishing-rod recipes record consumed materials and the crafted item.
- Dungeon loot records item creation; moving items between inventory, chest, and equipment is tracked as an internal transfer rather than creation/consumption.
- Existing profiles get a one-time opening-balance baseline event, distinct from currency minting.

These additions improve coverage but do not yet prove that every mutation path is covered; test and reconciliation work remains required.

## Finite shared resource nodes added

- `WorldResourceStore` holds finite shared stocks for T1-T3 gathering resources, creatures, and T1 fish.
- Gathering, hunting, and fishing consume from this shared store before adding resources to an agent inventory.
- A failed harvest does not partially reduce stock; depleted sources do not award items or hunting currency.
- The UI persists the shared stock snapshot separately and displays remaining units and depleted source types.
- Tests cover atomic harvest, exhausted sources, and restoring stock counts after serialization.

This is still not a full closed economy: scripted tutorial starter materials/rewards are explicitly recorded as such, and some recipes and currency changes still need full coverage. Resource regeneration, location-specific nodes, and ledger reconciliation with agent state remains future work.
