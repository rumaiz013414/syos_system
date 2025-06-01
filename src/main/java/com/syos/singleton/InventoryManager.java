package com.syos.singleton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.syos.model.StockBatch;
import com.syos.observer.StockObserver;
import com.syos.repository.ShelfStockRepository;
import com.syos.repository.StockBatchRepository;
import com.syos.strategy.ShelfStrategy;

//singleton that handles both Moving stock from back‐store to shelf Deducting shelf stock on purchase (and alerting observers)

public class InventoryManager {
	private static InventoryManager instance;

	private final StockBatchRepository batchRepository = new StockBatchRepository();
	private final ShelfStockRepository shelfRepository = new ShelfStockRepository();
	private final ShelfStrategy strategy;
	private final List<StockObserver> observers = new ArrayList<>();

	private InventoryManager(ShelfStrategy strategy) {
		this.strategy = strategy;
	}

	public static synchronized InventoryManager getInstance(ShelfStrategy strat) {
		if (instance == null) {
			instance = new InventoryManager(strat);
		}
		return instance;
	}

	public void addObserver(StockObserver obs) {
		observers.add(obs);
	}

	private void notifyLow(String code, int remaining) {
		for (var o : observers)
			o.onStockLow(code, remaining);
	}

	public void receiveStock(String productCode, LocalDate purchaseDate, LocalDate expiryDate, int quantity) {
		batchRepository.createBatch(productCode, purchaseDate, expiryDate, quantity);
		System.out.printf("Received batch: %s qty=%d exp=%s%n", productCode, quantity, expiryDate);
	}

	// move up to qtyToMove from back‐store to shelf

	public void moveToShelf(String productCode, int qtyToMove) {
		int remainingToMove = qtyToMove;

		// Keep grabbing single batches via the strategy until we run out
		List<StockBatch> batches = batchRepository.findByProduct(productCode);

		while (remainingToMove > 0 && !batches.isEmpty()) {
			// Let the strategy pick exactly one batch from the current list
			StockBatch chosenBatch = strategy.selectBatch(batches);
			if (chosenBatch == null) {
				break;
			}

			int available = chosenBatch.getQuantityRemaining();
			int used = Math.min(available, remainingToMove);

			// Update that batch’s remaining quantity in the DB
			chosenBatch.setQuantityRemaining(available - used);
			batchRepository.updateQuantity(chosenBatch.getId(), chosenBatch.getQuantityRemaining());

			// Upsert onto shelf
			shelfRepository.upsertQuantity(productCode, used);

			remainingToMove -= used;

			// If this batch is now fully consumed, remove it from the local list
			if (chosenBatch.getQuantityRemaining() == 0) {
				batches.remove(chosenBatch);
			}
			// Otherwise, update our `batches` list so next iteration re‐evaluates it.
		}

		System.out.printf("Moved %d units of %s to shelf%n", qtyToMove - remainingToMove, productCode);
	}

	// deduct purchased items from shelf and alert if low

	public void deductFromShelf(String productCode, int qty) {
		shelfRepository.deductQuantity(productCode, qty);
		int remain = shelfRepository.getQuantity(productCode);
		if (remain < 50) {
			notifyLow(productCode, remain);
		}
	}
	
	public int getQuantityOnShelf(String productCode) {
	    return shelfRepository.getQuantity(productCode);
	}
}
