# Fase B — Ledger econômico parcial

## Implementado

- Modelo de evento tipado em `EconomyLedger.kt`.
- Cada evento exige agente, tick não negativo, item/identificador, quantidade positiva e causa.
- O coordenador de mercado registra a transação no ledger para comprador e vendedor.
- A tela Eventos mostra a quantidade de liquidações e informa o escopo limitado do ledger.
- Testes cobrem validação de eventos e registro de liquidações.

## Limitações conhecidas

- O ledger existe somente em memória durante a sessão; ainda não é persistido nem reconstruído a partir dos perfis.
- As rotinas de coleta, caça, pesca, refino, fabricação e tutorial ainda não estão todas ligadas ao ledger.
- Não existe um estoque natural compartilhado finito; por isso ainda há caminhos em que a produção pode gerar itens diretamente.
- A moeda inicial e algumas recompensas ainda não têm uma contabilidade global completa.
- A UI não deve apresentar o ledger atual como balanço econômico total.

## Próximos critérios de aceite

1. Cada mutação de inventário ou saldo deve emitir evento causal.
2. Transferências de comércio devem gerar eventos pareados sem contar transferência como criação de moeda.
3. Estoques do mundo devem ser finitos, compartilhados e atualizados de forma determinística.
4. O ledger deve sobreviver a reinício e permitir conferir saldo/quantidades.
5. Testes devem verificar conservação de itens e moeda em compra, fabricação, refino, recompensa, reparo e falha de ação.
