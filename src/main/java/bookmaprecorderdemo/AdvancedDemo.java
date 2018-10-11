package bookmaprecorderdemo;

import bookmaprecorderdemo.recorders.RecorderBMF;
import bookmaprecorderdemo.wrappers.HandlerBookmapIndicators;

public final class AdvancedDemo {
	public static void main(String[] args) throws Exception {
	    // You can download sample file at https://bookmap.com/shared/feeds/BookmapRecorderDemo_ES-CL_20181002.zip
		String fin = "BookmapRecorderDemo_ES-CL_20181002.txt";
		String fout = fin.replace(".txt", ".bmf");
		new HandlerBookmapIndicators(new RecorderBMF(fout), fin).run();
	}
}
