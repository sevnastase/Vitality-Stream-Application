# Vitality-Stream-Application
CHINESPORT SPA
Via Croazia, 2
33100 – Udine – IT
www.chinesport.com
Motolife – Praxfit integration – Rev.001 1/14
AR20011
MOTOLIFE
AR20012
MOTOLIFE Evo
Arm and Leg Cycle Ergometer
Leg Cycle Ergometer
Motolife Praxfit integration
Release 10.0.6
Rev. 001 (28/11/2023)
EN
CHINESPORT Spa – ITALIA
MOTOLIFE
Motolife – Remote Display Activation – Rev.1.9.3-special 2/14
EN ...................................................................................................................................................................3
1 GENERAL INFORMATION ............................................................................................................................3
1.1 PURPOSE, CONTENT AND RECIPIENTS OF THESE INSTRUCTIONS.........................................................................................3
1.2 MANUFACTURER......................................................................................................................................................3
2 DESCRIPTION OF SYSTEM............................................................................................................................3
2.1 CONFIGURATION OF THE SYSTEM ................................................................................................................................3
2.2 NETWORK CONFIGURATION FOR ACCESS POINT .........................................................................................................3
2.3 CONFIGURATION OF THE SYSTEM POST SHIPPING...........................................................................................................4
2.4 NETWORK CONFIGURATION FOR MQTT......................................................................................................................9
3 MQTT PROTOCOL .......................................................................................................................................9
3.1 TOPICS ...................................................................................................................................................................9
3.1.1 Chinesport/Motolife/LWT..............................................................................................................................10
3.1.2 Chinesport/Motolife/[serial number] ............................................................................................................10
3.1.2.1 Command type messages (QOS 1) ..............................................................................................................10
3.1.2.1.1 StartLeg.....................................................................................................................................................11
3.1.2.1.2 StartArm...................................................................................................................................................11
3.1.2.1.3 Pause ........................................................................................................................................................11
3.1.2.1.4 Stop...........................................................................................................................................................11
3.1.2.1.5 Resume.....................................................................................................................................................11
3.1.2.1.6 End............................................................................................................................................................12
3.1.2.1.7 Spasm .......................................................................................................................................................12
3.1.2.1.8 JumpX .......................................................................................................................................................12
3.1.2.2 Status type messages (QOS 0).....................................................................................................................12
3.1.2.2.1 Speed........................................................................................................................................................13
3.1.2.2.2 Power .......................................................................................................................................................13
3.1.2.2.3 Mode ........................................................................................................................................................13
3.1.2.2.4 Direction...................................................................................................................................................13
3.1.2.2.5 Time..........................................................................................................................................................13
4 TIMEOUT BEHAVIOUR ..............................................................................................................................13
5 BUG REPORTING.......................................................................................................................................14
CHINESPORT Spa - ITALIA
Motolife – Praxfit integration – Rev.001 3/14
EN
1 GENERAL INFORMATION
This manual is intended only for Chinesport AR20011 and AR20012 products. Please
read carefully.
1.1 Purpose, content and recipients of these instructions
The aim of this manual is to provide the necessary information to connect the Motolife
software to a Praxfit system and describe the communication protocol. It’s needed some
technical skills to proper configure the system the first time.
1.2 Manufacturer
CHINESPORT S.P.A, Via Croazia,2-33100 Udine, Italy
tel.+39 0432 621 621–fax+39 0432 621 620 – website: www.chinesport.com.
The company has a quality system according to the UNI EN ISO 13485:2012.
2 DESCRIPTION OF SYSTEM
To connect the Motolife to a Praxfit software, it’s necessary to configure a system that
allow the reception of network connection from Motolife. The System has the following
key object:
• Motolife AR20011 or AR20012 (supplied from Chinesport)
• Motolife software release version > 1.10.6 (supplied from Chinesport)
• Special WiFi USB dongle (supplied from Chinesport already connected)
• Wi-Fi Access Point 802.11 b/g/n compliant (optional, can be supplied by
Chinesport)
• MQTT broker (inside the Praxfit system or outside, local network)
• Praxfit device with special version (supplied by Praxtour)
2.1 Configuration of the system
The items shipped from Chinesport are already preconfigured. The Wi-Fi dongle is
already connected on the Motolife AR20011 / AR20012 model equipped with the last
software release. Initially the Motolife is configured to connect to a specific WiFi network,
but can be connected to any network.
Motolife is configured with initial IP address 192.168.3.197.
2.2 Network configuration for ACCESS POINT
To enable connection for the Motolife with an access point, there are several options:
1) Receive the AP already configured from Chinesport;
   CHINESPORT Spa - ITALIA
   Motolife – Remote Display Activation – Rev.1.9.3-special 4/14
