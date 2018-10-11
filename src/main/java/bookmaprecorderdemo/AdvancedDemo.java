package bookmaprecorderdemo;

import bookmaprecorderdemo.recorders.RecorderBMF;
import bookmaprecorderdemo.wrappers.HandlerBookmapIndicators;

public final class AdvancedDemo {
	public static void main(String[] args) throws Exception {
		String fin = "../data/BookmapRecorderDemo_20181002.txt";
		String fout = fin.replace(".txt", ".bmf");
		new HandlerBookmapIndicators(new RecorderBMF(fout), fin).run();
	}
}
