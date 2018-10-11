package bookmaprecorderdemo.utils;

import java.util.Map;
import java.util.Map.Entry;

public class IntrinsicPrice {
	private final OrderBookMBP orderBook = new OrderBookMBP();

	public void onDepth(boolean isBuy, long price, long size) {
		orderBook.onDepth(isBuy, price, size);
	}

	public double calcIntrinsic(boolean isBid, long hmos) {
		Map<Long, Long> book = isBid ? orderBook.bids : orderBook.asks;
		double executionPrice = 0;
		long size = hmos;
		for (Entry<Long, Long> entry : book.entrySet()) {
			if (size == 0) {
				break;
			}
			long matchedSize = Math.min(entry.getValue(), size);
			size -= matchedSize;
			executionPrice += entry.getKey() * matchedSize;
		}
		return size == 0 ? executionPrice / hmos : Double.NaN;
	}
}