2) Configure a commercial Wi-Fi access point with these parameters (please refer to the
   access point user manual):
   Band: 2.4GHz
   SSID: “Motolife-Wifi”
   Encryption: WPA2 PSK key: “chinemotolife”
   Network address: 192.168.43.1, DHCP enabled
3) Connect to a customer WiFi network with the described procedure
4) Connect to the ad-hoc network created by the Praxfit device
   2.3 Configuration of the system post shipping
   In case the Motolife has been shipped without Wi-Fi configurations, some more steps
   are needed. Follow this step carefully.
   On the Motolife that we want to
   enable Wi-Fi, when ON:
   Go to SETTING -> SERVICE and
   put the password 1234
   On the SERVICE menu, push the
   EXIT button to exit to OS
    1234 
   Now put the USB key in Motolife USB
   port.
   Then double-tap on My Device
   CHINESPORT Spa - ITALIA
   Motolife – Remote Display Activation – Rev.1.9.3-special 5/14
   On the File explorer, go to FFSDISK by
   double tapping on it
   Find the LM816 folder and double tap
   on
   Double-tap on the netrtwlanu icon
   On the windows that appear, click on
   Folder-up icon
   CHINESPORT Spa - ITALIA
   Motolife – Remote Display Activation – Rev.1.9.3-special 6/14
   Then tap once on the FFSDISK icon
   and then tap on the OK button
   Tap again OK button on the next
   window and wait the end of the
   installation process.
   Now it’s necessary that the Access
   Point is ON.
   After that remove the USB key and
   then plug the Wi-Fi dongle, shipped by
   Chinesport. After several seconds the
   Wi-Fi configuration will popup.
   Search on the list the Motolife-WiFi
   network or the other network that will
   be used and tap Connect
   To show keyboard, tap on keyboard
   button on the right-lower side of the
   monitor and then tap keyboard.
   Put the network key that is
   configurated with the access point
   (default is chinemotolife) and press
   ok.
   CHINESPORT Spa - ITALIA
   Motolife – Remote Display Activation – Rev.1.9.3-special 7/14
   After that disable the checkbox “Notify
   me when new wireless networks are
   available”
   Press OK
   Double tap to my device
   Double tap to Control Panel
   Double tap to Network and Dial-up
   Connections
   CHINESPORT Spa - ITALIA
   Motolife – Remote Display Activation – Rev.1.9.3-special 8/14
   Double tap on NETRTWLANUI
   Now show ahain keyboard if is not
   showed tapping on keyboard icon and
   then keyboard
   Tap on “Specify an IP address”
   Write IP Address: 192.168.43.55
   Subnet Mask: 255.255.255.0
   And Tap OK
   CHINESPORT Spa - ITALIA
   Motolife – Remote Display Activation – Rev.1.9.3-special 9/14
   Now close all windows and go back to
   My Device - > FFSDISK
   Double Tap on SAVE icon and wait the
   quick pop-up disappear.
   Now Motolife can be tourned off, and
   again ON
   2.4 Network Configuration for MQTT
   The PC or laptop that is going to be used to connect with Motolife should be connected
   via Ethernet cable or via Wi-Fi to the Access point previously configured. It should be
   configured with dynamic IP (DHCP) and connected to the network of the Access point.
   Make sure the IP address of the PC is in the interval 192.168.43.X, except the one used
   for Motolife.
   3 MQTT PROTOCOL
   Motolife will publish data with the MQTT protocol. The MQTT Broker should stay in the
   same network of the Motolife and the IP address of the Broker, along with the
   credentials to connect, should be inserted in the Motolife configuration file.
   Motolife is configured as device that publish in specific topics, but does not subscribe
   any.
   3.1 Topics
   Mainly the Topics on which Motolife will publish are 2:
   Chinesport/Motolife/LWT
   Chinesport/Motolife/[serial number]
   CHINESPORT Spa - ITALIA
   Motolife – Remote Display Activation – Rev.1.9.3-special 10/14
   3.1.1 Chinesport/Motolife/LWT
   The first topic is used to communicate the availability of the devices. In these topics all
   the Motolife devices will publish the status. The subscribers of this topics will read all the
   messages of the “connected” Motolife.
   The message that is possible to find in these topics are:
   { "Device":"Motolife", "Model":"AR20012", "Serial Number":"190000133", "Status":"ONLINE",
   "Time":"28/11/2023 13.24.02" }
   Here we can read that the device with serial number 190000133 is ONLINE. Here the
   LWT message usually is retained, but is not important since the Motolife will send a keep
   alive message every 15 seconds, so once the subscriber will subscribe this topic, will
   know the status of the devices.
   3.1.2 Chinesport/Motolife/[serial number]
   In this topic the specific device will publish status or command messages. For the device
   in the first example, the topic will be:
   Chinesport/Motolife/190000133
   The command messages are of this type:
   { "Type":"CommandType", "Value":"CommandValue" }
   Where CommandType can be:
- Command
- Status
  And the CommandValue depending on command type.
  3.1.2.1 Command type messages (QOS 1)
  For the type Command there are these possible values for CommandValue:
- StartLeg
- StartArm
- Stop
- Jump1
- Jump2
- Jump3
- Jump4
- Jump5
- Jump6
- Pause
- Spasm
- Resume
- End
  The command type messages are published with QOS = 1
  CHINESPORT Spa - ITALIA
  Motolife – Remote Display Activation – Rev.1.9.3-special 11/14
  3.1.2.1.1 StartLeg
  The user hit the START LEG button on the interface. The training start for leg exercise
  and the motor start to spin pedals.
  On topic Chinesport/Motolife/[serial number] the published message is:
  { "Type":"Command", "Value":"StartLeg" }
  Here the Praxfit interface should start the last choosen film.
  If in the settings there is an option for “Default film for Leg exercise”, that should be used.
  3.1.2.1.2 StartArm
  The user hit the START ARM button on the interface. The training start for arm exercise
  and the motor start to spin the hand knobs.
  On topic Chinesport/Motolife/[serial number] the published message is:
  { "Type":"Command", "Value":"StartArm" }
  Here the Praxfit interface should start the last choosen film.
  If in the settings there is an option for “Default film for Arm exercise”, that should be
  used.
  3.1.2.1.3 Pause
  The user hit the STOP button on the user interface. The training goes to Pause and ask
  the user if want to terminate or resume training.
  On topic Chinesport/Motolife/[serial number] the published message is:
  { "Type":"Command", "Value":"Pause" }
  Here the Praxfit interface should pause without exit.
  3.1.2.1.4 Stop
  The user hit the STOP button on the user interface. The training goes to Pause and ask
  the user if want to terminate or resume training. If the user hit Terminate, the stop
  command is issued.
  On topic Chinesport/Motolife/[serial number] the published message is:
  { "Type":"Command", "Value":"Stop" }
  Here the Praxfit interface should stop the film and go to home.
  3.1.2.1.5 Resume
  The user hit the STOP button on the user interface. The training goes to Pause and ask
  the user if want to terminate or resume training. If the user hit Resume, the resume
  command is issued.
  On topic Chinesport/Motolife/[serial number] the published message is:
  { "Type":"Command", "Value":"Resume" }
  CHINESPORT Spa - ITALIA
  Motolife – Remote Display Activation – Rev.1.9.3-special 12/14
  Here the Praxfit interface should resume the previously paused film, it the rotation
  direction is forward (see Status / Direction).
  3.1.2.1.6 End
  When the training comes to the end, the End command is issued.
  On topic Chinesport/Motolife/[serial number] the published message is:
  { "Type":"Command", "Value":"End" }
  Here the Praxfit interface should stop the film and go to home. Maybe if there is a
  “congratulation” screen on the end of the full film, it can be showed here.
  3.1.2.1.7 Spasm
  When the user has a spasm, the Motolife detects it and then, depending on settings:
