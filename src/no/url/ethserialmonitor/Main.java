package no.url.ethserialmonitor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	Pattern pattern = Pattern.compile("gpu[0-9]*\\.(hash|temp|fan|watt)[0-9]");
	
	Server server;
	String com_port;
	boolean hwmon;
	
	int delay_sec_show;
	int delay_fsec_show;
	int delay_halfmin_show;

	private Socket sock; // Keep socket open.
	private BufferedWriter bw; // Closing the writer terminates the socket
	private BufferedReader br;
	
	List<String[]> sec_lines = new ArrayList<String[]>();
	List<String[]> fsec_lines = new ArrayList<String[]>();
	List<String[]> hmin_lines = new ArrayList<String[]>();
	
	Status status;
	
	
	public Main(String[] args) {
		if(args.length > 0) {
			
		} else {
			File config = new File("config.ini");
			if (!config.exists()) {
				try {
					System.out.println("[EthSerialMonitor] No Configuration found, generating config.ini");
					config.createNewFile();
					BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(config)));
					bw.write("## Configuration ##\r\n\r\n"
							+ "#IPaddress and port of the server to pole\r\n"
							+ "#Example: server={ipaddress}:{port}\r\n" 
							+ "server=127.0.0.1:3333\r\n\r\n"
							+ "#Com Port: COM{port number}\r\n" //TODO: Windows is COM{#}, Linux maybe ttyS{#},ttyUSB{#}, or even /dev/ttyS{#},/dev/ttyUSB{#}
							+ "com_port=COM1\r\n\r\n"
							//+ "#Example: server={ipaddress}:{port}\r\n" 
							//+ "server=127.0.0.1:3333\r\n\r\n"
							);
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(config)));
				String[] lines = new String[2];
				String line;
				while ((line = br.readLine()) != null) {
					if (!line.startsWith("#") && line.contains("=")) {
						String[] kv = line.split("=");
						if (kv[0].equalsIgnoreCase("server")) {
							if (kv[1].contains(":")) {
								String[] ip_port = kv[1].split(":");
								server = new Server(ip_port[0], Integer.parseInt(ip_port[1]));
							} else {
								server = new Server(kv[1], 3333);
							}
						}
						if (kv[0].equalsIgnoreCase("hwmon")) {
							hwmon = kv[1].equalsIgnoreCase("true");
						}
						if (kv[0].equalsIgnoreCase("com_port")) {
							com_port = kv[1];
						}
						//delay_fsec_show
						if (kv[0].equalsIgnoreCase("delay_sec_show")) {
							delay_sec_show = Integer.parseInt(kv[1]);
						}
						if (kv[0].equalsIgnoreCase("delay_fsec_show")) {
							delay_fsec_show = Integer.parseInt(kv[1]);
						}
						if (kv[0].equalsIgnoreCase("delay_halfmin_show")) {
							delay_halfmin_show = Integer.parseInt(kv[1]);
						}
						if (kv[0].equalsIgnoreCase("sec_line1")) {
							lines[0] = kv[1];
							if(lines[1] != null) { 
								sec_lines.add(lines);
								lines = new String[2];
							}
						}
						if (kv[0].equalsIgnoreCase("sec_line2")) {
							lines[1] = kv[1];
							if(lines[0] != null) { 
								sec_lines.add(lines);
								lines = new String[2];
							}
						}
						if (kv[0].equalsIgnoreCase("fsec_line1")) {
							lines[0] = kv[1];
							if(lines[1] != null) { 
								fsec_lines.add(lines);
								lines = new String[2];
							}
						}
						if (kv[0].equalsIgnoreCase("fsec_line2")) {
							lines[1] = kv[1];
							if(lines[0] != null) { 
								fsec_lines.add(lines);
								lines = new String[2];
							}
						}
						if (kv[0].equalsIgnoreCase("halfmin_line1")) {
							lines[0] = kv[1];
							if(lines[1] != null) { 
								hmin_lines.add(lines);
								lines = new String[2];
							}
						}
						if (kv[0].equalsIgnoreCase("halfmin_line2")) {
							lines[1] = kv[1];
							if(lines[0] != null) { 
								hmin_lines.add(lines);
								lines = new String[2];
							}
						}
					}
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (RUNNING) {
					try {
						if (hwmon) {
							while ((status = getStatusHR(server)) == null) {
								Thread.sleep(1000);
								continue;
							}
						} else {
							while ((status = getStatusOne(server)) == null) {
								Thread.sleep(1000);
								continue;
							}
						}
						Thread.sleep(1000);
					} catch (IOException e) {
						e.printStackTrace();

					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	
	public static void main(String[] args) {
		main = new Thread(new Main(args));
		main.start();
	}

	@Override
	public void run() {
		//TODO Output all comports for configuration sake
		SerialPort serial = null;
		for (SerialPort comPort : SerialPort.getCommPorts()) {
			String comName = comPort.getSystemPortName();
			System.out.println("[EthSerialMonitor] Com Port found! \""+comName+"\"");
			if(comName.equals(com_port)) {
				serial = comPort;
				break;
			}
		}
		if(serial == null) {
			System.out.println("Error Connecting to Com Port: \""+ com_port+"\"\nExiting...");
			System.exit(1);
		}
		//9600 baud, 8 bit, no parity, 1 stop bit.
		
		try {
			serial.setBaudRate(9600);
			serial.setNumDataBits(8);
			serial.setParity(SerialPort.NO_PARITY);
			serial.setNumStopBits(SerialPort.ONE_STOP_BIT);
			serial.openPort();
			
			serial.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
			
			
			OutputStreamWriter osw = new OutputStreamWriter(serial.getOutputStream());
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
			
			//TODO: Maybe Flash when share found

			long half_sec = System.currentTimeMillis();
			long sec = System.currentTimeMillis();
			long fsec = System.currentTimeMillis();
			long half_min = System.currentTimeMillis();
			long min = System.currentTimeMillis();
			
			int fstep = 0;
			
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
					
					//TODO: Translate config lines
					for(String[] lines : sec_lines) {
						//Status status;
						/*
						if(hwmon) {
							while((status = getStatusHR(server)) == null) {
								osw.write(LCD.SPECIAL);
								osw.write(LCD.HOME);
								osw.flush();
								delay(20);
								String line1 = "Connecting...";
								while(line1.length()<16)
									line1 += " ";
								System.out.println("[SCREEN] " + line1);
								osw.write(line1);
								osw.write(line1);
								osw.flush();
								delay(1000);
								continue;
							}
						} else {
							while((status = getStatusOne(server)) == null) {
								osw.write(LCD.SPECIAL);
								osw.write(LCD.HOME);
								osw.flush();
								delay(20);
								String line1 = "Connecting...";
								while(line1.length()<16)
									line1 += " ";
								System.out.println("[SCREEN] " + line1);
								osw.write(line1);
								osw.write(line1);
								osw.flush();
								delay(1000);
								continue;
							}
						}
						*/
						//osw.write(LCD.SPECIAL);
						//osw.write(LCD.CLEAR_SCREEN);
						//osw.flush();
						//delay(20);
						osw.write(LCD.SPECIAL);
						osw.write(LCD.HOME);
						osw.flush();
						delay(20);
						//System.out.println("[Serial][Writer] Sent: " + LCD.SPECIAL + LCD.CLEAR_SCREEN);
						
						String line1 = this.translate(lines[0], status);
						String line2 = this.translate(lines[1], status);
						String command = line1+line2;
						System.out.println("\n[SCREEN] {" + line1 + "}\n[SCREEN] {" + line2+"}");
						//System.out.println("[COLOR ] " +r+","+g+","+b);
						osw.write(command);
						osw.flush();
						//delay(delay_sec_show);
					}
					sec = System.currentTimeMillis();
				}else if (System.currentTimeMillis() - fsec >= 10000) {
					//Test: May not have to clear
					
					osw.write(LCD.SPECIAL);
					osw.write(LCD.CLEAR_SCREEN);
					osw.flush();
					delay(20);
					
					if(fstep >= fsec_lines.size()) 
						fstep = 0;
					String[] lines = fsec_lines.get(fstep);
					osw.write(LCD.SPECIAL);
					osw.write(LCD.HOME);
					osw.flush();
					delay(20);
					String line1 = this.translate(lines[0], status);
					String line2 = this.translate(lines[1], status);
					String command = line1+line2;
					System.out.println("\n[SCREEN] {" + line1 + "}\n[SCREEN] {" + line2+"}");
					osw.write(command);
					osw.flush();
					delay(delay_fsec_show);
					fstep++;
					fsec = System.currentTimeMillis();					
				}else if (System.currentTimeMillis() - half_min >= 30000) {
					//osw.write(LCD.SPECIAL);
					//osw.write(LCD.CLEAR_SCREEN);
					//osw.flush();
					//delay(20);
					osw.write(LCD.SPECIAL);
					osw.write(LCD.HOME);
					osw.flush();
					delay(20);
					//System.out.println("[Serial][Writer] Sent: " + LCD.SPECIAL + LCD.CLEAR_SCREEN);
					/*
					StatusHR status = (StatusHR) getStatusHR(server);
					if(status == null)
						continue;
						*/
					for(String[] lines : hmin_lines) {
						String line1 = this.translate(lines[0], status);
						String line2 = this.translate(lines[1], status);
						String command = line1+line2;
						System.out.println("\n[SCREEN] {" + line1 + "}\n[SCREEN] {" + line2+"}");
						osw.write(command);
						osw.flush();
						/*
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
						 * 
						 */
						delay(delay_halfmin_show);
					}
					half_min = System.currentTimeMillis();
				}				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	/*
	# avghash.{spaces} = Average Hashrate
	# avgtemp.{spaces} = Average Temperature
	# avgfan.{spaces} = Average Fan Speed
	# shares.{spaces} = Total Current Amount of Shares
	# runtime.{spaces} = Total Current Runtime in Minutes
	# totwatt.{spaces} = Total Wattage Calculated
	# gpu{id}.hash{spaces} = Hashrate of specific GPU
	# gpu{id}.temp{spaces} = Temperature of specific GPU
	# gpu{id}.fan{spaces} = Fan speed of specific GPU
	# gpu{id}.watt{spaces} = Wattage of specific GPU 
	 */
	public String translate(String line, Status status) {
		line = this.swap("avghash.", line, String.valueOf(status.getHashrate()));
		line = this.swap("avgtemp.", line, String.valueOf(status.getAvgTemp()));
		line = this.swap("avgfan.", line, String.valueOf(status.getAvgFan()));
		line = this.swap("shares.", line, String.valueOf(status.getShares()));
		line = this.swap("shareavg.", line, String.valueOf(status.getSharesPerMin()));
		line = this.swap("invalid.", line, String.valueOf(status.getInvalid()));
		line = this.swap("runtime.", line, String.valueOf(status.getRuntime()));
		if(status instanceof StatusHR)
			line = this.swap("totwatt.", line, String.valueOf(((StatusHR) status).getTotalPower()));
		else
			line = this.swap("totwatt.", line, "N/a");
		if(line.contains("pool.")) {
			line = String.valueOf(status.getPool());
		}
		
		Matcher matcher = pattern.matcher(line);
		while(matcher.find()) {
			String source = matcher.group();			
			String[] info = source.replaceAll("gpu", "").split("\\.");
			int gpu_id = Integer.parseInt(info[0]);
			String cmd = info[1].replaceAll("[0-9]+", "").trim();
			int val = Integer.parseInt(info[1].replaceAll("(hash|temp|fan|watt)", "").trim());
			String replace = "";
			switch(cmd) {
				case "hash" : 
					replace = String.valueOf(status.getGPURate(gpu_id));
					break;
				case "temp" : 
					replace = String.valueOf(status.getSpecificTemp(gpu_id));
					break;
				case "fan" : 
					replace = String.valueOf(status.getSpecificFan(gpu_id));
					break;
				case "watt" : 
					if(status instanceof StatusHR) 
						replace = String.valueOf(((StatusHR) status).getSpecificPower(gpu_id));
					else
						replace = "N/a";
			}
			if(replace.length() > val)
				line = line.replaceAll(source, String.valueOf(replace).substring(0, val));
			else
				line = line.replaceAll(source, String.valueOf(replace));			
		}
		
		
		while(line.length()<16)
			line += " ";
		
		if(line.length()>16)
			line = line.substring(0,16);
		return line;
	}
	
	public String swap(String key, String find, String replace) {
		if(find.contains(key)) {
			int val = Integer.parseInt(find.split("\\.")[1].substring(0, 1));
			if(replace.length() > val)
				return find.replaceAll(key.substring(0, key.length()-1)+"\\.[0-9]", String.valueOf(replace).substring(0, val));
			else
				return find.replaceAll(key.substring(0, key.length()-1)+"\\.[0-9]", String.valueOf(replace));
		}
		return find;
	}
	
	public void delay(long mils) {
		try {
			Thread.sleep(mils);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	//Borrowed from EthMonitor
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
			e.printStackTrace();
		}
		return null;
	}

	//Borrowed from EthMonitor
	public StatusHR getStatusHR(Server s) throws IOException {
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

	//Borrowed from EthMonitor
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
		}
		return "{}";
	}

}
