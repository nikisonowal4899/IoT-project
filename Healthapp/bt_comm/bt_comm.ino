#define USE_ARDUINO_INTERRUPTS true    // Set-up low-level interrupts for most acurate BPM math.
#include<SoftwareSerial.h>
#include <PulseSensorPlayground.h>
#include<string.h>

// bluetooth module
#define TXD 3
#define RXD 2
SoftwareSerial BTserial(RXD, TXD);
boolean btConnected = false;
void wait4Conection(); // function that checks whether bluetooth is connected or not
const int state = 7;
//

// pulse sensor module
const int PulseWire = A5; // signal in for heart beat sensor
const int LED13 = 13;
int Threshold = 550;
PulseSensorPlayground pulseSensor;  // Creates an instance of the PulseSensorPlayground object called "pulseSensor"
//

// temperature module
const int sensorInput = A0;
double R = 10000, Rt;
double logRt;
double A = 0.001125308852122, B =  0.000235711863267, C =  0.000000085663516; // Steinhart Hart coefficients
double Temperature;
double tempModule(); // returns the temperature in degree celcius
//


void setup() {
  Serial.begin(9600);

  // bluetooth
  pinMode(state, INPUT);
  Serial.println("Arduino is Ready");
  Serial.println("Waiting for HC-05 to connect to Android device");
  //

  // pulse sensor
  pulseSensor.analogInput(PulseWire);   
  pulseSensor.blinkOnPulse(LED13);
  pulseSensor.setThreshold(Threshold); 
  if (pulseSensor.begin()) {
    Serial.println("We created a pulseSensor Object !");
  }
  //

  wait4Conection();
  Serial.println("HC-05 Connected"); 
}

void loop() {
  //check for connection status every time
  if(digitalRead(state) == LOW) { // connection lost
    Serial.println("Connection lost. Wait for connection..");
    wait4Conection();
  }

  //
  int myBPM = pulseSensor.getBeatsPerMinute();
  Serial.print("BPM: ");
  Serial.println(myBPM);
  //
  
  //
  double Temp = tempModule(); 
  String BTsend = "A" + String(Temp) + "Z";
  delay(3000); // send updated data every 3 second interval
  
  for(unsigned int i=0; i<BTsend.length(); i++) {
    BTserial.write(BTsend[i]);
    delay(70);
  }
  BTserial.flush();
  Serial.flush();
}

double tempModule() { // take the temp reading from the sensor
  double Vin = analogRead(sensorInput);
  Vin = (5.0/1023.0) * Vin; // Now signal is in 0-5 V range
  Rt = (R*5.0/Vin) - R; // Rt is the thermistor resistance
  logRt = log(Rt);
  Temperature = 1.0/(A + B*logRt + C*logRt*logRt*logRt); // Steinhart Hart equation
  Temperature = Temperature - 273.15; // Temperature in celcius
  
  Serial.print("Temperature: "); 
  Serial.print(Temperature);
  Serial.println(" °C"); 

  return Temperature;
}

void wait4Conection() {
  while(!btConnected) {
    if(digitalRead(state) == HIGH) btConnected = true;
  }
  BTserial.begin(9600); // Starting Serial communication with the Module
  return ;
}
