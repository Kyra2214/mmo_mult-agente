package com.kyra2214.mmomultiagente

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldResourceStoreTest {
    @Test fun harvestConsumesFiniteStockAndRejectsOversizedRequestAtomically() {
        val store = WorldResourceStore(mapOf("Madeira T1" to 3))

        assertTrue(store.harvest("Madeira T1", 2))
        assertEquals(1, store.available("Madeira T1"))
        assertFalse(store.harvest("Madeira T1", 2))
        assertEquals(1, store.available("Madeira T1"))
        assertTrue(store.harvest("Madeira T1", 1))
        assertEquals(0, store.available("Madeira T1"))
    }

    @Test fun encodedStocksCanBeRestoredWithoutRefillingDepletedNodes() {
        val original = WorldResourceStore(mapOf("Minério T1" to 8, "Peixe T1" to 4))
        assertTrue(original.harvest("Peixe T1", 2))

        val restored = WorldResourceStore.decode(original.encode())

        assertEquals(8, restored.available("Minério T1"))
        assertEquals(2, restored.available("Peixe T1"))
    }

    @Test fun missingStockIsNotCreatedByHarvest() {
        val store = WorldResourceStore(emptyMap())

        assertFalse(store.harvest("Fragmento Arcano T1", 1))
        assertEquals(0, store.available("Fragmento Arcano T1"))
    }
}
