package bookmaprecorderdemo.utils;

public class IndicatorsPack {

	private final IntrinsicPrice intrinsicPrice = new IntrinsicPrice();
	private final DynamicAverage avgSize = new DynamicAverage();
	private final Ema[] emaBuy;
	private final Ema[] emaSell;

	public IndicatorsPack(int numVolumeEma, long minEmaHalfLifeNanoseconds, double stepMultEmaHalfLife) {
		emaBuy = new Ema[numVolumeEma];
		emaSell = new Ema[numVolumeEma];
		long halfLife = minEmaHalfLifeNanoseconds;
		for (int i = 0; i < numVolumeEma; i++) {
			emaBuy[i] = new Ema(halfLife);
			emaSell[i] = new Ema(halfLife);
			halfLife = Math.round(halfLife * stepMultEmaHalfLife);
		}
	}

	public void onDepth(boolean isBuy, long price, long size) {
		intrinsicPrice.onDepth(isBuy, price, size);
		avgSize.update(size);
	}

	public void onTrade(long t, boolean isBuy, long size) {
		Ema[] ema = isBuy ? emaBuy : emaSell;
		for (Ema e : ema) {
			e.onUpdate(t, size);
		}
	}

	public double getIntrinsic(boolean isBid, double mult) {
		long hypotheticalMarketOrderSize = Math.round(mult * avgSize.getAverage());
		double intrinsic = intrinsicPrice.calcIntrinsic(isBid, hypotheticalMarketOrderSize);
		return intrinsic;
	}

	public double getEma(long t, boolean isBuy, int index) {
		double ema = (isBuy ? emaBuy : emaSell)[index].getValue(t);
		return ema;
	}
}
