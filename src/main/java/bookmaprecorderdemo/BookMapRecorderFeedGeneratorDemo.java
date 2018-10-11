package bookmaprecorderdemo;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import velox.recorder.DataRecorder;
import velox.recorder.DataRecorderConfiguration;
import velox.recorder.IRecorder;
import velox.recorder.InstrumentDefinition;
import velox.recorder.LicenseException;
import velox.recorder.TagsExt;

public class BookMapRecorderFeedGeneratorDemo {
    
    private static final HashMap<Integer, IndicatorsManager> indicatorManagers = new HashMap<>();
    
    public static void main(String[] args) throws LicenseException, IOException {
        IRecorder recorder = new DataRecorder();
        DataRecorderConfiguration config = new DataRecorderConfiguration();
        
        /* this map will contain information about instrument pips value
         * in pairs <instrument id> - <instrument pips>
         * we will need this values later when reading feed file to convert price level to actual price value for given instrument
         * we will add pairs to this map when reading "contract details" string from feed file
         */
        Map<Integer, Double> instrumentPipsMap = new HashMap<>(); 
        
        // here are the fields that you should configure
        // name of file you will write feed to
        config.file = new File("feed_bookmap_recorder_generator_demo.bmf");
        config.licenseKey = "<put your license key here>";
        config.licenseHash = "<put your hash here>";
        
        // name of feed file you will read feed from
        String feedFileName = "feed_S5_20160108_111329_027.txt";
        File sourceFeedFile = new File(feedFileName);
        
        
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFeedFile))) {
            // each line in feed file represent an event
            String line;
            
            String instrumentIdString;
            String priceString;
            String priceLevelString;
            String sizeString;
            int instrumentId;
            String orderIdString;
            int priceLevel;
            double price;
            int size;
            
            while ((line = reader.readLine()) != null) {
                //each line of feed file has number of comma separated tokens
                String token[] = line.split(",");
                // first token in every string is tag, represented by single character
                // tags will look like 'f' for feed source, 'c' for contract details, etc
                char tag = token[0].charAt(0);
                
                // second token in each line is event's Unix timestamp in nanoseconds
                long time = Long.parseLong(token[1]);
                
                //depending on tag, next tokens in string may differ
                switch (tag) {
                case 'f': // feed source, will be first line in feed file
                    recorder.init(time, config);
                    break;
                case 'c': // contract details
                    // format: <tag>,<time>,<instrument id>,<exchange>,<alias>,<type>,<pips>,<multiplier>,<depth>
                    instrumentIdString = token[2];
                    String exchangeString = token[3];
                    String aliasString = token[4];
                    String typeString = token[5];
                    String pipsString = token[6];
                    String multiplierString = token[7];
                    String depthString = token[8];
                    
                    // note that all values are String type, and we may need to convert some of them to primitive types
                    InstrumentDefinition instrumentDefinition = new InstrumentDefinition();
                    instrumentDefinition.id = Integer.parseInt(instrumentIdString);
                    instrumentDefinition.exchange = exchangeString;
                    instrumentDefinition.alias = aliasString;
                    instrumentDefinition.type = typeString;
                    instrumentDefinition.pips = Double.parseDouble(pipsString);
                    instrumentDefinition.multiplier = Double.parseDouble(multiplierString);
                    instrumentDefinition.depth = Integer.parseInt(depthString);
                    
                    recorder.onInstrumentDefinition(time, instrumentDefinition);
                    
                    // we need to add information about this instrument's pips value to our instrumentPips map
                    instrumentPipsMap.put(instrumentDefinition.id, instrumentDefinition.pips);
                    
                    // define custom indicators for this instrument
                    IndicatorsManager indicatorsManager = new IndicatorsManager(instrumentDefinition, recorder);
                    indicatorsManager.createIndicators(time);
                    indicatorManagers.put(instrumentDefinition.id, indicatorsManager);
                    break;
                case 'j': // information about multiple depth updates
                    // format: <tag>,<time>,<instrument id>,<is bid?>,<multiple pairs of: (<price level>,<size>)>
                    instrumentIdString = token[2];
                    String isBidString = token[3];
                    
                    instrumentId = Integer.parseInt(instrumentIdString);
                    boolean isBid = isBidString.equals("1");
                    
                    // we already know 4 tokens: <tag>,<time>,<instrument id>,<is bid?>
                    // all other tokens are <price level>,<size> pairs
                    // let's calculate number of pairs in this string
                    int offset = 4; 
                    int numberOfPairs = (token.length - offset) / 2;
                    
                    // now let's process all events
                    for (int i = 0; i < numberOfPairs; i++) {
                        // get <price level> and <size> values for this pair
                        priceLevelString = token[offset + 2 * i];
                        sizeString = token[offset + 2 * i + 1];
                        
                        priceLevel = Integer.parseInt(priceLevelString);
                        size = Integer.parseInt(sizeString);
                        
                        // note that we are give price level, and not actual price value
                        // if we have bid/ask with price 800 and pips value of 0.5 for this instrument, price level will be 1600
                        // actual price value equals <price level>*<this instrument pips>
                        // we get this instrument pips value from instrumentPipsMap, using instrumentId as a key
                        
                        // if we have no stored pips value for instrument with our id in instrumentPipsMap, we have bad feed file
                        assert (instrumentPipsMap.containsKey(instrumentId)):
                            "operation on undefiend instrument";
                        
                        double instrumentPips = instrumentPipsMap.get(instrumentId);
                        
                        // let's calculate actual price based on it's level and instrument pips
                        price = instrumentPips * priceLevel; 
                        
                        // let's record depth update now
                        recorder.onDepth(time, instrumentId, isBid, price, size);
                    }
                    break;
                case 'T': //trade event
                    // format: <tag>,<time>,<instrument id>,<price>,<size>,<aggressor>,<otc code>
                    
                    instrumentIdString = token[2];
                    priceString = token[3];
                    sizeString = token[4];
                    String aggressorString = token[5];
                    String otcCodeString = token[6];
                    
                    instrumentId = Integer.parseInt(instrumentIdString);
                    price = Double.parseDouble(priceString);
                    size = Integer.parseInt(sizeString);
                    int aggressor = Integer.parseInt(aggressorString);
                    int otcCode = Integer.parseInt(otcCodeString);

                    // aggressor can have only one of following values
                    assert (aggressor == TagsExt.BUY_SIDE || aggressor == TagsExt.SELL_SIDE || aggressor == TagsExt.UNKNOWN_SIDE):
                        "aggressor value is invalid";
                    
                    recorder.onTrade(time, instrumentId, price, size, aggressor, otcCode);
                    
                    // Update custom indicators
                    indicatorManagers.get(instrumentId).onTrade(time, price, size, aggressor);
                    break;
                case 'S': // create a new order
                    // format: <tag>,<time>,<order id>,<alias>,<is buy?>,<price>,<size>
                    
                    orderIdString = token[2];
                    aliasString = token[3];
                    String isBuyString = token[4];
                    priceString = token[5];
                    sizeString = token[6];
                    
                    boolean isBuy = isBuyString.equals("1");
                    price = Double.parseDouble(priceString);
                    size = Integer.parseInt(sizeString);
                    
                    recorder.recordNewOrder(time, orderIdString, aliasString, isBuy, price, size);
                    break;
                case 'U': // update order
                    // format: <tag>,<time>,<order id>,<price>,<size>
                    orderIdString = token[2];
                    priceString = token[3];
                    sizeString = token[4];
                    
                    price = Double.parseDouble(priceString);
                    size = Integer.parseInt(sizeString);
                    
                    recorder.recordUpdateOrder(time, orderIdString, price, size);
                    break;
                case 'C': //cancel order
                    // format: <tag>,<time>,<order id>
                    
                    orderIdString = token[2];
                    
                    recorder.recordCancelOrder(time, orderIdString);
                    break;
                case 'E': // execute order
                    // format: <tag>,<time>,<order id>,<price>,<size>
                    
                    orderIdString = token[2];
                    priceString = token[3];
                    sizeString = token[4];
                    
                    price = Double.parseDouble(priceString);
                    size = Integer.parseInt(sizeString);
                    
                    recorder.recordExecution(time, orderIdString, price, size);
                    break;
                case 'F': // mark order as filled
                    // format: <tag>,<time>,<order id>
                    
                    orderIdString = token[2];
                    
                    recorder.recordFilled(time, orderIdString);
                    break;
                default:
                    break;
                }
            }
        } finally {
            // Don't forget to call it after recording is finished - recorder has to
            // write internal buffers to the file
            if (recorder != null) {
                recorder.fini();
            }
        }
    }
}

