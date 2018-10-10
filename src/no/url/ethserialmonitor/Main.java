package no.url.ethserialmonitor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fazecast.jSerialComm.SerialPort;

import no.url.ethapi.Server;
import no.url.ethapi.Status;
import no.url.ethapi.StatusDetail;
import no.url.ethapi.StatusHR;
import no.url.ethapi.StatusOne;

public class Main implements Runnable {
	static Thread main;

	static boolean RUNNING = true;
	static boolean DISCONNECTED = false;
	static boolean RECONNECT = false;
	
	JSONParser parser = new JSONParser();
	Pattern pattern = Pattern.compile("gpu[0-9]*\\.(hash|temp|fan|watt)[0-9]");
	
	Server server;
	String com_port;
	//boolean hwmon = false;
	boolean verbose = true;
	
	int delay_sec_show = 1000;
	int delay_fsec_show = 1000;
	int delay_halfmin_show = 2000;

	private Socket sock; // Keep socket open.
	private BufferedWriter bw; // Closing the writer terminates the socket
	private BufferedReader br;
	
	String default_lines = 
			"#Show every second\r\n"
			+ "#Multiple line group accepted (not recommended)\r\n" 
			+ "sec_line1=avghash.7 MH/s \r\n"
			+ "sec_line2=avgtemp.2C avgfan.2% totwatt.5W\r\n" + "\r\n" 
			+ "#Show every five second\r\n"
			+ "#Alternate Display\r\n" 
			+ "#Multiple line groups accepted\r\n"
			+ "fsec_line1=shares.3 Shares \r\n" 
			+ "fsec_line2=invalid.3 Invalid\r\n" + "\r\n"
			+ "fsec_line1=shareavg.7 Avg/min\r\n" 
			+ "fsec_line2=runtime.2 mins\r\n" + "\r\n"
			+ "fsec_line1=pool.\r\n" 
			+ "fsec_line2=Mine On!\r\n" + "\r\n"
			+ "#Show every 30 seconds \r\n" + "#Loops through all line groups\r\n"
			+ "#Multiple line groups accepted\r\n" + "halfmin_line1=GPU#1 gpu0.hash5 MH/s\r\n"
			+ "halfmin_line2=gpu0.temp2C gpu0.fan2% gpu0.watt2W\r\n" + "\r\n"
			+ "halfmin_line1=GPU#2 gpu1.hash5 MH/s\r\n"
			+ "halfmin_line2=gpu1.temp2C gpu1.fan2% gpu1.watt2W\r\n" + "\r\n"
			+ "halfmin_line1=GPU#3 gpu2.hash5 MH/s\r\n"
			+ "halfmin_line2=gpu2.temp2C gpu2.fan2% gpu2.watt2W\r\n" + "\r\n"
			+ "halfmin_line1=GPU#4 gpu3.hash5 MH/s\r\n"
			+ "halfmin_line2=gpu3.temp2C gpu3.fan2% gpu3.watt2W\r\n" + "\r\n"
			+ "halfmin_line1=GPU#5 gpu4.hash5 MH/s\r\n"
			+ "halfmin_line2=gpu4.temp2C gpu4.fan2% gpu4.watt2W\r\n" + "\r\n";
	
	List<String[]> sec_lines = new ArrayList<String[]>();
	List<String[]> fsec_lines = new ArrayList<String[]>();
	List<String[]> hmin_lines = new ArrayList<String[]>();
	
	Status status;
	int r = 0,g = 0,b = 0;
	
