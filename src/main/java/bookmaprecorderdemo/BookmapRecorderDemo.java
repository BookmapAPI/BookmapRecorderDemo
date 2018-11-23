/**
 * The first thing you should to is to add BookmapRecorder.jar to your project.
 * It's shipped with BookMap and is located in lib subfolder inside your
 * installation folder (typically C:\Program Files (x86)\BookMap\lib for x64 OS
 * and C:\Program Files\BookMap\lib for x32 OS)
 */
package bookmaprecorderdemo;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import velox.recorder.DataRecorder;
import velox.recorder.DataRecorderConfiguration;
import velox.recorder.IRecorder;
import velox.recorder.InstrumentDefinition;
import velox.recorder.LicenseException;
import velox.recorder.Tags;
import velox.recorder.TagsExt;

public class BookmapRecorderDemo {

    /**
     * Some point in time, just for convenience (nanoseconds)
     */
    private static final long INITIAL_TIME = 1400000000_000000000L;

    /**
     * Number of nanoseconds in one second
     */
    private static final long NS_IN_SEC = 1_000_000_000L;

    public static void main(String[] args) throws IOException, LicenseException {
        IRecorder recorder = new DataRecorder();

        DataRecorderConfiguration config = new DataRecorderConfiguration();

        // Here are the fields that you should configure
        config.file = new File("feed_recorder_demo.bmf");
        config.licenseKey = "<put your license key here>";
        config.licenseHash = "<put your hash here>";

        long currentTime = INITIAL_TIME;
        recorder.init(INITIAL_TIME, config);

        // Instrument1 defined 1 second after feed is started
        InstrumentDefinition instrument1 = new InstrumentDefinition();
        instrument1.alias = "Test instrument";
        instrument1.id = 1; // Unique instrument ID, it's required later to record events
        instrument1.pips = 25; // So prices will be like 100, 125, 150, 175 ....
        recorder.onInstrumentDefinition(currentTime += 1 * NS_IN_SEC, instrument1);

        // And the second instrument is defined at the same point in time
        InstrumentDefinition instrument2 = new InstrumentDefinition();
        instrument2.alias = "Test instrument 2";
        instrument2.id = 2; // Unique instrument ID, it's required later to record events
        instrument2.pips = 10; // So prices will be like 100, 110, 120 ....
        recorder.onInstrumentDefinition(currentTime, instrument2);

        currentTime += NS_IN_SEC;

        // Let's generate 10 bid + 10 ask levels for 1'st instrument, and 5+5 for second.
        for (int i = 1; i <= 10; ++i) {
            final int sizeBid = i * 22;
            recorder.onDepth(currentTime, 1, true, 1000 - 25 * i, sizeBid);

            final int sizeAsk = i * 15;
            recorder.onDepth(currentTime, 1, false, 1000 + 25 * i, sizeAsk);
        }
        for (int i = 1; i <= 5; ++i) {
            final int sizeBid = i * 2;
            recorder.onDepth(currentTime, 2, true, 5000 - 10 * i, sizeBid);

            final int sizeAsk = i * 1;
            recorder.onDepth(currentTime, 2, false, 5000 + 10 * i, sizeAsk);
        }

        // Advance time 1 sec forward.
        currentTime += NS_IN_SEC;

        // Now let's start changing the data (for both instruments)
        for (int q = 0; q <= 50; ++q) {
            // Remove old level
            recorder.onDepth(currentTime += NS_IN_SEC / 20, 1, false, 1000 + 25 * (q + 1), 0);
            // Add new level
            final int sizeBid1 = q * 5 + 100;
            recorder.onDepth(currentTime += NS_IN_SEC / 20, 1, false, 1000 + 25 * (q + 1 + 10), sizeBid1);

            // Remove old level
            recorder.onDepth(currentTime += NS_IN_SEC / 20, 1, true, 1000 + 25 * (q - 1 - 10), 0);
            // Add new level
            final int sizeAsk1 = q * 10 + 100;
            recorder.onDepth(currentTime += NS_IN_SEC / 20, 1, true, 1000 + 25 * (q - 1), sizeAsk1);

            // Remove old level
            recorder.onDepth(currentTime += NS_IN_SEC / 20, 2, false, 5000 + 10 * (-q + 1 + 5), 0);
            // Add new level
            final int sizeBid2 = q * 5 + 100;
            recorder.onDepth(currentTime += NS_IN_SEC / 20, 2, false, 5000 + 10 * (-q + 1), sizeBid2);

            // Remove old level
            recorder.onDepth(currentTime += NS_IN_SEC / 20, 2, true, 5000 + 10 * (-q - 1), 0);
            // Add new level
            final int sizeAsk2 = q * 10 + 100;
            recorder.onDepth(currentTime += NS_IN_SEC / 20, 2, true, 5000 + 10 * (-q - 1 - 5), sizeAsk2);
        }
        
        
        BufferedImage icon = ImageIO.read(new File("icon_accept.gif"));
        // Line and icons
        recorder.onIndicatorDefinition(
                currentTime += NS_IN_SEC / 10, 1, "Test instrument 2", "Indicator 1",
                (short)0xFFFF, (short)1, 1, Color.ORANGE, 
                (short)0xFF08, (short)1, 2, 
                icon, -icon.getWidth() / 2, -icon.getHeight() / 2, true);
        // No line, only icons
//        recorder.onIndicatorDefinition(
//                currentTime += NS_IN_SEC / 10, 1, "Test instrument 2", "Indicator 1",
//                (short)0x0000, (short)1, 1, Color.ORANGE,
//                (short)0x0000, (short)1, 2,
//                icon, -icon.getWidth() / 2, -icon.getHeight() / 2, true);
        // No icon, different line style
//        recorder.onIndicatorDefinition(
//                currentTime += NS_IN_SEC / 10, 1, "Test instrument 2", "Indicator 1",
//                (short)0x5555, (short)20, 5, Color.ORANGE,
//                (short)0x5555, (short)40, 10,
//                null, 0, 0, true);
        recorder.onIndicatorPoint(currentTime += NS_IN_SEC / 10, 1, 4440.0);
        recorder.onIndicatorPoint(currentTime += NS_IN_SEC, 1, 4450.0);
        recorder.onIndicatorPoint(currentTime += NS_IN_SEC, 1, Double.NaN);
        recorder.onIndicatorPoint(currentTime += NS_IN_SEC, 1, 4450.0);
        

        // Let's create a trade for the 2'nd instrument. We won't update depth data for simplicity.
        recorder.onTrade(currentTime, 2, 4500.0, 150, TagsExt.UNKNOWN_SIDE, TagsExt.OTC_TYPE_NONE);

        //  Let's create an order
        recorder.recordNewOrder(currentTime += NS_IN_SEC, "order1", "Test instrument 2", false, 4580, 5);
        
        // Let's record order position data. If you comment this out BookMap will compute position using built-in algorithms
        for (int position = 310 /* 315 is the size on the order's price level, order size is 5, so initially there are 310 shares before our order*/;
                position > 100; --position) {
            // Decreasing order position - will look like it advances to the head of the queue
            recorder.recordOrderQueuePosition(currentTime += NS_IN_SEC / 30, "order1", position);
        }
        
        // Let's decrease the price
        recorder.recordUpdateOrder(currentTime += NS_IN_SEC, "order1", 4480, 5);
        // Let's execute the order
        recorder.recordExecution(currentTime += NS_IN_SEC, "order1", 4480, 5);
        // And mark it as filled
        recorder.recordFilled(currentTime, "order1");

        // Don't forget to call it after recording is finished - recorder has to
        // write internal buffers to the file
        recorder.fini();
    }

}
