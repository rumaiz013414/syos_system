package com.syos.singleton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.syos.model.StockBatch;
import com.syos.observer.StockObserver;
import com.syos.repository.ShelfStockRepository;
import com.syos.repository.StockBatchRepository;
import com.syos.strategy.ShelfStrategy;
import com.syos.util.StockBatchIterator;

//Singleton that handles both Moving stock from back‐store to shelf Deducting shelf stock on purchase (and alerting observers)

public class InventoryManager {
	private static InventoryManager instance;

	private final StockBatchRepository batchRepo = new StockBatchRepository();
	private final ShelfStockRepository shelfRepo = new ShelfStockRepository();
	private final List<StockObserver> observers = new ArrayList<>();
	private final ShelfStrategy strategy;

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
		batchRepo.createBatch(productCode, purchaseDate, expiryDate, quantity);
		System.out.printf("Received batch: %s qty=%d exp=%s%n", productCode, quantity, expiryDate);
	}

	/**
	 * Move up to qtyToMove from back‐store to shelf.
	 */
	public void moveToShelf(String productCode, int qtyToMove) {
		int remainingToMove = qtyToMove;
		List<StockBatch> batches = batchRepo.findByProduct(productCode);
		var it = new StockBatchIterator(batches, strategy.getComparator());

		while (remainingToMove > 0 && it.hasNext()) {
			StockBatch batch = it.next();
			int available = batch.getQuantityRemaining();
			int used = Math.min(available, remainingToMove);

			batch.setQuantityRemaining(available - used);
			batchRepo.updateQuantity(batch.getId(), batch.getQuantityRemaining());

			shelfRepo.upsertQuantity(productCode, used);
			remainingToMove -= used;
		}
		System.out.printf("Moved %d units of %s to shelf%n", qtyToMove - remainingToMove, productCode);
	}

	// move `qtyToMove from store‐batches to shelf, using the strategy.

//	public void moveToShelf(String productCode, int qtyToMove) {
//		int left = qtyToMove;
//		List<StockBatch> batches = batchRepo.findByProduct(productCode);
//
//		while (left > 0 && !batches.isEmpty()) {
//			StockBatch batch = strategy.selectBatch(batches);
//			if (batch == null)
//				break;
//
//			int used = Math.min(batch.getQuantityRemaining(), left);
//			batch.setQuantityRemaining(batch.getQuantityRemaining() - used);
//			batchRepo.updateQuantity(batch.getId(), batch.getQuantityRemaining());
//
//			shelfRepo.upsertQuantity(productCode, used);
//			left -= used;
//
//			// remove exhausted batch
//			batches.remove(batch);
//		}
//	}

	// deduct purchased items from shelf and alert if low

	public void deductFromShelf(String productCode, int qty) {
		shelfRepo.deductQuantity(productCode, qty);
		int remain = shelfRepo.getQuantity(productCode);
		if (remain < 50) {
			notifyLow(productCode, remain);
		}
	}
}
