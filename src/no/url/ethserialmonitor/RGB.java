package no.url.ethserialmonitor;

import java.util.Random;

public class RGB {
	static Random gen = new Random();
	int r = 0,g = 0,b = 0;
	public RGB(int red, int green, int blue) {
		r=red;
		g=green;
		b=blue;
	}
	
	public int getRed() {
		return r;
	}
	
	public int getGreen() {
		return g;
	}
	
	public int getBlue() {
		return b;
	}
	public String toString() {
		return String.valueOf(r)+","+String.valueOf(g)+","+String.valueOf(b);
	}

	static RGB blue() {
		return new RGB(0,0,255);
	}
	
	static RGB red() {
		return new RGB(255,0,0);
	}
	
	static RGB green() {
		return new RGB(0,255,0);
	}
	
	static RGB purple() {
		return new RGB(255,0,255);
	}
	
	static RGB orange() { 
		return new RGB(255,128,0);
	}
	
	static RGB teal() { 
		return new RGB(0,255,255);
	}
	
	static RGB white() { 
		return new RGB(255,255,255);
	}
	
	static RGB half_white() { 
		return new RGB(128,128,128);
	}
	
	static RGB off() { 
		return new RGB(0,0,0);
	}
	
	static RGB random() {
		return new RGB(gen.nextInt(255),gen.nextInt(255),gen.nextInt(255));
	}
}
