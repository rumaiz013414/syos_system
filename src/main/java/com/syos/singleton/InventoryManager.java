package com.syos.singleton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.syos.model.StockBatch;
import com.syos.model.ShelfStock;
import com.syos.observer.StockObserver;
import com.syos.repository.ShelfStockRepository;
import com.syos.repository.StockBatchRepository;
import com.syos.repository.ProductRepository;
import com.syos.strategy.ShelfStrategy;

public class InventoryManager {
	private static InventoryManager instance;

	private final StockBatchRepository batchRepository;
	private final ShelfStockRepository shelfRepository;
	private final ShelfStrategy strategy;
	private final List<StockObserver> observers = new ArrayList<>();

	public InventoryManager(ShelfStrategy strategy, StockBatchRepository batchRepository,
			ShelfStockRepository shelfRepository, ProductRepository productRepository) {
		this.strategy = strategy;
		this.batchRepository = batchRepository;
		this.shelfRepository = shelfRepository;
	}

	public static synchronized InventoryManager getInstance(ShelfStrategy strat) {
		if (instance == null) {
			ProductRepository productRepo = new ProductRepository();
			instance = new InventoryManager(strat, new StockBatchRepository(), new ShelfStockRepository(productRepo),
					productRepo);
		}
		return instance;
	}

	public static synchronized void resetInstance() {
		instance = null;
	}

	public void addObserver(StockObserver obs) {
		observers.add(obs);
	}

	protected void notifyLow(String code, int remaining) {
		for (var o : observers) {
			o.onStockLow(code, remaining);
		}
	}

	public void receiveStock(String productCode, LocalDate purchaseDate, LocalDate expiryDate, int quantity) {
		if (productCode == null || productCode.trim().isEmpty()) {
			throw new IllegalArgumentException("Product code cannot be empty.");
		}
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be positive.");
		}
		if (purchaseDate == null || expiryDate == null) {
			throw new IllegalArgumentException("Purchase date and expiry date cannot be null.");
		}
		if (expiryDate.isBefore(purchaseDate)) {
			throw new IllegalArgumentException("Expiry date cannot be before purchase date.");
		}

