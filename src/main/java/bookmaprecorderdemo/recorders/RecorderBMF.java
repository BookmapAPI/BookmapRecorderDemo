package bookmaprecorderdemo.recorders;

import java.io.File;
import java.io.IOException;

import bookmaprecorderdemo.utils.Instrument;
import velox.recorder.DataRecorder;
import velox.recorder.DataRecorderConfiguration;
import velox.recorder.InstrumentDefinition;
import velox.recorder.LicenseException;

public class RecorderBMF extends DataRecorder {

    final String fout;
    
    public RecorderBMF(String fout) {
        this.fout = fout;
    }

    public void init(long t) throws Exception {
        DataRecorderConfiguration config = new DataRecorderConfiguration();
        config.licenseKey = "<put your license key here>";
        config.licenseHash = "<put your hash here>";
        config.file = new File(fout);
        super.init(t, config);
    }

    public void onInstrument(long t, Instrument instr) throws IOException {
        InstrumentDefinition instrDef = new InstrumentDefinition();
        instrDef.alias = instr.symbol;
        instrDef.id = instr.id;
        instrDef.pips = 1;
        super.onInstrumentDefinition(t, instrDef);
    }
    
    @Override
    public void init(long t, DataRecorderConfiguration config) throws IOException, LicenseException {
        throw new IOException("not supported");
    }
}
