# EthSerialMonitor
An eth api socket to serial interface for a 16x2 LCD Display

**Any ideas, comments, or suggestions can be made via inline comments in a commit, a pull request, or issue tracker.**
## Concept
*****

To see the state of a headless miner, regardless of OS, by mounting a small inexpensive 16x2 RGB LCD to the chassis. 

## Scope
*****

IP and Port configurable. !Done!

Com Port configurable. !Done!

Alternative Logfile Reader. //TODO

std::out Wrapper. //Probably not

Display configurable. !Done!

Display Size Configurable ex. 20x2 or 20x4 //TODO

Timing configurable. !Done!

RGB configurable. //TODO

RGB presets. //TODO

Better Console Presentation. //TODO

More and Less Verbose Console Output. //TODO


## Compilation
*****

Using maven to handle dependencies.

* Install [Maven 3](http://maven.apache.org/download.html)
* Clone this repo and: `mvn clean install`

## Downloads
*****
[Downloads](https://github.com/deathmarine/EthMonitor/releases)

## Links
*****
Order:

![Item](https://cdn-shop.adafruit.com/970x728/782-09.jpg)

[https://www.adafruit.com/product/782](https://www.adafruit.com/product/782)

Command Examples:

[https://github.com/adafruit/Adafruit_RGB_LCD_backpack](https://github.com/adafruit/Adafruit_RGB_LCD_backpack)

[https://learn.adafruit.com/usb-plus-serial-backpack/command-reference](https://learn.adafruit.com/usb-plus-serial-backpack/command-reference)

Dependencies:

[https://github.com/Fazecast/jSerialComm](https://github.com/Fazecast/jSerialComm)

## Configuration
*****
First configure ethminer to open a port. Add the argument below to your startup script.
```
--api-port 3333
```
For example:
```
ethminer.exe -HWMON 1 -RH -G -P stratum+tcp://abcdefghijklmnopqrstuvwxyz1234567890.your_name_here@us1.ethermine.org:4444 --opencl-device 0 --api-port -3333
```
Optionally in linux you can run ethminer headless/nogui, with this gist.

[Headless Script](https://gist.github.com/deathmarine/f29f541318247b9066a00194da08ad2f)


Point EthSerialMonitor to your server by adding lines to you initialization file (config.ini).
```
## Configuration ##

# IPaddress and port of the server to pole
# Example: server={ipaddress}:{port}
server=127.0.01:3333
hwmon=true

# Com Port: COM{port number}
com_port=COM1

##Update delays
delay_sec_show=1000
delay_fsec_show=2000
delay_halfmin_show=2000


# Line configuration:
#    -variables-
# All values truncated left (150.63 = 6 spaces)
# Total size must equal LCD size for line (16 spaces)
# Do not exceed 9 spaces per variable, 
# 10's are counted as 1 and a 0 Character
# avghash.{spaces} = Average Hashrate
# avgtemp.{spaces} = Average Temperature
# avgfan.{spaces} = Average Fan Speed
# shares.{spaces} = Total Current Amount of Shares
# shareavg.{spaces} = Average Shares per Minute
# invalid.{spaces} = Invalid Shares
# runtime.{spaces} = Total Current Runtime in Minutes
# totwatt.{spaces} = Total Wattage Calculated
# gpu{id}.hash{spaces} = Hashrate of specific GPU
# gpu{id}.temp{spaces} = Temperature of specific GPU
# gpu{id}.fan{spaces} = Fan speed of specific GPU
# gpu{id}.watt{spaces} = Wattage of specific GPU
# pool. = Show the pool connected (Single Line)

#Show every second
#Multiple line group accepted (not recommended)
sec_line1=avghash.7 MH/s 
sec_line2=avgtemp.2C avgfan.2% totwatt.5W

#Show every five second
#Alternate Display
#Multiple line groups accepted
fsec_line1=shares.3 Shares 
fsec_line2=invalid.3 Invalid

fsec_line1=shareavg.7 Avg/min
fsec_line2=runtime.2 mins

fsec_line1=pool.
fsec_line2=Mine On!

#Show every 30 seconds 
#Loops through all line groups
#Multiple line groups accepted
halfmin_line1=GPU#1 gpu0.hash5 MH/s
halfmin_line2=gpu0.temp2C gpu0.fan2% gpu0.watt2W

halfmin_line1=GPU#2 gpu1.hash5 MH/s
halfmin_line2=gpu1.temp2C gpu1.fan2% gpu1.watt2W

halfmin_line1=GPU#3 gpu2.hash5 MH/s
halfmin_line2=gpu2.temp2C gpu2.fan2% gpu2.watt2W

halfmin_line1=GPU#4 gpu3.hash5 MH/s
halfmin_line2=gpu3.temp2C gpu3.fan2% gpu3.watt2W

halfmin_line1=GPU#5 gpu4.hash5 MH/s
halfmin_line2=gpu4.temp2C gpu4.fan2% gpu4.watt2W


```
If you do not have a config.ini one will be generated for you on start up.



