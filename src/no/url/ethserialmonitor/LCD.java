package no.url.ethserialmonitor;

public class LCD {

	static char NEW_LINE = 0x0A;
	static char CRETURN = 0x0D; //is not listened to
	static char BACKSPACE = 0x08; 
	static char SPECIAL = 0xFE;
	
	static char DISPLAY_ON = 0x42; //followed by time in mins to stay on
	static char DISPLAY_OFF = 0x46;
	
	
	//Special: Setup
	static char BRIGHTNESS = 0x99; //arg (255 max)
	static char SET_BRIGHTNESS = 0x98;
	static char CONTRAST = 0x50; //arg (255 max, normal = 200)
	static char SET_CONTRAST = 0x91;
	static char AUTO_SCROLL_ON = 0x51;
	static char AUTO_SCROLL_OFF = 0x52;
	static char SIZE = 0xD1;
	
	static char SPLASH_SCREEN = 0x40; //write up to 32 chars for 16x2
	
	//Special: TextArea Manipulation
	static char CLEAR_SCREEN = 0x58;
	static char CURSOR_POS = 0x47;
	static char HOME = 0x48;
	static char BACK = 0x4C;
	static char FORWARD = 0x4D;
	static char UNDERLINE_ON = 0x4A;
	static char UNDERLINE_OFF = 0x4B;
	static char BLOCK_ON = 0x53;
	static char BLOCK_OFF = 0x54;

	//Special: Backlight
	static char RGB =  0xD0; //three arguments 0-255
	
	
	
	
}
