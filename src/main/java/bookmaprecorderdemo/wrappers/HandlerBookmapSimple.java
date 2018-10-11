package bookmaprecorderdemo.wrappers;

import java.text.SimpleDateFormat;
import java.util.HashMap;

import bookmaprecorderdemo.recorders.RecorderBMF;
import velox.recorder.InstrumentDefinition;
import velox.recorder.TagsExt;

public class HandlerBookmapSimple extends HandlerBase {

	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS");
	private boolean initialized = false;
	protected final HashMap<Integer, InstrumentDefinition> instruments = new HashMap<>();

	public HandlerBookmapSimple(RecorderBMF recorder, String fin) throws Exception {
		super(recorder, fin);
	}

	private long getNanoseconds(String s) throws Exception {
		String strMillis = s.substring(0, 21);
		String strNanos = s.substring(21);
		long t = 1_000_000L * sdf.parse(strMillis).getTime() + Long.parseLong(strNanos);
		return t;
	}

	@Override
	protected void processLine(String line) throws Exception {
		String[] s = line.split(",");
		long t = getNanoseconds(s[0]);
		int instrID = Integer.parseInt(s[1]);
		String eventType = s[2];
		if (!initialized) {
			recorder.init(t);
			initialized = true;
		}
		if (eventType.equals("Quote")) {
			onDepth(t, instrID, s[3].equals("Buy"), Long.parseLong(s[4]), Long.parseLong(s[5]));
		} else if (eventType.equals("BBO")) {
		} else if (eventType.equals("Trade")) {
			onTrade(t, instrID, s[3].equals("Buy"), Double.parseDouble(s[4]), Long.parseLong(s[5]));
		} else if (eventType.equals("InstrumentAdded")) {
			onInstrument(t, instrID, s[3].split("=")[1], Double.parseDouble(s[4].split("=")[1]),
					Double.parseDouble(s[5].split("=")[1]));
		} else if (eventType.equals("InstrumentRemoved")) {
		} else {
			throw new Exception("HandlerBookmapSimple: unrecognized event type: " + eventType);
		}
	}

	protected void onDepth(long t, int id, boolean isBuy, long price, long size) throws Exception {
		recorder.onDepth(t, id, isBuy, instruments.get(id).pips * price, (int)size);
	}

	protected void onTrade(long t, int id, boolean isBuy, double price, long size) throws Exception {
		int aggressor = isBuy ? TagsExt.BUY_SIDE : TagsExt.SELL_SIDE;
		recorder.onTrade(t, id, instruments.get(id).pips * price, (int)size, aggressor, TagsExt.OTC_TYPE_NONE);
	}

	protected void onInstrument(long t, int id, String alias, double pips, double multiplier) throws Exception {
		InstrumentDefinition instrDef = new InstrumentDefinition();
		instrDef.alias = alias;
		instrDef.id = id;
		instrDef.pips = pips;
		instrDef.multiplier = multiplier;
		instrDef.type = alias;
		instruments.put(id, instrDef);
		recorder.onInstrumentDefinition(t, instrDef);
	}
}