	public Main(String[] args) {
		if(args.length > 0) {
			for(String arg :args) {
				if(arg.contains("--server=") || arg.matches("-[sS]=")) {
					String kv = arg.split("=")[1];
					if (kv.contains(":")) {
						String[] ip_port = kv.split(":");
						server = new Server(ip_port[0], Integer.parseInt(ip_port[1]));
					} else {
						server = new Server(kv, 3333);
					}
				}
				if(arg.contains("--com_port=") || arg.matches("-[cC]=")) {
					com_port = arg.split("=")[1];
				}
				/*
				if (arg.contains("--hwmon=") || arg.matches("-[mM]=")) {
					String kv = arg.split("=")[1];
					hwmon = kv.equalsIgnoreCase("true");
				}*/
			}
			boolean fail = false;
			if(server == null) {
				System.out.println("No Server Argument!\r\n--server or -s");
				fail = true;
			}
			if(com_port == null) {
				System.out.println("No Com Port Argument!\r\n--com_port or -c");
				fail = true;
			}
			if(fail) {
				System.out.println("Failed to start!\r\nUse configuration or start with required arguements!\r\nType --help / -h");
				System.exit(1);
			}
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(default_lines.getBytes())));
				this.fillSettings(br);
				br.close();
			} catch (NumberFormatException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			File config = new File("config.ini");
			if (!config.exists()) {
				try {
					System.out.println("[EthSerialMonitor] No Configuration found, generating config.ini");
					config.createNewFile();
					BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(config)));
					bw.write("## Configuration ##\r\n\r\n"
							+ "verbose=true\r\n"
							+ "#IPaddress and port of the server to pole\r\n"
							+ "#Example: server={ipaddress}:{port}\r\n" 
							+ "server=127.0.0.1:3333\r\n\r\n"
							+ "#Com Port: COM{port number}\r\n" //TODO: Windows is COM{#}, Linux ttyS{#},ttyUSB{#}
							+ "com_port=COM1\r\n\r\n"
							+ "##Update delays\r\n" + "delay_sec_show=1000\r\n" + "delay_fsec_show=2000\r\n"
							+ "delay_halfmin_show=2000\r\n" + "\r\n" + "\r\n" + "# Line configuration:\r\n"
							+ "#    -variables-\r\n" + "# All values truncated left (150.63 = 6 spaces)\r\n"
							+ "# Total size must equal LCD size for line (16 spaces)\r\n"
							+ "# Do not exceed 9 spaces per variable, \r\n"
							+ "# 10's are counted as 1 and a 0 Character\r\n"
							+ "# avghash.{spaces} = Average Hashrate\r\n"
							+ "# avgtemp.{spaces} = Average Temperature\r\n"
							+ "# avgfan.{spaces} = Average Fan Speed\r\n"
							+ "# shares.{spaces} = Total Current Amount of Shares\r\n"
							+ "# shareavg.{spaces} = Average Shares per Minute\r\n"
							+ "# invalid.{spaces} = Invalid Shares\r\n"
							+ "# runtime.{spaces} = Total Current Runtime in Minutes\r\n"
							+ "# totwatt.{spaces} = Total Wattage Calculated\r\n"
							+ "# gpu{id}.hash{spaces} = Hashrate of specific GPU\r\n"
							+ "# gpu{id}.temp{spaces} = Temperature of specific GPU\r\n"
							+ "# gpu{id}.fan{spaces} = Fan speed of specific GPU\r\n"
							+ "# gpu{id}.watt{spaces} = Wattage of specific GPU\r\n"
							+ "# pool. = Show the pool connected (Single Line)\r\n" + "\r\n" 
							+ default_lines
							);
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(config)));
				this.fillSettings(br);
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * Status Thread
		 */
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (RUNNING) {
					try {
						//Intuitive Selecting defaulting with Detail-> HR-> One
						//if (hwmon) {
							while ((status = getStatusHR(server)) == null) {
								Thread.sleep(1000);
								continue;
							}
						/*} else {
							while ((status = getStatusOne(server)) == null) {
								Thread.sleep(1000);
								continue;
							}
						}*/
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
		
	}
	
	private void fillSettings(BufferedReader br) throws NumberFormatException, IOException {
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
				if (kv[0].equalsIgnoreCase("verbose")) {
					verbose = kv[1].equalsIgnoreCase("true");
				}
				/*
				if (kv[0].equalsIgnoreCase("hwmon")) {
					hwmon = kv[1].equalsIgnoreCase("true");
				}*/
				if (kv[0].equalsIgnoreCase("com_port")) {
					com_port = kv[1];
				}
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
	}

	public static void main(String[] args) {
		
		main = new Thread(new Main(args));
		main.start();
		
		//new Thread(new Test_Main()).start();
	}

	@Override
	public void run() {
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
			System.out.println("[EthSerialMonitor] Error Connecting to Com Port...\nExiting.");
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
			osw.write(0xD1);
			osw.write(16);
			osw.write(2);
			osw.flush();
			delay(20);			
			osw.write(LCD.SPECIAL);
			osw.write(0x50);
			osw.write(200);
			osw.flush();
			delay(20);			
			osw.write(LCD.SPECIAL);
			osw.write(0x99);
			osw.flush();
			delay(20);			
			osw.write(LCD.SPECIAL);
			osw.write(0x4B);
			osw.flush();
			delay(20);			
			osw.write(LCD.SPECIAL);
			osw.write(0x54);
			osw.flush();			
			osw.write(LCD.SPECIAL);
			osw.write(0x52);
			osw.flush();
			delay(20);			
			osw.write(LCD.SPECIAL);	
			osw.write(0x58);
			osw.flush();			
			osw.write(LCD.SPECIAL);
			osw.write(0x48);
			osw.flush();
			delay(20);
			osw.write("Connecting...");
			osw.flush();
			delay(20);
			long fsec = System.currentTimeMillis();
			long hsec = System.currentTimeMillis();
			int r = 255,g=10,b=10;
			int c = 0;
			//TODO: Maybe Flash when share found

			long half_sec = System.currentTimeMillis();
			long sec = System.currentTimeMillis();
			long half_min = System.currentTimeMillis();
			long min = System.currentTimeMillis();
			
			int fstep = 0;
			
			
			
			while (RUNNING) {
				if (System.currentTimeMillis() - hsec >= 200) {
					switch(c) {
						case 0 :{
							if(b<255)
								b++;
							if(b>=200)
							r--;
							if(b>=255 && r == 50) 
								c++;						
							break;
						}
						case 1 :{
							if(g<255)
								g++;
							if(g>=200)
								b--;
							if(g>=255 && b == 50) 
								c++;					
							break;
						}
						case 2 :{
							if(r<255)
								r++;
							if(r>=200)
								g--;
							if(r>=255 && g == 50) 
								c = 0;
							break;
						}
					}
					osw.write(LCD.SPECIAL);
					osw.write(LCD.RGB);
					osw.write(r);
					osw.write(g);
					osw.write(b);
					osw.flush();
					delay(10);
					hsec = System.currentTimeMillis();
				}
				if (System.currentTimeMillis() - sec >= 1000) {
					if(DISCONNECTED) {
						osw.write(LCD.SPECIAL);
						osw.write(LCD.HOME);
						osw.flush();
						delay(20);
						osw.write("Disconnected!                   ");
						osw.flush();
						delay(20);
						continue;
					}
					for(String[] lines : sec_lines) {
						osw.write(LCD.SPECIAL);
						osw.write(LCD.HOME);
						osw.flush();
						delay(20);
						//System.out.println("[Serial][Writer] Sent: " + LCD.SPECIAL + LCD.CLEAR_SCREEN);
						
						String line1 = this.translate(lines[0], status);
						String line2 = this.translate(lines[1], status);
						String command = line1+line2;
						if(verbose) 
							System.out.println("\n[SCREEN] {" + line1 + "}\n[SCREEN] {" + line2+"}");
						osw.write(command);
						osw.flush();
						//delay(delay_sec_show); //No Delays
					}
					sec = System.currentTimeMillis();
				}else if (System.currentTimeMillis() - fsec >= 10000) {
					//Test: May not have to clear
					osw.write(LCD.SPECIAL);
					osw.write(LCD.CLEAR_SCREEN);
					osw.flush();
					delay(20);
					//Test End					
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
					if(verbose) 
						System.out.println("\n[SCREEN] {" + line1 + "}\n[SCREEN] {" + line2+"}");
					osw.write(command);
					osw.flush();
					delay(delay_fsec_show);
					fstep++;
					fsec = System.currentTimeMillis();					
				}else if (System.currentTimeMillis() - half_min >= 30000) {
					osw.write(LCD.SPECIAL);
					osw.write(LCD.HOME);
					osw.flush();
					delay(20);
					for(String[] lines : hmin_lines) {
						String line1 = this.translate(lines[0], status);
						String line2 = this.translate(lines[1], status);
						String command = line1+line2;
						if(verbose) 
							System.out.println("\n[SCREEN] {" + line1 + "}\n[SCREEN] {" + line2+"}");
						osw.write(command);
						osw.flush();
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
		if(status == null)
			return "";
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
			int val = 0;
			try {
				val = Integer.parseInt(find.split("\\.")[1].substring(0, 1)); //NFE?
			} catch (NumberFormatException e) {
				System.out.print("Tried to parse "+find.split("\\.")[1].substring(0, 1)+" to a number.");
				System.out.print("Original:"+find);
			}
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
	public StatusDetail getStatusDetail(Server s) throws IOException {
		String data = this.connect(s.getIPAddress(), s.getPort(), StatusOne.COMMAND);
		if (data != null)
			return new StatusDetail(data);
		return null;
	}
	/*
	//Borrowed from EthMonitor
	@Deprecated
	public StatusOne getStatusOne(Server s) throws IOException {
		String data = this.connect(s.getIPAddress(), s.getPort(), StatusOne.COMMAND);
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
	*/
	//Borrowed from EthMonitor
	@Deprecated
	public StatusHR getStatusHR(Server s) throws IOException {
		String data = this.connect(s.getIPAddress(), s.getPort(), StatusHR.COMMAND);
		if (data != null)
			return new StatusHR(data);
		return null;

	}
	
	
	//Borrowed from EthMonitor
	public String connect(String ip_address, int port, String command) throws UnknownHostException {
		try {
			if (sock == null || RECONNECT) {
				if(verbose) 
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
				System.out.println(e.getMessage());
				System.out.println("[Socket] Disconnected from server!");
			}
		}
		return null;
	}

}
