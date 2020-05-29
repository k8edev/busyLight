#include <SoftwareSerial.h>

#define Rx 0
#define Tx 1

char blueToothVal;           //value sent over via bluetooth
char lastValue;              //stores last state of device (on/off)

SoftwareSerial Bluetooth(Rx, Tx); //Rx, Tx

void setup()
{
  //  Serial.begin(9600);
  pinMode(Tx, OUTPUT);                                                    // Configure Tx as OUTPUT (Transmitter)
  pinMode(Rx, INPUT);
  delay(1000);
  Bluetooth.begin(9600);
  delay(1000);
  Serial.print("Bluetooth ready");
  Bluetooth.flush();
  pinMode(13, OUTPUT);
}


void loop()
{
  if (Serial.available())
  { //if there is data being recieved
    blueToothVal = Serial.read(); //read it
  }

  if (Bluetooth.available()) {                                            // Wait for data recieved from Bluetooth device
    blueToothVal = Bluetooth.read();                                        // Put recieved data in memory
    Serial.print("Data recieved from Bluetooth device: ");
    Serial.print(blueToothVal);
  }

  if (blueToothVal == 'n')
  { //if value from bluetooth serial is n
    digitalWrite(13, HIGH);           //switch on LED
    if (lastValue != 'n')
      Serial.println(F("LED is on")); //print LED is on
    lastValue = blueToothVal;
  }
  else if (blueToothVal == 'f')
  { //if value from bluetooth serial is n
    digitalWrite(13, LOW);            //turn off LED
    if (lastValue != 'f')
      Serial.println(F("LED is off")); //print LED is on
    lastValue = blueToothVal;
  }
  else {
    Serial.println("blueToothVal was: ");
    Serial.println(blueToothVal);
  }
  delay(1000);
}