- Pause training with a warning message
- Invert motor rotation with the aim to resolve the spasm
  On topic Chinesport/Motolife/[serial number] the published message is:
  { "Type":"Command", "Value":"Spasm" }
  Here the Praxfit interface should pause without exit, ad wait the user interaction or the
  automatic resolving that could be:
- Resume training if the user hit resume in the interface (see Resume)
- Stop training if the user hit terminate in the interface (see Stop)
- Restart rotation in the opposite direction (see Resume)
  3.1.2.1.8 JumpX
  The user hit one of the numbered button on the Speed game / Praxfit screen with the
  aim of Jump to a specific segment of the film. The training does not show differences
  because the buttons are specific to command the film. The X number is the button that
  the user has pressed.
  On topic Chinesport/Motolife/[serial number] the published message is:
  { "Type":"Command", "Value":"JumpX" }
  X: 1 to 6
  Here the Praxfit interface should act in the same way when a user hit the number in the
  user interface.
  3.1.2.2 Status type messages (QOS 0)
  The status messages are sent each second when the Motolife is in training mode. The
  status messages are sent with QOS = 0 because is not important the priority or the
  certainty of the message, because the next second will be sent another message.
  The value payload is the content of the status and could have more or less information:
  CHINESPORT Spa - ITALIA
  Motolife – Remote Display Activation – Rev.1.9.3-special 13/14
  On topic Chinesport/Motolife/[serial number] the published message is:
  { "Type":"Status", "Value": {
  "Speed":"xx",
  "Power":"xx",
  "Mode":"Leg",
  "Direction":"Forward",
  "Time":"mm:ss / mm:ss"
  } }
  3.1.2.2.1 Speed
  Speed is the actual speed measured at pedal, expressed in RPM. This is the most
  mandatory value because is the one that gives the cadence of the film.
  3.1.2.2.2 Power
  Power is the actual pedalling power, expressed in W. This value could be used or not,
  depending on what information are shown on the Praxfit interface.
  3.1.2.2.3 Mode
  The mode value report if the training is going in arm mode or leg mode. The mode value
  can be:
- Leg
- Arm
  3.1.2.2.4 Direction
  The direction value report if the motor is spinning pedals forward or backward. The
  direction value can be:
- Forward
- Backward
  For the purpose of this application the Praxfit should stay at 0 RPM when the motor spin
  backward.
  3.1.2.2.5 Time
  The Time value report the elapsed time / remaining time of the training, in form of
  mm:ss. This value could be used or not, depending on what information are shown on
  the Praxfit interface
  4 TIMEOUT BEHAVIOUR
  When the Praxfit does not receive data for Motolife in a point where is supposed to
  receive, the video should pause. Due to the MQTT protocol features, there should be a
  LWT message that is automatically published from the MQTT broker when the
  connection is lost. This mean that in the topics Chinesport/Motolife/LWT is possible to
  check the status of the Motolife. After a Timeout of 60 seconds, the OFFLINE status
  message is automatically published. Here the praxfit can exit and return to Home.
  CHINESPORT Spa - ITALIA
  Motolife – Remote Display Activation – Rev.1.9.3-special 14/14
  5 BUG REPORTING
  If you encounter difficulties or some malfunctioning, please write to the following e-mail
  address:
  massimiliano.donno@chinesport.it
  Take note of the problem encountered, the sequence of operations that bring at that
  problem and eventually all messages shown on display. If possible, also take some
  pictures of the display.