class IndicatorsManager {
    // generator for indicator identifiers (should be unique within a file)
    private static int nextIndicatorId = 0;

    final InstrumentDefinition instrumentDefinition;
    IRecorder recorder;
    
    // indicator identifiers
    int vwapIndicatorId;
    int largeBuyIndicatorId;
    int largeSellIndicatorId;
    
    // data to calculate VWAP
    private double accumulatedPrice = 0;
    private long accumulatedSize = 0;
    
    public IndicatorsManager(InstrumentDefinition instrumentDefinition, IRecorder recorder) {
        this.instrumentDefinition = instrumentDefinition;
        this.recorder = recorder;
    }
    
    public void createIndicators(long time) throws IOException {
        
        String alias = instrumentDefinition.alias;
        
        vwapIndicatorId = nextIndicatorId++;        
        // define custom indicator (session accumulated VWAP)
        // solid white line on the left of timeline, dashed line on the right, no icons
        recorder.onIndicatorDefinition(time, vwapIndicatorId, alias,
                (short)0xFFFF, (short)1, 2, Color.WHITE, 
                (short)0xFF00, (short)1, 2, 
                null, 0, 0, true);
        
        // load icons for two next indicators
        BufferedImage arrowUp;
        BufferedImage arrowDown;
        try {
            arrowUp = ImageIO.read(new File("arrow-up.png"));
            arrowDown = ImageIO.read(new File("arrow-down.png"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load indicator icons");
        }

        largeBuyIndicatorId = nextIndicatorId++;
        // define another custom indicator (icons showing large trades)
        // no lines, icons only; lines color does not matter because pattern is 0
        recorder.onIndicatorDefinition(time, largeBuyIndicatorId, alias,
                (short)0, (short)0, 0, Color.BLACK, 
                (short)0, (short)0, 0, 
                arrowDown, -arrowUp.getWidth() / 2, 20, true);

        largeSellIndicatorId = nextIndicatorId++;
        // define another custom indicator (icons showing large trades)
        // no lines, icons only; lines color does not matter because pattern is 0
        recorder.onIndicatorDefinition(time, largeSellIndicatorId, alias,
                (short)0, (short)0, 0, Color.BLACK, 
                (short)0, (short)0, 0, 
                arrowUp, -arrowUp.getWidth() / 2, -arrowUp.getHeight() - 20, true);
    }

    public void onTrade(long time, double price, int size, int aggressor) throws IOException {
        // update VWAP indicator
        accumulatedSize += size;
        accumulatedPrice += price * size;
        double currentVwap = accumulatedPrice / accumulatedSize;
        recorder.onIndicatorPoint(time, vwapIndicatorId, currentVwap);
        
        // if trade is big enough add an arrow marker
        boolean aggressorSideDefined = aggressor == TagsExt.BUY_SIDE || aggressor == TagsExt.SELL_SIDE;
        boolean tradeIsBig = size > 5;
        if (aggressorSideDefined && tradeIsBig) {
            boolean isBuyAgressor = aggressor == TagsExt.BUY_SIDE;
            int indicatorId = isBuyAgressor ? largeBuyIndicatorId : largeSellIndicatorId;
            recorder.onIndicatorPoint(time, indicatorId, price);
        }
    }
}
