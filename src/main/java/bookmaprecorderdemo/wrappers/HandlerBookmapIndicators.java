package bookmaprecorderdemo.wrappers;

import java.awt.Color;
import java.util.HashMap;
import java.util.Random;

import bookmaprecorderdemo.recorders.RecorderBMF;
import bookmaprecorderdemo.utils.IndicatorsPack;

public class HandlerBookmapIndicators extends HandlerBookmapSimple {

    private double probability = 0.2;

    private final int numIntrinsicIndicators = 10;
    private final double[] intrinsicParams = generateParams(numIntrinsicIndicators, 4, 1.4);

    private final int numVolumeEmaIndicators = 10;
    private final double[] emaParams = generateParams(numVolumeEmaIndicators, 1e9, 2);

    private Random rand = new Random();
    private HashMap<Integer, IndicatorsPack> datas = new HashMap<>();

    public HandlerBookmapIndicators(RecorderBMF recorder, String fin) throws Exception {
        super(recorder, fin);
    }

    @Override
    protected void onDepth(long t, int id, boolean isBuy, long price, long size) throws Exception {
        super.onDepth(t, id, isBuy, price, size);
        recorder.onTextData(t, instruments.get(id).alias, "Raw depth", isBuy, price, size, "Depth update " + price + "=" + size);
        datas.get(id).onDepth(isBuy, price, size);
        onEvent(t);
    }

    @Override
    protected void onTrade(long t, int id, boolean isBuy, double price, long size) throws Exception {
        super.onTrade(t, id, isBuy, price, size);
        datas.get(id).onTrade(t, isBuy, size);
        onEvent(t);
    }

    @Override
    protected void onInstrument(long t, int id, String alias, double pips, double multiplier) throws Exception {
        super.onInstrument(t, id, alias, pips, multiplier);
        initIndicators(t, id);
    }

    private int getFirstIndicatorId(int instrId) {
        return instrId * 2 * (numIntrinsicIndicators + numVolumeEmaIndicators);
    }

    private void initIndicators(long t, int id) throws Exception {
        datas.put(id, new IndicatorsPack(intrinsicParams, emaParams));
        int currentIndicatorId = getFirstIndicatorId(id);
        String alias = instruments.get(id).alias;
        for (int i = 0; i < numIntrinsicIndicators; i++) {
            recorder.onIndicatorDefinition(
                    t, currentIndicatorId, alias, "Intrinsic bid #" + i,
                    (short) 0xFFFF, (short) 1, 1, Color.WHITE,
                    (short) 0xFF08, (short) 1, 1, null, 0, 0, true, null);
            currentIndicatorId++;
        }
        for (int i = 0; i < numIntrinsicIndicators; i++) {
            recorder.onIndicatorDefinition(
                    t, currentIndicatorId, alias, "Intrinsic ask #" + i,
                    (short) 0xFFFF, (short) 1, 1, Color.WHITE,
                    (short) 0xFF08, (short) 1, 1, null, 0, 0, true, null);
            currentIndicatorId++;
        }
        for (int i = 0; i < numVolumeEmaIndicators; i++) {
            recorder.onIndicatorDefinition(
                    t, currentIndicatorId, alias, "Volume EMA bid #" + i,
                    (short) 0xFFFF, (short) 1, 1,
                    new Color(46, 204, 113), (short) 0xFF08, (short) 1, 1, null, 0, 0, false, "%6.3e");
            currentIndicatorId++;
        }
        for (int i = 0; i < numVolumeEmaIndicators; i++) {
            recorder.onIndicatorDefinition(
                    t, currentIndicatorId, alias, "Volume EMA ask #" + i, 
                    (short) 0xFFFF, (short) 1, 1,
                    new Color(213, 76, 60), (short) 0xFF08, (short) 1, 1, null, 0, 0, false, "%6.3e");
            currentIndicatorId++;
        }
    }

    private void onEvent(long t) throws Exception {
        if (rand.nextDouble() < probability) {
            int id = (int) datas.keySet().toArray()[rand.nextInt(instruments.size())];
            IndicatorsPack pack = datas.get(id);
            int firstIndicatorId = getFirstIndicatorId(id);
            if (rand.nextBoolean()) {
                boolean isBid = rand.nextBoolean();
                int idx = rand.nextInt(numIntrinsicIndicators);
                double intrinsic = instruments.get(id).pips * pack.getIntrinsic(isBid, idx);
                int indicatorId = firstIndicatorId + (isBid ? 0 : numIntrinsicIndicators) + idx;
                recorder.onIndicatorPoint(t, indicatorId, intrinsic);
                recorder.onTextData(t, instruments.get(id).alias, "Indicator " + idx, isBid, intrinsic, Double.NaN, "Indicator " + indicatorId + " updated to " + intrinsic);
            } else {
                int idx = rand.nextInt(numVolumeEmaIndicators);
                boolean isBuy = rand.nextBoolean();
                double ema = pack.getEma(t, isBuy, idx);
                int indicatorId = firstIndicatorId + 2 * numIntrinsicIndicators + (isBuy ? 0 : numVolumeEmaIndicators)
                        + idx;
                recorder.onIndicatorPoint(t, indicatorId, ema);
                recorder.onTextData(t, instruments.get(id).alias, "Indicator " + idx, isBuy, ema, Double.NaN, "Indicator " + indicatorId + " updated to " + ema);
            }
        }
    }

    private static double[] generateParams(int len, double first, double factor) {
        double[] params = new double[len];
        for (int i = 0; i < len; i++) {
            params[i] = first * Math.pow(factor, i);
        }
        return params;
    }
}
