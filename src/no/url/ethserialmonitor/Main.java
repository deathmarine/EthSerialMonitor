package no.url.ethserialmonitor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fazecast.jSerialComm.SerialPort;

import no.url.ethmonitor.Server;
import no.url.ethmonitor.Status;
import no.url.ethmonitor.StatusHR;
import no.url.ethmonitor.StatusOne;

public class Main implements Runnable {
	static Thread main;
	
	static String OVERALL_STATUS = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_getstat1\"}\r\n";
	static String DETAILED_STATUS = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_getstathr\"}\r\n";

	static boolean RUNNING = true;
	static boolean DISCONNECTED = false;
	static boolean RECONNECT = false;
	
	JSONParser parser = new JSONParser();
	Server server = new Server("192.168.50.77", 3333);

	private Socket sock; // Keep socket open.
	private BufferedWriter bw; // Closing the writer terminates the socket
	private BufferedReader br;
	
	
	public static void main(String[] args) {
		main = new Thread(new Main());
		main.start();
	}

	@Override
	public void run() {

		for (SerialPort comPort : SerialPort.getCommPorts()) {
			System.out.println(comPort.getSystemPortName());
			//9600 baud, 8 bit, no parity, 1 stop bit.
			
			
			// comPort.openPort();
			try {
				comPort.setBaudRate(9600);
				comPort.setNumDataBits(8);
				comPort.setParity(SerialPort.NO_PARITY);
				comPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
				comPort.openPort();
				
				comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
				
				
				OutputStreamWriter osw = new OutputStreamWriter(comPort.getOutputStream());
				//LCD setup
				osw.write(LCD.SPECIAL);
				osw.write(LCD.SIZE);
				osw.write(16);
				osw.write(2);
				osw.flush();
				delay(20);
				osw.write(LCD.SPECIAL);
				osw.write(LCD.BRIGHTNESS);
				osw.write(200);
				osw.flush();
				delay(20);
				osw.write(LCD.SPECIAL);
				osw.write(LCD.BRIGHTNESS);
				osw.flush();
				delay(20);
				osw.write(LCD.SPECIAL);
				osw.write(LCD.UNDERLINE_OFF);
				osw.flush();
				delay(20);
				osw.write(LCD.SPECIAL);
				osw.write(LCD.BLOCK_OFF);
				osw.flush();
				delay(20);
				osw.write(LCD.SPECIAL);
				osw.write(LCD.CLEAR_SCREEN);
				osw.flush();
				delay(20);
				osw.write(LCD.SPECIAL);
				osw.write(LCD.HOME);
				osw.flush();
				delay(20);
				osw.write("Connecting...   ");
				osw.flush();
				delay(20);
				
				//Flash when share found

				long half_sec = System.currentTimeMillis();
				long sec = System.currentTimeMillis();
				long half_min = System.currentTimeMillis();
				long min = System.currentTimeMillis();
				
				int r = 0,g = 0,b = 0;
				boolean rb = false,gb = false,bb = false;
				
				boolean sync = false; //all from 0 to 255 (White I think)
				
				boolean redgreen_stepup; //r0-255 then g0-255 then b0-255
				boolean redblue_stepup; //r0-255 then b0-255 then g0-255
				boolean red_pulse; //r0-255 only
				
				boolean greenblue_stepup; //g0-255 then b0-255 then r0-255
				boolean greenred_stepup; //g0-255 the r0-255 then b0-255
				boolean green_pulse;
				
				boolean bluered_stepup; //b0-255 then r0-255 then g0-255
				boolean bluegreen_stepup; //b0-255 then g0-255 then r0-255
				boolean blue_pulse; 
				
				
				
				boolean custom = true;
				
				
				while (RUNNING) {
					if (System.currentTimeMillis() - half_sec >= 100) {
						if(sync){
							if(r<255 && !rb)
								r++;
							if(g<255 && !gb)
								g++;
							if(b<255 && !bb)
								b++;
							if(r>=255)
								rb = true;
							if(g>=255)
								gb = true;
							if(b>=255)
								bb = true;
							if(rb)
								r--;
							if(gb)
								g--;
							if(bb)
								b--;
							if(rb&&gb&&bb&&r==0&&g==0&&b==0) {
								rb = false;
								gb = false;
								bb = false;
								sync = false;
							}
						}
						if(custom) {
							r = 255;
							if(g<255 && b == 0 && !gb)
								g++;
							if(!gb && g >= 255 && b == 0)
								gb = true;
							if(b == 0 && gb)
								g--;
							if(gb && b<255 && g == 0)
								b++;
							if(gb && b >= 255 && g == 0)
								gb = false;
							if(!gb && g == 0)
								b--;
						}
						osw.write(LCD.SPECIAL);
						osw.write(LCD.RGB);
						osw.write(r);
						osw.write(g);
						osw.write(b);
						osw.flush();
						delay(20);
						half_sec = System.currentTimeMillis();
					}
					if (System.currentTimeMillis() - sec >= 1000) {
						osw.write(LCD.SPECIAL);
						osw.write(LCD.CLEAR_SCREEN);
						osw.flush();
						delay(20);
						osw.write(LCD.SPECIAL);
						osw.write(LCD.HOME);
						osw.flush();
						delay(20);
						//System.out.println("[Serial][Writer] Sent: " + LCD.SPECIAL + LCD.CLEAR_SCREEN);
						
						StatusOne status;
						while((status = getStatusOne(server)) == null) {
							osw.write(LCD.SPECIAL);
							osw.write(LCD.CLEAR_SCREEN);
							osw.flush();
							delay(20);
							osw.write(LCD.SPECIAL);
							osw.write(LCD.HOME);
							osw.flush();
							delay(20);
							String line1 = "Connecting...";
							while(line1.length()<16)
								line1 += " ";
							System.out.println("[SCREEN] " + line1);
							osw.write(line1);
							osw.flush();
							delay(20);
							continue;
						}
						String rate = String.valueOf(status.getHashrate());
						if(rate.length() > 9)
							rate = String.valueOf(status.getHashrate()).substring(0, 10); //Out of bounds
						String line1 = rate + " MH/s";
						while(line1.length()<16)
							line1 += " ";
						
						String temp = String.valueOf(status.getAvgTemp());
						if(temp.length() > 4)
							temp = String.valueOf(status.getAvgTemp()).substring(0, 4); //Out of bounds
						String mins = String.valueOf(status.getRuntime());
						String line2 = temp + "C - "+ mins + " mins";
						while(line2.length()<16)
							line2 += " ";
								
						
						String command = line1+line2;

						System.out.println("\n[SCREEN] {" + line1 + "}\n[SCREEN] {" + line2+"}");
						//System.out.println("[COLOR ] " +r+","+g+","+b);
						osw.write(command);
						osw.flush();
						delay(20);
						sec = System.currentTimeMillis();
					}else if (System.currentTimeMillis() - half_min >= 30000) {
						osw.write(LCD.SPECIAL);
						osw.write(LCD.CLEAR_SCREEN);
						osw.flush();
						delay(20);
						osw.write(LCD.SPECIAL);
						osw.write(LCD.HOME);
						osw.flush();
						delay(20);
						//System.out.println("[Serial][Writer] Sent: " + LCD.SPECIAL + LCD.CLEAR_SCREEN);
						
						StatusHR status = (StatusHR) getStatusHR(server);
						if(status == null)
							continue;
						for(int i=0;i<status.getAmtGPUs();i++) {
							String line1 = "GPU#" + String.valueOf(i)+" "+String.valueOf(status.getGPURate(i));
							while(line1.length()<16)
								line1 += " ";
							String temp = String.valueOf(status.getSpecificTemp(i));
							if(temp.length() > 4)
								temp = String.valueOf(status.getSpecificTemp(i)).substring(0, 4); //Out of bounds
							String fan = String.valueOf(status.getSpecificFan(i));
							String line2 = temp + "C - "+ fan + "% Fan";
							while(line2.length()<16)
								line2 += " ";
							System.out.println("\n[SCREEN] {" + line1 + "}\n[SCREEN] {" + line2+"}");
							//System.out.println("[COLOR ] " +r+","+g+","+b);
							delay(2000);
						}
						half_min = System.currentTimeMillis();
					}
					
					
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
	public void delay(long mils) {
		try {
			Thread.sleep(mils);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	public StatusOne getStatusOne(Server s) throws IOException {
		String data = this.connect(s.getIPAddress(), s.getPort(), OVERALL_STATUS);
		if (data == null)
			return null;
		JSONObject json_obj;
		try {
			json_obj = (JSONObject) parser.parse(data);
			if (json_obj != null) {
				Object obj = json_obj.get("result");
				if (obj instanceof JSONArray) {
					JSONArray jarray = (JSONArray) obj;
					return new StatusOne(jarray);
				}
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public Status getStatusHR(Server s) throws IOException {
		String data = this.connect(s.getIPAddress(), s.getPort(), DETAILED_STATUS);
		if (data == null)
			return null;
		JSONObject json_obj;
		try {
			json_obj = (JSONObject) parser.parse(data);
			if (json_obj != null) {
				Object obj = json_obj.get("result");
				if (obj instanceof JSONObject)
					return new StatusHR((JSONObject) obj);
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	public String connect(String ip_address, int port, String command) throws UnknownHostException {
		try {
			if (sock == null || RECONNECT) {
				System.out.println("[Socket] Opening Socket to " + ip_address + ":" + port + " !");
				sock = new Socket(InetAddress.getByName(ip_address), port);
				RECONNECT = false;
			}
			bw = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));

			bw.write(command);
			bw.flush();

			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			String line = br.readLine();
			DISCONNECTED = false;
			return line;
		} catch (IOException e) {
			if (!DISCONNECTED) {
				DISCONNECTED = true;
				RECONNECT = true;
				System.out.print(e.getMessage());
				System.out.println("[Socket] Disconnected from server!");
			}
			// System.out.print(e.getMessage());
		}
		// bw.close();
		// br.close();
		// sock.close();
		return "{}";
	}

}