		batchRepository.createBatch(productCode, purchaseDate, expiryDate, quantity);
		System.out.printf("Received batch: %s qty=%d exp=%s%n", productCode, quantity, expiryDate);
	}

	public void moveToShelf(String productCode, int qtyToMove) {
		if (productCode == null || productCode.trim().isEmpty()) {
			throw new IllegalArgumentException("Product code cannot be empty.");
		}
		if (qtyToMove <= 0) {
			throw new IllegalArgumentException("Quantity to move must be positive.");
		}

		int remainingToMove = qtyToMove;

		List<StockBatch> backStoreBatches = batchRepository.findByProduct(productCode);

		if (backStoreBatches == null || backStoreBatches.isEmpty()) {
			throw new IllegalArgumentException("No stock batches found in back-store for product: " + productCode);
		}

		int totalAvailableInBackStore = backStoreBatches.stream().mapToInt(StockBatch::getQuantityRemaining).sum();
		if (totalAvailableInBackStore < qtyToMove) {
			throw new IllegalArgumentException(
					String.format("Insufficient stock in back-store for %s. Available: %d, Requested: %d.", productCode,
							totalAvailableInBackStore, qtyToMove));
		}

		while (remainingToMove > 0 && !backStoreBatches.isEmpty()) {
			StockBatch chosenBackStoreBatch = strategy.selectBatchFromBackStore(backStoreBatches);

			if (chosenBackStoreBatch == null) {
				throw new IllegalStateException(
						"Shelf strategy returned null batch unexpectedly during move from back-store.");
			}

			int availableInBackStoreBatch = chosenBackStoreBatch.getQuantityRemaining();
			int usedFromBackStoreBatch = Math.min(availableInBackStoreBatch, remainingToMove);

			chosenBackStoreBatch.setQuantityRemaining(availableInBackStoreBatch - usedFromBackStoreBatch);
			batchRepository.updateQuantity(chosenBackStoreBatch.getId(), chosenBackStoreBatch.getQuantityRemaining());

			shelfRepository.upsertBatchQuantityOnShelf(productCode, chosenBackStoreBatch.getId(),
					usedFromBackStoreBatch, chosenBackStoreBatch.getExpiryDate());
			System.out.printf("Moved %d units from back-store batch %d to shelf for %s.%n", usedFromBackStoreBatch,
					chosenBackStoreBatch.getId(), productCode);

			remainingToMove -= usedFromBackStoreBatch;

			if (chosenBackStoreBatch.getQuantityRemaining() == 0) {
				backStoreBatches.remove(chosenBackStoreBatch);
			}
		}
		System.out.printf("Successfully moved %d units of %s to shelf.%n", qtyToMove, productCode);
	}

	public void deductFromShelf(String productCode, int quantity) {
		if (productCode == null || productCode.trim().isEmpty()) {
			throw new IllegalArgumentException("Product code cannot be empty.");
		}
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity to deduct must be positive.");
		}

		int currentShelfQuantity = shelfRepository.getQuantity(productCode);
		if (currentShelfQuantity < quantity) {
			throw new IllegalArgumentException(
					String.format("Insufficient stock on shelf for %s. Available: %d, Requested: %d.", productCode,
							currentShelfQuantity, quantity));
		}

		int remainingToDeduct = quantity;
		List<ShelfStock> shelfBatches = shelfRepository.getBatchesOnShelf(productCode);

		while (remainingToDeduct > 0 && !shelfBatches.isEmpty()) {
			ShelfStock chosenShelfBatch = strategy.selectBatchFromShelf(shelfBatches);

			if (chosenShelfBatch == null) {
				throw new IllegalStateException("Shelf strategy returned null batch unexpectedly during deduction.");
			}

			int availableInShelfBatch = chosenShelfBatch.getQuantity();
			int usedFromShelfBatch = Math.min(availableInShelfBatch, remainingToDeduct);

			shelfRepository.deductQuantityFromBatchOnShelf(productCode, chosenShelfBatch.getBatchId(),
					usedFromShelfBatch);
			System.out.printf("Deducted %d units from shelf batch %d for %s.%n", usedFromShelfBatch,
					chosenShelfBatch.getBatchId(), productCode);

			chosenShelfBatch.setQuantity(availableInShelfBatch - usedFromShelfBatch);

			remainingToDeduct -= usedFromShelfBatch;

			if (chosenShelfBatch.getQuantity() == 0) {
				shelfBatches.remove(chosenShelfBatch);
				shelfRepository.removeBatchFromShelf(productCode, chosenShelfBatch.getBatchId());
			}
		}

		int remain = shelfRepository.getQuantity(productCode);
		System.out.printf("Total deducted %d units of %s from shelf. Remaining on shelf: %d.%n", quantity, productCode,
				remain);

		if (remain < 50) {
			notifyLow(productCode, remain);
		}
	}

	public void removeEntireBatch(int batchId) {
		StockBatch backStoreBatch = batchRepository.findById(batchId);
		if (backStoreBatch == null) {
			throw new IllegalArgumentException("Batch with ID " + batchId + " not found in back-store records.");
		}

		String productCode = backStoreBatch.getProductCode();
		int quantityInBackStoreBatch = backStoreBatch.getQuantityRemaining();

		List<ShelfStock> batchesOnShelf = shelfRepository.getBatchesOnShelf(productCode);
		ShelfStock shelfBatchToRemove = null;
		for (ShelfStock ss : batchesOnShelf) {
			if (ss.getBatchId() == batchId) {
				shelfBatchToRemove = ss;
				break;
			}
		}

		if (shelfBatchToRemove != null) {
			int quantityOnShelfForBatch = shelfBatchToRemove.getQuantity();
			shelfRepository.removeBatchFromShelf(productCode, batchId);
			System.out.printf("Removed %d units of Batch ID %d (%s) from shelf.%n", quantityOnShelfForBatch, batchId,
					productCode);
		} else {
			System.out.printf("Batch ID %d (%s) was not found on the shelf, only in back-store records.%n", batchId,
					productCode);
		}

		if (quantityInBackStoreBatch > 0) {
			batchRepository.setBatchQuantityToZero(batchId);
			System.out.printf("Set remaining quantity of Batch ID %d (%s) to 0 in back-store (was %d).%n", batchId,
					productCode, quantityInBackStoreBatch);
		} else {
			System.out.printf(
					"Batch ID %d (%s) already has 0 quantity in back-store. No change made to back-store record.%n",
					batchId, productCode);
		}

		System.out.printf("Operation completed for Batch ID %d (%s).%n", batchId, productCode);

		int remainOnShelf = shelfRepository.getQuantity(productCode);
		if (remainOnShelf < 50) {
			notifyLow(productCode, remainOnShelf);
		}
	}

	public int getQuantityOnShelf(String productCode) {
		if (productCode == null || productCode.trim().isEmpty()) {
			throw new IllegalArgumentException("Product code cannot be empty.");
		}
		return shelfRepository.getQuantity(productCode);
	}

	public List<ShelfStock> getBatchesOnShelfForProduct(String productCode) {
		return shelfRepository.getBatchesOnShelf(productCode);
	}

	public List<StockBatch> getBatchesForProduct(String productCode) {
		return batchRepository.findByProductAllBatches(productCode);
	}

	public List<String> getAllProductCodes() {
		List<String> codes = new ArrayList<>();
		codes.addAll(shelfRepository.getAllProductCodes());
		codes.addAll(batchRepository.getAllProductCodesWithBatches());
		return codes.stream().distinct().collect(Collectors.toList());
	}

	public List<String> getAllProductCodesWithExpiringBatches(int daysThreshold) {
		List<StockBatch> allExpiringBatches = batchRepository.findAllExpiringBatches(daysThreshold);
		return allExpiringBatches.stream().map(StockBatch::getProductCode).distinct().collect(Collectors.toList());
	}

	public List<StockBatch> getExpiringBatchesForProduct(String productCode, int daysThreshold) {
		return batchRepository.findExpiringBatches(productCode, daysThreshold);
	}

	public void removeQuantityFromShelf(String productCode, int quantity) {
		deductFromShelf(productCode, quantity);
	}

	public List<StockBatch> getAllExpiringBatches(int daysThreshold) {
		return batchRepository.findAllExpiringBatches(daysThreshold);
	}

	public int getAvailableStock(String productCode) {
		int backStoreQty = batchRepository.findByProduct(productCode).stream()
				.mapToInt(StockBatch::getQuantityRemaining).sum();
		int shelfQty = shelfRepository.getQuantity(productCode);
		return backStoreQty + shelfQty;
	}

	public void discardBatchQuantity(int batchId, int quantityToDiscard) {
		StockBatch batch = batchRepository.findById(batchId);
		if (batch == null) {
			throw new IllegalArgumentException("Batch with ID " + batchId + " not found.");
		}
		if (quantityToDiscard <= 0) {
			throw new IllegalArgumentException("Quantity to discard must be positive.");
		}
		if (batch.getQuantityRemaining() < quantityToDiscard) {
			throw new IllegalArgumentException(
					String.format("Cannot discard %d units from batch %d. Only %d remaining.", quantityToDiscard,
							batchId, batch.getQuantityRemaining()));
		}

		int newQuantity = batch.getQuantityRemaining() - quantityToDiscard;
		batchRepository.updateQuantity(batchId, newQuantity);
		System.out.printf("Discarded %d units from batch ID %d. Remaining quantity: %d.%n", quantityToDiscard, batchId,
				newQuantity);
	}
}