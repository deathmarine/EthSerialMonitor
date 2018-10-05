# EthSerialMonitor
A eth api socket to serial interface for a 16x2 LCD Display

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


Point EthSerialMonitor to your server by adding lines to you initialization file (config.ini). //TODO

If you do not have a config.ini one will be generated for you on start up. //TODO



