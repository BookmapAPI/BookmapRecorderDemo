package bookmaprecorderdemo.wrappers;

import java.io.BufferedReader;
import java.io.FileReader;

import bookmaprecorderdemo.recorders.RecorderBMF;

public abstract class HandlerBase {

    protected final RecorderBMF recorder;
    private final String filenameIn;
    protected boolean skipFirstLine = false;

    public HandlerBase(RecorderBMF recorder, String fin) {
        this.recorder = recorder;
        this.filenameIn = fin;
    }

    public void run() throws Exception {
        String line;
        BufferedReader reader = new BufferedReader(new FileReader(filenameIn));
        if (skipFirstLine) {
            reader.readLine();
        }
        int n = 0;
        while ((line = reader.readLine()) != null) {
            processLine(line);
            n++;
        }
        System.out.println("Lines processed: " + n);
        recorder.fini();
        reader.close();
    }

    protected abstract void processLine(String line) throws Exception;
}
