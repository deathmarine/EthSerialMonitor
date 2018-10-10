package no.url.ethserialmonitor;

public class LCD {


	static byte NEW_LINE = 0x0A;
	static byte CRETURN = 0x0D; //is not listened to
	static byte BACKSPACE = 0x08; 
	static int SPECIAL = 0xFE; 			//Tested GOOD
	
	static byte DISPLAY_ON = 0x42; //followed by time in mins to stay on
	static byte DISPLAY_OFF = 0x46;
	
	
	//Special: Setup
	static int BRIGHTNESS = 0x99; //arg (255 max)
	static int SET_BRIGHTNESS = 0x98;
	static byte CONTRAST = 0x50; //arg (255 max, normal = 200)
	static int SET_CONTRAST = 0x91;
	static byte AUTO_SCROLL_ON = 0x51;
	static byte AUTO_SCROLL_OFF = 0x52;
	static int SIZE = 0xD1;
	
	static byte SPLASH_SCREEN = 0x40; //write up to 32 bytes for 16x2
	
	//Special: TextArea Manipulation
	static byte CLEAR_SCREEN = 0x58; 	//Tested GOOD
	static byte CURSOR_POS = 0x47;
	static byte HOME = 0x48; 			//Tested GOOD
	static byte BACK = 0x4C;
	static byte FORWARD = 0x4D;
	static byte UNDERLINE_ON = 0x4A;
	static byte UNDERLINE_OFF = 0x4B;
	static byte BLOCK_ON = 0x53;
	static byte BLOCK_OFF = 0x54;

	//Special: Backlight
	static int RGB =  0xD0; //three arguments 0-255
	
	
}
